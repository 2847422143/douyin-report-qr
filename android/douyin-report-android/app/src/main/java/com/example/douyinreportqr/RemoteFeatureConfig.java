package com.example.douyinreportqr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RemoteFeatureConfig {
    private static final Pattern ENABLED_PATTERN = Pattern.compile("\"enabled\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    final boolean enabled;
    final String message;

    private RemoteFeatureConfig(boolean enabled, String message) {
        this.enabled = enabled;
        this.message = message;
    }

    static RemoteFeatureConfig localDefault() {
        return new RemoteFeatureConfig(true, "功能可用");
    }

    static RemoteFeatureConfig unavailable(String message) {
        return new RemoteFeatureConfig(false, message);
    }

    static RemoteFeatureConfig parse(String json) {
        boolean enabled = true;
        String message = "功能可用";

        Matcher enabledMatcher = ENABLED_PATTERN.matcher(json == null ? "" : json);
        if (enabledMatcher.find()) {
            enabled = "true".equalsIgnoreCase(enabledMatcher.group(1));
        }

        Matcher messageMatcher = MESSAGE_PATTERN.matcher(json == null ? "" : json);
        if (messageMatcher.find() && !messageMatcher.group(1).trim().isEmpty()) {
            message = messageMatcher.group(1).trim();
        }

        return new RemoteFeatureConfig(enabled, message);
    }
}
