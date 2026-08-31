package com.tutormgmt.report;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByUserIdOrderByGeneratedAtDesc(UUID userId);

    List<Report> findByUserIdAndMonth(UUID userId, YearMonth month);
}
