package com.tutormgmt.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tutormgmt.common.error.ApiException;
import com.tutormgmt.lesson.Lesson;
import com.tutormgmt.lesson.LessonRepository;
import com.tutormgmt.lesson.LessonStatus;
import com.tutormgmt.report.export.MonthlyReportSheetExporter;
import com.tutormgmt.student.LessonFormat;
import com.tutormgmt.student.Student;
import com.tutormgmt.student.StudentRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ReportServiceExportTest {

    @Mock LessonRepository lessonRepository;
    @Mock StudentRepository studentRepository;
    @Mock ReportRepository reportRepository;
    @Mock MonthlyReportSheetExporter sheetExporter;

    ReportService service;

    private final UUID userId = UUID.randomUUID();
    private final YearMonth month = YearMonth.of(2026, 8);

    @BeforeEach
    void setUp() {
        service = new ReportService(lessonRepository, studentRepository, reportRepository, sheetExporter);
    }

    private Lesson completed(UUID studentId, BigDecimal price) {
        Lesson l = Lesson.create(userId, studentId);
        l.setStartTime(OffsetDateTime.of(2026, 8, 10, 12, 0, 0, 0, ZoneOffset.UTC));
        l.setEndTime(l.getStartTime().plusHours(1));
        l.setPrice(price);
        l.setStatus(LessonStatus.COMPLETED);
        return l;
    }

    private Student student(String number) {
        Student s = Student.create(userId, number);
        s.setFirstName("Maksim");
        s.setLastName("Ivanov");
        s.setSubject("Эстонский");
        s.setLessonFormat(LessonFormat.ONLINE);
        return s;
    }

    @Test
    void exportsAndPersistsSnapshot() {
        UUID studentId = UUID.randomUUID();
        when(lessonRepository.findByUserIdAndStatusAndStartTimeGreaterThanEqualAndStartTimeLessThan(
                eq(userId), eq(LessonStatus.COMPLETED), any(), any()))
                .thenReturn(List.of(completed(studentId, new BigDecimal("12")),
                        completed(studentId, new BigDecimal("12"))));
        when(studentRepository.findByIdAndUserId(studentId, userId))
                .thenReturn(Optional.of(student("1")));
        when(sheetExporter.export(eq(userId), any(), anyBoolean()))
                .thenReturn(new MonthlyReportSheetExporter.ExportOutcome(
                        "sheet-1", "https://docs.google.com/spreadsheets/d/sheet-1", "август 26", false));

        ReportExportResultDto result = service.export(userId, month, false);

        assertThat(result.spreadsheetId()).isEqualTo("sheet-1");
        assertThat(result.worksheetTitle()).isEqualTo("август 26");

        ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(saved.capture());
        assertThat(saved.getValue().getTotalLessons()).isEqualTo(2);
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("24");
        assertThat(saved.getValue().getMonth()).isEqualTo(month);
        assertThat(saved.getValue().getSpreadsheetId()).isEqualTo("sheet-1");
    }

    @Test
    void refusesExportWhenReportHasErrors() {
        UUID orphanStudentId = UUID.randomUUID();
        when(lessonRepository.findByUserIdAndStatusAndStartTimeGreaterThanEqualAndStartTimeLessThan(
                eq(userId), eq(LessonStatus.COMPLETED), any(), any()))
                .thenReturn(List.of(completed(orphanStudentId, new BigDecimal("12"))));
        when(studentRepository.findByIdAndUserId(orphanStudentId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.export(userId, month, false))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("REPORT_NOT_EXPORTABLE"));
    }
}
