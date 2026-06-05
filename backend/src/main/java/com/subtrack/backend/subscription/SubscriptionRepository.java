package com.subtrack.backend.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdOrderByNextPaymentDateAsc(Long userId);

    List<Subscription> findByUserIdAndStatusOrderByNextPaymentDateAsc(
            Long userId,
            SubscriptionStatus status
    );

    Optional<Subscription> findByIdAndUserId(Long id, Long userId);

    List<Subscription> findByUserIdAndNextPaymentDateBetweenOrderByNextPaymentDateAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}