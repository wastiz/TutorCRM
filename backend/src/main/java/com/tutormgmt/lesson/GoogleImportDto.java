package com.tutormgmt.lesson;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Google Calendar → app import (pull direction). */
public final class GoogleImportDto {

    private GoogleImportDto() {}

    public record Preview(
            boolean enabled,
            OffsetDateTime from,
            OffsetDateTime to,
            List<NewEvent> newEvents,
            List<MovedLesson> movedLessons,
            int alreadyLinked
    ) {}

    /** A calendar event with no lesson yet. */
    public record NewEvent(
            String eventId,
            String calendarId,
            String summary,
            OffsetDateTime start,
            OffsetDateTime end,
            /** Best-guess student from the event title / attendees; may be null. */
            UUID suggestedStudentId,
            String suggestedStudentName,
            BigDecimal suggestedPrice
    ) {}

    /** A lesson whose linked event was moved in Google Calendar. */
    public record MovedLesson(
            UUID lessonId,
            String studentName,
            String eventId,
            OffsetDateTime currentStart,
            OffsetDateTime currentEnd,
            OffsetDateTime newStart,
            OffsetDateTime newEnd
    ) {}

    public record ImportRequest(
            @NotNull OffsetDateTime from,
            @NotNull OffsetDateTime to,
            @NotEmpty List<Item> items,
            boolean updateMoved
    ) {
        public record Item(
                @NotNull String eventId,
                @NotNull UUID studentId,
                BigDecimal price
        ) {}
    }

    public record ImportResult(int imported, int updated) {}
}
