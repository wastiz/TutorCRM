package com.tutormgmt.student.importer;

import java.util.List;

/** CLAUDE.md section 16/49 — what the Preview screen renders. */
public record StudentImportPreviewDto(
        ImportedStudentDto student,
        List<String> warnings,
        int fieldCount,
        String rawText
) {}
