package com.tutormgmt.integration.google;

import com.tutormgmt.authentication.AuthenticationDto;
import com.tutormgmt.authentication.AuthenticationService;
import com.tutormgmt.integration.google.calendar.CalendarSummaryDto;
import com.tutormgmt.integration.google.calendar.GoogleCalendarService;
import com.tutormgmt.security.AppPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Google connection status and resource pickers for the Settings page (CLAUDE.md section 47). */
@RestController
@RequestMapping("/api/integrations/google")
@RequiredArgsConstructor
public class GoogleIntegrationController {

    private final AuthenticationService authenticationService;
    private final GoogleCalendarService calendarService;

    @GetMapping("/status")
    public AuthenticationDto status(@AuthenticationPrincipal AppPrincipal principal) {
        return authenticationService.getStatus(principal.userId());
    }

    @GetMapping("/calendars")
    public List<CalendarSummaryDto> calendars(@AuthenticationPrincipal AppPrincipal principal) {
        return calendarService.listCalendars(principal.userId());
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal AppPrincipal principal) {
        authenticationService.disconnect(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
