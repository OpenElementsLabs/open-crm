package com.openelements.crm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.contact.ContactEntity;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TaskDtoTest {

    private static TaskEntity createEntity() throws Exception {
        Constructor<TaskEntity> ctor = TaskEntity.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        TaskEntity entity = ctor.newInstance();
        entity.setAction("Test task");
        entity.setDueDate(LocalDate.of(2026, 6, 15));
        entity.setStatus(TaskStatus.OPEN);
        return entity;
    }

    private static <T> T createWithReflection(Class<T> clazz) throws Exception {
        Constructor<T> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Nested
    @DisplayName("fromEntity mapping")
    class FromEntityMapping {

        @Test
        @DisplayName("Maps all scalar fields from entity")
        void mapsAllScalarFields() throws Exception {
            TaskEntity entity = createEntity();
            UUID id = UUID.randomUUID();
            Instant createdAt = Instant.parse("2026-01-15T10:00:00Z");
            Instant updatedAt = Instant.parse("2026-01-16T10:00:00Z");

            setField(entity, "id", id);
            entity.setAction("Call back client");
            entity.setDueDate(LocalDate.of(2026, 4, 1));
            entity.setStatus(TaskStatus.IN_PROGRESS);
            setField(entity, "createdAt", createdAt);
            setField(entity, "updatedAt", updatedAt);

            TaskDto dto = TaskDto.fromEntity(entity);

            assertEquals(id, dto.id());
            assertEquals("Call back client", dto.action());
            assertEquals(LocalDate.of(2026, 4, 1), dto.dueDate());
            assertEquals(TaskStatus.IN_PROGRESS, dto.status());
            assertEquals(createdAt, dto.createdAt());
            assertEquals(updatedAt, dto.updatedAt());
        }

        @Test
        @DisplayName("Maps company fields when company is set")
        void mapsCompanyFields() throws Exception {
            TaskEntity entity = createEntity();
            CompanyEntity company = createWithReflection(CompanyEntity.class);
            UUID companyId = UUID.randomUUID();
            setField(company, "id", companyId);
            company.setName("Acme Corp");
            entity.setCompany(company);

            TaskDto dto = TaskDto.fromEntity(entity);

            assertEquals(companyId, dto.companyId());
            assertEquals("Acme Corp", dto.companyName());
            assertNull(dto.contactId());
            assertNull(dto.contactName());
        }

        @Test
        @DisplayName("Maps contact fields when contact is set")
        void mapsContactFields() throws Exception {
            TaskEntity entity = createEntity();
            ContactEntity contact = createWithReflection(ContactEntity.class);
            UUID contactId = UUID.randomUUID();
            setField(contact, "id", contactId);
            contact.setFirstName("Jane");
            contact.setLastName("Doe");
            entity.setContact(contact);

            TaskDto dto = TaskDto.fromEntity(entity);

            assertNull(dto.companyId());
            assertNull(dto.companyName());
            assertEquals(contactId, dto.contactId());
            assertEquals("Jane Doe", dto.contactName());
        }

        @Test
        @DisplayName("Maps empty tag list when no tags assigned")
        void mapsEmptyTagList() throws Exception {
            TaskEntity entity = createEntity();

            TaskDto dto = TaskDto.fromEntity(entity);

            assertNotNull(dto.tagIds());
            assertTrue(dto.tagIds().isEmpty());
        }
    }
}
