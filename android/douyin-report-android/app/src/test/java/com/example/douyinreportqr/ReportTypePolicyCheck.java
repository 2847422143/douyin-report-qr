package com.example.douyinreportqr;

public class ReportTypePolicyCheck {
    public static void main(String[] args) {
        assertEquals("video", ReportTypePolicy.toReportType("video"));
        assertEquals("images", ReportTypePolicy.toReportType("note"));
        assertEquals("images", ReportTypePolicy.toReportType("images"));
        assertEquals("video", ReportTypePolicy.toReportType(""));
        System.out.println("report type policy check passed");
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }
}
