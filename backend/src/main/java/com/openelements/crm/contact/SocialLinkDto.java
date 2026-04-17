package com.openelements.crm.contact;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Social network link")
public record SocialLinkDto(
    @Schema(description = "Network type", example = "GITHUB") String networkType,
    @Schema(description = "Normalized value (username/handle)") String value,
    @Schema(description = "Full URL") String url
) {
    public static SocialLinkDto fromCreateDto(SocialLinkCreateDto socialLinkCreateDto) {
        return new SocialLinkDto(
            socialLinkCreateDto.networkType(),
            socialLinkCreateDto.value(),
            null // URL will be generated based on network type and value
        );
    }
}
