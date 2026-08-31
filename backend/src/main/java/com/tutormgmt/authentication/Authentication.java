package com.tutormgmt.authentication;

import com.tutormgmt.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Links a {@link com.tutormgmt.user.User} to their Google OAuth credentials
 * (CLAUDE.md section 10.2). Tokens are stored encrypted at rest.
 */
@Entity
@Table(name = "authentication")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Authentication extends Auditable {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    /** AES-GCM ciphertext. */
    @Column(name = "access_token", nullable = false, length = 4096)
    private String accessToken;

    /** AES-GCM ciphertext. May be null if Google did not return a refresh token. */
    @Column(name = "refresh_token", length = 4096)
    private String refreshToken;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    /** Space-separated scopes granted on the last authorization. */
    @Column(name = "granted_scopes", length = 2048)
    private String grantedScopes;

    public static Authentication create(UUID userId) {
        Authentication a = new Authentication();
        a.id = UUID.randomUUID();
        a.userId = userId;
        a.provider = AuthProvider.GOOGLE;
        return a;
    }
}
