package com.tutormgmt.integration.google.sheets;

import com.tutormgmt.common.error.ApiException;
import com.tutormgmt.report.MonthlyReportDto;
import com.tutormgmt.report.export.MonthlyReportSheetExporter;
import com.tutormgmt.report.export.ReportSheetGrid;
import com.tutormgmt.settings.UserSettings;
import com.tutormgmt.settings.UserSettingsRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Adapts the report slice's export port to Google Sheets (Report.md sections 13-18). */
@Component
@RequiredArgsConstructor
public class GoogleSheetsReportExporter implements MonthlyReportSheetExporter {

    private final UserSettingsRepository settingsRepository;
    private final GoogleSheetsService sheetsService;

    @Override
    public ExportOutcome export(UUID userId, MonthlyReportDto report, boolean overwrite) {
        UserSettings settings = settingsRepository.findByUserId(userId)
                .filter(s -> s.getReportSpreadsheetId() != null && !s.getReportSpreadsheetId().isBlank())
                .orElseThrow(() -> ApiException.badRequest("REPORT_SPREADSHEET_NOT_CONFIGURED",
                        "Choose a spreadsheet for monthly reports in Settings first"));

        // Report.md section 18 (preferred): a per-month worksheet, unless a fixed title is set.
        String worksheetTitle = settings.getReportWorksheetTitle() != null
                ? settings.getReportWorksheetTitle()
                : report.title();

        GoogleSheetsService.ExportOutcome outcome = sheetsService.export(
                userId, settings.getReportSpreadsheetId(), worksheetTitle,
                ReportSheetGrid.of(report), overwrite);

        return new ExportOutcome(outcome.spreadsheetId(), outcome.spreadsheetUrl(),
                outcome.worksheetTitle(), outcome.updatedExisting());
    }
}
