package com.tutormgmt.user;

import com.tutormgmt.common.error.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
    }

    /**
     * Finds the user by Google {@code sub}, creating it on first login and
     * keeping the profile fields in sync on subsequent logins.
     */
    @Transactional
    public User upsertFromGoogle(String googleSubject, String email, String firstName, String lastName) {
        User user = repository.findByGoogleSubject(googleSubject).orElse(null);
        if (user == null) {
            user = User.create(googleSubject, email, firstName, lastName);
            log.info("Registered new user via Google (sub hash={})", Integer.toHexString(googleSubject.hashCode()));
        } else {
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
        }
        return repository.save(user);
    }
}
