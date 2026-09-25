package com.frost.envoys.client;

import java.util.UUID;

public final class ScriptNames {

    private ScriptNames() {
    }

    public static String fileName(String npcName, UUID npcId) {
        String sanitized = sanitize(npcName);
        String suffix = npcId == null ? "0000" : npcId.toString().replace("-", "");
        if (suffix.length() > 4) {
            suffix = suffix.substring(0, 4);
        }
        return sanitized + "_" + suffix + ".lua";
    }

    public static String sanitize(String name) {
        String transliterated = transliterate(name == null ? "" : name);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < transliterated.length(); i++) {
            char c = transliterated.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_' || c == '-';
            sb.append(ok ? c : '_');
        }
        String result = sb.toString().replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        if (result.isEmpty()) {
            result = "npc";
        }
        if (result.length() > 32) {
            result = result.substring(0, 32);
        }
        return result;
    }

    private static String transliterate(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String mapped = switch (Character.toLowerCase(c)) {
                case 'а' -> "a"; case 'б' -> "b"; case 'в' -> "v"; case 'г' -> "g"; case 'д' -> "d";
                case 'е', 'ё' -> "e"; case 'ж' -> "zh"; case 'з' -> "z"; case 'и' -> "i"; case 'й' -> "y";
                case 'к' -> "k"; case 'л' -> "l"; case 'м' -> "m"; case 'н' -> "n"; case 'о' -> "o";
                case 'п' -> "p"; case 'р' -> "r"; case 'с' -> "s"; case 'т' -> "t"; case 'у' -> "u";
                case 'ф' -> "f"; case 'х' -> "h"; case 'ц' -> "ts"; case 'ч' -> "ch"; case 'ш' -> "sh";
                case 'щ' -> "sch"; case 'ъ', 'ь' -> ""; case 'ы' -> "y"; case 'э' -> "e"; case 'ю' -> "yu";
                case 'я' -> "ya";
                default -> null;
            };
            if (mapped != null) {
                sb.append(Character.isUpperCase(c) ? capitalize(mapped) : mapped);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
