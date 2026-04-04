package com.openelements.crm.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("WebhookService")
class WebhookServiceTest {

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private WebhookRepository webhookRepository;

    @BeforeEach
    void setUp() {
        webhookRepository.deleteAll();
    }

    private WebhookDto createWebhook(final String url) {
        return webhookService.create(new WebhookCreateDto(url));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create webhook with valid URL")
        void shouldCreateWithValidUrl() {
            final WebhookDto result = createWebhook("https://example.com/hook");

            assertNotNull(result.id());
            assertEquals("https://example.com/hook", result.url());
            assertTrue(result.active());
            assertNotNull(result.createdAt());
            assertNotNull(result.updatedAt());
        }

        @Test
        @DisplayName("should persist webhook to database")
        void shouldPersistToDatabase() {
            final WebhookDto created = createWebhook("https://example.com/hook");
            final WebhookDto found = webhookService.getById(created.id());

            assertEquals(created.id(), found.id());
            assertEquals(created.url(), found.url());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return webhook by ID")
        void shouldReturnById() {
            final WebhookDto created = createWebhook("https://example.com/hook");
            final WebhookDto found = webhookService.getById(created.id());

            assertEquals(created.id(), found.id());
            assertEquals("https://example.com/hook", found.url());
        }

        @Test
        @DisplayName("should throw 404 for unknown ID")
        void shouldThrow404ForUnknownId() {
            final var exception = assertThrows(ResponseStatusException.class,
                    () -> webhookService.getById(java.util.UUID.randomUUID()));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update URL")
        void shouldUpdateUrl() {
            final WebhookDto created = createWebhook("https://old.com/hook");
            final WebhookDto updated = webhookService.update(created.id(),
                    new WebhookUpdateDto("https://new.com/hook", true));

            assertEquals("https://new.com/hook", updated.url());
            assertTrue(updated.active());
        }

        @Test
        @DisplayName("should deactivate webhook")
        void shouldDeactivate() {
            final WebhookDto created = createWebhook("https://example.com/hook");
            final WebhookDto updated = webhookService.update(created.id(),
                    new WebhookUpdateDto("https://example.com/hook", false));

            assertFalse(updated.active());
        }

        @Test
        @DisplayName("should throw 404 for unknown ID")
        void shouldThrow404ForUnknownId() {
            final var exception = assertThrows(ResponseStatusException.class,
                    () -> webhookService.update(java.util.UUID.randomUUID(),
                            new WebhookUpdateDto("https://example.com", true)));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete webhook")
        void shouldDelete() {
            final WebhookDto created = createWebhook("https://example.com/hook");
            webhookService.delete(created.id());

            final var exception = assertThrows(ResponseStatusException.class,
                    () -> webhookService.getById(created.id()));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }

        @Test
        @DisplayName("should throw 404 for unknown ID")
        void shouldThrow404ForUnknownId() {
            final var exception = assertThrows(ResponseStatusException.class,
                    () -> webhookService.delete(java.util.UUID.randomUUID()));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }

    @Nested
    @DisplayName("list")
    class ListWebhooks {

        @Test
        @DisplayName("should return paginated results")
        void shouldReturnPaginated() {
            for (int i = 0; i < 25; i++) {
                createWebhook("https://example.com/hook" + i);
            }

            final var page = webhookService.list(PageRequest.of(0, 20));
            assertEquals(20, page.getContent().size());
            assertEquals(25, page.getTotalElements());
        }

        @Test
        @DisplayName("should return empty when no webhooks exist")
        void shouldReturnEmptyWhenNone() {
            final var page = webhookService.list(PageRequest.of(0, 20));
            assertTrue(page.getContent().isEmpty());
            assertEquals(0, page.getTotalElements());
        }
    }
}
