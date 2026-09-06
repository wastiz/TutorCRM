package com.tutormgmt.security;

import com.tutormgmt.authentication.AuthenticationService;
import com.tutormgmt.config.AppProperties;
import com.tutormgmt.user.User;
import com.tutormgmt.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * After Google finishes the OAuth dance:
 * <ol>
 *   <li>upsert the {@link User} from the OIDC claims,</li>
 *   <li>persist the Google access/refresh tokens (encrypted),</li>
 *   <li>issue the app session JWT as an http-only cookie,</li>
 *   <li>redirect the browser back to the Angular SPA.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final SessionCookies sessionCookies;
    private final AppProperties props;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OidcUser oidcUser = (OidcUser) oauthToken.getPrincipal();

        String googleSub = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String firstName = oidcUser.getGivenName();
        String lastName = oidcUser.getFamilyName();

        User user = userService.upsertFromGoogle(googleSub, email, firstName, lastName);

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
        if (client != null && client.getAccessToken() != null) {
            String access = client.getAccessToken().getTokenValue();
            String refresh = client.getRefreshToken() == null ? null : client.getRefreshToken().getTokenValue();
            Instant expiresAt = client.getAccessToken().getExpiresAt();
            String scopes = String.join(" ", client.getAccessToken().getScopes());
            authenticationService.storeGoogleTokens(user.getId(), access, refresh, expiresAt, scopes);
        } else {
            log.warn("No authorized client / access token available after login for user {}", user.getId());
        }

        response.addHeader("Set-Cookie", sessionCookies.issue(user).toString());
        getRedirectStrategy().sendRedirect(request, response, props.webBaseUrl() + "/auth/callback");
    }
}
