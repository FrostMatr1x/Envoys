package com.frost.envoys.action.handler;

import java.util.List;
import java.util.Random;

import com.frost.envoys.action.ActionContext;
import com.frost.envoys.action.NpcActionHandler;
import com.frost.envoys.action.model.ActionRandomizer;

public final class RandomizerActionHandler implements NpcActionHandler<ActionRandomizer> {
    private static final Random RANDOM = new Random();

    @Override
    public void execute(ActionRandomizer action, ActionContext context) {
        List<String> valid = action.options == null ? List.of() :
            action.options.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList();

        if (valid.isEmpty()) {
            context.advance(null);
            return;
        }

        String chosen = valid.get(RANDOM.nextInt(valid.size()));
        context.advance(chosen);
    }
}
