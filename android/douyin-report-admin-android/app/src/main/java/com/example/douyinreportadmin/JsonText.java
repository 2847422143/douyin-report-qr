package com.example.douyinreportadmin;

final class JsonText {
    private JsonText() {
    }

    static String escape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\') builder.append("\\\\");
            else if (ch == '"') builder.append("\\\"");
            else if (ch == '\n') builder.append("\\n");
            else if (ch == '\r') builder.append("\\r");
            else if (ch == '\t') builder.append("\\t");
            else builder.append(ch);
        }
        return builder.toString();
    }

    static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\");
    }
}
