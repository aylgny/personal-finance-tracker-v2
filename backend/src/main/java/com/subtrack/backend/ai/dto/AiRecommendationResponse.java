package com.subtrack.backend.ai.dto;

import java.util.List;

public record AiRecommendationResponse(
        List<RecommendationItem> recommendations
) {
    public record RecommendationItem(
            String title,
            String description,
            String saving
    ) {
    }
}