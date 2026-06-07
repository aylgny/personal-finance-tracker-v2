package com.subtrack.backend.currency;

import com.subtrack.backend.currency.dto.CurrencyResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    public CurrencyService(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    public List<CurrencyResponse> getCurrencies() {
        // Currencies are global reference data, so all users see the same list.
        return currencyRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CurrencyResponse toResponse(Currency currency) {
        // Convert the entity into a small DTO for frontend dropdown usage.
        return new CurrencyResponse(
                currency.getId(),
                currency.getCode(),
                currency.getSymbol(),
                currency.getName(),
                currency.getExchangeRate()
        );
    }
}