package com.openelements.crm.tag;

import java.time.Instant;
import java.util.UUID;

public record TagDto(UUID id, String name, String description, String color,
                     Instant createdAt, Instant updatedAt) {

    public static TagDto fromEntity(final TagEntity entity) {
        return new TagDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getColor(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
