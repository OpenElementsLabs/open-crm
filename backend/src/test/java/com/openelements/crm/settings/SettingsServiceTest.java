package com.openelements.crm.settings;

import java.util.Optional;

import com.openelements.spring.base.services.settings.SettingsDataService;
import com.openelements.spring.base.services.settings.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SettingsService")
class SettingsServiceTest {

    @Autowired
    private SettingsDataService settingsService;

    @Autowired
    private SettingsRepository settingsRepository;

    @BeforeEach
    void setUp() {
        settingsRepository.deleteAll();
    }

    @Test
    @DisplayName("get returns empty for nonexistent key")
    void getReturnsEmptyForNonexistentKey() {
        final Optional<String> result = settingsService.get("nonexistent.key");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("set stores a new value")
    void setStoresNewValue() {
        settingsService.set("test.key", "test-value");

        assertTrue(settingsRepository.findById("test.key").isPresent());
        assertEquals("test-value", settingsRepository.findById("test.key").get().getValue());
    }

    @Test
    @DisplayName("set updates existing value (upsert)")
    void setUpdatesExistingValue() {
        settingsService.set("test.key", "original");

        settingsService.set("test.key", "updated");

        assertEquals("updated", settingsService.get("test.key").orElse(null));
        assertEquals(1, settingsRepository.count());
    }

    @Test
    @DisplayName("get returns stored value")
    void getReturnsStoredValue() {
        settingsService.set("my.key", "my-value");

        final Optional<String> result = settingsService.get("my.key");

        assertTrue(result.isPresent());
        assertEquals("my-value", result.get());
    }

    @Test
    @DisplayName("delete removes value")
    void deleteRemovesValue() {
        settingsService.set("to.delete", "value");

        settingsService.delete("to.delete");

        assertTrue(settingsService.get("to.delete").isEmpty());
    }

    @Test
    @DisplayName("delete nonexistent key does not throw")
    void deleteNonexistentKeyDoesNotThrow() {
        assertDoesNotThrow(() -> settingsService.delete("nonexistent.key"));
    }
}
