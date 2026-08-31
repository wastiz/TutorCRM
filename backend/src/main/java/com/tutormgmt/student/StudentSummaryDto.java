package com.tutormgmt.student;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Row model for the Students table (CLAUDE.md section 39). */
public record StudentSummaryDto(
        UUID id,
        String studentNumber,
        String fullName,
        String subject,
        Integer grade,
        LessonFormat lessonFormat,
        BigDecimal lessonPrice,
        StudentStatus status,
        String email,
        String phone,
        /** Populated from Lesson data (Phase 4); null until then. */
        OffsetDateTime nextLessonAt
) {}
