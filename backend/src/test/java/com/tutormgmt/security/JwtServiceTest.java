package com.tutormgmt.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutormgmt.config.AppProperties;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(props(Duration.ofHours(12)));

    private static AppProperties props(Duration ttl) {
        return new AppProperties(
                "http://localhost:8080",
                "http://localhost:4200",
                new AppProperties.Cors(List.of("http://localhost:4200")),
                new AppProperties.Jwt("test-secret-that-is-definitely-long-enough-32", ttl, "TMS_SESSION", false, "Lax"),
                new AppProperties.Crypto("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="));
    }

    @Test
    void issuesAndParsesRoundTrip() {
        UUID id = UUID.randomUUID();
        String token = jwtService.issue(id, "tutor@example.com", "Jane Tutor");

        AppPrincipal principal = jwtService.parse(token);

        assertThat(principal).isNotNull();
        assertThat(principal.userId()).isEqualTo(id);
        assertThat(principal.email()).isEqualTo("tutor@example.com");
        assertThat(principal.displayName()).isEqualTo("Jane Tutor");
    }

    @Test
    void rejectsGarbage() {
        assertThat(jwtService.parse("not-a-jwt")).isNull();
        assertThat(jwtService.parse(null)).isNull();
        assertThat(jwtService.parse("")).isNull();
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtService shortLived = new JwtService(props(Duration.ofMillis(1)));
        String token = shortLived.issue(UUID.randomUUID(), "x@y.z", "X");
        Thread.sleep(50);
        assertThat(shortLived.parse(token)).isNull();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        AppProperties other = new AppProperties(
                "http://localhost:8080", "http://localhost:4200",
                new AppProperties.Cors(List.of()),
                new AppProperties.Jwt("a-totally-different-secret-key-32-bytes!!", Duration.ofHours(1), "TMS_SESSION", false, "Lax"),
                new AppProperties.Crypto("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="));
        String foreign = new JwtService(other).issue(UUID.randomUUID(), "x@y.z", "X");
        assertThat(jwtService.parse(foreign)).isNull();
    }
}
