package com.tutormgmt.authentication;

import com.tutormgmt.security.AppPrincipal;
import com.tutormgmt.security.SessionCookies;
import com.tutormgmt.user.User;
import com.tutormgmt.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Session endpoints (CLAUDE.md section 52). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final SessionCookies sessionCookies;

    @GetMapping("/me")
    public CurrentUserDto me(@AuthenticationPrincipal AppPrincipal principal) {
        User user = userService.getById(principal.userId());
        return new CurrentUserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                authenticationService.isConnected(user.getId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.clearing().toString())
                .build();
    }

    public record CurrentUserDto(
            java.util.UUID id,
            String email,
            String firstName,
            String lastName,
            boolean googleConnected
    ) {}
}
