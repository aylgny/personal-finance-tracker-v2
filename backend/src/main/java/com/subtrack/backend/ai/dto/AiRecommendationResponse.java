package com.subtrack.backend.ai.dto;

import java.util.List;

public record AiRecommendationResponse(
        List<String> recommendations
) {
}