package com.tutormgmt.dev;

import java.util.UUID;

/** What a seeding run produced — returned by {@code POST /dev/seed}. */
public record DevSeedResult(
        boolean seeded,
        UUID userId,
        String email,
        long students,
        long lessons,
        int emailTemplates
) {}
