package com.example.douyinreportadmin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GistConfig {
    private static final Pattern ENABLED_PATTERN = Pattern.compile("\"enabled\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    final boolean enabled;
    final String message;

    GistConfig(boolean enabled, String message) {
        this.enabled = enabled;
        this.message = message == null || message.trim().isEmpty() ? (enabled ? "功能可用" : "服务维护中") : message.trim();
    }

    static GistConfig parse(String json) {
        boolean enabled = true;
        String message = "功能可用";
        Matcher enabledMatcher = ENABLED_PATTERN.matcher(json == null ? "" : json);
        if (enabledMatcher.find()) enabled = "true".equalsIgnoreCase(enabledMatcher.group(1));
        Matcher messageMatcher = MESSAGE_PATTERN.matcher(json == null ? "" : json);
        if (messageMatcher.find()) message = JsonText.unescape(messageMatcher.group(1));
        return new GistConfig(enabled, message);
    }

    String toJson() {
        return "{\n"
                + "  \"enabled\": " + enabled + ",\n"
                + "  \"message\": \"" + JsonText.escape(message) + "\",\n"
                + "  \"updated_at\": \"" + JsonText.escape(IsoClock.now()) + "\"\n"
                + "}";
    }
}
