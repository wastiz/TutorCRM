package com.tutormgmt.student;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Create / update payload for a student (CLAUDE.md section 48/49, validation section 54).
 * {@code studentNumber} is server-assigned and therefore not part of the request.
 */
public record StudentRequest(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @Email @Size(max = 320) String email,
        @Size(max = 64) String phone,
        @Size(max = 32) String isikukood,
        @Min(1) @Max(120) Integer age,
        @Min(0) @Max(20) Integer grade,
        @Size(max = 255) String school,
        @Size(max = 255) String subject,
        @Size(max = 4000) String goal,
        @Size(max = 64) String level,
        @Size(max = 4000) String notes,
        LessonFormat lessonFormat,
        @PositiveOrZero BigDecimal lessonPrice,
        StudentStatus status,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 255) String parentName,
        @Size(max = 64) String parentPhone,
        @Email @Size(max = 320) String parentEmail,
        @Size(max = 64) String parentSecondaryPhone,
        @Valid List<ScheduleEntry> schedules,
        /** When true, skip the duplicate check on create (CLAUDE.md section 26). */
        boolean ignoreDuplicates
) {
    public record ScheduleEntry(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            @Min(0) @Max(21) Integer lessonsPerWeek
    ) {}
}
