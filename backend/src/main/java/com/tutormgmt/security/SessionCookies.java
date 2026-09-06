package com.tutormgmt.security;

import com.tutormgmt.config.AppProperties;
import com.tutormgmt.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Builds the app session cookie. Shared by the Google login handler, the logout endpoint and
 * the local-only dev sign-in, so all three agree on name, flags and lifetime.
 */
@Component
@RequiredArgsConstructor
public class SessionCookies {

    private final JwtService jwtService;
    private final AppProperties props;

    /** A signed session for this user, valid for the configured JWT TTL. */
    public ResponseCookie issue(User user) {
        String jwt = jwtService.issue(user.getId(), user.getEmail(), displayName(user));
        return build(jwt, jwtService.ttl().getSeconds());
    }

    /** Same cookie with a zero max-age — used on logout. */
    public ResponseCookie clearing() {
        return build("", 0);
    }

    private ResponseCookie build(String value, long maxAgeSeconds) {
        return ResponseCookie.from(props.jwt().cookieName(), value)
                .httpOnly(true)
                .secure(props.jwt().cookieSecure())
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(props.jwt().cookieSameSite())
                .build();
    }

    private static String displayName(User user) {
        if (!StringUtils.hasText(user.getFirstName())) {
            return user.getEmail();
        }
        return (user.getFirstName() + " "
                + (StringUtils.hasText(user.getLastName()) ? user.getLastName() : "")).trim();
    }
}
