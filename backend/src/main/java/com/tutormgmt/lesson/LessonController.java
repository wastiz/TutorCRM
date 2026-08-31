package com.tutormgmt.lesson;

import com.tutormgmt.security.AppPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

/** CLAUDE.md section 50. */
@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService service;

    @GetMapping
    public List<LessonDto> list(@AuthenticationPrincipal AppPrincipal principal,
                                @RequestParam(required = false) UUID studentId,
                                @RequestParam(required = false) LessonStatus status,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return service.list(principal.userId(), studentId, status, from, to);
    }

    @GetMapping("/{id}")
    public LessonDto get(@AuthenticationPrincipal AppPrincipal principal, @PathVariable UUID id) {
        return service.get(principal.userId(), id);
    }

    @GetMapping("/student/{studentId}/overview")
    public LessonStudentOverviewDto studentOverview(@AuthenticationPrincipal AppPrincipal principal,
                                                    @PathVariable UUID studentId) {
        return service.studentOverview(principal.userId(), studentId);
    }

    @PostMapping
    public ResponseEntity<LessonDto> create(@AuthenticationPrincipal AppPrincipal principal,
                                            @Valid @RequestBody LessonRequest request) {
        LessonDto created = service.create(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/lessons/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public LessonDto update(@AuthenticationPrincipal AppPrincipal principal,
                            @PathVariable UUID id,
                            @Valid @RequestBody LessonRequest request) {
        return service.update(principal.userId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppPrincipal principal, @PathVariable UUID id) {
        service.delete(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    public LessonDto complete(@AuthenticationPrincipal AppPrincipal principal, @PathVariable UUID id) {
        return service.changeStatus(principal.userId(), id, LessonStatus.COMPLETED);
    }

    @PostMapping("/{id}/cancel")
    public LessonDto cancel(@AuthenticationPrincipal AppPrincipal principal, @PathVariable UUID id) {
        return service.changeStatus(principal.userId(), id, LessonStatus.CANCELLED);
    }

    @PostMapping("/{id}/no-show")
    public LessonDto noShow(@AuthenticationPrincipal AppPrincipal principal, @PathVariable UUID id) {
        return service.changeStatus(principal.userId(), id, LessonStatus.NO_SHOW);
    }

    @PostMapping("/{id}/repeat")
    public List<LessonDto> repeat(@AuthenticationPrincipal AppPrincipal principal,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody RepeatLessonRequest request) {
        return service.repeat(principal.userId(), id, request);
    }

    @PostMapping("/{id}/sync-calendar")
    public LessonDto syncCalendar(@AuthenticationPrincipal AppPrincipal principal, @PathVariable UUID id) {
        return service.syncCalendar(principal.userId(), id);
    }
}
