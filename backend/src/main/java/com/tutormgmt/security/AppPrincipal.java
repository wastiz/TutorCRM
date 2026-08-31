package com.tutormgmt.security;

import java.util.UUID;

/**
 * The authenticated tutor, exposed to controllers via {@code @AuthenticationPrincipal}.
 */
public record AppPrincipal(UUID userId, String email, String displayName) {
}
