package com.subtrack.backend.paymentmethod;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // Returns all payment methods owned by a specific user.
    List<PaymentMethod> findByUserIdOrderBySortOrderAscNameAsc(Long userId);

    // Returns only enabled payment methods for a specific user.
    List<PaymentMethod> findByUserIdAndEnabledTrueOrderBySortOrderAscNameAsc(Long userId);

    // Finds a payment method only if it belongs to the authenticated user.
    // This prevents users from using another user's paymentMethodId.
    Optional<PaymentMethod> findByIdAndUserId(Long id, Long userId);
}