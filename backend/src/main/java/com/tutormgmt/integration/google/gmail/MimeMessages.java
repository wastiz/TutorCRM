package com.tutormgmt.integration.google.gmail;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Builds the RFC 5322 message Gmail's {@code messages.send} expects.
 *
 * <p>Written by hand rather than pulling in a mail stack: we only ever send one plain-text UTF-8
 * body, and this keeps the SMTP dependencies out of an app that speaks to Gmail over HTTPS.
 * The subject is encoded per RFC 2047 so Cyrillic subjects survive.
 */
final class MimeMessages {

    private MimeMessages() {
    }

    /** The whole message, base64url-encoded the way the Gmail API wants it. */
    static String rawBase64Url(String to, String from, String subject, String body) {
        String message = build(to, from, subject, body);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(message.getBytes(StandardCharsets.UTF_8));
    }

    static String build(String to, String from, String subject, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append("To: ").append(headerValue(to)).append("\r\n");
        if (from != null && !from.isBlank()) {
            sb.append("From: ").append(headerValue(from)).append("\r\n");
        }
        sb.append("Subject: ").append(encodeHeader(subject)).append("\r\n");
        sb.append("MIME-Version: 1.0\r\n");
        sb.append("Content-Type: text/plain; charset=\"UTF-8\"\r\n");
        sb.append("Content-Transfer-Encoding: base64\r\n");
        sb.append("\r\n");
        sb.append(wrap(Base64.getEncoder().encodeToString(
                (body == null ? "" : body).getBytes(StandardCharsets.UTF_8))));
        return sb.toString();
    }

    /** RFC 2047 "encoded-word" — needed for anything outside US-ASCII. */
    private static String encodeHeader(String value) {
        String v = value == null ? "" : value;
        if (v.chars().allMatch(c -> c >= 32 && c < 127)) {
            return v;
        }
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(v.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    /** Header injection guard: a newline in an address would forge extra headers. */
    private static String headerValue(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
    }

    /** Base64 bodies must be split into lines of at most 76 characters. */
    private static String wrap(String base64) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < base64.length(); i += 76) {
            sb.append(base64, i, Math.min(i + 76, base64.length())).append("\r\n");
        }
        return sb.toString();
    }
}
