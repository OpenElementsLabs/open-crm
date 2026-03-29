package com.openelements.crm.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openelements.crm.company.CompanyEntity;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContactDtoTest {

    private static ContactEntity createEntity() throws Exception {
        Constructor<ContactEntity> ctor = ContactEntity.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ContactEntity entity = ctor.newInstance();
        entity.setFirstName("John");
        entity.setLastName("Doe");
        return entity;
    }

    private static CompanyEntity createCompanyEntity() throws Exception {
        Constructor<CompanyEntity> ctor = CompanyEntity.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        CompanyEntity entity = ctor.newInstance();
        entity.setName("Acme Corp");
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
        @DisplayName("Maps all scalar fields from entity")
        void mapsAllScalarFieldsFromEntity() throws Exception {
            ContactEntity entity = createEntity();

            UUID id = UUID.randomUUID();
            Instant createdAt = Instant.parse("2025-03-10T08:00:00Z");
            Instant updatedAt = Instant.parse("2025-03-15T12:00:00Z");
            LocalDate birthday = LocalDate.of(1990, 5, 20);

            setField(entity, "id", id);
            entity.setFirstName("Jane");
            entity.setLastName("Smith");
            entity.setEmail("jane@example.com");
            entity.setPosition("CTO");
            entity.setGender(Gender.FEMALE);
            entity.setLinkedInUrl("https://linkedin.com/in/janesmith");
            entity.setPhoneNumber("+49 123 456");
            entity.setBirthday(birthday);
            entity.setSyncedToBrevo(true);
            entity.setLanguage(Language.DE);
            setField(entity, "createdAt", createdAt);
            setField(entity, "updatedAt", updatedAt);

            ContactDto dto = ContactDto.fromEntity(entity, 7);

            assertEquals(id, dto.id());
            assertEquals("Jane", dto.firstName());
            assertEquals("Smith", dto.lastName());
            assertEquals("jane@example.com", dto.email());
            assertEquals("CTO", dto.position());
            assertEquals(Gender.FEMALE, dto.gender());
            assertEquals("https://linkedin.com/in/janesmith", dto.linkedInUrl());
            assertEquals("+49 123 456", dto.phoneNumber());
            assertEquals(birthday, dto.birthday());
            assertTrue(dto.syncedToBrevo());
            assertEquals(Language.DE, dto.language());
            assertEquals(7, dto.commentCount());
            assertEquals(createdAt, dto.createdAt());
            assertEquals(updatedAt, dto.updatedAt());
        }

        @Test
        @DisplayName("Resolves company name and deleted status when company is set")
        void resolvesCompanyFieldsWhenCompanySet() throws Exception {
            ContactEntity entity = createEntity();
            CompanyEntity company = createCompanyEntity();

            UUID companyId = UUID.randomUUID();
            setField(company, "id", companyId);
            company.setName("Big Corp");
            company.setDeleted(true);

            entity.setCompany(company);

            ContactDto dto = ContactDto.fromEntity(entity, 0);

            assertEquals(companyId, dto.companyId());
            assertEquals("Big Corp", dto.companyName());
            assertTrue(dto.companyDeleted());
        }

        @Test
        @DisplayName("Sets company fields to null/false when no company is set")
        void setsCompanyFieldsNullWhenNoCompany() throws Exception {
            ContactEntity entity = createEntity();

            ContactDto dto = ContactDto.fromEntity(entity, 0);

            assertNull(dto.companyId());
            assertNull(dto.companyName());
            assertFalse(dto.companyDeleted());
        }

        @Test
        @DisplayName("Sets hasPhoto to true when photo bytes are present")
        void setsHasPhotoTrueWhenPhotoBytesPresent() throws Exception {
            ContactEntity entity = createEntity();
            entity.setPhoto(new byte[]{10, 20, 30});

            ContactDto dto = ContactDto.fromEntity(entity, 0);

            assertTrue(dto.hasPhoto());
        }

        @Test
        @DisplayName("Sets hasPhoto to false when photo bytes are null")
        void setsHasPhotoFalseWhenPhotoBytesNull() throws Exception {
            ContactEntity entity = createEntity();

            ContactDto dto = ContactDto.fromEntity(entity, 0);

            assertFalse(dto.hasPhoto());
        }
    }
}
