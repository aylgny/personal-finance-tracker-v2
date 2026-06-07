package com.subtrack.backend.paymentmethod;

import com.subtrack.backend.paymentmethod.dto.PaymentMethodResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public List<PaymentMethodResponse> getPaymentMethods() {
        // Payment methods are global reference data, but disabled options are hidden from forms.
        return paymentMethodRepository.findByEnabledTrueOrderBySortOrderAscNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentMethodResponse toResponse(PaymentMethod paymentMethod) {
        // Return only fields needed by the frontend dropdown.
        return new PaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getName(),
                paymentMethod.getEnabled(),
                paymentMethod.getSortOrder()
        );
    }
}