package com.payin.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProvider {
    private UUID id;
    private String name; // Stripe, PayPal, etc.
    private String code; // STRIPE, PAYPAL, etc.
    private String baseUrl;
    private String apiKey;
    private String webhookSecret;
    private boolean active;
    private int priority; // For provider selection
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
