package com.openelements.crm.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Direct unit tests for {@link TranslationService} that exercise the configuration logic and
 * the input validation path of {@link TranslationService#translate(String, String)} without
 * spinning up a Spring context or making upstream HTTP calls.
 */
class TranslationServiceTest {

    @Test
    void serviceIsUnconfiguredWhenAllEnvVarsMissing() {
        final TranslationService service = new TranslationService("", "", "");
        assertFalse(service.isConfigured());
    }

    @Test
    void serviceIsUnconfiguredWhenAnyEnvVarMissing() {
        assertFalse(new TranslationService("https://example.com", "", "model").isConfigured());
        assertFalse(new TranslationService("", "key", "model").isConfigured());
        assertFalse(new TranslationService("https://example.com", "key", "").isConfigured());
    }

    @Test
    void serviceIsUnconfiguredWhenEnvVarsAreWhitespace() {
        final TranslationService service = new TranslationService("   ", "   ", "   ");
        assertFalse(service.isConfigured());
    }

    @Test
    void serviceIsConfiguredWhenAllEnvVarsSet() {
        final TranslationService service = new TranslationService(
                "https://example.com", "key", "model");
        assertTrue(service.isConfigured());
    }

    @Test
    void translateThrows503WhenNotConfigured() {
        final TranslationService service = new TranslationService("", "", "");
        final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.translate("Hallo", "en"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    void translateThrows400ForUnsupportedLanguage() {
        final TranslationService service = new TranslationService(
                "https://example.com", "key", "model");
        final ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.translate("Hallo", "fr"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
