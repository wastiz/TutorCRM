package com.tutormgmt.settings;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserSettingsRepository repository;

    @Transactional
    public UserSettings getOrCreate(UUID userId) {
        return repository.findByUserId(userId)
                .orElseGet(() -> repository.save(UserSettings.create(userId)));
    }

    @Transactional(readOnly = true)
    public SettingsDto get(UUID userId) {
        return repository.findByUserId(userId).map(SettingsDto::from)
                .orElse(new SettingsDto(null, null, null, null, null));
    }

    @Transactional
    public SettingsDto update(UUID userId, SettingsDto dto) {
        UserSettings s = getOrCreate(userId);
        s.setCalendarId(blankToNull(dto.calendarId()));
        s.setCalendarSummary(blankToNull(dto.calendarSummary()));
        s.setReportSpreadsheetId(blankToNull(dto.reportSpreadsheetId()));
        s.setReportSpreadsheetName(blankToNull(dto.reportSpreadsheetName()));
        s.setReportWorksheetTitle(blankToNull(dto.reportWorksheetTitle()));
        return SettingsDto.from(repository.save(s));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
