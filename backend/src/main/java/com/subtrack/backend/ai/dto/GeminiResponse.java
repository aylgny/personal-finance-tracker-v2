package com.subtrack.backend.ai.dto;

import java.util.List;

public record GeminiResponse(
        List<Candidate> candidates
) {
    public record Candidate(
            Content content
    ) {
    }

    public record Content(
            List<Part> parts
    ) {
    }

    public record Part(
            String text
    ) {
    }

    public String firstTextOrEmpty() {
        // Safely extracts the first generated text from Gemini response.
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }

        Candidate candidate = candidates.get(0);

        if (candidate == null || candidate.content() == null) {
            return "";
        }

        List<Part> parts = candidate.content().parts();

        if (parts == null || parts.isEmpty() || parts.get(0) == null) {
            return "";
        }

        return parts.get(0).text() == null ? "" : parts.get(0).text();
    }
}