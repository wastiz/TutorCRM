package com.tutormgmt.integration.google;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.tutormgmt.common.error.ApiException;
import java.io.IOException;

/** Turns Google API failures into user-facing {@link ApiException}s (CLAUDE.md section 31). */
public final class GoogleErrors {

    private GoogleErrors() {}

    public static ApiException translate(String what, IOException e) {
        if (e instanceof GoogleJsonResponseException g) {
            int code = g.getStatusCode();
            String detail = g.getDetails() != null && g.getDetails().getMessage() != null
                    ? g.getDetails().getMessage() : g.getStatusMessage();
            return switch (code) {
                case 401 -> new ApiException(org.springframework.http.HttpStatus.CONFLICT,
                        "GOOGLE_REAUTH_REQUIRED", "Google authorization expired — reconnect Google");
                case 403 -> new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,
                        "GOOGLE_PERMISSION_DENIED",
                        "You don't have permission to " + what + " (" + detail + ")");
                case 404 -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "GOOGLE_RESOURCE_NOT_FOUND", "The Google resource for " + what + " was not found");
                case 429 -> new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                        "GOOGLE_RATE_LIMITED", "Google rate limit hit — try again shortly");
                default -> new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                        "GOOGLE_API_ERROR", "Google API error while trying to " + what + ": " + detail);
            };
        }
        return new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                "GOOGLE_API_UNAVAILABLE", "Could not reach Google to " + what, e);
    }
}
