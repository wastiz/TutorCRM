package com.tutormgmt.report;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;

public record ReportExportRequest(
        @NotNull @JsonFormat(pattern = "yyyy-MM") YearMonth month,
        /** Report.md section 18 — confirm updating an already-existing worksheet. */
        boolean overwrite
) {}
