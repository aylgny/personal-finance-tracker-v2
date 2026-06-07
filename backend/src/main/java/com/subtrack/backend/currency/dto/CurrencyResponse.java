package com.subtrack.backend.currency.dto;

import java.math.BigDecimal;

public record CurrencyResponse(
        Long id,
        String code,
        String symbol,
        String name,
        BigDecimal exchangeRate
) {
}