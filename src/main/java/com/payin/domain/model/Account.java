package com.payin.domain.model;

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
public class Account {
    private UUID id;
    private UUID customerId;
    private String accountNumber;
    private String accountType; // CHECKING, SAVINGS, etc.
    private String currency;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
