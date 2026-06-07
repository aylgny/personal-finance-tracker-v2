package com.subtrack.backend.subscription.dto;

import com.subtrack.backend.subscription.BillingCycle;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSubscriptionRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
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

        @Min(value = 0, message = "Notify days before cannot be negative")
        Integer notifyDaysBefore,

        Long categoryId,

        Long paymentMethodId,

        @NotNull(message = "Currency is required")
        Long currencyId,

        String notes
) {
}