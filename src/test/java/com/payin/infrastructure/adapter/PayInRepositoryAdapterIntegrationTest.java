package com.payin.infrastructure.adapter;

import com.payin.domain.model.PayIn;
import com.payin.domain.ports.output.PayInRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("PayInRepositoryAdapter Integration Tests")
class PayInRepositoryAdapterIntegrationTest {

    @Autowired
    private PayInRepositoryPort payInRepository;

    @Test
    @DisplayName("Should save PayIn and generate ID")
    void save_shouldPersistPayInAndReturnWithId() {
        // Given
        PayIn payIn = createValidPayIn();

        // When
        PayIn saved = payInRepository.save(payIn);

        // Then
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(payIn.getAmount(), saved.getAmount());
        assertEquals(payIn.getCurrency(), saved.getCurrency());
        assertEquals(payIn.getCustomerId(), saved.getCustomerId());
        assertEquals(payIn.getTransactionId(), saved.getTransactionId());
    }

    @Test
    @DisplayName("Should find PayIn by ID")
    void findById_withExistingId_shouldReturnPayIn() {
        // Given
        PayIn payIn = createValidPayIn();
        PayIn saved = payInRepository.save(payIn);

        // When
        Optional<PayIn> found = payInRepository.findById(saved.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(saved.getTransactionId(), found.get().getTransactionId());
    }

    @Test
    @DisplayName("Should return empty when PayIn not found by ID")
    void findById_withNonExistingId_shouldReturnEmpty() {
        // Given
        UUID nonExistingId = UUID.randomUUID();

        // When
        Optional<PayIn> found = payInRepository.findById(nonExistingId);

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should find PayIn by transaction ID")
    void findByTransactionId_withExistingTransactionId_shouldReturnPayIn() {
        // Given
        PayIn payIn = createValidPayIn();
        PayIn saved = payInRepository.save(payIn);

        // When
        Optional<PayIn> found = payInRepository.findByTransactionId(saved.getTransactionId());

        // Then
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(saved.getTransactionId(), found.get().getTransactionId());
    }

    @Test
    @DisplayName("Should return empty when PayIn not found by transaction ID")
    void findByTransactionId_withNonExistingTransactionId_shouldReturnEmpty() {
        // Given
        String nonExistingTransactionId = "TXN999999";

        // When
        Optional<PayIn> found = payInRepository.findByTransactionId(nonExistingTransactionId);

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should update existing PayIn")
    void save_shouldUpdateExistingPayIn() {
        // Given
        PayIn payIn = createValidPayIn();
        PayIn saved = payInRepository.save(payIn);

        // Modify the PayIn
        saved.setStatus(PayIn.PayInStatus.PROCESSED);
        saved.setUpdatedAt(LocalDateTime.now());

        // When
        PayIn updated = payInRepository.save(saved);

        // Then
        assertNotNull(updated);
        assertEquals(saved.getId(), updated.getId());
        assertEquals(PayIn.PayInStatus.PROCESSED, updated.getStatus());
    }

    @Test
    @DisplayName("Should preserve all PayIn fields")
    void save_shouldPreserveAllFields() {
        // Given
        PayIn payIn = PayIn.builder()
                .id(UUID.randomUUID())
                .transactionId("TXN" + System.currentTimeMillis())
                .amount(new BigDecimal("150.75"))
                .currency("EUR")
                .customerId("CUST002")
                .paymentMethodId("PM002")
                .status(PayIn.PayInStatus.VALIDATED)
                .description("Integration test payment")
                .errorMessage(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        PayIn saved = payInRepository.save(payIn);

        // Then
        assertNotNull(saved);
        assertEquals(payIn.getTransactionId(), saved.getTransactionId());
        assertEquals(payIn.getAmount(), saved.getAmount());
        assertEquals(payIn.getCurrency(), saved.getCurrency());
        assertEquals(payIn.getCustomerId(), saved.getCustomerId());
        assertEquals(payIn.getPaymentMethodId(), saved.getPaymentMethodId());
        assertEquals(payIn.getStatus(), saved.getStatus());
        assertEquals(payIn.getDescription(), saved.getDescription());
        assertEquals(payIn.getErrorMessage(), saved.getErrorMessage());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle decimal amounts correctly")
    void save_withDecimalAmount_shouldPersistCorrectly() {
        // Given
        PayIn payIn = createValidPayIn();
        payIn.setAmount(new BigDecimal("99.9999"));

        // When
        PayIn saved = payInRepository.save(payIn);

        // Then
        assertNotNull(saved);
        assertEquals(new BigDecimal("99.9999"), saved.getAmount());
    }

    @Test
    @DisplayName("Should handle FAILED status with error message")
    void save_withFailedStatusAndErrorMessage_shouldPersist() {
        // Given
        PayIn payIn = createValidPayIn();
        payIn.setStatus(PayIn.PayInStatus.FAILED);
        payIn.setErrorMessage("Payment gateway timeout");

        // When
        PayIn saved = payInRepository.save(payIn);

        // Then
        assertNotNull(saved);
        assertEquals(PayIn.PayInStatus.FAILED, saved.getStatus());
        assertEquals("Payment gateway timeout", saved.getErrorMessage());
    }

    @Test
    @DisplayName("Should handle different currency codes")
    void save_withDifferentCurrencies_shouldPersist() {
        // Given
        String[] currencies = {"USD", "EUR", "COP", "GBP", "JPY"};

        for (String currency : currencies) {
            PayIn payIn = createValidPayIn();
            payIn.setTransactionId("TXN" + System.currentTimeMillis() + currency);
            payIn.setCurrency(currency);

            // When
            PayIn saved = payInRepository.save(payIn);

            // Then
            assertNotNull(saved);
            assertEquals(currency, saved.getCurrency());
        }
    }

    private PayIn createValidPayIn() {
        return PayIn.builder()
                .id(UUID.randomUUID())
                .transactionId("TXN" + System.currentTimeMillis())
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.CREATED)
                .description("Test payment")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
