package com.tutormgmt.integration.google.sheets;

import java.util.List;

/** For the Settings spreadsheet / worksheet pickers (Report.md sections 17, 47). */
public record SpreadsheetSummaryDto(String id, String name, String url) {

    public record Worksheet(String title, Integer sheetId) {}

    public record Detail(String id, String name, String url, List<Worksheet> worksheets) {}
}
