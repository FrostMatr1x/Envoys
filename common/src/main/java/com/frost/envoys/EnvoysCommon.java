package com.frost.envoys;

import com.frost.envoys.action.ActionEngineManager;

public final class EnvoysCommon {
    private static boolean initialized;

    private EnvoysCommon() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ActionEngineManager.initialize();
    }
}
