package com.tutormgmt.student.importer;

import com.tutormgmt.student.StudentDto;
import com.tutormgmt.student.StudentRequest;
import com.tutormgmt.student.StudentService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates raw-message import (CLAUDE.md section 17). The parser only turns
 * text into a DTO; persistence and validation stay in {@link StudentService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentImportService {

    private final StudentImportParser parser;
    private final StudentService studentService;

    public StudentImportPreviewDto preview(String rawText) {
        StudentImportPreviewDto preview = parser.parse(rawText);
        log.info("Parsed student import: {} field(s), {} warning(s)",
                preview.fieldCount(), preview.warnings().size());
        return preview;
    }

    public StudentDto create(UUID userId, StudentRequest request) {
        return studentService.create(userId, request);
    }
}
