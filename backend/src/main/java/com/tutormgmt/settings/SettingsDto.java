package com.tutormgmt.settings;

public record SettingsDto(
        String calendarId,
        String calendarSummary,
        String reportSpreadsheetId,
        String reportSpreadsheetName,
        String reportWorksheetTitle
) {
    public static SettingsDto from(UserSettings s) {
        return new SettingsDto(s.getCalendarId(), s.getCalendarSummary(),
                s.getReportSpreadsheetId(), s.getReportSpreadsheetName(), s.getReportWorksheetTitle());
    }
}
