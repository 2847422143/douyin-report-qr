package com.example.douyinreportqr;

public class RemoteFeatureConfigCheck {
    public static void main(String[] args) throws Exception {
        RemoteFeatureConfig disabled = RemoteFeatureConfig.parse("{\"enabled\":false,\"message\":\"维护中\"}");
        assertFalse(disabled.enabled);
        assertEquals("维护中", disabled.message);

        RemoteFeatureConfig enabled = RemoteFeatureConfig.parse("{\"enabled\":true,\"message\":\"可以使用\"}");
        assertTrue(enabled.enabled);
        assertEquals("可以使用", enabled.message);

        RemoteFeatureConfig defaultConfig = RemoteFeatureConfig.parse("{}");
        assertTrue(defaultConfig.enabled);
        assertEquals("功能可用", defaultConfig.message);

        System.out.println("remote feature config check passed");
    }

    private static void assertTrue(boolean value) {
        if (!value) throw new AssertionError("expected true");
    }

    private static void assertFalse(boolean value) {
        if (value) throw new AssertionError("expected false");
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }
}
