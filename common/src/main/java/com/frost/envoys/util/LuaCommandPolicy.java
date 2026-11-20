package com.frost.envoys.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LuaCommandPolicy {

    private static final Set<String> FORBIDDEN = Set.of(
        "op", "deop",
        "ban", "ban-ip", "banlist", "pardon", "pardon-ip",
        "whitelist",
        "stop", "reload", "datapack",
        "function", "schedule",
        "save-all", "save-off", "save-on",
        "setidletimeout", "transfer", "publish",
        "debug", "jfr", "perf"
    );
    private static final Set<String> EXECUTE_SIDE_EFFECTS = Set.of("summon", "store");
    private static final int MAX_LENGTH = 2048;

    private LuaCommandPolicy() {
    }

    public static boolean isAllowed(CommandDispatcher<CommandSourceStack> dispatcher,
                                    CommandSourceStack source,
                                    String rawCommand) {
        String command = normalize(rawCommand);
        if (command.isEmpty() || command.length() > MAX_LENGTH) {
            return false;
        }

        ParseResults<CommandSourceStack> parsed;
        try {
            parsed = dispatcher.parse(new StringReader(command), source);
        } catch (RuntimeException e) {
            return false;
        }

        if (parsed.getReader().canRead() || !parsed.getExceptions().isEmpty()) {
            return false;
        }

        List<ParsedCommandNode<CommandSourceStack>> nodes = new ArrayList<>();
        collectNodes(parsed.getContext(), nodes);
        boolean expectRoot = true;
        boolean inExecute = false;

        for (ParsedCommandNode<CommandSourceStack> parsedNode : nodes) {
            if (!(parsedNode.getNode() instanceof LiteralCommandNode<?> literal)) {
                continue;
            }
            String name = canonical(literal.getName());

            if (expectRoot) {
                if (FORBIDDEN.contains(name)) {
                    return false;
                }
                expectRoot = false;
                inExecute = name.equals("execute");
                continue;
            }

            if (name.equals("run")) {
                expectRoot = true;
                inExecute = false;
                continue;
            }

            if (inExecute && EXECUTE_SIDE_EFFECTS.contains(name)) {
                return false;
            }
        }
        return true;
    }

    private static void collectNodes(CommandContextBuilder<CommandSourceStack> context,
                                     List<ParsedCommandNode<CommandSourceStack>> nodes) {
        nodes.addAll(context.getNodes());
        CommandContextBuilder<CommandSourceStack> child = context.getChild();
        if (child != null) {
            collectNodes(child, nodes);
        }
    }

    private static String normalize(String raw) {
        String s = raw == null ? "" : raw.strip();
        while (s.startsWith("/")) {
            s = s.substring(1).stripLeading();
        }
        return s;
    }

    private static String canonical(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        int colon = lower.indexOf(':');
        return colon >= 0 ? lower.substring(colon + 1) : lower;
    }
}
