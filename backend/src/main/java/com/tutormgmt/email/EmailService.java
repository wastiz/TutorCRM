package com.tutormgmt.email;

import com.tutormgmt.common.error.ApiException;
import com.tutormgmt.lesson.Lesson;
import com.tutormgmt.lesson.LessonRepository;
import com.tutormgmt.student.Student;
import com.tutormgmt.student.StudentRepository;
import com.tutormgmt.user.User;
import com.tutormgmt.user.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Template CRUD, rendering and manual sending (owner decision, 2026-09-05: no automatic
 * triggers — the tutor picks a template and presses Send).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailTemplateRepository repository;
    private final EmailTemplateRenderer renderer;
    private final EmailSender sender;
    private final StudentRepository studentRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    // --- templates ---

    @Transactional
    public List<EmailDto.TemplateDto> list(UUID userId) {
        if (!repository.existsByUserId(userId)) {
            seedStarterTemplates(userId);
        }
        return repository.findByUserIdOrderByNameAsc(userId).stream().map(EmailService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public EmailDto.TemplateDto get(UUID userId, UUID id) {
        return toDto(require(userId, id));
    }

    @Transactional
    public EmailDto.TemplateDto create(UUID userId, EmailDto.TemplateRequest request) {
        EmailTemplate template = EmailTemplate.create(userId);
        apply(template, request);
        log.info("Created e-mail template \"{}\" for user {}", template.getName(), userId);
        return toDto(repository.save(template));
    }

    @Transactional
    public EmailDto.TemplateDto update(UUID userId, UUID id, EmailDto.TemplateRequest request) {
        EmailTemplate template = require(userId, id);
        apply(template, request);
        return toDto(repository.save(template));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repository.delete(require(userId, id));
    }

    public List<EmailDto.PlaceholderDto> placeholders() {
        return EmailPlaceholders.catalogue();
    }

    // --- rendering & sending ---

    @Transactional(readOnly = true)
    public EmailDto.PreviewDto preview(UUID userId, EmailDto.SendRequest request) {
        return build(userId, request).preview();
    }

    @Transactional(readOnly = true)
    public EmailDto.SendResultDto send(UUID userId, EmailDto.SendRequest request) {
        Outgoing outgoing = build(userId, request);
        if (!StringUtils.hasText(outgoing.to)) {
            throw ApiException.badRequest("EMAIL_RECIPIENT_MISSING",
                    "No recipient: the student has neither an e-mail nor a parent e-mail");
        }
        if (!sender.enabledFor(userId)) {
            throw ApiException.badRequest("EMAIL_NOT_CONFIGURED",
                    "Connect Google in Settings to send e-mail from your own address");
        }
        String messageId = sender.send(userId,
                new EmailSender.Message(outgoing.to, outgoing.subject, outgoing.body));
        log.info("Sent e-mail to {} for student {} (message {})",
                outgoing.to, request.studentId(), messageId);
        return new EmailDto.SendResultDto(outgoing.to, outgoing.subject, messageId, Instant.now());
    }

    // --- internals ---

    private Outgoing build(UUID userId, EmailDto.SendRequest request) {
        Student student = studentRepository.findByIdAndUserId(request.studentId(), userId)
                .orElseThrow(() -> ApiException.badRequest("STUDENT_NOT_FOUND", "Student not found"));
        Lesson lesson = request.lessonId() == null ? null
                : lessonRepository.findByIdAndUserId(request.lessonId(), userId).orElse(null);
        User tutor = userRepository.findById(userId).orElse(null);

        String subjectSource = request.subject();
        String bodySource = request.body();
        if (request.templateId() != null && !StringUtils.hasText(subjectSource)
                && !StringUtils.hasText(bodySource)) {
            EmailTemplate template = require(userId, request.templateId());
            subjectSource = template.getSubject();
            bodySource = template.getBody();
        }
        if (!StringUtils.hasText(subjectSource) && !StringUtils.hasText(bodySource)) {
            throw ApiException.badRequest("EMAIL_EMPTY", "Pick a template or write a subject and body");
        }

        Map<String, String> values = EmailPlaceholders.of(tutor, student, lesson);
        EmailTemplateRenderer.Rendered rendered = renderer.render(subjectSource, bodySource, values);

        List<String> warnings = new ArrayList<>();
        if (!rendered.unresolvedPlaceholders().isEmpty()) {
            warnings.add("No value for: " + String.join(", ", rendered.unresolvedPlaceholders()));
        }
        String to = recipient(request, student, warnings);
        return new Outgoing(to, rendered.subject(), rendered.body(),
                rendered.unresolvedPlaceholders(), warnings);
    }

    /** Explicit address wins, then the student's, then the parent's. */
    private static String recipient(EmailDto.SendRequest request, Student student, List<String> warnings) {
        if (StringUtils.hasText(request.to())) {
            return request.to().trim();
        }
        if (StringUtils.hasText(student.getEmail())) {
            return student.getEmail().trim();
        }
        if (StringUtils.hasText(student.getParentEmail())) {
            warnings.add("The student has no e-mail — sending to the parent instead");
            return student.getParentEmail().trim();
        }
        warnings.add("No recipient address on the student or the parent");
        return null;
    }

    private EmailTemplate require(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("EMAIL_TEMPLATE_NOT_FOUND", "Template not found"));
    }

    private static void apply(EmailTemplate template, EmailDto.TemplateRequest request) {
        template.setName(request.name().trim());
        template.setKind(request.kind() == null ? EmailTemplateKind.GENERAL : request.kind());
        template.setSubject(request.subject().trim());
        template.setBody(request.body());
    }

    private static EmailDto.TemplateDto toDto(EmailTemplate t) {
        return new EmailDto.TemplateDto(t.getId(), t.getName(), t.getKind(), t.getSubject(),
                t.getBody(), t.getCreatedAt(), t.getUpdatedAt());
    }

    /** First visit: give the tutor three working templates instead of an empty screen. */
    private void seedStarterTemplates(UUID userId) {
        List<EmailDto.TemplateRequest> starters = List.of(
                new EmailDto.TemplateRequest("Знакомство", EmailTemplateKind.WELCOME,
                        "Занятия по предмету {{student.subject}}",
                        """
                        Здравствуйте, {{student.firstName}}!

                        Меня зовут {{tutor.name}}, я буду вести у Вас занятия по предмету \
                        {{student.subject}}. Стоимость занятия — {{student.rate}} € за 60 минут.

                        Если у Вас есть вопросы, просто ответьте на это письмо.

                        С уважением,
                        {{tutor.name}}"""),
                new EmailDto.TemplateRequest("Напоминание об уроке", EmailTemplateKind.LESSON,
                        "Напоминание: урок {{lesson.date}} в {{lesson.time}}",
                        """
                        Здравствуйте, {{student.firstName}}!

                        Напоминаю о занятии {{lesson.date}} в {{lesson.time}} \
                        (длительность {{lesson.duration}}).

                        До встречи,
                        {{tutor.name}}"""),
                new EmailDto.TemplateRequest("Итоги месяца", EmailTemplateKind.MONTHLY_SUMMARY,
                        "Занятия за месяц — {{student.fullName}}",
                        """
                        Здравствуйте, {{parent.name}}!

                        Направляю итоги занятий {{student.fullName}} за прошедший месяц.

                        С уважением,
                        {{tutor.name}}"""));

        for (EmailDto.TemplateRequest starter : starters) {
            EmailTemplate template = EmailTemplate.create(userId);
            apply(template, starter);
            repository.save(template);
        }
        log.info("Seeded {} starter e-mail templates for user {}", starters.size(), userId);
    }

    private record Outgoing(String to, String subject, String body,
                            List<String> unresolved, List<String> warnings) {

        EmailDto.PreviewDto preview() {
            return new EmailDto.PreviewDto(to, subject, body, unresolved, warnings);
        }
    }
}
