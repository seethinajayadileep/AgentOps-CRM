package com.agentopscrm.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a one-line follow-up draft into short email paragraphs.
 */
public final class FollowUpEmailBody {

    private static final Pattern GREETING = Pattern.compile(
            "^(Dear|Hi|Hello|Hey)\\s+[^\\n,!.]{1,80}[,!]\\s*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SENTENCE = Pattern.compile("(?<=[.!?])\\s+");

    private FollowUpEmailBody() {}

    public static String plain(String raw) {
        return String.join("\n\n", paragraphs(raw));
    }

    public static String html(String raw) {
        StringBuilder html = new StringBuilder();
        html.append("<div style=\"font-family:Georgia,serif;font-size:16px;line-height:1.6;")
                .append("color:#222222;max-width:560px\">");
        for (String paragraph : paragraphs(raw)) {
            html.append("<p style=\"margin:0 0 16px 0\">")
                    .append(escape(paragraph).replace("\n", "<br>"))
                    .append("</p>");
        }
        html.append("</div>");
        return html.toString();
    }

    static List<String> paragraphs(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of("");
        }
        String normalized = raw.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.contains("\n")) {
            List<String> blocks = new ArrayList<>();
            for (String block : normalized.split("\\n\\s*\\n")) {
                String compact = block.replaceAll("[ \\t]*\\n[ \\t]*", " ").trim();
                if (!compact.isEmpty()) {
                    blocks.add(compact);
                }
            }
            return blocks.isEmpty() ? List.of(normalized) : blocks;
        }

        String collapsed = normalized.replaceAll("\\s+", " ").trim();
        List<String> parts = new ArrayList<>();
        Matcher greeting = GREETING.matcher(collapsed);
        String rest = collapsed;
        if (greeting.find()) {
            parts.add(greeting.group().trim());
            rest = collapsed.substring(greeting.end()).trim();
        }
        if (rest.isEmpty()) {
            return parts.isEmpty() ? List.of(collapsed) : parts;
        }
        String[] sentences = SENTENCE.split(rest);
        List<String> kept = new ArrayList<>();
        for (String sentence : sentences) {
            String piece = sentence.trim();
            if (!piece.isEmpty()) {
                kept.add(piece);
            }
        }
        if (kept.size() <= 1) {
            parts.add(rest);
            return parts;
        }
        parts.add(String.join(" ", kept.subList(0, kept.size() - 1)));
        parts.add(kept.get(kept.size() - 1));
        return parts;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
