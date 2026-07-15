package com.example.douyinreportadmin;

public class GistConfigCheck {
    public static void main(String[] args) throws Exception {
        GistConfig off = new GistConfig(false, "服务维护中");
        assertContains(off.toJson(), "\"enabled\": false");
        assertContains(off.toJson(), "\"message\": \"服务维护中\"");

        GistConfig parsed = GistConfig.parse("{\"enabled\":true,\"message\":\"功能可用\"}");
        assertTrue(parsed.enabled);
        assertEquals("功能可用", parsed.message);

        String updatePayload = GistPayload.updateFile("config.json", off.toJson());
        assertContains(updatePayload, "\"files\"");
        assertContains(updatePayload, "\"config.json\"");
        assertContains(updatePayload, "\\\"enabled\\\": false");

        String createPayload = GistPayload.createSecret("config.json", off.toJson());
        assertContains(createPayload, "\"public\": false");
        assertContains(createPayload, "Douyin report QR remote config");

        System.out.println("gist config check passed");
    }

    private static void assertTrue(boolean value) {
        if (!value) throw new AssertionError("expected true");
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected " + expected + " but got " + actual);
    }

    private static void assertContains(String value, String part) {
        if (!value.contains(part)) throw new AssertionError("expected to contain " + part + " in " + value);
    }
}
