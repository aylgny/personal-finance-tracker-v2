package com.subtrack.backend.paymentmethod.dto;

public record PaymentMethodResponse(
        Long id,
        String name,
        Boolean enabled,
        Integer sortOrder
) {
}