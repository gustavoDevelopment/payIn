package com.payin.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.payin.domain.model.PayIn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayInResponse {
    private UUID id;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String customerId;
    private String paymentMethodId;
    private String status;
    private String description;
    private String errorMessage;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;
    
    public static PayInResponse fromDomain(PayIn payIn) {
        return PayInResponse.builder()
                .id(payIn.getId())
                .transactionId(payIn.getTransactionId())
                .amount(payIn.getAmount())
                .currency(payIn.getCurrency())
                .customerId(payIn.getCustomerId())
                .paymentMethodId(payIn.getPaymentMethodId())
                .status(payIn.getStatus().name())
                .description(payIn.getDescription())
                .errorMessage(payIn.getErrorMessage())
                .createdAt(payIn.getCreatedAt())
                .updatedAt(payIn.getUpdatedAt())
                .build();
    }
}
