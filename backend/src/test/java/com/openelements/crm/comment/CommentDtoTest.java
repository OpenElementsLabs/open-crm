package com.openelements.crm.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.contact.ContactEntity;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommentDtoTest {

    private static CommentEntity createEntity() throws Exception {
        Constructor<CommentEntity> ctor = CommentEntity.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        CommentEntity entity = ctor.newInstance();
        entity.setText("A comment");
        entity.setAuthor("admin");
        return entity;
    }

    private static CompanyEntity createCompanyEntity() throws Exception {
        Constructor<CompanyEntity> ctor = CompanyEntity.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        CompanyEntity entity = ctor.newInstance();
        entity.setName("Test Company");
        return entity;
    }

    private static ContactEntity createContactEntity() throws Exception {
        Constructor<ContactEntity> ctor = ContactEntity.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ContactEntity entity = ctor.newInstance();
        entity.setFirstName("John");
        entity.setLastName("Doe");
        return entity;
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
        @DisplayName("Maps all fields for a company comment")
        void mapsAllFieldsForCompanyComment() throws Exception {
            CommentEntity entity = createEntity();

            UUID id = UUID.randomUUID();
            Instant createdAt = Instant.parse("2025-04-01T09:00:00Z");
            Instant updatedAt = Instant.parse("2025-04-02T10:00:00Z");

            setField(entity, "id", id);
            entity.setText("Company feedback");
            entity.setAuthor("reviewer");

            setField(entity, "createdAt", createdAt);
            setField(entity, "updatedAt", updatedAt);

            CompanyEntity company = createCompanyEntity();
            UUID companyId = UUID.randomUUID();
            setField(company, "id", companyId);
            entity.setCompany(company);

            CommentDto dto = CommentDto.fromEntity(entity);

            assertEquals(id, dto.id());
            assertEquals("Company feedback", dto.text());
            assertEquals("reviewer", dto.author());
            assertEquals(companyId, dto.companyId());
            assertNull(dto.contactId());
            assertEquals(createdAt, dto.createdAt());
            assertEquals(updatedAt, dto.updatedAt());
        }

        @Test
        @DisplayName("Maps all fields for a contact comment")
        void mapsAllFieldsForContactComment() throws Exception {
            CommentEntity entity = createEntity();

            UUID id = UUID.randomUUID();
            Instant createdAt = Instant.parse("2025-05-01T11:00:00Z");
            Instant updatedAt = Instant.parse("2025-05-02T12:00:00Z");

            setField(entity, "id", id);
            entity.setText("Contact note");
            entity.setAuthor("sales");

            setField(entity, "createdAt", createdAt);
            setField(entity, "updatedAt", updatedAt);

            ContactEntity contact = createContactEntity();
            UUID contactId = UUID.randomUUID();
            setField(contact, "id", contactId);
            entity.setContact(contact);

            CommentDto dto = CommentDto.fromEntity(entity);

            assertEquals(id, dto.id());
            assertEquals("Contact note", dto.text());
            assertEquals("sales", dto.author());
            assertNull(dto.companyId());
            assertEquals(contactId, dto.contactId());
            assertEquals(createdAt, dto.createdAt());
            assertEquals(updatedAt, dto.updatedAt());
        }

        @Test
        @DisplayName("Handles entity with both company and contact null")
        void handlesBothCompanyAndContactNull() throws Exception {
            CommentEntity entity = createEntity();

            UUID id = UUID.randomUUID();
            setField(entity, "id", id);

            CommentDto dto = CommentDto.fromEntity(entity);

            assertEquals(id, dto.id());
            assertNull(dto.companyId());
            assertNull(dto.contactId());
        }
    }
}
