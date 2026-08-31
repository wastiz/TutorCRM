package com.tutormgmt.student;

import com.tutormgmt.security.AppPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService service;

    @GetMapping
    public List<StudentSummaryDto> list(@AuthenticationPrincipal AppPrincipal principal,
                                        @RequestParam(required = false) String search,
                                        @RequestParam(required = false) StudentStatus status) {
        return service.list(principal.userId(), search, status);
    }

    @GetMapping("/{id}")
    public StudentDto get(@AuthenticationPrincipal AppPrincipal principal, @PathVariable UUID id) {
        return service.get(principal.userId(), id);
    }

    @PostMapping
    public ResponseEntity<StudentDto> create(@AuthenticationPrincipal AppPrincipal principal,
                                             @Valid @RequestBody StudentRequest request) {
        StudentDto created = service.create(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/students/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public StudentDto update(@AuthenticationPrincipal AppPrincipal principal,
                             @PathVariable UUID id,
                             @Valid @RequestBody StudentRequest request) {
        return service.update(principal.userId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppPrincipal principal, @PathVariable UUID id) {
        service.delete(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/archive")
    public StudentDto archive(@AuthenticationPrincipal AppPrincipal principal, @PathVariable UUID id) {
        return service.archive(principal.userId(), id);
    }

    @PostMapping("/duplicates")
    public Map<String, List<StudentSummaryDto>> duplicates(@AuthenticationPrincipal AppPrincipal principal,
                                                           @RequestBody DuplicateCheckRequest request) {
        return Map.of("duplicates", service.findDuplicates(principal.userId(), request));
    }
}
