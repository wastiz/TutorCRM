package com.tutormgmt.settings;

import com.tutormgmt.security.AppPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CLAUDE.md section 47. */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService service;

    @GetMapping
    public SettingsDto get(@AuthenticationPrincipal AppPrincipal principal) {
        return service.get(principal.userId());
    }

    @PutMapping
    public SettingsDto update(@AuthenticationPrincipal AppPrincipal principal,
                              @RequestBody SettingsDto dto) {
        return service.update(principal.userId(), dto);
    }
}
