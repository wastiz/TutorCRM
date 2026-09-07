package com.tutormgmt.student;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutormgmt.lesson.LessonRepository;
import com.tutormgmt.security.AppPrincipal;
import com.tutormgmt.support.AbstractPostgresIT;
import com.tutormgmt.user.User;
import com.tutormgmt.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@AutoConfigureMockMvc
class StudentApiIT extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    UserRepository userRepository;
    @Autowired
    StudentRepository studentRepository;
    @Autowired
    LessonRepository lessonRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        userId = userRepository.save(User.create("sub-" + UUID.randomUUID(), "tutor@x.ee", "T", "T")).getId();
    }

    private RequestPostProcessor asUser() {
        return authentication(new UsernamePasswordAuthenticationToken(
                new AppPrincipal(userId, "tutor@x.ee", "T T"), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    void rejectsUnauthenticated() throws Exception {
        mvc.perform(get("/api/students")).andExpect(status().isUnauthorized());
    }

    @Test
    void createsListsAndFetchesStudent() throws Exception {
        String body = """
                {"firstName":"Maksim","lastName":"Ivanov","email":"k@x.ee","lessonPrice":20,
                 "schedules":[{"dayOfWeek":"THURSDAY","startTime":"17:00","endTime":"18:00","lessonsPerWeek":1}]}
                """;

        mvc.perform(post("/api/students").with(asUser())
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentNumber").value("1"))
                .andExpect(jsonPath("$.schedules", hasSize(1)));

        mvc.perform(get("/api/students").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("Maksim Ivanov"));
    }

    @Test
    void duplicateCreateReturns409WithCandidates() throws Exception {
        String body = "{\"firstName\":\"Maksim\",\"lastName\":\"Ivanov\",\"email\":\"dup@x.ee\",\"schedules\":[]}";
        mvc.perform(post("/api/students").with(asUser()).contentType("application/json").content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/students").with(asUser()).contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POSSIBLE_DUPLICATE"))
                .andExpect(jsonPath("$.details.duplicates", hasSize(1)));
    }

    @Test
    void validationErrorIsStructured() throws Exception {
        // first name and e-mail are both missing — the two required fields besides the last name
        mvc.perform(post("/api/students").with(asUser())
                        .contentType("application/json").content("{\"lastName\":\"X\",\"schedules\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", hasSize(2)));
    }

    /** Only first name, last name and e-mail are required; the rest is filled in later. */
    @Test
    void createsStudentWithNameAndEmailOnly() throws Exception {
        mvc.perform(post("/api/students").with(asUser()).contentType("application/json")
                        .content("{\"firstName\":\"Anna\",\"lastName\":\"Ivanova\",\"email\":\"anna@x.ee\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentNumber").value("1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createWithoutEmailIsRejected() throws Exception {
        mvc.perform(post("/api/students").with(asUser()).contentType("application/json")
                        .content("{\"firstName\":\"Anna\",\"lastName\":\"Ivanova\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void parsesRawMessage() throws Exception {
        String raw = String.join("\t", "Maksim Ivanov", "maksim.ivanov@example.com", "5555 0101", "14", "9",
                "Tlvl", "Эстонский язык", "goal", "ЧЕТВЕРГ", "17-20", "2", "оба варианта подходят",
                "", "Natalia Ivanova", "", "51109180007", "47808060003");
        String body = json.writeValueAsString(java.util.Map.of("rawText", raw));

        mvc.perform(post("/api/students/import/parse").with(asUser())
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.firstName").value("Maksim"))
                .andExpect(jsonPath("$.student.lessonFormat").value("BOTH"))
                .andExpect(jsonPath("$.student.isikukood").value("51109180007"))
                .andExpect(jsonPath("$.student.parentIsikukood").value("47808060003"))
                .andExpect(jsonPath("$.warnings", hasSize(0)));
    }
}
