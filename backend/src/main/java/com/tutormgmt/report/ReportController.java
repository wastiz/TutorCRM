package com.tutormgmt.report;

import com.tutormgmt.security.AppPrincipal;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CLAUDE.md section 51, Report.md. */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    public MonthlyReportDto monthly(@AuthenticationPrincipal AppPrincipal principal,
                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return reportService.generate(principal.userId(), month);
    }
}
