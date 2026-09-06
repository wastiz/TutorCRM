package com.tutormgmt.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tutormgmt.common.error.ApiException;
import com.tutormgmt.lesson.LessonRepository;
import com.tutormgmt.student.StudentDto;
import com.tutormgmt.student.StudentRepository;
import com.tutormgmt.student.StudentRequest;
import com.tutormgmt.student.StudentService;
import com.tutormgmt.support.AbstractPostgresIT;
import com.tutormgmt.user.User;
import com.tutormgmt.user.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import(EmailServiceIT.FakeSenderConfig.class)
class EmailServiceIT extends AbstractPostgresIT {

    @Autowired EmailService emailService;
    @Autowired EmailTemplateRepository templateRepository;
    @Autowired StudentService studentService;
    @Autowired StudentRepository studentRepository;
    @Autowired LessonRepository lessonRepository;
    @Autowired UserRepository userRepository;
    @Autowired FakeSender sender;

    private UUID userId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        templateRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        sender.sent.clear();
        sender.enabled = true;
        userId = userRepository.save(User.create("sub-" + UUID.randomUUID(), "tutor@x.ee", "Anna", "Tutor"))
                .getId();
        StudentDto s = studentService.create(userId, StudentRequest.builder()
                .firstName("Kirill").lastName("Tsarenkov").email("kirill@x.ee")
                .subject("Эстонский язык").lessonPrice(new BigDecimal("20.00"))
                .parentName("Liudmila").parentEmail("mom@x.ee")
                .schedules(List.of()).build());
        studentId = s.id();
    }

    @Test
    void firstVisitGetsStarterTemplates() {
        List<EmailDto.TemplateDto> templates = emailService.list(userId);

        assertThat(templates).isNotEmpty();
        assertThat(templates).extracting(EmailDto.TemplateDto::kind)
                .contains(EmailTemplateKind.WELCOME, EmailTemplateKind.LESSON);
        // seeding happens once
        assertThat(emailService.list(userId)).hasSameSizeAs(templates);
    }

    @Test
    void previewRendersTheTemplateAgainstTheStudentWithoutSending() {
        EmailDto.TemplateDto template = emailService.create(userId, new EmailDto.TemplateRequest(
                "Hi", EmailTemplateKind.GENERAL, "{{student.subject}}",
                "Здравствуйте, {{student.firstName}}! Ставка {{student.rate}} €. {{tutor.name}}"));

        EmailDto.PreviewDto preview = emailService.preview(userId,
                new EmailDto.SendRequest(template.id(), studentId, null, null, null, null));

        assertThat(preview.to()).isEqualTo("kirill@x.ee");
        assertThat(preview.subject()).isEqualTo("Эстонский язык");
        assertThat(preview.body()).isEqualTo("Здравствуйте, Kirill! Ставка 20.00 €. Anna Tutor");
        assertThat(preview.unresolvedPlaceholders()).isEmpty();
        assertThat(sender.sent).isEmpty();
    }

    @Test
    void sendDeliversTheRenderedMessage() {
        EmailDto.TemplateDto template = emailService.create(userId, new EmailDto.TemplateRequest(
                "Hi", EmailTemplateKind.GENERAL, "Урок", "Привет, {{student.firstName}}"));

        EmailDto.SendResultDto result = emailService.send(userId,
                new EmailDto.SendRequest(template.id(), studentId, null, null, null, null));

        assertThat(result.to()).isEqualTo("kirill@x.ee");
        assertThat(sender.sent).singleElement().satisfies(m -> {
            assertThat(m.to()).isEqualTo("kirill@x.ee");
            assertThat(m.body()).isEqualTo("Привет, Kirill");
        });
    }

    @Test
    void fallsBackToTheParentAddressAndWarnsAboutIt() {
        StudentDto noEmail = studentService.create(userId, StudentRequest.builder()
                .firstName("Artjom").lastName("Zimin").email("artjom@x.ee")
                .parentEmail("julia@x.ee").schedules(List.of()).ignoreDuplicates(true).build());
        studentRepository.findById(noEmail.id()).ifPresent(s -> {
            s.setEmail(null);
            studentRepository.save(s);
        });

        EmailDto.PreviewDto preview = emailService.preview(userId, new EmailDto.SendRequest(
                null, noEmail.id(), null, null, "s", "b"));

        assertThat(preview.to()).isEqualTo("julia@x.ee");
        assertThat(preview.warnings()).anyMatch(w -> w.contains("parent"));
    }

    @Test
    void sendingIsRefusedWhenGoogleIsNotConnected() {
        sender.enabled = false;

        assertThatThrownBy(() -> emailService.send(userId,
                new EmailDto.SendRequest(null, studentId, null, null, "s", "b")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Connect Google");
    }

    @Test
    void adHocMessagesNeedSomeContent() {
        assertThatThrownBy(() -> emailService.preview(userId,
                new EmailDto.SendRequest(null, studentId, null, null, null, null)))
                .isInstanceOf(ApiException.class);
    }

    /** Records what the slice would hand to Gmail. */
    static class FakeSender implements EmailSender {

        final List<Message> sent = new ArrayList<>();
        boolean enabled = true;

        @Override
        public boolean enabledFor(UUID userId) {
            return enabled;
        }

        @Override
        public String send(UUID userId, Message message) {
            sent.add(message);
            return "msg-" + sent.size();
        }
    }

    @TestConfiguration
    static class FakeSenderConfig {
        @Bean
        @Primary
        FakeSender fakeSender() {
            return new FakeSender();
        }
    }
}
