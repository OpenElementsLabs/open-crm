package com.openelements.crm.task;

import com.openelements.crm.TestSecurityUtil;
import com.openelements.crm.company.CompanyCreateDto;
import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.company.CompanyService;
import com.openelements.crm.contact.ContactCreateDto;
import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.crm.contact.ContactService;
import com.openelements.crm.tag.TagRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Task Controller")
class TaskControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TaskService taskService;
    @Autowired private TaskRepository taskRepository;
    @Autowired private CompanyService companyService;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ContactService contactService;
    @Autowired private ContactRepository contactRepository;
    @Autowired private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        contactRepository.deleteAll();
        companyRepository.deleteAll();
        tagRepository.deleteAll();
        TestSecurityUtil.setSecurityContext();
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtil.clearSecurityContext();
    }

    private CompanyDto createCompany(final String name) {
        return companyService.create(new CompanyCreateDto(name, null, null, null, null, null, null, null, null, null, null));
    }

    private ContactDto createContact(final String firstName, final String lastName) {
        return contactService.create(new ContactCreateDto(null, firstName, lastName, null, null, null, null, null, null, null, null, null, null));
    }

    @Nested
    @DisplayName("POST /api/tasks")
    class CreateTask {

        @Test
        @DisplayName("should create task and return 201")
        void shouldCreateTask() throws Exception {
            final CompanyDto company = createCompany("Acme");

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"action": "Call back", "dueDate": "2026-06-01", "companyId": "%s"}
                                    """.formatted(company.id())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.action", is("Call back")))
                    .andExpect(jsonPath("$.dueDate", is("2026-06-01")))
                    .andExpect(jsonPath("$.status", is("OPEN")))
                    .andExpect(jsonPath("$.companyId", is(company.id().toString())))
                    .andExpect(jsonPath("$.companyName", is("Acme")));
        }

        @Test
        @DisplayName("should return 400 for blank action")
        void shouldReturn400ForBlankAction() throws Exception {
            final CompanyDto company = createCompany("Acme");

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"action": "", "dueDate": "2026-06-01", "companyId": "%s"}
                                    """.formatted(company.id())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for missing dueDate")
        void shouldReturn400ForMissingDueDate() throws Exception {
            final CompanyDto company = createCompany("Acme");

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"action": "Task", "companyId": "%s"}
                                    """.formatted(company.id())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for no owner")
        void shouldReturn400ForNoOwner() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"action": "Task", "dueDate": "2026-06-01"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/{id}")
    class GetTask {

        @Test
        @DisplayName("should return task")
        void shouldReturnTask() throws Exception {
            final CompanyDto company = createCompany("Acme");
            final TaskDto task = taskService.create(new TaskCreateDto("Task", LocalDate.of(2026, 6, 1), null, company.id(), null, null));

            mockMvc.perform(get("/api/tasks/{id}", task.id()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.action", is("Task")));
        }

        @Test
        @DisplayName("should return 404 for non-existent task")
        void shouldReturn404() throws Exception {
            mockMvc.perform(get("/api/tasks/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/tasks/{id}")
    class UpdateTask {

        @Test
        @DisplayName("should update task")
        void shouldUpdateTask() throws Exception {
            final CompanyDto company = createCompany("Acme");
            final TaskDto task = taskService.create(new TaskCreateDto("Old", LocalDate.of(2026, 6, 1), null, company.id(), null, null));

            mockMvc.perform(put("/api/tasks/{id}", task.id())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"action": "New", "dueDate": "2026-07-01", "status": "DONE"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.action", is("New")))
                    .andExpect(jsonPath("$.status", is("DONE")));
        }

        @Test
        @DisplayName("should return 400 for blank action")
        void shouldReturn400ForBlankAction() throws Exception {
            final CompanyDto company = createCompany("Acme");
            final TaskDto task = taskService.create(new TaskCreateDto("Task", LocalDate.of(2026, 6, 1), null, company.id(), null, null));

            mockMvc.perform(put("/api/tasks/{id}", task.id())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"action": "  ", "dueDate": "2026-07-01", "status": "OPEN"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 for non-existent task")
        void shouldReturn404() throws Exception {
            mockMvc.perform(put("/api/tasks/{id}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"action": "Task", "dueDate": "2026-07-01", "status": "OPEN"}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/tasks/{id}")
    class DeleteTask {

        @Test
        @DisplayName("should delete task and return 204")
        void shouldDeleteTask() throws Exception {
            final CompanyDto company = createCompany("Acme");
            final TaskDto task = taskService.create(new TaskCreateDto("Task", LocalDate.of(2026, 6, 1), null, company.id(), null, null));

            mockMvc.perform(delete("/api/tasks/{id}", task.id()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/tasks/{id}", task.id()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for non-existent task")
        void shouldReturn404() throws Exception {
            mockMvc.perform(delete("/api/tasks/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks")
    class ListTasks {

        @Test
        @DisplayName("should return paginated tasks")
        void shouldReturnPaginatedTasks() throws Exception {
            final CompanyDto company = createCompany("Acme");
            taskService.create(new TaskCreateDto("T1", LocalDate.of(2026, 6, 1), null, company.id(), null, null));
            taskService.create(new TaskCreateDto("T2", LocalDate.of(2026, 6, 2), null, company.id(), null, null));

            mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements", is(2)));
        }

        @Test
        @DisplayName("should filter by status")
        void shouldFilterByStatus() throws Exception {
            final CompanyDto company = createCompany("Acme");
            taskService.create(new TaskCreateDto("Open", LocalDate.of(2026, 6, 1), TaskStatus.OPEN, company.id(), null, null));
            taskService.create(new TaskCreateDto("Done", LocalDate.of(2026, 6, 2), TaskStatus.DONE, company.id(), null, null));

            mockMvc.perform(get("/api/tasks").param("status", "OPEN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements", is(1)));
        }
    }
}
