package com.tutormgmt.authentication;

import com.tutormgmt.common.error.ApiException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the Google OAuth credential lifecycle for a user (CLAUDE.md section 10.2, 28).
 * Tokens are encrypted before they touch the database and are never logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationRepository repository;
    private final TokenCipher cipher;

    @Transactional
    public void storeGoogleTokens(UUID userId, String accessToken, String refreshToken,
                                  Instant expiresAt, String scopes) {
        Authentication auth = repository.findByUserId(userId).orElseGet(() -> Authentication.create(userId));
        auth.setAccessToken(cipher.encrypt(accessToken));
        // Google only returns a refresh token on the first consent; keep the existing one otherwise.
        if (refreshToken != null && !refreshToken.isBlank()) {
            auth.setRefreshToken(cipher.encrypt(refreshToken));
        }
        auth.setAccessTokenExpiresAt(expiresAt);
        if (scopes != null && !scopes.isBlank()) {
            auth.setGrantedScopes(scopes);
        }
        repository.save(auth);
        log.info("Stored Google tokens for user {} (refreshToken present: {})", userId, refreshToken != null);
    }

    @Transactional
    public void updateAccessToken(UUID userId, String accessToken, Instant expiresAt) {
        Authentication auth = requireConnection(userId);
        auth.setAccessToken(cipher.encrypt(accessToken));
        auth.setAccessTokenExpiresAt(expiresAt);
        repository.save(auth);
    }

    @Transactional(readOnly = true)
    public boolean isConnected(UUID userId) {
        return repository.existsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public AuthenticationDto getStatus(UUID userId) {
        return repository.findByUserId(userId)
                .map(a -> new AuthenticationDto(
                        true,
                        a.getProvider().name(),
                        a.getAccessTokenExpiresAt(),
                        a.getGrantedScopes() == null ? List.of() : Arrays.asList(a.getGrantedScopes().split(" ")),
                        a.getCreatedAt(),
                        a.getUpdatedAt()))
                .orElseGet(AuthenticationDto::disconnected);
    }

    /** For the integration layer only. */
    @Transactional(readOnly = true)
    public GoogleTokens getDecryptedTokens(UUID userId) {
        Authentication a = requireConnection(userId);
        return new GoogleTokens(
                cipher.decrypt(a.getAccessToken()),
                cipher.decrypt(a.getRefreshToken()),
                a.getAccessTokenExpiresAt());
    }

    @Transactional
    public void disconnect(UUID userId) {
        repository.deleteByUserId(userId);
        log.info("Disconnected Google for user {}", userId);
    }

    private Authentication requireConnection(UUID userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> ApiException.badRequest("GOOGLE_NOT_CONNECTED",
                        "Google account is not connected"));
    }

    public record GoogleTokens(String accessToken, String refreshToken, Instant expiresAt) {}
}
