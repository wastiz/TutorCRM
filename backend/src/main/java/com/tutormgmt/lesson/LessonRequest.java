package com.tutormgmt.lesson;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Create / update payload for a lesson (CLAUDE.md section 50, validation section 54). */
public record LessonRequest(
        @NotNull UUID studentId,
        @NotNull OffsetDateTime startTime,
        @NotNull OffsetDateTime endTime,
        /** Optional on create — defaults to the student's current lesson price. */
        @PositiveOrZero BigDecimal price,
        LessonStatus status,
        @Size(max = 4000) String notes
) {}
