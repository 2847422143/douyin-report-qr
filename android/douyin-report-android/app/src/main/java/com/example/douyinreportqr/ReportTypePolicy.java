package com.example.douyinreportqr;

final class ReportTypePolicy {
    private ReportTypePolicy() {
    }

    static String toReportType(String objectType) {
        if ("note".equals(objectType) || "images".equals(objectType)) {
            return "images";
        }
        return "video";
    }
}
