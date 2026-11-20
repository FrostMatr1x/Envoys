package com.frost.envoys;

public final class EnvoysCommon {
    private static boolean initialized;

    private EnvoysCommon() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
    }
}
