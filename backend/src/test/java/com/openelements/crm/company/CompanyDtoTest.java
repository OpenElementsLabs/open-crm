package com.openelements.crm.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CompanyDtoTest {

    private static CompanyEntity createEntity() throws Exception {
        Constructor<CompanyEntity> ctor = CompanyEntity.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        CompanyEntity entity = ctor.newInstance();
        entity.setName("Test Company");
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
            CompanyEntity entity = createEntity();

            UUID id = UUID.randomUUID();
            Instant createdAt = Instant.parse("2025-01-15T10:30:00Z");
            Instant updatedAt = Instant.parse("2025-06-20T14:00:00Z");

            setField(entity, "id", id);
            entity.setName("Acme Corp");
            entity.setEmail("info@acme.com");
            entity.setWebsite("https://acme.com");
            entity.setStreet("Main Street");
            entity.setHouseNumber("42");
            entity.setZipCode("12345");
            entity.setCity("Springfield");
            entity.setCountry("US");
            entity.setDeleted(true);
            setField(entity, "createdAt", createdAt);
            setField(entity, "updatedAt", updatedAt);

            CompanyDto dto = CompanyDto.fromEntity(entity, 5, 3);

            assertEquals(id, dto.id());
            assertEquals("Acme Corp", dto.name());
            assertEquals("info@acme.com", dto.email());
            assertEquals("https://acme.com", dto.website());
            assertEquals("Main Street", dto.street());
            assertEquals("42", dto.houseNumber());
            assertEquals("12345", dto.zipCode());
            assertEquals("Springfield", dto.city());
            assertEquals("US", dto.country());
            assertTrue(dto.deleted());
            assertEquals(5, dto.contactCount());
            assertEquals(3, dto.commentCount());
            assertEquals(createdAt, dto.createdAt());
            assertEquals(updatedAt, dto.updatedAt());
        }

        @Test
        @DisplayName("Sets hasLogo to true when logo bytes are present")
        void setsHasLogoTrueWhenLogoBytesPresent() throws Exception {
            CompanyEntity entity = createEntity();
            entity.setLogo(new byte[]{1, 2, 3});

            CompanyDto dto = CompanyDto.fromEntity(entity, 0, 0);

            assertTrue(dto.hasLogo());
        }

        @Test
        @DisplayName("Sets hasLogo to false when logo bytes are null")
        void setsHasLogoFalseWhenLogoBytesNull() throws Exception {
            CompanyEntity entity = createEntity();

            CompanyDto dto = CompanyDto.fromEntity(entity, 0, 0);

            assertFalse(dto.hasLogo());
        }

        @Test
        @DisplayName("Sets brevo to true when brevoCompanyId is present")
        void setsBrevoTrueWhenBrevoCompanyIdPresent() throws Exception {
            CompanyEntity entity = createEntity();
            entity.setBrevoCompanyId("brevo-123");

            CompanyDto dto = CompanyDto.fromEntity(entity, 0, 0);

            assertTrue(dto.brevo());
        }

        @Test
        @DisplayName("Sets brevo to false when brevoCompanyId is null")
        void setsBrevoFalseWhenBrevoCompanyIdNull() throws Exception {
            CompanyEntity entity = createEntity();

            CompanyDto dto = CompanyDto.fromEntity(entity, 0, 0);

            assertFalse(dto.brevo());
        }

        @Test
        @DisplayName("Maps description when present")
        void mapsDescriptionWhenPresent() throws Exception {
            CompanyEntity entity = createEntity();
            entity.setDescription("A test company");

            CompanyDto dto = CompanyDto.fromEntity(entity, 0, 0);

            assertEquals("A test company", dto.description());
        }

        @Test
        @DisplayName("Maps description as null when absent")
        void mapsDescriptionNullWhenAbsent() throws Exception {
            CompanyEntity entity = createEntity();

            CompanyDto dto = CompanyDto.fromEntity(entity, 0, 0);

            assertNull(dto.description());
        }

        @Test
        @DisplayName("Handles null optional fields")
        void handlesNullOptionalFields() throws Exception {
            CompanyEntity entity = createEntity();

            CompanyDto dto = CompanyDto.fromEntity(entity, 0, 0);

            assertNull(dto.email());
            assertNull(dto.website());
            assertNull(dto.street());
            assertNull(dto.houseNumber());
            assertNull(dto.zipCode());
            assertNull(dto.city());
            assertNull(dto.country());
            assertFalse(dto.deleted());
        }
    }
}
