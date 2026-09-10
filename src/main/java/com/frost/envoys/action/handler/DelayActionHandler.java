package com.frost.envoys.action.handler;

import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.model.ActionDelay;

public final class DelayActionHandler implements NpcActionHandler<ActionDelay> {

    @Override
    public void execute(ActionDelay action, ActionContext context) {
        int ticks = toTicks(action.duration, action.timeUnit);

        if (ticks <= 0) {
            context.advance(action.nextActionId);
        } else {
            context.waitForDelay(ticks);
        }
    }

    static int toTicks(int duration, char timeUnit) {
        return switch (Character.toLowerCase(timeUnit)) {
            case 's' -> duration * 20;
            case 'm' -> duration * 1200;
            case 'h' -> duration * 72000;
            default -> duration;
        };
    }
}
