package com.tutormgmt.dev;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the local development conveniences ({@code app.dev.*}).
 *
 * <p>Everything in this package is additionally gated on the {@code local} Spring profile,
 * so none of it can be switched on by an environment variable in production.
 */
@ConfigurationProperties(prefix = "app.dev")
public record DevProperties(
        /** Seed demo data on startup when the demo tutor has none yet. */
        Boolean seedOnStartup,
        /** Wipe the demo tutor's data and seed it again on every startup. */
        Boolean resetOnStartup,
        /** Identity of the demo tutor the seeded data belongs to. */
        String email,
        String firstName,
        String lastName
) {

    /** Marks the seeded user so it can be found (and wiped) again — never a real Google subject. */
    public static final String DEV_GOOGLE_SUBJECT = "dev-local-seed";

    public DevProperties {
        seedOnStartup = seedOnStartup == null || seedOnStartup;
        resetOnStartup = resetOnStartup != null && resetOnStartup;
        email = email == null || email.isBlank() ? "dev.tutor@example.com" : email;
        firstName = firstName == null || firstName.isBlank() ? "Dev" : firstName;
        lastName = lastName == null || lastName.isBlank() ? "Tutor" : lastName;
    }
}
