package com.tutormgmt.report.export;

import com.tutormgmt.report.MonthlyReportDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure transform: {@link MonthlyReportDto} -> the exact cell grid of the tutor's working
 * spreadsheet (Report.md sections 3-7, 15). No Google types here — unit tested directly.
 *
 * <pre>
 * Row 1 : {title}                       e.g. "август 26"
 * Row 2 : headers A..J
 * Row 3.. : one row per student
 * Last  : monthly total (G = lesson count, J = amount)
 * </pre>
 */
public final class ReportSheetGrid {

    public static final List<String> HEADERS = List.of(
            "Имя фамилия латиницей", "Номер уч.", "Почта", "Имя родителя", "Isikukood",
            "Предмет", "Количество уроков", "Цена урока", "Сумма", "Итого");

    public static final int COLUMNS = 10;
    public static final int HEADER_ROW_INDEX = 1;   // 0-based

    private final List<List<Object>> rows;
    private final int totalRowIndex;

    private ReportSheetGrid(List<List<Object>> rows, int totalRowIndex) {
        this.rows = rows;
        this.totalRowIndex = totalRowIndex;
    }

    public static ReportSheetGrid of(MonthlyReportDto report) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(row(report.title()));
        rows.add(new ArrayList<>(HEADERS));

        for (MonthlyReportDto.StudentReportRowDto s : report.students()) {
            rows.add(row(
                    nz(s.fullName()),
                    nz(s.studentNumber()),
                    nz(s.email()),
                    nz(s.parentName()),
                    nz(s.isikukood()),
                    nz(s.subject()),
                    s.lessonCount(),
                    s.lessonPrice() == null ? "" : s.lessonPrice(),
                    s.total()));
        }

        int totalRowIndex = rows.size();
        rows.add(row("", "", "", "", "", "",
                report.totalLessons(), "", "", report.totalAmount()));

        return new ReportSheetGrid(rows, totalRowIndex);
    }

    public List<List<Object>> rows() {
        return rows;
    }

    public int rowCount() {
        return rows.size();
    }

    public int totalRowIndex() {
        return totalRowIndex;
    }

    private static List<Object> row(Object... cells) {
        List<Object> out = new ArrayList<>(cells.length);
        for (Object c : cells) {
            out.add(c);
        }
        return out;
    }

    private static Object nz(String s) {
        return s == null ? "" : s;
    }
}
