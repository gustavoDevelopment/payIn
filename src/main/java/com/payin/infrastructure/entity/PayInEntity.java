package com.payin.infrastructure.entity;

import com.payin.domain.model.PayIn;
import jakarta.persistence.*;
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
@Entity
@Table(name = "payins")
public class PayInEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String transactionId;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "payment_method_id", nullable = false)
    private String paymentMethodId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayIn.PayInStatus status;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public static PayInEntity fromDomain(PayIn payIn) {
        return PayInEntity.builder()
                .id(payIn.getId())
                .transactionId(payIn.getTransactionId())
                .amount(payIn.getAmount())
                .currency(payIn.getCurrency())
                .customerId(payIn.getCustomerId())
                .paymentMethodId(payIn.getPaymentMethodId())
                .status(payIn.getStatus())
                .description(payIn.getDescription())
                .errorMessage(payIn.getErrorMessage())
                .createdAt(payIn.getCreatedAt())
                .updatedAt(payIn.getUpdatedAt())
                .build();
    }
    
    public PayIn toDomain() {
        return PayIn.builder()
                .id(this.id)
                .transactionId(this.transactionId)
                .amount(this.amount)
                .currency(this.currency)
                .customerId(this.customerId)
                .paymentMethodId(this.paymentMethodId)
                .status(this.status)
                .description(this.description)
                .errorMessage(this.errorMessage)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
