package com.tutormgmt.student.importer;

import com.tutormgmt.student.LessonFormat;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import lombok.Builder;

/**
 * Structured result of parsing a raw message. Field names mirror
 * {@link com.tutormgmt.student.StudentRequest} so the SPA can drop it straight into the edit form.
 */
@Builder
public record ImportedStudentDto(
        String firstName,
        String lastName,
        String email,
        String phone,
        String isikukood,
        Integer age,
        Integer grade,
        String school,
        String subject,
        String goal,
        String level,
        String notes,
        LessonFormat lessonFormat,
        BigDecimal lessonPrice,
        String parentName,
        String parentPhone,
        String parentEmail,
        String parentSecondaryPhone,
        String parentIsikukood,
        String telegram,
        List<DayOfWeek> preferredDays,
        String preferredTimeFrom,
        String preferredTimeTo,
        /** Original time token, kept when it could not be parsed (CLAUDE.md section 22). */
        String preferredTimeRaw,
        Integer lessonsPerWeek,
        List<ScheduleEntryDto> schedules
) {
    public record ScheduleEntryDto(
            DayOfWeek dayOfWeek,
            String startTime,
            String endTime,
            Integer lessonsPerWeek
    ) {}
}
