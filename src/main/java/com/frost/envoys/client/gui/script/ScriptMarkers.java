package com.frost.envoys.client.gui.script;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScriptMarkers {

    public static final String PREFIX = "-- @gui:";
    public static final String EVENT_START = "event:start";
    public static final String EVENT_END = "event:end";
    public static final String ACTION_START = "action:start";
    public static final String ACTION_END = "action:end";
    public static final String BRANCH_START = "branch:start";
    public static final String BRANCH_END = "branch:end";
    public static final String COMMAND_LINE = "command:line";
    public static final String TRADE_ITEM = "trade:item";

    private ScriptMarkers() {
    }

    public static boolean isMarker(String trimmedLine) {
        return trimmedLine.startsWith(PREFIX);
    }

    public static String kind(String trimmedLine) {
        if (!isMarker(trimmedLine)) {
            return null;
        }
        String rest = trimmedLine.substring(PREFIX.length());
        int space = rest.indexOf(' ');
        return space < 0 ? rest : rest.substring(0, space);
    }

    public static Map<String, String> attrs(String trimmedLine) {
        if (!isMarker(trimmedLine)) {
            return Map.of();
        }
        String rest = trimmedLine.substring(PREFIX.length());
        int space = rest.indexOf(' ');
        return space < 0 ? Map.of() : parseAttrs(rest.substring(space + 1));
    }

    public static String line(String kind, Map<String, String> attrs, Map<String, List<String>> lists) {
        StringBuilder sb = new StringBuilder(PREFIX).append(kind);
        if (attrs != null) {
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                sb.append(' ').append(entry.getKey()).append('=').append(formatValue(entry.getValue()));
            }
        }
        if (lists != null) {
            for (Map.Entry<String, List<String>> entry : lists.entrySet()) {
                sb.append(' ').append(entry.getKey()).append('=').append(formatList(entry.getValue()));
            }
        }
        return sb.toString();
    }

    public static String actionStart(GraphNode node) {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("id", node.id);
        attrs.put("type", node.type);
        attrs.put("next", node.nextId == null || node.nextId.isBlank() ? "null" : node.nextId);
        attrs.putAll(node.params);
        attrs.remove("commands");
        if (ScriptNodeTypes.TRADE.equals(node.type)) {
            attrs.put("offers", Integer.toString(node.offers.size()));
        }
        return line(ACTION_START, attrs, null);
    }

    public static String actionEnd(String id) {
        return line(ACTION_END, Map.of("id", id), null);
    }

    public static String branchStart(GraphNode node) {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("id", node.id);
        attrs.put("type", node.type);
        attrs.put("next", node.nextId == null || node.nextId.isBlank() ? "null" : node.nextId);
        attrs.putAll(node.params);
        attrs.remove("options");
        attrs.remove("keys");
        if (ScriptNodeTypes.RANDOM.equals(node.type)) {
            attrs.put("count", Integer.toString(node.options.size()));
            return line(BRANCH_START, attrs, null);
        }
        if (ScriptNodeTypes.QUEST_CHECK.equals(node.type)) {
            return line(BRANCH_START, attrs, null);
        }
        List<String> labels = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (GraphNode.BranchOption option : node.options) {
            labels.add(option.label);
            keys.add(option.key);
        }
        Map<String, List<String>> lists = new LinkedHashMap<>();
        lists.put("options", labels);
        lists.put("keys", keys);
        return line(BRANCH_START, attrs, lists);
    }

    public static String branchEnd(String id) {
        return line(BRANCH_END, Map.of("id", id), null);
    }

    public static String eventStart(String name, String arg, boolean enabled) {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("name", name);
        if (arg != null && !arg.isBlank()) {
            attrs.put("arg", arg);
        }
        if (!enabled) {
            attrs.put("enabled", "false");
        }
        return line(EVENT_START, attrs, null);
    }

    public static String eventEnd(String name) {
        return line(EVENT_END, Map.of("name", name), null);
    }

    public static String commandLine(String text) {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("text", text);
        return line(COMMAND_LINE, attrs, null);
    }

    public static String tradeItem(GraphNode.TradeOffer offer) {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("in1", offer.in1);
        attrs.put("in1count", Integer.toString(offer.in1count));
        attrs.put("in2", offer.in2 == null ? "" : offer.in2);
        attrs.put("in2count", Integer.toString(offer.in2count));
        attrs.put("out", offer.out);
        attrs.put("outcount", Integer.toString(offer.outcount));
        attrs.put("priceMultiplier", Float.toString(offer.priceMultiplier));
        attrs.put("demand", Integer.toString(offer.demand));
        attrs.put("maxTrades", Integer.toString(offer.maxTrades));
        attrs.put("resetTime", Integer.toString(offer.resetTime));
        attrs.put("requiredLevel", Integer.toString(offer.requiredLevel));
        return line(TRADE_ITEM, attrs, null);
    }

    public static String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    public static String formatList(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(quote(values.get(i)));
        }
        return sb.toString();
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String unescape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String formatValue(String value) {
        String safe = value == null ? "" : value;
        if (needsQuote(safe)) {
            return quote(safe);
        }
        return safe;
    }

    private static boolean needsQuote(String value) {
        if (value.isEmpty()) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.' || c == ':' || c == '/';
            if (!allowed) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, String> parseAttrs(String input) {
        Map<String, String> result = new LinkedHashMap<>();
        if (input == null || input.isBlank()) {
            return result;
        }
        for (String token : splitTokens(input)) {
            int eq = token.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = token.substring(0, eq);
            String raw = token.substring(eq + 1);
            result.put(key, raw);
        }
        return result;
    }

    public static String value(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.length() >= 2 && raw.charAt(0) == '"' && raw.charAt(raw.length() - 1) == '"') {
            return unescape(raw.substring(1, raw.length() - 1));
        }
        return raw;
    }

    public static List<String> parseList(String input) {
        List<String> result = new ArrayList<>();
        if (input == null || input.isBlank()) {
            return result;
        }
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == ',') {
                i++;
                continue;
            }
            if (c == '"') {
                StringBuilder sb = new StringBuilder();
                i++;
                while (i < input.length() && input.charAt(i) != '"') {
                    char ch = input.charAt(i);
                    if (ch == '\\' && i + 1 < input.length()) {
                        sb.append(input.charAt(++i));
                    } else {
                        sb.append(ch);
                    }
                    i++;
                }
                i++;
                result.add(unescape(sb.toString()));
            } else if (!Character.isWhitespace(c)) {
                int start = i;
                while (i < input.length() && input.charAt(i) != ',' && !Character.isWhitespace(input.charAt(i))) {
                    i++;
                }
                result.add(input.substring(start, i));
            } else {
                i++;
            }
        }
        return result;
    }

    private static List<String> splitTokens(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && inQuote && i + 1 < input.length()) {
                current.append(c).append(input.charAt(++i));
                continue;
            }
            if (c == '"') {
                inQuote = !inQuote;
                current.append(c);
                continue;
            }
            if (Character.isWhitespace(c) && !inQuote) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}
