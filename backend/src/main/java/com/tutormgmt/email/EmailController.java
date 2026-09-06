package com.tutormgmt.email;

import com.tutormgmt.security.AppPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

/** Template management and manual sending. */
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService service;

    @GetMapping("/templates")
    public List<EmailDto.TemplateDto> templates(@AuthenticationPrincipal AppPrincipal principal) {
        return service.list(principal.userId());
    }

    @GetMapping("/templates/{id}")
    public EmailDto.TemplateDto template(@AuthenticationPrincipal AppPrincipal principal,
                                         @PathVariable UUID id) {
        return service.get(principal.userId(), id);
    }

    @PostMapping("/templates")
    public ResponseEntity<EmailDto.TemplateDto> create(@AuthenticationPrincipal AppPrincipal principal,
                                                       @Valid @RequestBody EmailDto.TemplateRequest request) {
        EmailDto.TemplateDto created = service.create(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/email/templates/" + created.id())).body(created);
    }

    @PutMapping("/templates/{id}")
    public EmailDto.TemplateDto update(@AuthenticationPrincipal AppPrincipal principal,
                                       @PathVariable UUID id,
                                       @Valid @RequestBody EmailDto.TemplateRequest request) {
        return service.update(principal.userId(), id, request);
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppPrincipal principal,
                                       @PathVariable UUID id) {
        service.delete(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    /** The variables a template may use — rendered next to the editor. */
    @GetMapping("/placeholders")
    public List<EmailDto.PlaceholderDto> placeholders() {
        return service.placeholders();
    }

    /** Render without sending (CLAUDE.md section 15 style: always preview first). */
    @PostMapping("/preview")
    public EmailDto.PreviewDto preview(@AuthenticationPrincipal AppPrincipal principal,
                                       @Valid @RequestBody EmailDto.SendRequest request) {
        return service.preview(principal.userId(), request);
    }

    @PostMapping("/send")
    public EmailDto.SendResultDto send(@AuthenticationPrincipal AppPrincipal principal,
                                       @Valid @RequestBody EmailDto.SendRequest request) {
        return service.send(principal.userId(), request);
    }
}
