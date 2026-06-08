package com.subtrack.backend.ai.dto;

import java.util.List;

public record GeminiRequest(
        List<Content> contents
) {
    public record Content(
            List<Part> parts
    ) {
    }

    public record Part(
            String text
    ) {
    }

    public static GeminiRequest fromPrompt(String prompt) {
        // Gemini expects a list of contents, each content has one or more text parts.
        return new GeminiRequest(
                List.of(
                        new Content(
                                List.of(new Part(prompt))
                        )
                )
        );
    }
}