package com.tutormgmt.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Wire models of the e-mail slice. */
public final class EmailDto {

    private EmailDto() {
    }

    public record TemplateDto(
            UUID id,
            String name,
            EmailTemplateKind kind,
            String subject,
            String body,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record TemplateRequest(
            @NotBlank @Size(max = 255) String name,
            EmailTemplateKind kind,
            @NotBlank @Size(max = 500) String subject,
            @NotBlank @Size(max = 20_000) String body
    ) {}

    /** A placeholder the tutor may use, shown next to the template editor. */
    public record PlaceholderDto(String name, String description) {}

    /**
     * What to render / send. Either {@code templateId} or an ad-hoc {@code subject}+{@code body}.
     * {@code to} defaults to the student's e-mail (falling back to the parent's).
     */
    public record SendRequest(
            UUID templateId,
            @NotNull UUID studentId,
            UUID lessonId,
            @Email @Size(max = 320) String to,
            @Size(max = 500) String subject,
            @Size(max = 20_000) String body
    ) {}

    /** Exactly what would leave the mailbox, before the tutor confirms. */
    public record PreviewDto(
            String to,
            String subject,
            String body,
            List<String> unresolvedPlaceholders,
            List<String> warnings
    ) {}

    public record SendResultDto(String to, String subject, String providerMessageId, Instant sentAt) {}
}
