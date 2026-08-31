package com.tutormgmt.report;

import com.tutormgmt.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A snapshot of a monthly report at the moment it was exported to Google Sheets
 * (CLAUDE.md section 10.5). The live figures are always recomputed from lessons;
 * this row is history/audit only.
 */
@Entity
@Table(name = "report")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends Auditable {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Convert(converter = YearMonthConverter.class)
    @Column(name = "report_month", nullable = false, length = 7)
    private YearMonth month;

    @Column(name = "total_lessons", nullable = false)
    private Integer totalLessons;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "spreadsheet_id")
    private String spreadsheetId;

    @Column(name = "worksheet_title")
    private String worksheetTitle;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public static Report snapshot(UUID userId, YearMonth month, int totalLessons, BigDecimal totalAmount) {
        Report r = new Report();
        r.id = UUID.randomUUID();
        r.userId = userId;
        r.month = month;
        r.totalLessons = totalLessons;
        r.totalAmount = totalAmount;
        r.generatedAt = Instant.now();
        return r;
    }
}
