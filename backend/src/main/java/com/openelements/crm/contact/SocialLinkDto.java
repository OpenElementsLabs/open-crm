package com.openelements.crm.contact;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Social network link")
public record SocialLinkDto(
    @Schema(description = "Network type", example = "GITHUB") String networkType,
    @Schema(description = "Normalized value (username/handle)") String value,
    @Schema(description = "Full URL") String url
) {
    public static SocialLinkDto fromEntity(final SocialLinkEntity entity) {
        return new SocialLinkDto(
            entity.getNetworkType().name(),
            entity.getValue(),
            entity.getUrl()
        );
    }
}
