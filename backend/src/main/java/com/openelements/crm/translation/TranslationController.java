package com.openelements.crm.translation;

import com.openelements.spring.base.services.translation.Language;
import com.openelements.spring.base.services.translation.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for the translation feature. Exposes a configuration probe endpoint and a
 * proxy endpoint that translates text via an OpenAI-compatible API.
 */
@RestController
@RequestMapping("/api/translate")
@Tag(name = "Translation", description = "Translate descriptions and comments")
@SecurityRequirement(name = "oidc")
public class TranslationController {

    private final TranslationService translationService;

    /**
     * Creates a new TranslationController.
     *
     * @param translationService the translation service
     */
    public TranslationController(final TranslationService translationService) {
        this.translationService = Objects.requireNonNull(translationService,
                "translationService must not be null");
    }

    /**
     * Returns whether the translation feature is configured. Used by the frontend to decide
     * whether to show translate buttons.
     *
     * @return the configuration status
     */
    @GetMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get translation configuration status")
    @ApiResponse(responseCode = "200", description = "Configuration status returned")
    public TranslationConfigDto getSettings() {
        return new TranslationConfigDto(translationService.isConfigured());
    }

    /**
     * Translates the given text into the requested target language.
     *
     * @param request the translation request
     * @return the translated text
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Translate text")
    @ApiResponse(responseCode = "200", description = "Translation returned")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "502", description = "Upstream translation API failed")
    @ApiResponse(responseCode = "503", description = "Translation feature not configured")
    public TranslateResponseDto translate(@Valid @RequestBody final TranslateRequestDto request) {
        final Language language;
        try {
            language = Language.valueOf(request.targetLanguage().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported target language: " + request.targetLanguage());
        }
        final String translated = translationService.translate(request.text(), language);
        return new TranslateResponseDto(translated);
    }
}
