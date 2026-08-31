package com.tutormgmt.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import com.tutormgmt.config.AppProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class TokenCipherTest {

    private final TokenCipher cipher = new TokenCipher(new AppProperties(
            null, null, new AppProperties.Cors(List.of()),
            new AppProperties.Jwt("x".repeat(32), Duration.ofHours(1), "c", false, "Lax"),
            new AppProperties.Crypto("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")));

    @Test
    void roundTrips() {
        String secret = "ya29.a0AfB_byC-some-google-access-token";
        String enc = cipher.encrypt(secret);

        assertThat(enc).isNotEqualTo(secret);
        assertThat(cipher.decrypt(enc)).isEqualTo(secret);
    }

    @Test
    void producesDifferentCiphertextEachTime() {
        assertThat(cipher.encrypt("same")).isNotEqualTo(cipher.encrypt("same"));
    }

    @Test
    void handlesNull() {
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.decrypt(null)).isNull();
    }
}
