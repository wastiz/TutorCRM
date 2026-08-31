package com.tutormgmt.authentication;

import java.time.Instant;
import java.util.List;

/** Google-connection status shown on the Settings page (never carries raw tokens). */
public record AuthenticationDto(
        boolean connected,
        String provider,
        Instant accessTokenExpiresAt,
        List<String> grantedScopes,
        Instant connectedAt,
        Instant updatedAt
) {
    public static AuthenticationDto disconnected() {
        return new AuthenticationDto(false, null, null, List.of(), null, null);
    }
}
