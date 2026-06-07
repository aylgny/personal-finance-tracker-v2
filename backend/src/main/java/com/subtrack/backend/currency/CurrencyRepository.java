package com.subtrack.backend.currency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    // Returns the global currency list for dropdowns.
    List<Currency> findAllByOrderByCodeAsc();
}