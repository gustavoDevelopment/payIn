package com.payin.domain.model;

import com.payin.domain.exceptions.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PayIn Domain Model Tests")
class PayInTest {

    @Test
    @DisplayName("Should create PayIn with valid data")
    void createPayIn_withValidData_shouldSucceed() {
        // Given
        PayIn payIn = PayIn.builder()
                .id(UUID.randomUUID())
                .transactionId("TXN123")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.CREATED)
                .description("Test payment")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Then
        assertNotNull(payIn);
        assertEquals(new BigDecimal("100.00"), payIn.getAmount());
        assertEquals("USD", payIn.getCurrency());
        assertEquals(PayIn.PayInStatus.CREATED, payIn.getStatus());
    }

    @Test
    @DisplayName("Should validate PayIn successfully with valid data")
    void validate_withValidData_shouldSetStatusToValidated() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.CREATED)
                .build();

        // When
        payIn.validate();

        // Then
        assertEquals(PayIn.PayInStatus.VALIDATED, payIn.getStatus());
        assertNotNull(payIn.getUpdatedAt());
    }

    @Test
    @DisplayName("Should throw DomainException when amount is null")
    void validate_withNullAmount_shouldThrowDomainException() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(null)
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .build();

        // When & Then
        DomainException exception = assertThrows(DomainException.class, payIn::validate);
        assertEquals("PAYIN-001", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("nulo"));
    }

    @Test
    @DisplayName("Should throw DomainException when amount is zero")
    void validate_withZeroAmount_shouldThrowDomainException() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(BigDecimal.ZERO)
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .build();

        // When & Then
        DomainException exception = assertThrows(DomainException.class, payIn::validate);
        assertEquals("PAYIN-002", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("mayor a cero"));
    }

    @Test
    @DisplayName("Should throw DomainException when amount is negative")
    void validate_withNegativeAmount_shouldThrowDomainException() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("-10.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .build();

        // When & Then
        DomainException exception = assertThrows(DomainException.class, payIn::validate);
        assertEquals("PAYIN-002", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw DomainException when amount exceeds maximum")
    void validate_withAmountExceedingMaximum_shouldThrowDomainException() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("1000001.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .build();

        // When & Then
        DomainException exception = assertThrows(DomainException.class, payIn::validate);
        assertEquals("PAYIN-003", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("límite"));
    }

    @Test
    @DisplayName("Should throw DomainException when currency is null")
    void validate_withNullCurrency_shouldThrowDomainException() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency(null)
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .build();

        // When & Then
        DomainException exception = assertThrows(DomainException.class, payIn::validate);
        assertEquals("PAYIN-004", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw DomainException when currency is blank")
    void validate_withBlankCurrency_shouldThrowDomainException() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency("   ")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .build();

        // When & Then
        DomainException exception = assertThrows(DomainException.class, payIn::validate);
        assertEquals("PAYIN-004", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw DomainException when currency format is invalid")
    void validate_withInvalidCurrencyFormat_shouldThrowDomainException() {
        // Given - Currency with 2 letters
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency("US")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .build();

        // When & Then
        DomainException exception = assertThrows(DomainException.class, payIn::validate);
        assertEquals("PAYIN-005", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw DomainException when currency has lowercase letters")
    void validate_withLowercaseCurrency_shouldThrowDomainException() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency("usd")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .build();

        // When & Then
        DomainException exception = assertThrows(DomainException.class, payIn::validate);
        assertEquals("PAYIN-005", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw DomainException when customerId is null")
    void validate_withNullCustomerId_shouldThrowDomainException() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId(null)
                .paymentMethodId("PM001")
                .build();

        // When & Then
        DomainException exception = assertThrows(DomainException.class, payIn::validate);
        assertEquals("PAYIN-006", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw DomainException when customerId is blank")
    void validate_withBlankCustomerId_shouldThrowDomainException() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("   ")
                .paymentMethodId("PM001")
                .build();

        // When & Then
        DomainException exception = assertThrows(DomainException.class, payIn::validate);
        assertEquals("PAYIN-006", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw DomainException when paymentMethodId is null")
    void validate_withNullPaymentMethodId_shouldThrowDomainException() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId(null)
                .build();

        // When & Then
        DomainException exception = assertThrows(DomainException.class, payIn::validate);
        assertEquals("PAYIN-007", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw DomainException when paymentMethodId is blank")
    void validate_withBlankPaymentMethodId_shouldThrowDomainException() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("   ")
                .build();

        // When & Then
        DomainException exception = assertThrows(DomainException.class, payIn::validate);
        assertEquals("PAYIN-007", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should process PayIn successfully when status is VALIDATED")
    void process_withValidatedStatus_shouldSetStatusToProcessed() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.VALIDATED)
                .build();

        // When
        payIn.process();

        // Then
        assertEquals(PayIn.PayInStatus.PROCESSED, payIn.getStatus());
        assertNotNull(payIn.getUpdatedAt());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when processing non-validated PayIn")
    void process_withNonValidatedStatus_shouldThrowIllegalStateException() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.CREATED)
                .build();

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                payIn::process
        );
        assertTrue(exception.getMessage().contains("validated before processing"));
    }

    @Test
    @DisplayName("Should fail PayIn with error message")
    void fail_shouldSetStatusToFailedAndSetErrorMessage() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.VALIDATED)
                .build();
        String errorMessage = "Payment gateway timeout";

        // When
        payIn.fail(errorMessage);

        // Then
        assertEquals(PayIn.PayInStatus.FAILED, payIn.getStatus());
        assertEquals(errorMessage, payIn.getErrorMessage());
        assertNotNull(payIn.getUpdatedAt());
    }

    @Test
    @DisplayName("Should accept maximum allowed amount")
    void validate_withMaximumAllowedAmount_shouldSucceed() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("1000000.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .build();

        // When
        payIn.validate();

        // Then
        assertEquals(PayIn.PayInStatus.VALIDATED, payIn.getStatus());
    }

    @Test
    @DisplayName("Should accept valid currency codes")
    void validate_withValidCurrencyCodes_shouldSucceed() {
        // Given
        String[] validCurrencies = {"USD", "EUR", "COP", "GBP", "JPY"};

        for (String currency : validCurrencies) {
            PayIn payIn = PayIn.builder()
                    .amount(new BigDecimal("100.00"))
                    .currency(currency)
                    .customerId("CUST001")
                    .paymentMethodId("PM001")
                    .build();

            // When
            payIn.validate();

            // Then
            assertEquals(PayIn.PayInStatus.VALIDATED, payIn.getStatus(),
                    "Should validate currency: " + currency);
        }
    }

    @Test
    @DisplayName("Should handle decimal amounts correctly")
    void validate_withDecimalAmounts_shouldSucceed() {
        // Given
        PayIn payIn = PayIn.builder()
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .build();

        // When
        payIn.validate();

        // Then
        assertEquals(PayIn.PayInStatus.VALIDATED, payIn.getStatus());
        assertEquals(new BigDecimal("99.99"), payIn.getAmount());
    }
}
