package com.example.douyinreportadmin;

final class GistPayload {
    private GistPayload() {
    }

    static String createSecret(String filename, String content) {
        return "{"
                + "\"description\":\"Douyin report QR remote config\","
                + "\"public\": false,"
                + "\"files\":{" + fileObject(filename, content) + "}"
                + "}";
    }

    static String updateFile(String filename, String content) {
        return "{\"files\":{" + fileObject(filename, content) + "}}";
    }

    private static String fileObject(String filename, String content) {
        return "\"" + JsonText.escape(filename) + "\":{\"content\":\"" + JsonText.escape(content) + "\"}";
    }
}
