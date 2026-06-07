package com.subtrack.backend.currency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    // Returns all currencies owned by a specific user.
    List<Currency> findByUserIdOrderByCodeAsc(Long userId);

    // Finds a currency only if it belongs to the authenticated user.
    // This prevents IDOR issues when users submit currencyId values.
    Optional<Currency> findByIdAndUserId(Long id, Long userId);

    // Finds a currency by code within a specific user's currency list.
    Optional<Currency> findByUserIdAndCode(Long userId, String code);
}