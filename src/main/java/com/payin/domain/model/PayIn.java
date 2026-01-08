package com.payin.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.payin.domain.exceptions.DomainException;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayIn {
    private UUID id;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String customerId;
    private String paymentMethodId;
    private PayInStatus status;
    private String description;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum PayInStatus {
        CREATED,
        VALIDATED,
        PROCESSED,
        FAILED
    }
    
    /**
     * Valida las reglas de negocio del PayIn.
     * @throws DomainException Si alguna regla de negocio no se cumple
     */
    public void validate() {
        validateAmount();
        validateCurrency();
        validateCustomer();
        validatePaymentMethod();
        
        this.status = PayInStatus.VALIDATED;
        this.updatedAt = LocalDateTime.now();
    }
    
    private void validateAmount() {
        if (amount == null) {
            throw new DomainException("El monto no puede ser nulo", "PAYIN-001");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("El monto debe ser mayor a cero", "PAYIN-002");
        }
        // Validar máximo monto permitido (ejemplo: 1,000,000)
        BigDecimal maxAmount = new BigDecimal("1000000");
        if (amount.compareTo(maxAmount) > 0) {
            throw new DomainException("El monto excede el límite permitido", "PAYIN-003");
        }
    }
    
    private void validateCurrency() {
        if (currency == null || currency.isBlank()) {
            throw new DomainException("La moneda es requerida", "PAYIN-004");
        }
        // Validar formato de moneda (ejemplo: 3 letras mayúsculas)
        if (!currency.matches("^[A-Z]{3}$")) {
            throw new DomainException("Formato de moneda inválido. Debe ser un código de 3 letras mayúsculas", "PAYIN-005");
        }
    }
    
    private void validateCustomer() {
        if (customerId == null || customerId.isBlank()) {
            throw new DomainException("El ID de cliente es requerido", "PAYIN-006");
        }
        // Aquí podrías agregar más validaciones del cliente si es necesario
    }
    
    private void validatePaymentMethod() {
        if (paymentMethodId == null || paymentMethodId.isBlank()) {
            throw new DomainException("El método de pago es requerido", "PAYIN-007");
        }
        // Aquí podrías agregar validaciones específicas del método de pago
    }
    
    public void process() {
        if (this.status != PayInStatus.VALIDATED) {
            throw new IllegalStateException("PayIn must be validated before processing");
        }
        this.status = PayInStatus.PROCESSED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void fail(String errorMessage) {
        this.status = PayInStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
}
