package com.tutormgmt.statistics;

import com.tutormgmt.security.AppPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Statistics page (owner request, 2026-09-05). */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService service;

    @GetMapping
    public StatisticsDto overview(@AuthenticationPrincipal AppPrincipal principal) {
        return service.overview(principal.userId());
    }
}
