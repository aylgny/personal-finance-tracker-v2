package com.subtrack.backend.category.dto;

public record CategoryResponse(
        Long id,
        String name,
        Integer sortOrder
) {
}