package com.tutormgmt.email;

import com.tutormgmt.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A reusable e-mail the tutor sends to a student or parent.
 *
 * <p>Templates are per tutor and stored in the database (not in files) so they can be edited from
 * the Settings page without a deploy. Both subject and body may contain {@code {{placeholders}}}
 * resolved by {@link EmailTemplateRenderer}.
 */
@Entity
@Table(name = "email_template", indexes = {
        @Index(name = "ix_email_template_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailTemplate extends Auditable {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    /** What the template is about — drives which context the send screen offers. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EmailTemplateKind kind = EmailTemplateKind.GENERAL;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, length = 20000)
    private String body;

    public static EmailTemplate create(UUID userId) {
        EmailTemplate t = new EmailTemplate();
        t.id = UUID.randomUUID();
        t.userId = userId;
        return t;
    }
}
