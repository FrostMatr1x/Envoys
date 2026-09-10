package com.frost.envoys.action;

public final class NpcActionEngine {

    private final ActionHandlerRegistry registry = new ActionHandlerRegistry();

    public ActionHandlerRegistry registry() {
        return registry;
    }
}
