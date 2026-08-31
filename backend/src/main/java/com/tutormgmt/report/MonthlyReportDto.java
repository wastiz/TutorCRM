package com.tutormgmt.report;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Report.md section 19. Preview + export payload. */
public record MonthlyReportDto(
        @JsonFormat(pattern = "yyyy-MM") YearMonth month,
        /** Sheet title, e.g. "август 26" (Report.md section 3). */
        String title,
        List<StudentReportRowDto> students,
        int totalLessons,
        BigDecimal totalAmount,
        /** Non-blocking issues the user confirms before export (Report.md section 22). */
        List<String> warnings,
        /** Blocking issues — export is refused while any are present (Report.md section 22). */
        List<String> errors
) {
    public boolean exportable() {
        return errors.isEmpty();
    }

    public record StudentReportRowDto(
            String fullName,
            String studentNumber,
            String email,
            String parentName,
            String isikukood,
            String subject,
            int lessonCount,
            /** Null when the student had several different lesson prices in the month. */
            BigDecimal lessonPrice,
            BigDecimal total
    ) {}
}
