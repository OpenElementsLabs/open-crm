package com.openelements.crm.brevo;

import com.openelements.crm.settings.SettingsRepository;
import com.openelements.crm.settings.SettingsService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static com.openelements.crm.TestSecurityUtil.testJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("BrevoSyncController")
class BrevoSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private SettingsRepository settingsRepository;

    @MockitoBean
    private BrevoApiClient brevoApiClient;

    @BeforeEach
    void setUp() {
        settingsRepository.deleteAll();
        reset(brevoApiClient);
    }

    @Nested
    @DisplayName("Settings endpoints")
    class Settings {

        @Test
        @DisplayName("GET /api/brevo/settings returns apiKeyConfigured=false when no key")
        void getSettingsReturnsFalseWhenNoKey() throws Exception {
            mockMvc.perform(get("/api/brevo/settings").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.apiKeyConfigured").value(false));
        }

        @Test
        @DisplayName("GET /api/brevo/settings returns apiKeyConfigured=true when key exists")
        void getSettingsReturnsTrueWhenKeyExists() throws Exception {
            settingsService.set("brevo.api-key", "test-key");

            mockMvc.perform(get("/api/brevo/settings").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.apiKeyConfigured").value(true));
        }

        @Test
        @DisplayName("PUT /api/brevo/settings stores valid key")
        void putSettingsStoresValidKey() throws Exception {
            doNothing().when(brevoApiClient).validateApiKey("xkeysib-test");

            mockMvc.perform(put("/api/brevo/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"apiKey\":\"xkeysib-test\"}").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.apiKeyConfigured").value(true));

            mockMvc.perform(get("/api/brevo/settings").with(testJwt()))
                    .andExpect(jsonPath("$.apiKeyConfigured").value(true));
        }

        @Test
        @DisplayName("PUT /api/brevo/settings rejects blank key")
        void putSettingsRejectsBlankKey() throws Exception {
            mockMvc.perform(put("/api/brevo/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"apiKey\":\"\"}").with(testJwt()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PUT /api/brevo/settings rejects invalid key")
        void putSettingsRejectsInvalidKey() throws Exception {
            doThrow(new ResponseStatusException(HttpStatusCode.valueOf(400), "Invalid Brevo API key"))
                    .when(brevoApiClient).validateApiKey("bad-key");

            mockMvc.perform(put("/api/brevo/settings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"apiKey\":\"bad-key\"}").with(testJwt()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("DELETE /api/brevo/settings removes key")
        void deleteSettingsRemovesKey() throws Exception {
            settingsService.set("brevo.api-key", "test-key");

            mockMvc.perform(delete("/api/brevo/settings").with(testJwt()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/brevo/settings").with(testJwt()))
                    .andExpect(jsonPath("$.apiKeyConfigured").value(false));
        }
    }

    @Nested
    @DisplayName("Sync endpoint")
    class Sync {

        @Test
        @DisplayName("POST /api/brevo/sync returns 400 when no API key configured")
        void syncReturns400WhenNoApiKey() throws Exception {
            mockMvc.perform(post("/api/brevo/sync").with(testJwt()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/brevo/sync triggers sync and returns result")
        void syncTriggersAndReturnsResult() throws Exception {
            settingsService.set("brevo.api-key", "test-key");
            when(brevoApiClient.fetchAllCompanies()).thenReturn(List.of());
            when(brevoApiClient.fetchAllContacts()).thenReturn(List.of());

            mockMvc.perform(post("/api/brevo/sync").with(testJwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.companiesImported").value(0))
                    .andExpect(jsonPath("$.companiesUpdated").value(0))
                    .andExpect(jsonPath("$.contactsImported").value(0))
                    .andExpect(jsonPath("$.contactsUpdated").value(0));
        }
    }
}
