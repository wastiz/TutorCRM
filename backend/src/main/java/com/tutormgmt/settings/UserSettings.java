package com.tutormgmt.settings;

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

/** Per-tutor integration settings (CLAUDE.md section 47). */
@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettings extends Auditable {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    /** Google Calendar the lessons are mirrored to (may belong to another account). */
    @Column(name = "calendar_id")
    private String calendarId;

    @Column(name = "calendar_summary")
    private String calendarSummary;

    /** Existing spreadsheet used for the monthly payout report (Report.md section 17). */
    @Column(name = "report_spreadsheet_id")
    private String reportSpreadsheetId;

    @Column(name = "report_spreadsheet_name")
    private String reportSpreadsheetName;

    /**
     * When set, the monthly report is written to this exact worksheet.
     * When null, a per-month worksheet ("август 26") is used (Report.md section 18, Option B).
     */
    @Column(name = "report_worksheet_title")
    private String reportWorksheetTitle;

    public static UserSettings create(UUID userId) {
        UserSettings s = new UserSettings();
        s.id = UUID.randomUUID();
        s.userId = userId;
        return s;
    }
}
