package com.tutormgmt.integration.google.calendar;

/** A calendar the user can see, for the Settings picker (CLAUDE.md section 47). */
public record CalendarSummaryDto(
        String id,
        String summary,
        String description,
        String accessRole,
        boolean primary,
        boolean writable
) {}
