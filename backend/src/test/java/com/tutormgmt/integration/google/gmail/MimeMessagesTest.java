package com.tutormgmt.integration.google.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class MimeMessagesTest {

    @Test
    void buildsAUtf8PlainTextMessage() {
        String raw = MimeMessages.build("kid@mail.ee", "tutor@mail.ee", "Урок завтра", "Здравствуйте!");

        assertThat(raw).contains("To: kid@mail.ee");
        assertThat(raw).contains("From: tutor@mail.ee");
        assertThat(raw).contains("Content-Type: text/plain; charset=\"UTF-8\"");
        // non-ASCII subjects must be RFC 2047 encoded
        assertThat(raw).contains("Subject: =?UTF-8?B?");
        assertThat(decodedBody(raw)).isEqualTo("Здравствуйте!");
    }

    @Test
    void asciiSubjectsStayReadable() {
        assertThat(MimeMessages.build("a@b.ee", null, "Lesson tomorrow", "Hi"))
                .contains("Subject: Lesson tomorrow")
                .doesNotContain("From:");
    }

    @Test
    void newlinesInAnAddressCannotForgeExtraHeaders() {
        String raw = MimeMessages.build("a@b.ee\r\nBcc: evil@x.ee", null, "s", "b");

        assertThat(raw.lines().filter(l -> l.startsWith("Bcc:")).count()).isZero();
        assertThat(raw).contains("To: a@b.ee Bcc: evil@x.ee");
    }

    @Test
    void rawIsBase64UrlWithoutPadding() {
        String encoded = MimeMessages.rawBase64Url("a@b.ee", null, "s", "b");

        assertThat(encoded).doesNotContain("+").doesNotContain("/").doesNotContain("=");
        assertThat(new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8))
                .contains("To: a@b.ee");
    }

    private static String decodedBody(String raw) {
        String body = raw.substring(raw.indexOf("\r\n\r\n") + 4).replaceAll("\\s", "");
        return new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
    }
}
