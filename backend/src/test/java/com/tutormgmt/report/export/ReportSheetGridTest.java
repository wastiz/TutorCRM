package com.tutormgmt.report.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutormgmt.report.MonthlyReportDto;
import com.tutormgmt.report.MonthlyReportDto.StudentReportRowDto;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Report.md sections 3-7, 15 — the grid must match the tutor's working sheet layout. */
class ReportSheetGridTest {

    @Test
    void producesTitleHeaderStudentAndTotalRows() {
        MonthlyReportDto report = new MonthlyReportDto(
                YearMonth.of(2026, 8), "август 26",
                List.of(
                        new StudentReportRowDto("Pavel Kuznetsov", "1", "pavel.kuznetsov@example.com",
                                null, "39709250002", "Эстонский", 1, new BigDecimal("12"), new BigDecimal("12")),
                        new StudentReportRowDto("Артур", "8", null, null, "61701090009", "Эстонский",
                                2, new BigDecimal("18"), new BigDecimal("36"))),
                3, new BigDecimal("48"), List.of(), List.of());

        ReportSheetGrid grid = ReportSheetGrid.of(report);
        List<List<Object>> rows = grid.rows();

        assertThat(rows).hasSize(5); // title + header + 2 students + total
        assertThat(rows.get(0).get(0)).isEqualTo("август 26");
        assertThat(rows.get(1)).containsExactlyElementsOf(ReportSheetGrid.HEADERS);

        // Report.md section 5: Pavel Kuznetsov | 1 | ... | (blank parent) | 39709250002 | Эстонский | 1 | 12 | 12
        List<Object> pavel = rows.get(2);
        assertThat(pavel.get(0)).isEqualTo("Pavel Kuznetsov");
        assertThat(pavel.get(1)).isEqualTo("1");
        assertThat(pavel.get(3)).isEqualTo("");            // missing parent -> empty (section 21)
        assertThat(pavel.get(4)).isEqualTo("39709250002"); // stored as text, leading zeros safe
        assertThat(pavel.get(6)).isEqualTo(1);
        assertThat(pavel.get(8)).isEqualTo(new BigDecimal("12"));

        // total row: G = lesson count, J = amount, H/I blank (section 15: "7 | | | 96")
        List<Object> total = rows.get(grid.totalRowIndex());
        assertThat(total.get(0)).isEqualTo("");
        assertThat(total.get(6)).isEqualTo(3);
        assertThat(total.get(7)).isEqualTo("");
        assertThat(total.get(8)).isEqualTo("");
        assertThat(total.get(9)).isEqualTo(new BigDecimal("48"));
    }

    @Test
    void blankPriceWhenStudentHadMultiplePrices() {
        MonthlyReportDto report = new MonthlyReportDto(
                YearMonth.of(2026, 8), "август 26",
                List.of(new StudentReportRowDto("Multi Price", "2", null, null, null, null,
                        3, null, new BigDecimal("39"))),
                3, new BigDecimal("39"),
                List.of("Multi Price has multiple lesson prices in август 26"), List.of());

        List<Object> row = ReportSheetGrid.of(report).rows().get(2);
        assertThat(row.get(7)).isEqualTo("");                       // price cell blank
        assertThat(row.get(8)).isEqualTo(new BigDecimal("39"));     // total still correct
    }
}
