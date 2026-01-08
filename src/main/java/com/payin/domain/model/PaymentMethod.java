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
public class PaymentMethod {
    private UUID id;
    private UUID customerId;
    private String type; // CREDIT_CARD, BANK_ACCOUNT, E_WALLET, etc.
    private String token; // Tokenized payment method
    private boolean isDefault;
    private String lastFourDigits; // For cards
    private String provider; // VISA, MASTERCARD, etc.
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;
}
