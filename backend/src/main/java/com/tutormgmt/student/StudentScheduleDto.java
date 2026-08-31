package com.tutormgmt.student;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record StudentScheduleDto(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer lessonsPerWeek
) {}
