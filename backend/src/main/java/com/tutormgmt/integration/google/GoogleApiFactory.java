package com.tutormgmt.integration.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.tutormgmt.authentication.AuthenticationService;
import com.tutormgmt.common.error.ApiException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

/**
 * Builds Google API clients authorised as the current user (CLAUDE.md section 28-31).
 * Access tokens are refreshed transparently and the fresh token is persisted.
 */
@Slf4j
@Component
public class GoogleApiFactory {

    static final String APPLICATION_NAME = "tutor-management";
    private static final GsonFactory JSON = GsonFactory.getDefaultInstance();

    private final AuthenticationService authenticationService;
    private final ClientRegistration google;
    private final HttpTransport transport;

    public GoogleApiFactory(AuthenticationService authenticationService,
                            ClientRegistrationRepository registrations) {
        this.authenticationService = authenticationService;
        this.google = registrations.findByRegistrationId("google");
        try {
            this.transport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Cannot initialise Google HTTP transport", e);
        }
    }

    public Calendar calendar(UUID userId) {
        return new Calendar.Builder(transport, JSON, adapter(userId))
                .setApplicationName(APPLICATION_NAME).build();
    }

    public Sheets sheets(UUID userId) {
        return new Sheets.Builder(transport, JSON, adapter(userId))
                .setApplicationName(APPLICATION_NAME).build();
    }

    public Gmail gmail(UUID userId) {
        return new Gmail.Builder(transport, JSON, adapter(userId))
                .setApplicationName(APPLICATION_NAME).build();
    }

    public Drive drive(UUID userId) {
        return new Drive.Builder(transport, JSON, adapter(userId))
                .setApplicationName(APPLICATION_NAME).build();
    }

    /** Scopes we ask for at login (kept here for reference / re-consent checks). */
    public static List<String> requiredScopes() {
        return List.of("openid", "email", "profile",
                CalendarScopes.CALENDAR, SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_METADATA_READONLY,
                GmailScopes.GMAIL_SEND);
    }

    private HttpCredentialsAdapter adapter(UUID userId) {
        return new HttpCredentialsAdapter(userCredentials(userId));
    }

    private UserCredentials userCredentials(UUID userId) {
        AuthenticationService.GoogleTokens tokens = authenticationService.getDecryptedTokens(userId);
        if (tokens.refreshToken() == null || tokens.refreshToken().isBlank()) {
            throw new ApiException(org.springframework.http.HttpStatus.CONFLICT, "GOOGLE_REAUTH_REQUIRED",
                    "Google needs to be reconnected (no refresh token on file)");
        }
        UserCredentials creds = UserCredentials.newBuilder()
                .setClientId(google.getClientId())
                .setClientSecret(google.getClientSecret())
                .setRefreshToken(tokens.refreshToken())
                .build();
        if (tokens.accessToken() != null && tokens.expiresAt() != null) {
            creds = creds.toBuilder()
                    .setAccessToken(new AccessToken(tokens.accessToken(), Date.from(tokens.expiresAt())))
                    .build();
        }
        try {
            creds.refreshIfExpired();
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, "GOOGLE_TOKEN_REFRESH_FAILED",
                    "Could not refresh Google access — try reconnecting Google", e);
        }
        AccessToken current = creds.getAccessToken();
        if (current != null && !current.getTokenValue().equals(tokens.accessToken())) {
            authenticationService.updateAccessToken(userId, current.getTokenValue(),
                    current.getExpirationTime() == null ? null : current.getExpirationTime().toInstant());
        }
        return creds;
    }
}
