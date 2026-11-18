package com.frost.envoys.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern COLOR_PATTERN = Pattern.compile(
        "(?i)(?:&#|#|<#)([0-9a-fA-F]{6})>?|[&§]([0-9a-fk-or])"
    );

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        MutableComponent root = Component.empty();
        Matcher matcher = COLOR_PATTERN.matcher(text);

        int lastIndex = 0;
        Style currentStyle = Style.EMPTY;

        while (matcher.find()) {
            int start = matcher.start();
            if (start > lastIndex) {
                String segment = text.substring(lastIndex, start);
                root.append(Component.literal(segment).setStyle(currentStyle));
            }

            String hexCode = matcher.group(1);
            String legacyCodeStr = matcher.group(2);

            if (hexCode != null) {
                try {
                    int rgb = Integer.parseInt(hexCode, 16);
                    currentStyle = currentStyle.withColor(TextColor.fromRgb(rgb));
                } catch (NumberFormatException ignored) {}
            } else if (legacyCodeStr != null) {
                char code = Character.toLowerCase(legacyCodeStr.charAt(0));
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    if (formatting == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (formatting.isColor()) {
                        currentStyle = currentStyle.withColor(formatting);
                    } else {
                        currentStyle = currentStyle.applyFormat(formatting);
                    }
                }
            }

            lastIndex = matcher.end();
        }

        if (lastIndex < text.length()) {
            String segment = text.substring(lastIndex);
            root.append(Component.literal(segment).setStyle(currentStyle));
        }

        return root;
    }

    public static String toFormattedString(Component component) {
        if (component == null) return "";

        StringBuilder sb = new StringBuilder();

        class State {
            TextColor lastColor = null;
            boolean lastBold = false;
            boolean lastItalic = false;
            boolean lastUnderlined = false;
            boolean lastStrikethrough = false;
            boolean lastObfuscated = false;
        }

        State state = new State();

        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                TextColor color = style.getColor();
                if (color != null && !color.equals(state.lastColor)) {
                    String hexString = String.format("%06X", color.getValue());
                    sb.append("&#").append(hexString);
                    state.lastColor = color;
                }

                if (style.isBold() != state.lastBold) {
                    if (style.isBold()) sb.append("&l");
                    state.lastBold = style.isBold();
                }
                if (style.isItalic() != state.lastItalic) {
                    if (style.isItalic()) sb.append("&o");
                    state.lastItalic = style.isItalic();
                }
                if (style.isUnderlined() != state.lastUnderlined) {
                    if (style.isUnderlined()) sb.append("&n");
                    state.lastUnderlined = style.isUnderlined();
                }
                if (style.isStrikethrough() != state.lastStrikethrough) {
                    if (style.isStrikethrough()) sb.append("&m");
                    state.lastStrikethrough = style.isStrikethrough();
                }
                if (style.isObfuscated() != state.lastObfuscated) {
                    if (style.isObfuscated()) sb.append("&k");
                    state.lastObfuscated = style.isObfuscated();
                }

                sb.append(text);
            }
            return Optional.empty();
        }, Style.EMPTY);

        return sb.toString();
    }
}