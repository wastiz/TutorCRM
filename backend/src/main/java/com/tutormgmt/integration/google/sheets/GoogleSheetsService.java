package com.tutormgmt.integration.google.sheets;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Border;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.NumberFormat;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.UpdateBordersRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.tutormgmt.common.error.ApiException;
import com.tutormgmt.integration.google.GoogleApiFactory;
import com.tutormgmt.integration.google.GoogleErrors;
import com.tutormgmt.report.export.ReportSheetGrid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Google Sheets + Drive API wrapper (Report.md sections 13-18). No calculation here —
 * it receives a finished {@link ReportSheetGrid} and writes / formats it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSheetsService {

    private final GoogleApiFactory apiFactory;

    public List<SpreadsheetSummaryDto> listSpreadsheets(UUID userId) {
        try {
            Drive drive = apiFactory.drive(userId);
            List<SpreadsheetSummaryDto> out = new ArrayList<>();
            String pageToken = null;
            do {
                var result = drive.files().list()
                        .setQ("mimeType='application/vnd.google-apps.spreadsheet' and trashed=false")
                        .setFields("nextPageToken, files(id, name, webViewLink)")
                        .setPageSize(100)
                        .setOrderBy("modifiedTime desc")
                        .setPageToken(pageToken)
                        .execute();
                for (File f : result.getFiles()) {
                    out.add(new SpreadsheetSummaryDto(f.getId(), f.getName(), f.getWebViewLink()));
                }
                pageToken = result.getNextPageToken();
            } while (pageToken != null);
            return out;
        } catch (IOException e) {
            throw GoogleErrors.translate("list your spreadsheets", e);
        }
    }

    public SpreadsheetSummaryDto.Detail getSpreadsheet(UUID userId, String spreadsheetId) {
        try {
            Spreadsheet ss = apiFactory.sheets(userId).spreadsheets().get(spreadsheetId)
                    .setFields("spreadsheetId,spreadsheetUrl,properties.title,sheets.properties(sheetId,title)")
                    .execute();
            List<SpreadsheetSummaryDto.Worksheet> worksheets = ss.getSheets().stream()
                    .map(Sheet::getProperties)
                    .map(p -> new SpreadsheetSummaryDto.Worksheet(p.getTitle(), p.getSheetId()))
                    .toList();
            return new SpreadsheetSummaryDto.Detail(ss.getSpreadsheetId(), ss.getProperties().getTitle(),
                    ss.getSpreadsheetUrl(), worksheets);
        } catch (IOException e) {
            throw GoogleErrors.translate("open that spreadsheet", e);
        }
    }

    /**
     * Writes the grid into {@code worksheetTitle}. Report.md section 18:
     * if the worksheet already exists and {@code overwrite} is false, refuse with
     * {@code WORKSHEET_EXISTS} so the UI can prompt "Update existing / Cancel".
     */
    public ExportOutcome export(UUID userId, String spreadsheetId, String worksheetTitle,
                                ReportSheetGrid grid, boolean overwrite) {
        try {
            Sheets sheets = apiFactory.sheets(userId);
            Spreadsheet ss = sheets.spreadsheets().get(spreadsheetId)
                    .setFields("spreadsheetId,spreadsheetUrl,sheets.properties(sheetId,title)")
                    .execute();

            Integer sheetId = ss.getSheets().stream()
                    .map(Sheet::getProperties)
                    .filter(p -> worksheetTitle.equals(p.getTitle()))
                    .map(SheetProperties::getSheetId)
                    .findFirst().orElse(null);

            boolean existed = sheetId != null;
            if (existed && !overwrite) {
                throw new ApiException(org.springframework.http.HttpStatus.CONFLICT, "WORKSHEET_EXISTS",
                        "Worksheet \"" + worksheetTitle + "\" already exists",
                        Map.of("worksheetTitle", worksheetTitle));
            }
            if (!existed) {
                var response = sheets.spreadsheets().batchUpdate(spreadsheetId,
                        new BatchUpdateSpreadsheetRequest().setRequests(List.of(new Request().setAddSheet(
                                new AddSheetRequest().setProperties(new SheetProperties().setTitle(worksheetTitle))))))
                        .execute();
                sheetId = response.getReplies().get(0).getAddSheet().getProperties().getSheetId();
            } else {
                sheets.spreadsheets().values()
                        .clear(spreadsheetId, "'" + worksheetTitle + "'!A1:Z10000",
                                new com.google.api.services.sheets.v4.model.ClearValuesRequest())
                        .execute();
            }

            sheets.spreadsheets().values()
                    .update(spreadsheetId, "'" + worksheetTitle + "'!A1",
                            new ValueRange().setValues(grid.rows()))
                    .setValueInputOption("USER_ENTERED")
                    .execute();

            sheets.spreadsheets().batchUpdate(spreadsheetId,
                    new BatchUpdateSpreadsheetRequest().setRequests(formatting(sheetId, grid)))
                    .execute();

            return new ExportOutcome(spreadsheetId, ss.getSpreadsheetUrl(), worksheetTitle, existed);
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw GoogleErrors.translate("write the report to Google Sheets", e);
        }
    }

    // --- formatting (Report.md section 16 — deliberately minimal) ---

    private static List<Request> formatting(int sheetId, ReportSheetGrid grid) {
        List<Request> reqs = new ArrayList<>();
        reqs.add(boldRow(sheetId, ReportSheetGrid.HEADER_ROW_INDEX));
        reqs.add(boldRow(sheetId, grid.totalRowIndex()));
        reqs.add(border(sheetId, ReportSheetGrid.HEADER_ROW_INDEX, grid.totalRowIndex() + 1));
        reqs.add(numberFormat(sheetId, ReportSheetGrid.HEADER_ROW_INDEX + 1, grid.totalRowIndex() + 1, 6, 10));
        reqs.add(autoResize(sheetId));
        return reqs;
    }

    private static Request boldRow(int sheetId, int rowIndex) {
        return new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange().setSheetId(sheetId)
                        .setStartRowIndex(rowIndex).setEndRowIndex(rowIndex + 1))
                .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                        .setTextFormat(new TextFormat().setBold(true))))
                .setFields("userEnteredFormat.textFormat.bold"));
    }

    private static Request border(int sheetId, int startRow, int endRow) {
        Border line = new Border().setStyle("SOLID");
        return new Request().setUpdateBorders(new UpdateBordersRequest()
                .setRange(new GridRange().setSheetId(sheetId)
                        .setStartRowIndex(startRow).setEndRowIndex(endRow)
                        .setStartColumnIndex(0).setEndColumnIndex(ReportSheetGrid.COLUMNS))
                .setTop(line).setBottom(line).setLeft(line).setRight(line)
                .setInnerHorizontal(line).setInnerVertical(line));
    }

    private static Request numberFormat(int sheetId, int startRow, int endRow, int startCol, int endCol) {
        return new Request().setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange().setSheetId(sheetId)
                        .setStartRowIndex(startRow).setEndRowIndex(endRow)
                        .setStartColumnIndex(startCol).setEndColumnIndex(endCol))
                .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                        .setNumberFormat(new NumberFormat().setType("NUMBER").setPattern("0.##"))))
                .setFields("userEnteredFormat.numberFormat"));
    }

    private static Request autoResize(int sheetId) {
        return new Request().setAutoResizeDimensions(
                new com.google.api.services.sheets.v4.model.AutoResizeDimensionsRequest()
                        .setDimensions(new DimensionRange().setSheetId(sheetId).setDimension("COLUMNS")
                                .setStartIndex(0).setEndIndex(ReportSheetGrid.COLUMNS)));
    }

    public record ExportOutcome(String spreadsheetId, String spreadsheetUrl, String worksheetTitle, boolean updatedExisting) {}
}
