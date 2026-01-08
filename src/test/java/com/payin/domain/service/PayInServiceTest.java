package com.payin.domain.service;

import com.payin.domain.exceptions.DomainException;
import com.payin.domain.model.PayIn;
import com.payin.domain.ports.output.PayInRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PayInService Tests")
class PayInServiceTest {

    @Mock
    private PayInRepositoryPort payInRepository;

    @InjectMocks
    private PayInService payInService;

    private PayIn validPayIn;

    @BeforeEach
    void setUp() {
        validPayIn = PayIn.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .description("Test payment")
                .build();
    }

    @Test
    @DisplayName("Should create PayIn successfully with valid data")
    void createPayIn_withValidData_shouldReturnValidatedPayIn() {
        // Given
        when(payInRepository.save(any(PayIn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PayIn result = payInService.createPayIn(validPayIn);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getTransactionId());
        assertEquals(PayIn.PayInStatus.VALIDATED, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        verify(payInRepository, times(1)).save(any(PayIn.class));
    }

    @Test
    @DisplayName("Should generate unique transaction ID when creating PayIn")
    void createPayIn_shouldGenerateUniqueTransactionId() {
        // Given
        when(payInRepository.save(any(PayIn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PayIn result = payInService.createPayIn(validPayIn);

        // Then
        assertNotNull(result.getTransactionId());
        assertTrue(result.getTransactionId().startsWith("TXN"));
    }

    @Test
    @DisplayName("Should throw DomainException when creating PayIn with invalid amount")
    void createPayIn_withInvalidAmount_shouldThrowDomainException() {
        // Given
        PayIn invalidPayIn = PayIn.builder()
                .amount(new BigDecimal("-10.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .build();

        // When & Then
        assertThrows(DomainException.class, () -> payInService.createPayIn(invalidPayIn));
        verify(payInRepository, never()).save(any(PayIn.class));
    }

    @Test
    @DisplayName("Should process PayIn successfully when it exists and is validated")
    void processPayIn_withExistingValidatedPayIn_shouldReturnProcessedPayIn() {
        // Given
        UUID payInId = UUID.randomUUID();
        PayIn existingPayIn = PayIn.builder()
                .id(payInId)
                .transactionId("TXN123")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.VALIDATED)
                .build();

        when(payInRepository.findById(payInId)).thenReturn(Optional.of(existingPayIn));
        when(payInRepository.save(any(PayIn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PayIn result = payInService.processPayIn(payInId);

        // Then
        assertNotNull(result);
        assertEquals(PayIn.PayInStatus.PROCESSED, result.getStatus());
        verify(payInRepository, times(1)).findById(payInId);
        verify(payInRepository, times(1)).save(any(PayIn.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when PayIn not found")
    void processPayIn_withNonExistingPayIn_shouldThrowIllegalArgumentException() {
        // Given
        UUID payInId = UUID.randomUUID();
        when(payInRepository.findById(payInId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> payInService.processPayIn(payInId)
        );
        assertTrue(exception.getMessage().contains("not found"));
        verify(payInRepository, times(1)).findById(payInId);
        verify(payInRepository, never()).save(any(PayIn.class));
    }

    @Test
    @DisplayName("Should fail PayIn when processing throws exception")
    void processPayIn_whenExceptionOccurs_shouldFailPayIn() {
        // Given
        UUID payInId = UUID.randomUUID();
        PayIn existingPayIn = PayIn.builder()
                .id(payInId)
                .transactionId("TXN123")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.VALIDATED)
                .build();

        when(payInRepository.findById(payInId)).thenReturn(Optional.of(existingPayIn));
        when(payInRepository.save(any(PayIn.class)))
                .thenThrow(new RuntimeException("Database error"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PayIn result = payInService.processPayIn(payInId);

        // Then
        assertNotNull(result);
        assertEquals(PayIn.PayInStatus.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Database error"));
    }

    @Test
    @DisplayName("Should get PayIn by ID successfully")
    void getPayIn_withExistingId_shouldReturnPayIn() {
        // Given
        UUID payInId = UUID.randomUUID();
        PayIn existingPayIn = PayIn.builder()
                .id(payInId)
                .transactionId("TXN123")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.VALIDATED)
                .build();

        when(payInRepository.findById(payInId)).thenReturn(Optional.of(existingPayIn));

        // When
        PayIn result = payInService.getPayIn(payInId);

        // Then
        assertNotNull(result);
        assertEquals(payInId, result.getId());
        assertEquals("TXN123", result.getTransactionId());
        verify(payInRepository, times(1)).findById(payInId);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when getting non-existing PayIn by ID")
    void getPayIn_withNonExistingId_shouldThrowIllegalArgumentException() {
        // Given
        UUID payInId = UUID.randomUUID();
        when(payInRepository.findById(payInId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> payInService.getPayIn(payInId));
        verify(payInRepository, times(1)).findById(payInId);
    }

    @Test
    @DisplayName("Should get PayIn by transaction ID successfully")
    void getPayInByTransactionId_withExistingTransactionId_shouldReturnPayIn() {
        // Given
        String transactionId = "TXN123";
        PayIn existingPayIn = PayIn.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.VALIDATED)
                .build();

        when(payInRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(existingPayIn));

        // When
        PayIn result = payInService.getPayInByTransactionId(transactionId);

        // Then
        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        verify(payInRepository, times(1)).findByTransactionId(transactionId);
    }

    @Test
    @DisplayName("Should throw DomainException when getting non-existing PayIn by transaction ID")
    void getPayInByTransactionId_withNonExistingTransactionId_shouldThrowDomainException() {
        // Given
        String transactionId = "TXN999";
        when(payInRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

        // When & Then
        DomainException exception = assertThrows(
                DomainException.class,
                () -> payInService.getPayInByTransactionId(transactionId)
        );
        assertEquals("PAYIN-009", exception.getErrorCode());
        verify(payInRepository, times(1)).findByTransactionId(transactionId);
    }

    @Test
    @DisplayName("Should preserve PayIn data when saving")
    void createPayIn_shouldPreserveAllData() {
        // Given
        ArgumentCaptor<PayIn> payInCaptor = ArgumentCaptor.forClass(PayIn.class);
        when(payInRepository.save(any(PayIn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        payInService.createPayIn(validPayIn);

        // Then
        verify(payInRepository).save(payInCaptor.capture());
        PayIn savedPayIn = payInCaptor.getValue();

        assertEquals(validPayIn.getAmount(), savedPayIn.getAmount());
        assertEquals(validPayIn.getCurrency(), savedPayIn.getCurrency());
        assertEquals(validPayIn.getCustomerId(), savedPayIn.getCustomerId());
        assertEquals(validPayIn.getPaymentMethodId(), savedPayIn.getPaymentMethodId());
        assertEquals(validPayIn.getDescription(), savedPayIn.getDescription());
    }
}
