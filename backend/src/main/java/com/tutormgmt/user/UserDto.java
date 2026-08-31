package com.tutormgmt.user;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Instant createdAt,
        Instant updatedAt
) {}
