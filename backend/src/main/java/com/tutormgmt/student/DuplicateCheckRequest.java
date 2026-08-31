package com.tutormgmt.student;

/** Lightweight lookup used before creating a student (CLAUDE.md section 26). */
public record DuplicateCheckRequest(
        String email,
        String phone,
        String firstName,
        String lastName
) {}
