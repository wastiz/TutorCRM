package com.tutormgmt.student;

import jakarta.validation.constraints.Size;

/** Optional body of {@code POST /api/students/{id}/archive} — why the student stopped. */
public record ArchiveStudentRequest(@Size(max = 1000) String leaveReason) {}
