package com.frost.envoys.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.frost.envoys.Envoys;
import com.frost.envoys.action.event.EventType;
import com.frost.envoys.action.event.NpcEventData;
import com.frost.envoys.action.event.NpcRangeEvent;
import com.frost.envoys.action.model.AbstractActionData;
import com.frost.envoys.action.model.ActionMove;
import com.frost.envoys.action.model.ActionSavePoint;
import com.frost.envoys.action.model.EntityActionData;
import com.frost.envoys.npc.entity.BaseNPC;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class ScriptRunner {

    private static final int MAX_STEPS = 256;

    public static final Map<UUID, ScriptRunner> RUNNERS = new HashMap<>();

    private final NPCInteractManager manager;
    private final BaseNPC npc;

    private Player targetPlayer;
    private List<EntityActionData> chain = List.of();
    private int index;
    private int stepCounter;
    private boolean running;

    private int delayTicksRemaining;
    private boolean waitingForMove;
    private boolean waitingForDialog;
    private boolean waitingForTrade;

    private int repathCooldown = 0;
    private int stuckCheckTimer = 20;
    private int stuckSeconds = 0;
    private double lastX, lastY, lastZ;

    private ScriptRunner(BaseNPC npc) {
        this.npc = npc;
        this.manager = NPCInteractManager.byUUID(npc.getUUID())
                .orElseGet(() -> new NPCInteractManager(npc.getUUID()));
    }

    public static ScriptRunner getOrCreate(BaseNPC npc) {
        if (npc == null || npc.level().isClientSide()) {
            throw new IllegalArgumentException("[Envoys] ScriptRunner is server-side only");
        }
        return RUNNERS.computeIfAbsent(npc.getUUID(), uuid -> new ScriptRunner(npc));
    }

    public static void remove(UUID npcUuid) {
        RUNNERS.remove(npcUuid);
    }

    public static void clear() {
        RUNNERS.clear();
    }

    public BaseNPC npc() {
        return npc;
    }

    public Player targetPlayer() {
        return targetPlayer;
    }

    public NPCInteractManager manager() {
        return manager;
    }

    public boolean isRunning() {
        return running;
    }

    public void start(EventType type, Player player) {
        if (running || type == null) {
            return;
        }

        NpcEventData event = manager.getEvent(type);
        if (event == null || !event.enabled()) {
            return;
        }
        List<EntityActionData> actions = event.actions();
        if (actions == null || actions.isEmpty()) {
            return;
        }

        this.targetPlayer = (player != null) ? player : nearestPlayer(event);
        this.chain = new ArrayList<>(actions);
        this.index = 0;
        this.stepCounter = 0;
        this.running = true;

        executeCurrent();
    }

    private Player nearestPlayer(NpcEventData event) {
        float range = 10.0f;
        if (event instanceof NpcRangeEvent rangeEvent) {
            range = rangeEvent.rangeDistance;
        }
        if (!(npc.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getNearestPlayer(npc, range);
    }

    private void executeCurrent() {
        if (!running) {
            return;
        }
        if (index < 0 || index >= chain.size()) {
            finish();
            return;
        }

        stepCounter++;
        if (stepCounter > MAX_STEPS) {
            Envoys.LOGGER.warn("[Envoys] Action chain exceeded max steps for NPC {}", manager.npcUUID);
            finish();
            return;
        }

        EntityActionData action = chain.get(index);
        NpcActionEngine engine = ActionEngineManager.getInstance();
        if (engine == null) {
            Envoys.LOGGER.warn("[Envoys] Action engine is not initialized, stopping chain for NPC {}", manager.npcUUID);
            finish();
            return;
        }

        engine.registry().execute(action, new ActionContext(manager, this, targetPlayer));
    }

    public void advance(String nextActionId) {
        if (!running) {
            return;
        }
        if (nextActionId == null || nextActionId.isEmpty()) {
            finish();
            return;
        }

        for (int i = 0; i < chain.size(); i++) {
            EntityActionData candidate = chain.get(i);
            if (candidate != null && nextActionId.equals(candidate.getId())) {
                index = i;
                executeCurrent();
                return;
            }
        }

        Envoys.LOGGER.warn("[Envoys] Unknown next action id '{}' for NPC {}", nextActionId, manager.npcUUID);
        finish();
    }

    public ActionSavePoint findSavePoint(String checkpointUuid) {
        if (checkpointUuid == null || checkpointUuid.isEmpty()) {
            return null;
        }
        for (EntityActionData action : chain) {
            if (action instanceof ActionSavePoint savePoint && checkpointUuid.equals(savePoint.checkpointUuid)) {
                return savePoint;
            }
        }
        return null;
    }

    public void tick() {
        if (!running) {
            return;
        }

        if (delayTicksRemaining > 0) {
            delayTicksRemaining--;
            if (delayTicksRemaining <= 0) {
                advance(nextActionId(currentAction()));
            }
            return;
        }

        if (waitingForMove) {
            tickMovement();
        }
    }

    public void waitForDelay(int ticks) {
        this.delayTicksRemaining = Math.max(0, ticks);
    }

    public void waitForMove() {
        this.waitingForMove = true;
        this.stuckCheckTimer = 30; // 1.5 секунды форы на разгон моба
        this.stuckSeconds = 0;
        this.repathCooldown = 0;
        this.lastX = npc.getX();
        this.lastY = npc.getY();
        this.lastZ = npc.getZ();
    }

    public void waitForDialog() {
        this.waitingForDialog = true;
    }

    public void waitForTrade() {
        this.waitingForTrade = true;
    }

    public void onDialogAnswer(String nextActionId) {
        if (!running || !waitingForDialog) {
            return;
        }
        waitingForDialog = false;
        advance(nextActionId);
    }

    public void onDialogClosed() {
        if (!running || !waitingForDialog) {
            return;
        }
        waitingForDialog = false;
        advance(nextActionId(currentAction()));
    }

    public void onTradeClose() {
        if (!running || !waitingForTrade) {
            return;
        }
        waitingForTrade = false;
        advance(nextActionId(currentAction()));
    }

    private void tickMovement() {
        EntityActionData action = currentAction();
        if (!(action instanceof ActionMove move)) {
            waitingForMove = false;
            advance(nextActionId(action));
            return;
        }

        double dx = move.targetX - npc.getX();
        double dy = move.targetY - npc.getY();
        double dz = move.targetZ - npc.getZ();
        double horizontalDistSq = dx * dx + dz * dz;

        if (horizontalDistSq <= 2.25D && Math.abs(dy) <= 2.5D) {
            npc.getNavigation().stop();
            npc.endScriptedMovement(); //
            waitingForMove = false;
            advance(nextActionId(move));
            return;
        }

        if (--stuckCheckTimer <= 0) {
            stuckCheckTimer = 20;

            double movedSinceLastSec = (npc.getX() - lastX) * (npc.getX() - lastX) + 
                                       (npc.getZ() - lastZ) * (npc.getZ() - lastZ);

            if (movedSinceLastSec < 0.0225D) {
                stuckSeconds++;
                if (stuckSeconds >= 3) {
                    Envoys.LOGGER.warn("[Envoys] NPC {} is stuck moving to {},{},{}. Skipping.", 
                            manager.npcUUID, move.targetX, move.targetY, move.targetZ);
                    npc.getNavigation().stop();
                    npc.endScriptedMovement();
                    waitingForMove = false;
                    advance(nextActionId(move));
                    return;
                }
            } else {
                stuckSeconds = 0;
            }

            lastX = npc.getX();
            lastY = npc.getY();
            lastZ = npc.getZ();
        }

        // 3. Обновление маршрута
        if (--repathCooldown <= 0 || npc.getNavigation().isDone()) {
            repathCooldown = 15;
            npc.getNavigation().moveTo(move.targetX, move.targetY, move.targetZ, 1.0D);
        }
    }

    private static String nextActionId(EntityActionData action) {
        return action instanceof AbstractActionData abstractAction ? abstractAction.nextActionId : null;
    }

    private EntityActionData currentAction() {
        if (index < 0 || index >= chain.size()) {
            return null;
        }
        return chain.get(index);
    }

    private void finish() {
        running = false;
        targetPlayer = null;
        chain = List.of();
        index = 0;
        stepCounter = 0;
        delayTicksRemaining = 0;
        waitingForMove = false;
        waitingForDialog = false;
        waitingForTrade = false;
        repathCooldown = 0;
        stuckSeconds = 0;
        stuckCheckTimer = 20;

        npc.getNavigation().stop();
        npc.endScriptedMovement();
    }
}