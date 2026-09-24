package com.frost.envoys.lua;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.model.ActionTrade;
import com.frost.envoys.config.Config;
import com.frost.envoys.gui.menu.NPCMerchantMenu;
import com.frost.envoys.network.payload.OpenDialogPayload;
import com.frost.envoys.npc.NPCTrade;
import com.frost.envoys.npc.entity.BaseNPC;
import com.frost.envoys.npc.merchant.NPCMerchant;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.function.LuaClosure;
import org.squiddev.cobalt.function.LuaFunction;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class LuaNpcEngine {

    private final UUID npcUUID;

    private LuaState lua;
    private LuaSandbox.InstructionBudget budget;
    private LuaThread mainThread;
    private EnvoysLuaApi api;

    private final EnumMap<EventType, Callback> callbacks = new EnumMap<>(EventType.class);
    private final EnumMap<EventType, Script> runningCallbacks = new EnumMap<>(EventType.class);
    private final LuaEventQueue queue = new LuaEventQueue();
    private final List<Script> active = new ArrayList<>();
    private final Set<UUID> rangePlayers = new HashSet<>();

    private Script current;
    private LuaPlayerProxy currentTarget;
    private BaseNPC npc;

    private boolean started;
    private boolean errored;
    private String errorText = "";
    private int updateCounter;

    public LuaNpcEngine(UUID npcUUID) {
        this.npcUUID = npcUUID;
    }

    public UUID npcUUID() {
        return npcUUID;
    }

    public boolean isErrored() {
        return errored;
    }

    public String errorText() {
        return errorText;
    }

    public boolean isShutdown() {
        return lua == null;
    }

    public boolean load(String source) {
        try {
            LuaSandbox.InstructionBudget budget = new LuaSandbox.InstructionBudget();
            LuaState lua = LuaSandbox.createState(budget);
            LuaSandbox.installStandardGlobals(lua);
            this.api = new EnvoysLuaApi(this);
            this.api.install(lua);
            LuaClosure chunk = LuaSandbox.compile(lua, source);

            this.lua = lua;
            this.budget = budget;
            this.mainThread = new LuaThread(lua, chunk);
            this.errored = false;
            this.started = false;
            return true;
        } catch (CompileException e) {
            Envoys.LOGGER.error("[Envoys] Lua compile error for NPC {}: {}", npcUUID, e.getMessage());
            return false;
        } catch (LuaError | RuntimeException e) {
            Envoys.LOGGER.error("[Envoys] Lua setup error for NPC {}", npcUUID, e);
            return false;
        }
    }

    public void tick(BaseNPC npc) {
        if (lua == null || errored) {
            return;
        }

        this.npc = npc;

        if (!started) {
            started = true;
            Script script = new Script(mainThread, null, null);
            active.add(script);
            resume(script, Constants.NONE);
            if (errored) {
                return;
            }
        }

        tickRange();
        enqueueUpdate();
        drainQueue();
        advancePending();
    }

    public void dispatchEvent(EventType type, Player player) {
        if (lua == null || errored || type == null) {
            return;
        }
        LuaPlayerProxy proxy = null;
        if (player instanceof ServerPlayer serverPlayer) {
            proxy = new LuaPlayerProxy(serverPlayer.getUUID(), serverPlayer.serverLevel());
        }
        queue.push(new LuaEvent(type, proxy));
    }

    public void shutdown() {
        if (npc != null) {
            npc.getNavigation().stop();
            npc.endScriptedMovement();
        }
        active.clear();
        runningCallbacks.clear();
        queue.clear();
        callbacks.clear();
        rangePlayers.clear();
        current = null;
        currentTarget = null;
        npc = null;
        mainThread = null;
        lua = null;
        budget = null;
        api = null;
        started = false;
        updateCounter = 0;
    }

    private int instructionLimit() {
        return Config.LUA_INSTRUCTION_LIMIT.get();
    }

    private void resume(Script script, Varargs args) {
        if (lua == null || errored) {
            return;
        }

        script.pending = null;
        budget.arm(instructionLimit());
        current = script;
        currentTarget = script.target;

        try {
            LuaThread.run(script.thread, args);

            if (script.thread.getStatus() == LuaThread.Status.DEAD) {
                active.remove(script);
                if (script.callbackType != null) {
                    runningCallbacks.remove(script.callbackType, script);
                }
            } else if (script.pending == null) {
                if (budget.wasExhausted()) {
                    errorState("too long without yielding");
                } else {
                    errorState("script yielded without a pending action");
                }
            }
        } catch (LuaError e) {
            errorState(e.getMessage() != null ? e.getMessage() : e.toString());
        } catch (RuntimeException e) {
            errorState(e.toString());
        } finally {
            current = null;
            currentTarget = null;
        }
    }

    private void errorState(String message) {
        if (errored) {
            return;
        }
        errored = true;
        errorText = message == null ? "" : message;
        Envoys.LOGGER.error("[Envoys] Lua runtime error for NPC {}: {}", npcUUID, errorText);

        active.clear();
        runningCallbacks.clear();
        queue.clear();

        if (npc != null) {
            npc.getNavigation().stop();
            npc.endScriptedMovement();
        }
    }

    private void tickRange() {
        Callback callback = callbacks.get(EventType.RANGE);
        if (callback == null) {
            rangePlayers.clear();
            return;
        }
        if (!(npc.level() instanceof ServerLevel level)) {
            return;
        }

        double radiusSqr = callback.radius() * callback.radius();
        Set<UUID> inside = new HashSet<>();
        for (Player player : level.players()) {
            if (player.distanceToSqr(npc) <= radiusSqr) {
                UUID id = player.getUUID();
                inside.add(id);
                if (!rangePlayers.contains(id)) {
                    queue.push(new LuaEvent(EventType.RANGE, new LuaPlayerProxy(id, level)));
                }
            }
        }
        rangePlayers.clear();
        rangePlayers.addAll(inside);
    }

    private void enqueueUpdate() {
        Callback callback = callbacks.get(EventType.UPDATE);
        if (callback == null) {
            return;
        }
        if (++updateCounter >= callback.interval()) {
            updateCounter = 0;
            queue.push(new LuaEvent(EventType.UPDATE, null));
        }
    }

    private void drainQueue() {
        for (LuaEvent event : queue.drain()) {
            if (errored) {
                return;
            }

            Callback callback = callbacks.get(event.type);
            if (callback != null && !runningCallbacks.containsKey(event.type)) {
                invokeCallback(event.type, callback, event);
            }

            for (Script script : new ArrayList<>(active)) {
                PendingAction pending = script.pending;
                if (pending != null
                        && pending.kind == PendingAction.Kind.WAIT_EVENT
                        && !pending.eventReady
                        && pending.eventName.equals(event.type.jsonKey())) {
                    pending.eventReady = true;
                    pending.eventArgs = event.player != null ? event.player : Constants.NONE;
                    if (event.player instanceof LuaPlayerProxy proxy) {
                        script.target = proxy;
                    }
                }
            }
        }
    }

    private void invokeCallback(EventType type, Callback callback, LuaEvent event) {
        LuaThread thread = new LuaThread(lua, callback.function());
        LuaPlayerProxy target = event.player instanceof LuaPlayerProxy proxy ? proxy : null;
        Script script = new Script(thread, target, type);
        active.add(script);
        runningCallbacks.put(type, script);
        resume(script, event.player != null ? event.player : Constants.NONE);
    }

    private void advancePending() {
        for (Script script : new ArrayList<>(active)) {
            if (errored) {
                return;
            }
            PendingAction pending = script.pending;
            if (pending == null) {
                continue;
            }

            switch (pending.kind) {
                case WAIT_TICKS -> {
                    if (pending.ticks-- <= 0) {
                        resume(script, Constants.NONE);
                    }
                }
                case MOVE -> {
                    if (tickMove(pending)) {
                        resume(script, pending.moveArrived ? Constants.TRUE : Constants.FALSE);
                    }
                }
                case WAIT_EVENT -> {
                    if (pending.eventReady) {
                        resume(script, pending.eventArgs);
                    } else if (pending.hasTimeout && pending.timeoutRemaining-- <= 0) {
                        resume(script, Constants.NONE);
                    }
                }
                case DIALOG, TRADE -> {
                }
            }
        }
    }

    private boolean tickMove(PendingAction pending) {
        if (npc == null) {
            pending.moveArrived = false;
            return true;
        }

        double dx = pending.x - npc.getX();
        double dy = pending.y - npc.getY();
        double dz = pending.z - npc.getZ();
        double horizontal = dx * dx + dz * dz;

        if (horizontal <= 2.25D && Math.abs(dy) <= 2.5D) {
            stopMovement();
            pending.moveArrived = true;
            return true;
        }

        if (--pending.stuckCheckTimer <= 0) {
            pending.stuckCheckTimer = 20;

            double movedX = npc.getX() - pending.lastX;
            double movedZ = npc.getZ() - pending.lastZ;
            if (movedX * movedX + movedZ * movedZ < 0.0225D) {
                if (++pending.stuckSeconds >= 3) {
                    stopMovement();
                    pending.moveArrived = false;
                    return true;
                }
            } else {
                pending.stuckSeconds = 0;
            }

            pending.lastX = npc.getX();
            pending.lastZ = npc.getZ();
        }

        if (--pending.repathCooldown <= 0 || npc.getNavigation().isDone()) {
            pending.repathCooldown = 15;
            npc.getNavigation().moveTo(pending.x, pending.y, pending.z, pending.speed);
        }
        return false;
    }

    private void stopMovement() {
        if (npc != null) {
            npc.getNavigation().stop();
            npc.endScriptedMovement();
        }
    }

    private void setPending(LuaState state, PendingAction pending) throws LuaError {
        Script script = current;
        if (script == null || state.getCurrentThread() != script.thread) {
            throw new LuaError("suspending envoys call used outside the NPC script coroutine");
        }
        script.pending = pending;
    }

    void beginWaitTicks(LuaState state, int ticks) throws LuaError {
        setPending(state, PendingAction.waitTicks(ticks));
    }

    boolean beginMove(LuaState state, double x, double y, double z, double speed) throws LuaError {
        if (npc == null) {
            return false;
        }

        double actualSpeed = speed <= 0 ? 1.0D : speed;
        npc.beginScriptedMovement();
        npc.getNavigation().stop();

        boolean startedMove = npc.getNavigation().moveTo(x, y, z, actualSpeed);
        if (!startedMove) {
            npc.endScriptedMovement();
            return false;
        }

        setPending(state, PendingAction.move(x, y, z, actualSpeed, npc.getX(), npc.getZ()));
        return true;
    }

    void beginWaitEvent(LuaState state, String eventName, int timeout) throws LuaError {
        setPending(state, PendingAction.waitEvent(eventName, timeout));
    }

    void beginDialogue(LuaState state, String message, LinkedHashMap<String, String> answers, LuaPlayerProxy player) throws LuaError {
        ServerPlayer serverPlayer = player.resolve();
        PacketDistributor.sendToPlayer(serverPlayer, new OpenDialogPayload(
                npcUUID,
                "lua",
                npcDisplayName(),
                Component.literal(message == null ? "" : message),
                answers
        ));
        setPending(state, PendingAction.dialog(serverPlayer.getUUID()));
        Script script = current;
        if (script != null) {
            script.target = player;
        }
    }

    public void onDialogAnswer(UUID playerUuid, String choice) {
        if (lua == null || errored || playerUuid == null) {
            return;
        }
        for (Script script : new ArrayList<>(active)) {
            PendingAction pending = script.pending;
            if (pending != null
                    && pending.kind == PendingAction.Kind.DIALOG
                    && playerUuid.equals(pending.dialogPlayer)) {
                resume(script, (choice == null || choice.isEmpty()) ? Constants.NIL : LuaStrings.toLua(choice));
                return;
            }
        }
    }

    boolean beginTrade(LuaState state, List<NPCTrade> trades, LuaPlayerProxy player) throws LuaError {
        if (npc == null) {
            return false;
        }

        ServerPlayer serverPlayer = player.resolve();
        ActionTrade action = new ActionTrade("lua");
        action.trades.addAll(trades);

        Script script = current;
        NPCMerchant merchant = new NPCMerchant(npc, action, serverPlayer);
        merchant.openTradingScreen(serverPlayer, Component.literal("Торговля"), 0, () -> onTradeClosed(script));

        if (!(serverPlayer.containerMenu instanceof NPCMerchantMenu menu) || menu.getMerchant() != merchant) {
            return false;
        }

        setPending(state, PendingAction.trade());
        if (script != null) {
            script.target = player;
        }
        return true;
    }

    private void onTradeClosed(Script script) {
        if (lua == null || errored || script == null) {
            return;
        }
        PendingAction pending = script.pending;
        if (pending == null || pending.kind != PendingAction.Kind.TRADE) {
            return;
        }
        resume(script, Constants.NONE);
    }

    LuaPlayerProxy currentTargetProxy() {
        return currentTarget;
    }

    double npcYaw() {
        return npc == null ? 0.0D : npc.getYRot();
    }

    void playSound(String id, float volume, float pitch) throws LuaError {
        if (npc == null) {
            return;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.SOUND_EVENT.containsKey(key)) {
            throw new LuaError("unknown sound: " + id);
        }
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(key);
        npc.level().playSound(null, npc.getX(), npc.getY(), npc.getZ(), event, SoundSource.NEUTRAL, volume, pitch);
    }

    void spawnParticle(String id, int count, double speed, double dx, double dy, double dz) throws LuaError {
        if (npc == null) {
            return;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.PARTICLE_TYPE.containsKey(key)) {
            throw new LuaError("unknown particle: " + id);
        }
        if (!(BuiltInRegistries.PARTICLE_TYPE.get(key) instanceof ParticleOptions options)) {
            throw new LuaError("particle requires options: " + id);
        }
        if (npc.level() instanceof ServerLevel level) {
            level.sendParticles(options, npc.getX(), npc.getY(), npc.getZ(), count, dx, dy, dz, speed);
        }
    }

    void lookAt(LuaValue target) throws LuaError {
        if (npc == null) {
            return;
        }

        double x;
        double y;
        double z;
        if (target instanceof LuaPlayerProxy proxy) {
            ServerPlayer player = proxy.resolve();
            x = player.getX();
            y = player.getEyeY();
            z = player.getZ();
        } else if (target instanceof LuaTable table) {
            x = table.rawget("x").checkDouble();
            y = table.rawget("y").checkDouble();
            z = table.rawget("z").checkDouble();
        } else {
            throw new LuaError("lookAt expects a player or a position table");
        }

        double dx = x - npc.getX();
        double dy = y - npc.getEyeY();
        double dz = z - npc.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, horizontal) * (180.0D / Math.PI)));

        npc.setYRot(yaw);
        npc.yBodyRot = yaw;
        npc.yHeadRot = yaw;
        npc.setXRot(pitch);
    }

    void teleport(double x, double y, double z, float yaw, float pitch) {
        if (npc == null) {
            return;
        }
        npc.getNavigation().stop();
        npc.endScriptedMovement();
        if (npc.level() instanceof ServerLevel level) {
            npc.teleportTo(level, x, y, z, Set.<RelativeMovement>of(), yaw, pitch);
        }
    }

    void applyEffect(String id, int duration, int amplifier, boolean showParticles) throws LuaError {
        if (npc == null) {
            return;
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            throw new LuaError("unknown effect: " + id);
        }
        Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(key);
        if (holder.isEmpty()) {
            throw new LuaError("unknown effect: " + id);
        }
        npc.addEffect(new MobEffectInstance(holder.get(), duration, amplifier, false, showParticles));
    }

    String npcDisplayName() {
        if (npc == null) {
            return "NPC";
        }
        Component custom = npc.getCustomName();
        return custom != null ? custom.getString() : npc.getName().getString();
    }

    void registerCallback(EventType type, LuaFunction function, int extra) {
        int interval = Math.max(1, extra <= 0 ? 1 : extra);
        double radius = extra > 0 ? extra : 4.0D;
        callbacks.put(type, new Callback(function, interval, radius));
        if (type == EventType.RANGE) {
            rangePlayers.clear();
        }
    }

    void sendMessageToTarget(String message) {
        LuaPlayerProxy target = currentTarget;
        if (target == null) {
            return;
        }
        try {
            target.resolve().sendSystemMessage(Component.literal(message));
        } catch (LuaError ignored) {
        }
    }

    void runCommand(String command) {
        if (!Config.LUA_ALLOW_COMMANDS.get()) {
            return;
        }
        if (npc == null) {
            return;
        }
        MinecraftServer server = npc.getServer();
        if (server == null) {
            return;
        }

        String formatted = command == null ? "" : command.trim();
        if (formatted.startsWith("/")) {
            formatted = formatted.substring(1);
        }
        if (formatted.isEmpty()) {
            return;
        }

        CommandSourceStack source = npc.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, formatted);
    }

    LuaValue posTable() {
        LuaTable table = new LuaTable();
        if (npc != null) {
            table.rawset("x", ValueFactory.valueOf(npc.getX()));
            table.rawset("y", ValueFactory.valueOf(npc.getY()));
            table.rawset("z", ValueFactory.valueOf(npc.getZ()));
        }
        return table;
    }

    LuaValue npcName() {
        if (npc == null) {
            return Constants.EMPTYSTRING;
        }
        return LuaStrings.toLua(npcDisplayName());
    }

    LuaValue playersInRangeTable(int radius) {
        LuaTable table = new LuaTable();
        if (npc != null && npc.level() instanceof ServerLevel level) {
            double radiusSqr = (double) radius * radius;
            int index = 1;
            for (Player player : level.players()) {
                if (player.distanceToSqr(npc) <= radiusSqr) {
                    table.rawset(index++, new LuaPlayerProxy(player.getUUID(), level));
                }
            }
        }
        return table;
    }

    private record Callback(LuaFunction function, int interval, double radius) {
    }

    private static final class Script {
        private final LuaThread thread;
        private LuaPlayerProxy target;
        private final EventType callbackType;
        private PendingAction pending;

        private Script(LuaThread thread, LuaPlayerProxy target, EventType callbackType) {
            this.thread = thread;
            this.target = target;
            this.callbackType = callbackType;
        }
    }

    private static final class PendingAction {
        private enum Kind {
            WAIT_TICKS,
            MOVE,
            WAIT_EVENT,
            DIALOG,
            TRADE
        }

        private UUID dialogPlayer;

        private final Kind kind;

        private int ticks;

        private double x;
        private double y;
        private double z;
        private double speed;
        private boolean moveArrived;
        private int repathCooldown;
        private int stuckCheckTimer;
        private int stuckSeconds;
        private double lastX;
        private double lastZ;

        private String eventName = "";
        private int timeoutRemaining;
        private boolean hasTimeout;
        private boolean eventReady;
        private Varargs eventArgs = Constants.NONE;

        private PendingAction(Kind kind) {
            this.kind = kind;
        }

        private static PendingAction waitTicks(int ticks) {
            PendingAction pending = new PendingAction(Kind.WAIT_TICKS);
            pending.ticks = Math.max(0, ticks);
            return pending;
        }

        private static PendingAction move(double x, double y, double z, double speed, double lastX, double lastZ) {
            PendingAction pending = new PendingAction(Kind.MOVE);
            pending.x = x;
            pending.y = y;
            pending.z = z;
            pending.speed = speed;
            pending.lastX = lastX;
            pending.lastZ = lastZ;
            pending.stuckCheckTimer = 30;
            pending.repathCooldown = 0;
            return pending;
        }

        private static PendingAction waitEvent(String eventName, int timeout) {
            PendingAction pending = new PendingAction(Kind.WAIT_EVENT);
            pending.eventName = eventName;
            pending.hasTimeout = timeout > 0;
            pending.timeoutRemaining = timeout;
            return pending;
        }

        private static PendingAction dialog(UUID player) {
            PendingAction pending = new PendingAction(Kind.DIALOG);
            pending.dialogPlayer = player;
            return pending;
        }

        private static PendingAction trade() {
            return new PendingAction(Kind.TRADE);
        }
    }
}
