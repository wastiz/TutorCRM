package com.tutormgmt.lesson;

import com.tutormgmt.common.web.TimeParams;
import com.tutormgmt.security.AppPrincipal;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Google Calendar → app import (pull). Companion to the app → Google sync in {@link LessonService}. */
@RestController
@RequestMapping("/api/lessons/google")
@RequiredArgsConstructor
public class LessonImportController {

    private final LessonImportService service;

    @GetMapping("/preview")
    public GoogleImportDto.Preview preview(@AuthenticationPrincipal AppPrincipal principal,
                                           @RequestParam(required = false) String from,
                                           @RequestParam(required = false) String to) {
        OffsetDateTime f = from != null ? TimeParams.parseOrNull(from) : OffsetDateTime.now().minusDays(14);
        OffsetDateTime t = to != null ? TimeParams.parseOrNull(to) : OffsetDateTime.now().plusDays(60);
        return service.preview(principal.userId(), f, t);
    }

    @PostMapping("/import")
    public GoogleImportDto.ImportResult importSelected(@AuthenticationPrincipal AppPrincipal principal,
                                                       @Valid @RequestBody GoogleImportDto.ImportRequest request) {
        return service.importSelected(principal.userId(), request);
    }
}
