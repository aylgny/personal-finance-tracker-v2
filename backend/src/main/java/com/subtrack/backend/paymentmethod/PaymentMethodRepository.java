package com.subtrack.backend.paymentmethod;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // Returns enabled global payment methods for dropdowns.
    List<PaymentMethod> findByEnabledTrueOrderBySortOrderAscNameAsc();
}