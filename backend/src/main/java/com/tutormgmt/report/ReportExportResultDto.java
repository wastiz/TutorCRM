package com.tutormgmt.report;

public record ReportExportResultDto(
        String spreadsheetId,
        String spreadsheetUrl,
        String worksheetTitle,
        boolean updated
) {}
