package com.tutormgmt.student.importer;

import com.tutormgmt.security.AppPrincipal;
import com.tutormgmt.student.StudentDto;
import com.tutormgmt.student.StudentRequest;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CLAUDE.md section 49. */
@RestController
@RequestMapping("/api/students/import")
@RequiredArgsConstructor
public class StudentImportController {

    private final StudentImportService service;

    /** Parse only — never persists (CLAUDE.md section 15). */
    @PostMapping("/parse")
    public StudentImportPreviewDto parse(@Valid @RequestBody RawImportRequest request) {
        return service.preview(request.rawText());
    }

    /** Create the (possibly user-corrected) student after the Preview step. */
    @PostMapping("/create")
    public ResponseEntity<StudentDto> create(@AuthenticationPrincipal AppPrincipal principal,
                                             @Valid @RequestBody StudentRequest request) {
        StudentDto created = service.create(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/students/" + created.id())).body(created);
    }
}
