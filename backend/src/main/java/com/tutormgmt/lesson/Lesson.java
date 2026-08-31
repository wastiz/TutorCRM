package com.tutormgmt.lesson;

import com.tutormgmt.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single planned or completed lesson (CLAUDE.md section 10.4).
 *
 * <p>{@code price} is copied onto the lesson at creation time and is never rewritten
 * from the student's current price — historical reports must stay stable (section 10.4).
 */
@Entity
@Table(name = "lesson", indexes = {
        @Index(name = "ix_lesson_user_start", columnList = "user_id,start_time"),
        @Index(name = "ix_lesson_student", columnList = "student_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lesson extends Auditable {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LessonStatus status = LessonStatus.PLANNED;

    @Column(length = 4000)
    private String notes;

    @Column(name = "google_calendar_id")
    private String googleCalendarId;

    @Column(name = "google_calendar_event_id")
    private String googleCalendarEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "calendar_sync_status", nullable = false, length = 16)
    private CalendarSyncStatus calendarSyncStatus = CalendarSyncStatus.PENDING;

    public static Lesson create(UUID userId, UUID studentId) {
        Lesson l = new Lesson();
        l.id = UUID.randomUUID();
        l.userId = userId;
        l.studentId = studentId;
        l.status = LessonStatus.PLANNED;
        l.calendarSyncStatus = CalendarSyncStatus.PENDING;
        return l;
    }
}
