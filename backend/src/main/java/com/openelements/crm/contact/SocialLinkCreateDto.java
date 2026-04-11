package com.openelements.crm.contact;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating a social link")
public record SocialLinkCreateDto(
    @NotBlank @Size(max = 20)
    @Schema(description = "Network type", example = "GITHUB") String networkType,
    @NotBlank @Size(max = 500)
    @Schema(description = "Username, handle, or full URL", example = "hendrikebbers") String value
) {
}
