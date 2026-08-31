package com.tutormgmt.user;

import com.tutormgmt.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Application user — the tutor (CLAUDE.md section 10.1). */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends Auditable {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    /** Stable external identifier from Google (the {@code sub} claim). */
    @Column(name = "google_subject", nullable = false, unique = true)
    private String googleSubject;

    public static User create(String googleSubject, String email, String firstName, String lastName) {
        User u = new User();
        u.id = UUID.randomUUID();
        u.googleSubject = googleSubject;
        u.email = email;
        u.firstName = firstName;
        u.lastName = lastName;
        return u;
    }
}
