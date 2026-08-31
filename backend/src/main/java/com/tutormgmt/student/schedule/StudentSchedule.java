package com.tutormgmt.student.schedule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One preferred lesson slot for a student (CLAUDE.md section 27).
 * "ПЯТНИЦА, ВОСКРЕСЕНЬЕ" is normalised into one row per day — never a single string.
 */
@Entity
@Table(name = "student_schedule")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentSchedule {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Populated by the owning {@link com.tutormgmt.student.Student}'s join column. */
    @Column(name = "student_id", nullable = false, insertable = false, updatable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 16)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "lessons_per_week")
    private Integer lessonsPerWeek;

    public static StudentSchedule of(UUID studentId, DayOfWeek day, LocalTime from, LocalTime to, Integer perWeek) {
        StudentSchedule s = new StudentSchedule();
        s.id = UUID.randomUUID();
        s.studentId = studentId;
        s.dayOfWeek = day;
        s.startTime = from;
        s.endTime = to;
        s.lessonsPerWeek = perWeek;
        return s;
    }
}
