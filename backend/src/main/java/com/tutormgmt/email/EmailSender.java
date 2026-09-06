package com.tutormgmt.email;

import java.util.UUID;

/**
 * Port for actually delivering an e-mail. The Gmail implementation lives in
 * {@code integration.google.gmail} so this slice never imports Google classes
 * (CLAUDE.md section 63) and stays unit-testable.
 */
public interface EmailSender {

    /** True when the tutor can send right now (Google connected with the send scope granted). */
    boolean enabledFor(UUID userId);

    /** Sends the message and returns the provider's message id. */
    String send(UUID userId, Message message);

    record Message(String to, String subject, String body) {}
}
