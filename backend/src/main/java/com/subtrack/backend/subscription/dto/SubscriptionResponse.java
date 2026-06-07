package com.subtrack.backend.subscription.dto;

import com.subtrack.backend.subscription.BillingCycle;
import com.subtrack.backend.subscription.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(
        Long id,
        String name,
        String provider,
        BigDecimal price,
        String currencyCode,
        String currencySymbol,
        BillingCycle billingCycle,
        SubscriptionStatus status,
        LocalDate startDate,
        LocalDate nextPaymentDate,
        Boolean autoRenew,
        Boolean notifyEnabled,
        Integer notifyDaysBefore,
        String categoryName,
        String paymentMethodName,
        String websiteUrl,
        String notes
) {
}