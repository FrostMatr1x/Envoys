package com.frost.envoys.client.gui.script;

import net.minecraft.network.chat.Component;

/**
 * A scenario compilation error. The technical message is the translation key (used for logs),
 * while {@link #component()} returns the localized, user-facing text.
 */
public class ScenarioCompileException extends Exception {

    private final Component component;

    public ScenarioCompileException(String key, Object... args) {
        super(key);
        this.component = Component.translatable(key, args);
    }

    public Component component() {
        return component;
    }
}
