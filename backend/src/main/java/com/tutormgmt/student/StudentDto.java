package com.tutormgmt.student;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Full student read model. */
public record StudentDto(
        UUID id,
        String studentNumber,
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
        StudentStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String parentName,
        String parentPhone,
        String parentEmail,
        String parentSecondaryPhone,
        List<StudentScheduleDto> schedules,
        Instant createdAt,
        Instant updatedAt
) {}
