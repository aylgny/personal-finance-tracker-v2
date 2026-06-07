package com.subtrack.backend.subscription.dto;

import com.subtrack.backend.subscription.BillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSubscriptionRequest(

        @NotBlank(message = "Subscription name is required")
        @Size(max = 150, message = "Subscription name must be at most 150 characters")
        String name,

        @Size(max = 150, message = "Provider must be at most 150 characters")
        String provider,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        BigDecimal price,

        @NotNull(message = "Billing cycle is required")
        BillingCycle billingCycle,

        LocalDate startDate,

        @NotNull(message = "Next payment date is required")
        LocalDate nextPaymentDate,

        Boolean autoRenew,

        Boolean notifyEnabled,

        Integer notifyDaysBefore,

        Long categoryId,

        Long paymentMethodId,

        @NotNull(message = "Currency is required")
        Long currencyId,

        @Size(max = 500, message = "Website URL must be at most 500 characters")
        String websiteUrl,

        @Size(max = 1000, message = "Notes must be at most 1000 characters")
        String notes
) {
}