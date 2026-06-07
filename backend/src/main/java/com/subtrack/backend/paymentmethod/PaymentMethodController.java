package com.subtrack.backend.paymentmethod;

import com.subtrack.backend.paymentmethod.dto.PaymentMethodResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping
    public List<PaymentMethodResponse> getPaymentMethods() {
        // Returns global enabled payment methods for subscription form dropdowns.
        return paymentMethodService.getPaymentMethods();
    }
}