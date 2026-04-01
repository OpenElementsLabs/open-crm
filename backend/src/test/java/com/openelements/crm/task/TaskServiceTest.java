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
import com.openelements.crm.tag.TagCreateDto;
import com.openelements.crm.tag.TagDto;
import com.openelements.crm.tag.TagRepository;
import com.openelements.crm.tag.TagService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TaskService")
class TaskServiceTest {

    @Autowired private TaskService taskService;
    @Autowired private TaskRepository taskRepository;
    @Autowired private CompanyService companyService;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private ContactService contactService;
    @Autowired private ContactRepository contactRepository;
    @Autowired private TagService tagService;
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

    private TagDto createTag(final String name) {
        return tagService.create(new TagCreateDto(name, null, "#FF0000"));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create task for company")
        void shouldCreateForCompany() {
            final CompanyDto company = createCompany("Acme");
            final TaskDto result = taskService.create(new TaskCreateDto(
                    "Call back", LocalDate.of(2026, 6, 1), null, company.id(), null, null));

            assertNotNull(result.id());
            assertEquals("Call back", result.action());
            assertEquals(LocalDate.of(2026, 6, 1), result.dueDate());
            assertEquals(TaskStatus.OPEN, result.status());
            assertEquals(company.id(), result.companyId());
            assertEquals("Acme", result.companyName());
            assertNull(result.contactId());
        }

        @Test
        @DisplayName("should create task for contact")
        void shouldCreateForContact() {
            final ContactDto contact = createContact("Jane", "Doe");
            final TaskDto result = taskService.create(new TaskCreateDto(
                    "Follow up", LocalDate.of(2026, 7, 1), null, null, contact.id(), null));

            assertNotNull(result.id());
            assertEquals(contact.id(), result.contactId());
            assertEquals("Jane Doe", result.contactName());
            assertNull(result.companyId());
        }

        @Test
        @DisplayName("should create with explicit status")
        void shouldCreateWithExplicitStatus() {
            final CompanyDto company = createCompany("Corp");
            final TaskDto result = taskService.create(new TaskCreateDto(
                    "Urgent", LocalDate.of(2026, 6, 1), TaskStatus.IN_PROGRESS, company.id(), null, null));

            assertEquals(TaskStatus.IN_PROGRESS, result.status());
        }

        @Test
        @DisplayName("should create with tags")
        void shouldCreateWithTags() {
            final CompanyDto company = createCompany("Corp");
            final TagDto tag1 = createTag("Priority");
            final TagDto tag2 = createTag("Follow-up");
            final TaskDto result = taskService.create(new TaskCreateDto(
                    "Task", LocalDate.of(2026, 6, 1), null, company.id(), null, List.of(tag1.id(), tag2.id())));

            assertEquals(2, result.tagIds().size());
            assertTrue(result.tagIds().contains(tag1.id()));
            assertTrue(result.tagIds().contains(tag2.id()));
        }

        @Test
        @DisplayName("should fail without owner")
        void shouldFailWithoutOwner() {
            final var ex = assertThrows(ResponseStatusException.class,
                    () -> taskService.create(new TaskCreateDto("Task", LocalDate.of(2026, 6, 1), null, null, null, null)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should fail with both owners")
        void shouldFailWithBothOwners() {
            final CompanyDto company = createCompany("Corp");
            final ContactDto contact = createContact("Jane", "Doe");
            final var ex = assertThrows(ResponseStatusException.class,
                    () -> taskService.create(new TaskCreateDto("Task", LocalDate.of(2026, 6, 1), null, company.id(), contact.id(), null)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should fail with non-existent company")
        void shouldFailWithNonExistentCompany() {
            final var ex = assertThrows(ResponseStatusException.class,
                    () -> taskService.create(new TaskCreateDto("Task", LocalDate.of(2026, 6, 1), null, UUID.randomUUID(), null, null)));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should fail with non-existent contact")
        void shouldFailWithNonExistentContact() {
            final var ex = assertThrows(ResponseStatusException.class,
                    () -> taskService.create(new TaskCreateDto("Task", LocalDate.of(2026, 6, 1), null, null, UUID.randomUUID(), null)));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should throw 404 for deleted company")
        void shouldThrow404ForDeletedCompany() {
            final CompanyDto company = createCompany("Archived");
            companyService.delete(company.id(), false);

            final var ex = assertThrows(ResponseStatusException.class,
                    () -> taskService.create(new TaskCreateDto(
                            "Task", LocalDate.of(2026, 6, 1), null, company.id(), null, null)));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return task")
        void shouldReturnTask() {
            final CompanyDto company = createCompany("Corp");
            final TaskDto created = taskService.create(new TaskCreateDto(
                    "Task", LocalDate.of(2026, 6, 1), null, company.id(), null, null));

            final TaskDto result = taskService.getById(created.id());

            assertEquals(created.id(), result.id());
            assertEquals("Task", result.action());
        }

        @Test
        @DisplayName("should throw 404 for non-existent task")
        void shouldThrow404() {
            final var ex = assertThrows(ResponseStatusException.class,
                    () -> taskService.getById(UUID.randomUUID()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update action and due date")
        void shouldUpdateActionAndDueDate() {
            final CompanyDto company = createCompany("Corp");
            final TaskDto created = taskService.create(new TaskCreateDto(
                    "Old", LocalDate.of(2026, 6, 1), null, company.id(), null, null));

            final TaskDto updated = taskService.update(created.id(),
                    new TaskUpdateDto("New action", LocalDate.of(2026, 7, 15), TaskStatus.OPEN, null));

            assertEquals("New action", updated.action());
            assertEquals(LocalDate.of(2026, 7, 15), updated.dueDate());
        }

        @Test
        @DisplayName("should update status")
        void shouldUpdateStatus() {
            final CompanyDto company = createCompany("Corp");
            final TaskDto created = taskService.create(new TaskCreateDto(
                    "Task", LocalDate.of(2026, 6, 1), null, company.id(), null, null));

            final TaskDto updated = taskService.update(created.id(),
                    new TaskUpdateDto("Task", LocalDate.of(2026, 6, 1), TaskStatus.DONE, null));

            assertEquals(TaskStatus.DONE, updated.status());
        }

        @Test
        @DisplayName("should update tags")
        void shouldUpdateTags() {
            final CompanyDto company = createCompany("Corp");
            final TagDto tag1 = createTag("Old");
            final TagDto tag2 = createTag("New");
            final TaskDto created = taskService.create(new TaskCreateDto(
                    "Task", LocalDate.of(2026, 6, 1), null, company.id(), null, List.of(tag1.id())));

            final TaskDto updated = taskService.update(created.id(),
                    new TaskUpdateDto("Task", LocalDate.of(2026, 6, 1), TaskStatus.OPEN, List.of(tag2.id())));

            assertEquals(1, updated.tagIds().size());
            assertTrue(updated.tagIds().contains(tag2.id()));
        }

        @Test
        @DisplayName("should preserve tags when null tagIds")
        void shouldPreserveTagsWhenNull() {
            final CompanyDto company = createCompany("Corp");
            final TagDto tag = createTag("Keep");
            final TaskDto created = taskService.create(new TaskCreateDto(
                    "Task", LocalDate.of(2026, 6, 1), null, company.id(), null, List.of(tag.id())));

            final TaskDto updated = taskService.update(created.id(),
                    new TaskUpdateDto("Task", LocalDate.of(2026, 6, 1), TaskStatus.OPEN, null));

            assertEquals(1, updated.tagIds().size());
            assertTrue(updated.tagIds().contains(tag.id()));
        }

        @Test
        @DisplayName("should remove all tags when empty tagIds")
        void shouldRemoveAllTagsWhenEmpty() {
            final CompanyDto company = createCompany("Corp");
            final TagDto tag = createTag("Remove");
            final TaskDto created = taskService.create(new TaskCreateDto(
                    "Task", LocalDate.of(2026, 6, 1), null, company.id(), null, List.of(tag.id())));

            final TaskDto updated = taskService.update(created.id(),
                    new TaskUpdateDto("Task", LocalDate.of(2026, 6, 1), TaskStatus.OPEN, List.of()));

            assertTrue(updated.tagIds().isEmpty());
        }

        @Test
        @DisplayName("should not change owner on update")
        void shouldNotChangeOwner() {
            final CompanyDto company = createCompany("Corp");
            final TaskDto created = taskService.create(new TaskCreateDto(
                    "Task", LocalDate.of(2026, 6, 1), null, company.id(), null, null));

            final TaskDto updated = taskService.update(created.id(),
                    new TaskUpdateDto("Updated", LocalDate.of(2026, 8, 1), TaskStatus.DONE, null));

            assertEquals(company.id(), updated.companyId());
        }

        @Test
        @DisplayName("should throw 404 for non-existent task")
        void shouldThrow404() {
            final var ex = assertThrows(ResponseStatusException.class,
                    () -> taskService.update(UUID.randomUUID(),
                            new TaskUpdateDto("Task", LocalDate.of(2026, 6, 1), TaskStatus.OPEN, null)));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete task")
        void shouldDelete() {
            final CompanyDto company = createCompany("Corp");
            final TaskDto created = taskService.create(new TaskCreateDto(
                    "Task", LocalDate.of(2026, 6, 1), null, company.id(), null, null));

            taskService.delete(created.id());

            final var ex = assertThrows(ResponseStatusException.class, () -> taskService.getById(created.id()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should throw 404 for non-existent task")
        void shouldThrow404() {
            final var ex = assertThrows(ResponseStatusException.class,
                    () -> taskService.delete(UUID.randomUUID()));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("list")
    class ListTasks {

        @Test
        @DisplayName("should list sorted by due date ascending")
        void shouldListSortedByDueDate() {
            final CompanyDto company = createCompany("Corp");
            taskService.create(new TaskCreateDto("Later", LocalDate.of(2026, 9, 1), null, company.id(), null, null));
            taskService.create(new TaskCreateDto("Earliest", LocalDate.of(2026, 3, 1), null, company.id(), null, null));
            taskService.create(new TaskCreateDto("Middle", LocalDate.of(2026, 6, 1), null, company.id(), null, null));

            final var page = taskService.list(null, null, PageRequest.of(0, 20, Sort.by("dueDate")));

            assertEquals(3, page.getTotalElements());
            assertEquals("Earliest", page.getContent().get(0).action());
            assertEquals("Middle", page.getContent().get(1).action());
            assertEquals("Later", page.getContent().get(2).action());
        }

        @Test
        @DisplayName("should filter by status")
        void shouldFilterByStatus() {
            final CompanyDto company = createCompany("Corp");
            taskService.create(new TaskCreateDto("Open", LocalDate.of(2026, 6, 1), TaskStatus.OPEN, company.id(), null, null));
            taskService.create(new TaskCreateDto("Done", LocalDate.of(2026, 6, 2), TaskStatus.DONE, company.id(), null, null));

            final var page = taskService.list(TaskStatus.OPEN, null, PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Open", page.getContent().get(0).action());
        }

        @Test
        @DisplayName("should filter by tags")
        void shouldFilterByTags() {
            final CompanyDto company = createCompany("Corp");
            final TagDto tag = createTag("Priority");
            taskService.create(new TaskCreateDto("Tagged", LocalDate.of(2026, 6, 1), null, company.id(), null, List.of(tag.id())));
            taskService.create(new TaskCreateDto("Untagged", LocalDate.of(2026, 6, 2), null, company.id(), null, null));

            final var page = taskService.list(null, List.of(tag.id()), PageRequest.of(0, 20));

            assertEquals(1, page.getTotalElements());
            assertEquals("Tagged", page.getContent().get(0).action());
        }

        @Test
        @DisplayName("should return empty page when no tasks")
        void shouldReturnEmptyPage() {
            final var page = taskService.list(null, null, PageRequest.of(0, 20));
            assertEquals(0, page.getTotalElements());
        }

        @Test
        @DisplayName("should cascade-delete tasks when company is hard-deleted")
        void shouldCascadeDeleteTasksWhenCompanyHardDeleted() {
            final CompanyDto company = createCompany("Corp");
            taskService.create(new TaskCreateDto("Task", LocalDate.of(2026, 6, 1), null, company.id(), null, null));
            companyService.delete(company.id(), false);

            final var page = taskService.list(null, null, PageRequest.of(0, 20));

            assertEquals(0, page.getTotalElements());
        }
    }

    @Nested
    @DisplayName("cascade")
    class Cascade {

        @Test
        @DisplayName("contact hard-delete cascades tasks")
        void contactDeleteCascadesTasks() {
            final ContactDto contact = createContact("Jane", "Doe");
            taskService.create(new TaskCreateDto("T1", LocalDate.of(2026, 6, 1), null, null, contact.id(), null));
            taskService.create(new TaskCreateDto("T2", LocalDate.of(2026, 6, 2), null, null, contact.id(), null));
            taskService.create(new TaskCreateDto("T3", LocalDate.of(2026, 6, 3), null, null, contact.id(), null));

            contactService.delete(contact.id());

            assertEquals(0, taskRepository.count());
        }

        @Test
        @DisplayName("company hard-delete cascades tasks")
        void companyHardDeleteCascadesTasks() {
            final CompanyDto company = createCompany("Corp");
            taskService.create(new TaskCreateDto("T1", LocalDate.of(2026, 6, 1), null, company.id(), null, null));
            taskService.create(new TaskCreateDto("T2", LocalDate.of(2026, 6, 2), null, company.id(), null, null));

            companyService.delete(company.id(), false);

            assertEquals(0, taskRepository.count());
        }

        @Test
        @DisplayName("tag deletion removes tag from tasks")
        void tagDeletionRemovesTagFromTasks() {
            final CompanyDto company = createCompany("Corp");
            final TagDto tag1 = createTag("Keep");
            final TagDto tag2 = createTag("Delete");
            final TaskDto task = taskService.create(new TaskCreateDto(
                    "Task", LocalDate.of(2026, 6, 1), null, company.id(), null, List.of(tag1.id(), tag2.id())));

            tagService.delete(tag2.id());

            final TaskDto refreshed = taskService.getById(task.id());
            assertEquals(1, refreshed.tagIds().size());
            assertTrue(refreshed.tagIds().contains(tag1.id()));
        }
    }
}
