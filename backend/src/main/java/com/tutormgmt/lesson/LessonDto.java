package com.tutormgmt.lesson;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LessonDto(
        UUID id,
        UUID studentId,
        String studentName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        BigDecimal price,
        LessonStatus status,
        String notes,
        String googleCalendarId,
        String googleCalendarEventId,
        CalendarSyncStatus calendarSyncStatus,
        Instant createdAt,
        Instant updatedAt
) {}
