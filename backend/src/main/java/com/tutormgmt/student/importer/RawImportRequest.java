package com.tutormgmt.student.importer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** CLAUDE.md section 49 — {@code POST /api/students/import/parse}. */
public record RawImportRequest(
        @NotNull @Size(max = 20_000) String rawText
) {}
