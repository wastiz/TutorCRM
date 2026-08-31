package com.tutormgmt.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Standard error body (CLAUDE.md section 53).
 *
 * <pre>
 * {
 *   "code": "STUDENT_NOT_FOUND",
 *   "message": "Student not found",
 *   "timestamp": "...",
 *   "path": "..."
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        OffsetDateTime timestamp,
        String path,
        List<FieldViolation> errors
) {
    public record FieldViolation(String field, String message) {}

    public static ApiError of(String code, String message, String path) {
        return new ApiError(code, message, OffsetDateTime.now(), path, null);
    }

    public static ApiError of(String code, String message, String path, List<FieldViolation> errors) {
        return new ApiError(code, message, OffsetDateTime.now(), path, errors);
    }
}
