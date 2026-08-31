package com.tutormgmt.student.importer;

/**
 * Turns a raw Messenger message into a structured, validated preview.
 *
 * <p>Deterministic by design (CLAUDE.md section 25). The interface exists so an
 * {@code AiStudentImportParser} can be added later without touching callers.
 */
public interface StudentImportParser {

    StudentImportPreviewDto parse(String rawText);
}
