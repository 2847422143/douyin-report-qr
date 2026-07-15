package com.example.douyinreportqr;

public class AdminAuthCheck {
    public static void main(String[] args) {
        String hash = AdminAuth.hash("admin", "123456");
        assertTrue(AdminAuth.verify("admin", "123456", hash));
        assertFalse(AdminAuth.verify("admin", "wrong", hash));
        assertFalse(AdminAuth.verify("other", "123456", hash));
        System.out.println("admin auth check passed");
    }

    private static void assertTrue(boolean value) {
        if (!value) throw new AssertionError("expected true");
    }

    private static void assertFalse(boolean value) {
        if (value) throw new AssertionError("expected false");
    }
}
