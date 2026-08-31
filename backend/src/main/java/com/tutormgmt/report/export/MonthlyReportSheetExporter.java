package com.tutormgmt.report.export;

import com.tutormgmt.report.MonthlyReportDto;
import java.util.UUID;

/**
 * Port for pushing a computed report to the tutor's Google Sheet. The implementation
 * lives in {@code integration.google.sheets} so the report slice stays Google-free
 * (CLAUDE.md section 63, Report.md section 13).
 */
public interface MonthlyReportSheetExporter {

    ExportOutcome export(UUID userId, MonthlyReportDto report, boolean overwrite);

    record ExportOutcome(
            String spreadsheetId,
            String spreadsheetUrl,
            String worksheetTitle,
            boolean updatedExisting
    ) {}
}
