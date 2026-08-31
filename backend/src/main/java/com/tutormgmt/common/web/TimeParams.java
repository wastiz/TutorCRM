package com.tutormgmt.common.web;

import com.tutormgmt.common.error.ApiException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Lenient query-param time parsing. Accepts a full ISO offset date-time
 * ({@code 2026-08-31T00:00:00+03:00} / {@code ...Z}) or a plain date
 * ({@code 2026-08-31}, treated as start-of-day UTC). Calendar UIs send both.
 */
public final class TimeParams {

    private TimeParams() {}

    public static OffsetDateTime parseOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        try {
            return OffsetDateTime.parse(v);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDate.parse(v).atStartOfDay().atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("INVALID_TIME_PARAM",
                    "Expected an ISO date or date-time, got \"" + value + "\"");
        }
    }
}
