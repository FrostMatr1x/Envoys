package com.frost.envoys.lua;

import com.frost.envoys.config.Config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LuaEngineManager {

    private static final Map<UUID, LuaNpcEngine> ENGINES = new HashMap<>();
    private static final Set<UUID> ABSENT = new HashSet<>();
    private static final Set<UUID> STOPPED = new HashSet<>();

    private LuaEngineManager() {
    }

    public static void init() {
        if (!Config.LUA_ENABLED.get()) {
            return;
        }
        reloadAll();
    }

    public static LuaNpcEngine getEngine(UUID npcId) {
        if (!Config.LUA_ENABLED.get() || STOPPED.contains(npcId)) {
            return null;
        }
        return ENGINES.get(npcId);
    }

    public static void stop(UUID npcId) {
        if (npcId == null) {
            return;
        }
        STOPPED.add(npcId);
        LuaNpcEngine engine = ENGINES.remove(npcId);
        if (engine != null) {
            engine.shutdown();
        }
    }

    public static boolean isStopped(UUID npcId) {
        return npcId != null && STOPPED.contains(npcId);
    }

    public static LuaNpcEngine ensure(UUID npcId) {
        if (!Config.LUA_ENABLED.get() || STOPPED.contains(npcId)) {
            return null;
        }
        LuaNpcEngine existing = ENGINES.get(npcId);
        if (existing != null && !existing.isShutdown()) {
            return existing;
        }
        if (ABSENT.contains(npcId)) {
            return null;
        }
        try {
            return createOrRestart(npcId);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public static LuaNpcEngine createOrRestart(UUID npcId) {
        if (!Config.LUA_ENABLED.get()) {
            return null;
        }
        STOPPED.remove(npcId);
        LuaNpcEngine previous = ENGINES.remove(npcId);
        if (previous != null) {
            previous.shutdown();
        }

        String source = LuaScriptStore.readScript(npcId);
        if (source == null) {
            ABSENT.add(npcId);
            return null;
        }

        LuaNpcEngine engine = new LuaNpcEngine(npcId);
        if (engine.load(source)) {
            ENGINES.put(npcId, engine);
            ABSENT.remove(npcId);
            return engine;
        }
        ABSENT.add(npcId);
        return null;
    }

    public static ReloadResult reloadAll() {
        if (!Config.LUA_ENABLED.get()) {
            shutdownAll();
            ABSENT.clear();
            return new ReloadResult(0, 0);
        }

        STOPPED.clear();
        Set<UUID> known = new HashSet<>(LuaScriptStore.listNpcsWithScripts());
        known.addAll(ENGINES.keySet());
        known.addAll(ABSENT);
        ABSENT.clear();

        int loaded = 0;
        int failed = 0;

        for (UUID npcId : known) {
            LuaNpcEngine previous = ENGINES.remove(npcId);
            if (previous != null) {
                previous.shutdown();
            }

            String source = LuaScriptStore.readScript(npcId);
            if (source == null) {
                ABSENT.add(npcId);
                continue;
            }

            LuaNpcEngine engine = new LuaNpcEngine(npcId);
            if (engine.load(source)) {
                ENGINES.put(npcId, engine);
                loaded++;
            } else {
                ABSENT.add(npcId);
                failed++;
            }
        }

        return new ReloadResult(loaded, failed);
    }

    public static void remove(UUID npcId) {
        ABSENT.remove(npcId);
        LuaNpcEngine engine = ENGINES.remove(npcId);
        if (engine != null) {
            engine.shutdown();
        }
    }

    public static void shutdownAll() {
        for (LuaNpcEngine engine : ENGINES.values()) {
            engine.shutdown();
        }
        ENGINES.clear();
    }

    public record ReloadResult(int loaded, int failed) {
    }
}
