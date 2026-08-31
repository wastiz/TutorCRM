package com.tutormgmt.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the {@code app.*} configuration tree. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String apiBaseUrl,
        String webBaseUrl,
        Cors cors,
        Jwt jwt,
        Crypto crypto
) {
    public record Cors(List<String> allowedOrigins) {}

    public record Jwt(
            String secret,
            Duration ttl,
            String cookieName,
            boolean cookieSecure,
            String cookieSameSite
    ) {}

    public record Crypto(String tokenEncryptionKey) {}
}
