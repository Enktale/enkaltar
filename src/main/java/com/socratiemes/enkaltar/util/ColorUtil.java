package com.socratiemes.enkaltar.util;

import com.hypixel.hytale.server.core.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})|&([0-9a-fA-FlLrRoO])");
    private static final String[] COLOR_MAP = {
            "#000000", "#0000AA", "#00AA00", "#00AAAA",
            "#AA0000", "#AA00AA", "#FFAA00", "#AAAAAA",
            "#555555", "#5555FF", "#55FF55", "#55FFFF",
            "#FF5555", "#FF55FF", "#FFFF55", "#FFFFFF"
    };

    private ColorUtil() {
    }

    public static Message colorize(String input) {
        if (input == null || input.isEmpty()) {
            return Message.raw("");
        }

        List<Message> parts = new ArrayList<>();
        TextStyle style = new TextStyle();

        Matcher matcher = TOKEN_PATTERN.matcher(input);
        int last = 0;
        while (matcher.find()) {
            int start = matcher.start();
            if (start > last) {
                String text = input.substring(last, start);
                if (!text.isEmpty()) {
                    parts.add(styled(text, style));
                }
            }

            String hex = matcher.group(1);
            String code = matcher.group(2);
            if (hex != null) {
                style.color = "#" + hex.toUpperCase();
            } else if (code != null && !code.isEmpty()) {
                char c = Character.toLowerCase(code.charAt(0));
                if (c == 'l') {
                    style.bold = true;
                } else if (c == 'o') {
                    style.italic = true;
                } else if (c == 'r') {
                    style.reset();
                } else {
                    int digit = Character.digit(c, 16);
                    if (digit >= 0 && digit < COLOR_MAP.length) {
                        style.color = COLOR_MAP[digit];
                    }
                }
            }

            last = matcher.end();
        }

        if (last < input.length()) {
            String text = input.substring(last);
            if (!text.isEmpty()) {
                parts.add(styled(text, style));
            }
        }

        if (parts.isEmpty()) {
            return Message.raw(input);
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return Message.join(parts.toArray(new Message[0]));
    }

    private static Message styled(String text, TextStyle style) {
        Message message = Message.raw(text);
        if (style.color != null) {
            message = message.color(style.color);
        }
        if (style.bold) {
            message = message.bold(true);
        }
        if (style.italic) {
            message = message.italic(true);
        }
        return message;
    }

    private static final class TextStyle {
        private String color;
        private boolean bold;
        private boolean italic;

        private void reset() {
            color = null;
            bold = false;
            italic = false;
        }
    }
}
