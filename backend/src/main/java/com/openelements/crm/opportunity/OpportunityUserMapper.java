package com.openelements.crm.opportunity;

import com.openelements.spring.base.services.user.UserDto;
import com.openelements.spring.base.services.user.UserEntity;

/**
 * Maps a {@link UserEntity} to the nested {@link UserDto} carried by {@link OpportunityDto#owner()}.
 * Kept as a shared helper so the service and the search bootstrap step produce identical owner DTOs.
 */
final class OpportunityUserMapper {

    private OpportunityUserMapper() {
    }

    static UserDto toUserDto(final UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserDto(
            entity.getId(),
            entity.getName(),
            entity.getEmail(),
            entity.getAvatarUrl(),
            entity.getCreatedAt(),
            entity.getUpdatedAt());
    }
}
