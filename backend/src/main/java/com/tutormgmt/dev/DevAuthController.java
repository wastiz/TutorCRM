package com.tutormgmt.dev;

import com.tutormgmt.user.User;
import jakarta.servlet.http.HttpServletResponse;
import com.tutormgmt.config.AppProperties;
import com.tutormgmt.security.SessionCookies;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Local-only shortcuts for poking at the app without a Google account.
 *
 * <p>Guarded twice: the bean exists only under the {@code local} profile, and
 * {@link DevSecurityConfig} only opens {@code /dev/**} under that same profile. A production
 * deployment has neither, so these routes simply 401/404 there.
 */
@Slf4j
@Profile("local")
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevAuthController {

    private final DevDataSeeder seeder;
    private final SessionCookies sessionCookies;
    private final AppProperties props;

    /** Signs the browser in as the demo tutor and bounces it back to the SPA. */
    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        User tutor = seeder.demoTutor();
        log.info("Dev sign-in as {}", tutor.getEmail());
        return ResponseEntity.status(HttpServletResponse.SC_FOUND)
                .header(HttpHeaders.SET_COOKIE, sessionCookies.issue(tutor).toString())
                .location(URI.create(props.webBaseUrl() + "/auth/callback"))
                .build();
    }

    /** Re-runs the seeder; {@code reset=true} replaces the demo data with a freshly dated set. */
    @PostMapping("/seed")
    public DevSeedResult seed(@RequestParam(defaultValue = "false") boolean reset) {
        return seeder.seed(reset);
    }
}
