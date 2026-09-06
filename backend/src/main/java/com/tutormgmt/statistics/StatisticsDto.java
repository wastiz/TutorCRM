package com.tutormgmt.statistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Everything the statistics page shows, computed on demand from students + lessons. */
public record StatisticsDto(
        StudentCounts students,
        LessonCounts lessons,
        /** Sum of every COMPLETED lesson, all time. */
        BigDecimal totalEarnings,
        /** Why students stopped — free text, newest first. */
        List<LeaveReasonDto> leaveReasons,
        /** Last 12 months, oldest first. */
        List<MonthPointDto> byMonth
) {

    public record StudentCounts(long total, long active, long paused, long finished) {}

    public record LessonCounts(long total, long completed, long cancelled, long noShow, long planned) {

        /** Share of scheduled lessons that fell through, 0–100. */
        public BigDecimal cancellationRate() {
            long decided = completed + cancelled + noShow;
            if (decided == 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf((cancelled + noShow) * 100.0 / decided)
                    .setScale(1, java.math.RoundingMode.HALF_UP);
        }
    }

    public record LeaveReasonDto(UUID studentId, String studentName, LocalDate endDate, String reason) {}

    public record MonthPointDto(String month, long completed, long cancelled, long noShow,
                                BigDecimal earnings) {}
}
