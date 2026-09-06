package com.tutormgmt.integration.google.gmail;

import com.google.api.services.gmail.Gmail;
import com.tutormgmt.authentication.AuthenticationService;
import com.tutormgmt.common.error.ApiException;
import com.tutormgmt.email.EmailSender;
import com.tutormgmt.integration.google.GoogleApiFactory;
import com.tutormgmt.integration.google.GoogleErrors;
import com.tutormgmt.user.User;
import com.tutormgmt.user.UserRepository;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Sends through the tutor's own Gmail account (owner decision, 2026-09-05), so replies come back
 * to them and no separate SMTP credentials are needed. Requires the {@code gmail.send} scope —
 * a tutor who connected Google before that scope existed has to reconnect once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GmailEmailSender implements EmailSender {

    /** Gmail's alias for the authenticated account. */
    private static final String ME = "me";

    private final GoogleApiFactory apiFactory;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    @Override
    public boolean enabledFor(UUID userId) {
        return authenticationService.isConnected(userId);
    }

    @Override
    public String send(UUID userId, EmailSender.Message message) {
        String from = userRepository.findById(userId).map(User::getEmail).orElse(null);
        // fully qualified: Gmail's Message would clash with the port's own Message record
        com.google.api.services.gmail.model.Message payload =
                new com.google.api.services.gmail.model.Message().setRaw(MimeMessages.rawBase64Url(
                        message.to(), from, message.subject(), message.body()));
        try {
            Gmail gmail = apiFactory.gmail(userId);
            return gmail.users().messages().send(ME, payload).execute().getId();
        } catch (IOException e) {
            throw GoogleErrors.translate("send the e-mail", e);
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, "GMAIL_SEND_FAILED",
                    "Could not send the e-mail through Gmail", e);
        }
    }
}
