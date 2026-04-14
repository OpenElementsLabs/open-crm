package com.openelements.crm.tag;

import com.openelements.spring.base.data.WithId;

import java.util.UUID;

public record TagDtoWithCounts(UUID id, String name, String description, String color, long companyCount,
                               long contactCount, long taskCount) implements WithId {

}
