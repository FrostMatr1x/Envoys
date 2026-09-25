package com.frost.envoys.lua;

import com.frost.envoys.action.MerchantSlotData;
import com.frost.envoys.action.NPCInteractManager;
import com.frost.envoys.action.PlayerCheckpointData;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.npc.NPCTrade;
import com.frost.envoys.quest.PlayerQuestManager;
import com.frost.envoys.quest.QuestDefinition;
import com.frost.envoys.quest.QuestInventoryUtil;
import com.frost.envoys.quest.QuestResolver;
import com.frost.envoys.quest.QuestType;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.function.RegisteredFunction;
import org.squiddev.cobalt.function.ResumableVarArgFunction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public final class EnvoysLuaApi {

    private final LuaNpcEngine engine;

    public EnvoysLuaApi(LuaNpcEngine engine) {
        this.engine = engine;
    }

    public void install(LuaState state) {
        LuaTable envoys = new LuaTable();
        RegisteredFunction.bind(envoys, new RegisteredFunction[]{
                RegisteredFunction.ofV("say", this::say),
                RegisteredFunction.ofV("command", this::command),
                RegisteredFunction.ofV("on", this::on),
                RegisteredFunction.ofV("pos", this::pos),
                RegisteredFunction.ofV("name", this::name),
                RegisteredFunction.ofV("playersInRange", this::playersInRange),
                RegisteredFunction.ofV("sound", this::sound),
                RegisteredFunction.ofV("particle", this::particle),
                RegisteredFunction.ofV("lookAt", this::lookAt),
                RegisteredFunction.ofV("teleport", this::teleport),
                RegisteredFunction.ofV("effect", this::effect),
                RegisteredFunction.ofFactory("wait", () -> new WaitFunction(engine)),
                RegisteredFunction.ofFactory("move", () -> new MoveFunction(engine)),
                RegisteredFunction.ofFactory("waitEvent", () -> new WaitEventFunction(engine)),
                RegisteredFunction.ofFactory("dialogue", () -> new DialogueFunction(engine)),
                RegisteredFunction.ofFactory("trade", () -> new TradeFunction(engine)),
        });

        LuaTable quest = new LuaTable();
        RegisteredFunction.bind(quest, new RegisteredFunction[]{
                RegisteredFunction.ofV("status", this::questStatus),
                RegisteredFunction.ofV("stage", this::questStage),
                RegisteredFunction.ofV("progress", this::questProgress),
                RegisteredFunction.ofV("start", this::questStart),
                RegisteredFunction.ofV("advance", this::questAdvance),
                RegisteredFunction.ofV("complete", this::questComplete),
                RegisteredFunction.ofV("reset", this::questReset),
        });
        envoys.rawset("quest", quest);

        LuaTable merchant = new LuaTable();
        RegisteredFunction.bind(merchant, new RegisteredFunction[]{
                RegisteredFunction.ofV("getUnlocked", this::merchantGetUnlocked),
                RegisteredFunction.ofV("addUnlocked", this::merchantAddUnlocked),
        });
        envoys.rawset("merchant", merchant);

        LuaTable checkpoint = new LuaTable();
        RegisteredFunction.bind(checkpoint, new RegisteredFunction[]{
                RegisteredFunction.ofV("get", this::checkpointGet),
                RegisteredFunction.ofV("set", this::checkpointSet),
        });
        envoys.rawset("checkpoint", checkpoint);

        state.globals().rawset("envoys", envoys);
    }

    private Varargs say(LuaState state, Varargs args) throws LuaError {
        engine.sendMessageToTarget(LuaStrings.toJava(args.arg(1)));
        return Constants.NONE;
    }

    private Varargs command(LuaState state, Varargs args) throws LuaError {
        engine.runCommand(LuaStrings.toJava(args.arg(1)));
        return Constants.NONE;
    }

    private Varargs on(LuaState state, Varargs args) throws LuaError {
        String eventName = LuaStrings.toJava(args.arg(1));
        LuaFunction callback = args.arg(2).checkFunction();
        int extra = args.arg(3).optInteger(0);

        EventType type = EventType.fromKey(eventName);
        if (type == null) {
            throw new LuaError("unknown event '" + eventName + "'");
        }

        engine.registerCallback(type, callback, extra);
        return Constants.NONE;
    }

    private Varargs pos(LuaState state, Varargs args) {
        return engine.posTable();
    }

    private Varargs name(LuaState state, Varargs args) {
        return engine.npcName();
    }

    private Varargs playersInRange(LuaState state, Varargs args) throws LuaError {
        int radius = Math.max(0, args.arg(1).optInteger(4));
        return engine.playersInRangeTable(radius);
    }

    private Varargs sound(LuaState state, Varargs args) throws LuaError {
        String id = LuaStrings.toJava(args.arg(1));
        float volume = (float) args.arg(2).optDouble(1.0D);
        float pitch = (float) args.arg(3).optDouble(1.0D);
        engine.playSound(id, volume, pitch);
        return Constants.NONE;
    }

    private Varargs particle(LuaState state, Varargs args) throws LuaError {
        String id = LuaStrings.toJava(args.arg(1));
        int count = Math.max(0, args.arg(2).optInteger(1));
        double speed = args.arg(3).optDouble(0.0D);
        double dx = args.arg(4).optDouble(0.0D);
        double dy = args.arg(5).optDouble(0.0D);
        double dz = args.arg(6).optDouble(0.0D);
        engine.spawnParticle(id, count, speed, dx, dy, dz);
        return Constants.NONE;
    }

    private Varargs lookAt(LuaState state, Varargs args) throws LuaError {
        engine.lookAt(args.arg(1));
        return Constants.NONE;
    }

    private Varargs teleport(LuaState state, Varargs args) throws LuaError {
        double x = args.arg(1).checkDouble();
        double y = args.arg(2).checkDouble();
        double z = args.arg(3).checkDouble();
        float yaw = (float) args.arg(4).optDouble(engine.npcYaw());
        float pitch = (float) args.arg(5).optDouble(0.0D);
        engine.teleport(x, y, z, yaw, pitch);
        return Constants.NONE;
    }

    private Varargs effect(LuaState state, Varargs args) throws LuaError {
        String id = LuaStrings.toJava(args.arg(1));
        int duration = Math.max(1, args.arg(2).optInteger(20));
        int amplifier = Math.max(0, args.arg(3).optInteger(0));
        boolean showParticles = args.arg(4).optBoolean(true);
        engine.applyEffect(id, duration, amplifier, showParticles);
        return Constants.NONE;
    }

    private Varargs merchantGetUnlocked(LuaState state, Varargs args) throws LuaError {
        ServerPlayer player = resolvePlayer(engine, args, 1).resolve();
        return ValueFactory.valueOf(MerchantSlotData.getUnlocked(player, engine.npcUUID().toString()));
    }

    private Varargs merchantAddUnlocked(LuaState state, Varargs args) throws LuaError {
        int amount = args.arg(1).isNil() ? 1 : args.arg(1).checkInteger();
        if (amount <= 0) {
            throw new LuaError("amount must be greater than 0");
        }
        ServerPlayer player = resolvePlayer(engine, args, 2).resolve();
        MerchantSlotData.addUnlocked(player, engine.npcUUID().toString(), amount);
        return Constants.NONE;
    }

    private Varargs checkpointGet(LuaState state, Varargs args) throws LuaError {
        String key = validateCheckpointKey(LuaStrings.toJava(args.arg(1)));
        ServerPlayer player = resolvePlayer(engine, args, 2).resolve();
        String value = PlayerCheckpointData.getCheckpoint(player, engine.npcUUID().toString(), key);
        return value == null ? Constants.NIL : LuaStrings.toLua(value);
    }

    private Varargs checkpointSet(LuaState state, Varargs args) throws LuaError {
        String key = validateCheckpointKey(LuaStrings.toJava(args.arg(1)));
        String value = LuaStrings.toJava(args.arg(2));
        if (value.isEmpty() || value.length() > 256) {
            throw new LuaError("checkpoint value must be 1..256 characters");
        }
        ServerPlayer player = resolvePlayer(engine, args, 3).resolve();
        PlayerCheckpointData.setCheckpoint(player, engine.npcUUID().toString(), key, value);
        return Constants.NONE;
    }

    private static String validateCheckpointKey(String key) throws LuaError {
        if (key == null || !key.matches("[a-zA-Z0-9_]{1,64}")) {
            throw new LuaError("checkpoint key must match [a-zA-Z0-9_]{1,64}");
        }
        return key;
    }

    private Varargs questStatus(LuaState state, Varargs args) throws LuaError {
        QuestDefinition quest = resolveQuest(LuaStrings.toJava(args.arg(1)));
        ServerPlayer player = resolveQuestPlayer(args, 2);
        return LuaStrings.toLua(PlayerQuestManager.status(player, quest.questUuid).name());
    }

    private Varargs questStage(LuaState state, Varargs args) throws LuaError {
        QuestDefinition quest = resolveQuest(LuaStrings.toJava(args.arg(1)));
        ServerPlayer player = resolveQuestPlayer(args, 2);
        return ValueFactory.valueOf(questCurrent(player, quest));
    }

    private Varargs questProgress(LuaState state, Varargs args) throws LuaError {
        QuestDefinition quest = resolveQuest(LuaStrings.toJava(args.arg(1)));
        ServerPlayer player = resolveQuestPlayer(args, 2);
        QuestType type = quest.type == null ? QuestType.BOOLEAN : quest.type;

        LuaTable table = new LuaTable();
        table.rawset("status", LuaStrings.toLua(PlayerQuestManager.status(player, quest.questUuid).name()));
        table.rawset("current", ValueFactory.valueOf(questCurrent(player, quest)));
        table.rawset("target", ValueFactory.valueOf(questTarget(quest)));
        table.rawset("type", LuaStrings.toLua(type.name()));

        if (type == QuestType.BOOLEAN) {
            LuaTable steps = new LuaTable();
            int index = 1;
            for (String step : PlayerQuestManager.completedSteps(player, quest.questUuid)) {
                steps.rawset(index++, LuaStrings.toLua(step));
            }
            table.rawset("steps", steps);
        }
        return table;
    }

    private Varargs questStart(LuaState state, Varargs args) throws LuaError {
        QuestDefinition quest = resolveQuest(LuaStrings.toJava(args.arg(1)));
        ServerPlayer player = resolveQuestPlayer(args, 2);
        PlayerQuestManager.give(player, quest.questUuid);
        return Constants.NONE;
    }

    private Varargs questAdvance(LuaState state, Varargs args) throws LuaError {
        QuestDefinition quest = resolveQuest(LuaStrings.toJava(args.arg(1)));
        int amount = Math.max(1, args.arg(2).optInteger(1));
        ServerPlayer player = resolveQuestPlayer(args, 3);

        switch (quest.type == null ? QuestType.BOOLEAN : quest.type) {
            case KILL -> PlayerQuestManager.addKill(player, quest, amount);
            case BOOLEAN -> {
                int size = PlayerQuestManager.completedStepCount(player, quest.questUuid);
                for (int i = 0; i < amount; i++) {
                    String stepId = "lua_step_" + (size + i + 1) + "_" + System.nanoTime();
                    PlayerQuestManager.advanceStep(player, quest, stepId);
                }
            }
            case ITEM -> throw new LuaError("ITEM quests advance by collecting items");
        }
        return Constants.NONE;
    }

    private Varargs questComplete(LuaState state, Varargs args) throws LuaError {
        QuestDefinition quest = resolveQuest(LuaStrings.toJava(args.arg(1)));
        ServerPlayer player = resolveQuestPlayer(args, 2);
        PlayerQuestManager.markCompleted(player, quest.questUuid);
        return Constants.NONE;
    }

    private Varargs questReset(LuaState state, Varargs args) throws LuaError {
        QuestDefinition quest = resolveQuest(LuaStrings.toJava(args.arg(1)));
        ServerPlayer player = resolveQuestPlayer(args, 2);
        PlayerQuestManager.reset(player, quest.questUuid);
        return Constants.NONE;
    }

    private QuestDefinition resolveQuest(String id) throws LuaError {
        if (id == null || id.isBlank()) {
            throw new LuaError("quest not found: " + id);
        }

        NPCInteractManager manager = NPCInteractManager.byUUID(engine.npcUUID()).orElse(null);
        Optional<QuestDefinition> resolved = QuestResolver.resolve(manager, id);
        if (resolved.isPresent()) {
            return resolved.get();
        }

        throw new LuaError("quest not found: " + id);
    }

    private ServerPlayer resolveQuestPlayer(Varargs args, int index) throws LuaError {
        LuaValue value = args.arg(index);
        LuaPlayerProxy proxy;
        if (value instanceof LuaPlayerProxy playerProxy) {
            proxy = playerProxy;
        } else if (value.isNil()) {
            proxy = engine.currentTargetProxy();
        } else {
            throw new LuaError("expected a player");
        }
        if (proxy == null) {
            throw new LuaError("quest API requires a player");
        }
        return proxy.resolve();
    }

    private static int questCurrent(ServerPlayer player, QuestDefinition quest) {
        return switch (quest.type == null ? QuestType.BOOLEAN : quest.type) {
            case KILL -> PlayerQuestManager.currentKillCount(player, quest.questUuid);
            case BOOLEAN -> PlayerQuestManager.completedStepCount(player, quest.questUuid);
            case ITEM -> QuestInventoryUtil.count(player, quest.itemId);
        };
    }

    private static int questTarget(QuestDefinition quest) {
        return switch (quest.type == null ? QuestType.BOOLEAN : quest.type) {
            case KILL -> Math.max(1, quest.killCount);
            case BOOLEAN -> Math.max(1, quest.requiredCompletions);
            case ITEM -> Math.max(1, quest.itemCount);
        };
    }

    private static LuaPlayerProxy resolvePlayer(LuaNpcEngine engine, Varargs args, int index) throws LuaError {
        LuaValue value = args.arg(index);
        if (value instanceof LuaPlayerProxy proxy) {
            return proxy;
        }
        if (!value.isNil()) {
            throw new LuaError("expected a player");
        }
        LuaPlayerProxy target = engine.currentTargetProxy();
        if (target == null) {
            throw new LuaError("no player target");
        }
        return target;
    }

    private static List<NPCTrade> parseTrades(LuaTable table, ServerPlayer player, String npcUuid) throws LuaError {
        List<NPCTrade> trades = new ArrayList<>();
        int unlocked = MerchantSlotData.getUnlocked(player, npcUuid);
        int length = table.length();
        for (int i = 1; i <= length; i++) {
            LuaTable entry = table.rawget(i).checkTable();

            int requiredLevel = Math.max(0, entry.rawget("requiredLevel").optInteger(0));
            if (requiredLevel > unlocked) {
                continue;
            }

            double rawMultiplier = entry.rawget("priceMultiplier").optDouble(1.0D);
            if (!Double.isFinite(rawMultiplier)) {
                throw new LuaError("priceMultiplier must be a finite number");
            }

            NPCTrade trade = new NPCTrade();
            trade.input1 = parseItemStack(entry, "in1", "in1count");
            trade.input2 = entry.rawget("in2").isNil() ? ItemStack.EMPTY : parseItemStack(entry, "in2", "in2count");
            trade.output = parseItemStack(entry, "out", "outcount");
            trade.priceMultiplier = (float) Math.max(0.1D, Math.min(10.0D, rawMultiplier));
            trade.demand = Math.max(0, entry.rawget("demand").optInteger(0));
            trade.maxTrades = entry.rawget("maxTrades").optInteger(-1);
            trade.resetTime = entry.rawget("resetTime").optInteger(-1);
            trades.add(trade);
        }
        return trades;
    }

    private static ItemStack parseItemStack(LuaTable entry, String idKey, String countKey) throws LuaError {
        LuaValue idValue = entry.rawget(idKey);
        if (idValue.isNil()) {
            throw new LuaError("trade entry missing '" + idKey + "'");
        }
        String id = LuaStrings.toJava(idValue);
        int count = Math.max(1, entry.rawget(countKey).optInteger(1));
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            throw new LuaError("unknown item: " + id);
        }
        Item item = BuiltInRegistries.ITEM.get(key);
        return new ItemStack(item, count);
    }

    private static final class WaitFunction extends ResumableVarArgFunction<Void> {
        private final LuaNpcEngine engine;

        private WaitFunction(LuaNpcEngine engine) {
            this.engine = engine;
        }

        @Override
        protected Varargs invoke(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
            int ticks = Math.max(0, args.arg(1).checkInteger());
            engine.beginWaitTicks(state, ticks);
            return LuaThread.yield(state, Constants.NONE);
        }

        @Override
        public Varargs resume(LuaState state, Void object, Varargs value) {
            return value;
        }
    }

    private static final class MoveFunction extends ResumableVarArgFunction<Void> {
        private final LuaNpcEngine engine;

        private MoveFunction(LuaNpcEngine engine) {
            this.engine = engine;
        }

        @Override
        protected Varargs invoke(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
            double x = args.arg(1).checkDouble();
            double y = args.arg(2).checkDouble();
            double z = args.arg(3).checkDouble();
            double speed = args.arg(4).optDouble(1.0D);

            if (!engine.beginMove(state, x, y, z, speed)) {
                return Constants.FALSE;
            }
            return LuaThread.yield(state, Constants.NONE);
        }

        @Override
        public Varargs resume(LuaState state, Void object, Varargs value) {
            return value;
        }
    }

    private static final class WaitEventFunction extends ResumableVarArgFunction<Void> {
        private final LuaNpcEngine engine;

        private WaitEventFunction(LuaNpcEngine engine) {
            this.engine = engine;
        }

        @Override
        protected Varargs invoke(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
            String eventName = LuaStrings.toJava(args.arg(1));
            int timeout = Math.max(0, args.arg(2).optInteger(0));
            engine.beginWaitEvent(state, eventName, timeout);
            return LuaThread.yield(state, Constants.NONE);
        }

        @Override
        public Varargs resume(LuaState state, Void object, Varargs value) {
            return value;
        }
    }

    private static final class DialogueFunction extends ResumableVarArgFunction<Void> {
        private final LuaNpcEngine engine;

        private DialogueFunction(LuaNpcEngine engine) {
            this.engine = engine;
        }

        @Override
        protected Varargs invoke(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
            String message = LuaStrings.toJava(args.arg(1));
            LuaTable answersTable = args.arg(2).checkTable();
            LuaPlayerProxy player = resolvePlayer(engine, args, 3);

            LinkedHashMap<String, String> answers = new LinkedHashMap<>();
            LuaValue key = Constants.NIL;
            while (true) {
                Varargs pair = answersTable.next(key);
                key = pair.arg(1);
                if (key.isNil()) {
                    break;
                }
                String text = LuaStrings.toJava(key);
                String choice = LuaStrings.toJava(pair.arg(2));
                if (text.isEmpty()) {
                    throw new LuaError("dialogue answer text must not be empty");
                }
                if (choice.isEmpty()) {
                    throw new LuaError("dialogue answer key must not be empty");
                }
                answers.put(text, choice);
            }

            if (answers.isEmpty()) {
                throw new LuaError("dialogue requires at least one answer");
            }

            engine.beginDialogue(state, message, answers, player);
            return LuaThread.yield(state, Constants.NONE);
        }

        @Override
        public Varargs resume(LuaState state, Void object, Varargs value) {
            return value;
        }
    }

    private static final class TradeFunction extends ResumableVarArgFunction<Void> {
        private final LuaNpcEngine engine;

        private TradeFunction(LuaNpcEngine engine) {
            this.engine = engine;
        }

        @Override
        protected Varargs invoke(LuaState state, DebugFrame frame, Varargs args) throws LuaError, UnwindThrowable {
            LuaTable tradesTable = args.arg(1).checkTable();
            LuaPlayerProxy player = resolvePlayer(engine, args, 2);
            if (tradesTable.length() == 0) {
                throw new LuaError("trade requires at least one trade");
            }

            List<NPCTrade> trades = parseTrades(tradesTable, player.resolve(), engine.npcUUID().toString());
            if (!engine.beginTrade(state, trades, player)) {
                return Constants.NIL;
            }
            return LuaThread.yield(state, Constants.NONE);
        }

        @Override
        public Varargs resume(LuaState state, Void object, Varargs value) {
            return value;
        }
    }
}
