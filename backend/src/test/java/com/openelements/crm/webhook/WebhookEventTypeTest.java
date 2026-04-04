package com.openelements.crm.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WebhookEventType")
class WebhookEventTypeTest {

    @Test
    @DisplayName("should contain exactly 18 event types")
    void shouldContainExactly18EventTypes() {
        assertEquals(18, WebhookEventType.values().length);
    }

    @Test
    @DisplayName("should contain all expected event types")
    void shouldContainAllExpectedTypes() {
        final Set<String> expected = Set.of(
                "COMPANY_CREATED", "COMPANY_UPDATED", "COMPANY_DELETED",
                "CONTACT_CREATED", "CONTACT_UPDATED", "CONTACT_DELETED",
                "COMMENT_CREATED", "COMMENT_UPDATED", "COMMENT_DELETED",
                "TAG_CREATED", "TAG_UPDATED", "TAG_DELETED",
                "TASK_CREATED", "TASK_UPDATED", "TASK_DELETED",
                "USER_CREATED", "USER_UPDATED", "USER_DELETED"
        );

        for (final WebhookEventType type : WebhookEventType.values()) {
            assertNotNull(type.name());
        }

        for (final String name : expected) {
            assertNotNull(WebhookEventType.valueOf(name), "Missing event type: " + name);
        }
    }
}
