package com.payin.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payin.domain.model.PayIn;
import com.payin.domain.ports.input.PayInUseCase;
import com.payin.domain.exceptions.DomainException;
import com.payin.interfaces.rest.dto.CreatePayInRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PayInController.class)
@DisplayName("PayInController Integration Tests")
class PayInControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PayInUseCase payInUseCase;

    @Test
    @DisplayName("POST /api/v1/payins - Should create PayIn successfully")
    void createPayIn_withValidRequest_shouldReturn201() throws Exception {
        // Given
        CreatePayInRequest request = new CreatePayInRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setCustomerId("CUST001");
        request.setPaymentMethodId("PM001");
        request.setDescription("Test payment");

        PayIn createdPayIn = PayIn.builder()
                .id(UUID.randomUUID())
                .transactionId("TXN123456")
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .customerId(request.getCustomerId())
                .paymentMethodId(request.getPaymentMethodId())
                .status(PayIn.PayInStatus.VALIDATED)
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(payInUseCase.createPayIn(any(PayIn.class))).thenReturn(createdPayIn);

        // When & Then
        mockMvc.perform(post("/api/v1/payins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.transactionId").value("TXN123456"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("VALIDATED"));

        verify(payInUseCase, times(1)).createPayIn(any(PayIn.class));
    }

    @Test
    @DisplayName("POST /api/v1/payins - Should return 400 when amount is null")
    void createPayIn_withNullAmount_shouldReturn400() throws Exception {
        // Given
        CreatePayInRequest request = new CreatePayInRequest();
        request.setAmount(null);
        request.setCurrency("USD");
        request.setCustomerId("CUST001");
        request.setPaymentMethodId("PM001");

        // When & Then
        mockMvc.perform(post("/api/v1/payins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(payInUseCase, never()).createPayIn(any(PayIn.class));
    }

    @Test
    @DisplayName("POST /api/v1/payins - Should return 400 when amount is zero")
    void createPayIn_withZeroAmount_shouldReturn400() throws Exception {
        // Given
        CreatePayInRequest request = new CreatePayInRequest();
        request.setAmount(BigDecimal.ZERO);
        request.setCurrency("USD");
        request.setCustomerId("CUST001");
        request.setPaymentMethodId("PM001");

        // When & Then
        mockMvc.perform(post("/api/v1/payins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(payInUseCase, never()).createPayIn(any(PayIn.class));
    }

    @Test
    @DisplayName("POST /api/v1/payins - Should return 400 when currency is blank")
    void createPayIn_withBlankCurrency_shouldReturn400() throws Exception {
        // Given
        CreatePayInRequest request = new CreatePayInRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("");
        request.setCustomerId("CUST001");
        request.setPaymentMethodId("PM001");

        // When & Then
        mockMvc.perform(post("/api/v1/payins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(payInUseCase, never()).createPayIn(any(PayIn.class));
    }

    @Test
    @DisplayName("POST /api/v1/payins - Should return 400 when customerId is blank")
    void createPayIn_withBlankCustomerId_shouldReturn400() throws Exception {
        // Given
        CreatePayInRequest request = new CreatePayInRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setCustomerId("");
        request.setPaymentMethodId("PM001");

        // When & Then
        mockMvc.perform(post("/api/v1/payins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(payInUseCase, never()).createPayIn(any(PayIn.class));
    }

    @Test
    @DisplayName("POST /api/v1/payins/{id}/process - Should process PayIn successfully")
    void processPayIn_withExistingId_shouldReturn200() throws Exception {
        // Given
        UUID payInId = UUID.randomUUID();
        PayIn processedPayIn = PayIn.builder()
                .id(payInId)
                .transactionId("TXN123456")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.PROCESSED)
                .description("Test payment")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(payInUseCase.processPayIn(payInId)).thenReturn(processedPayIn);

        // When & Then
        mockMvc.perform(post("/api/v1/payins/{id}/process", payInId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payInId.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSED"));

        verify(payInUseCase, times(1)).processPayIn(payInId);
    }

    @Test
    @DisplayName("POST /api/v1/payins/{id}/process - Should return 404 when PayIn not found")
    void processPayIn_withNonExistingId_shouldReturn404() throws Exception {
        // Given
        UUID payInId = UUID.randomUUID();
        when(payInUseCase.processPayIn(payInId))
                .thenThrow(new IllegalArgumentException("PayIn not found with id: " + payInId));

        // When & Then
        mockMvc.perform(post("/api/v1/payins/{id}/process", payInId))
                .andExpect(status().isNotFound());

        verify(payInUseCase, times(1)).processPayIn(payInId);
    }

    @Test
    @DisplayName("POST /api/v1/payins/{id}/process - Should return 400 when PayIn not validated")
    void processPayIn_withNonValidatedPayIn_shouldReturn400() throws Exception {
        // Given
        UUID payInId = UUID.randomUUID();
        when(payInUseCase.processPayIn(payInId))
                .thenThrow(new IllegalStateException("PayIn must be validated before processing"));

        // When & Then
        mockMvc.perform(post("/api/v1/payins/{id}/process", payInId))
                .andExpect(status().isBadRequest());

        verify(payInUseCase, times(1)).processPayIn(payInId);
    }

    @Test
    @DisplayName("GET /api/v1/payins/{id} - Should get PayIn successfully")
    void getPayIn_withExistingId_shouldReturn200() throws Exception {
        // Given
        UUID payInId = UUID.randomUUID();
        PayIn payIn = PayIn.builder()
                .id(payInId)
                .transactionId("TXN123456")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.VALIDATED)
                .description("Test payment")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(payInUseCase.getPayIn(payInId)).thenReturn(payIn);

        // When & Then
        mockMvc.perform(get("/api/v1/payins/{id}", payInId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payInId.toString()))
                .andExpect(jsonPath("$.transactionId").value("TXN123456"))
                .andExpect(jsonPath("$.amount").value(100.00));

        verify(payInUseCase, times(1)).getPayIn(payInId);
    }

    @Test
    @DisplayName("GET /api/v1/payins/{id} - Should return 404 when PayIn not found")
    void getPayIn_withNonExistingId_shouldReturn404() throws Exception {
        // Given
        UUID payInId = UUID.randomUUID();
        when(payInUseCase.getPayIn(payInId))
                .thenThrow(new IllegalArgumentException("PayIn not found with id: " + payInId));

        // When & Then
        mockMvc.perform(get("/api/v1/payins/{id}", payInId))
                .andExpect(status().isNotFound());

        verify(payInUseCase, times(1)).getPayIn(payInId);
    }

    @Test
    @DisplayName("GET /api/v1/payins/transaction/{transactionId} - Should get PayIn successfully")
    void getPayInByTransactionId_withExistingTransactionId_shouldReturn200() throws Exception {
        // Given
        String transactionId = "TXN123456";
        PayIn payIn = PayIn.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .customerId("CUST001")
                .paymentMethodId("PM001")
                .status(PayIn.PayInStatus.VALIDATED)
                .description("Test payment")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(payInUseCase.getPayInByTransactionId(transactionId)).thenReturn(payIn);

        // When & Then
        mockMvc.perform(get("/api/v1/payins/transaction/{transactionId}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId))
                .andExpect(jsonPath("$.amount").value(100.00));

        verify(payInUseCase, times(1)).getPayInByTransactionId(transactionId);
    }

    @Test
    @DisplayName("GET /api/v1/payins/transaction/{transactionId} - Should return 400 when not found")
    void getPayInByTransactionId_withNonExistingTransactionId_shouldReturn400() throws Exception {
        // Given
        String transactionId = "TXN999999";
        when(payInUseCase.getPayInByTransactionId(transactionId))
                .thenThrow(new DomainException("No se encontró el PayIn", "PAYIN-009"));

        // When & Then
        mockMvc.perform(get("/api/v1/payins/transaction/{transactionId}", transactionId))
                .andExpect(status().isBadRequest());

        verify(payInUseCase, times(1)).getPayInByTransactionId(transactionId);
    }

    @Test
    @DisplayName("POST /api/v1/payins - Should handle decimal amounts correctly")
    void createPayIn_withDecimalAmount_shouldSucceed() throws Exception {
        // Given
        CreatePayInRequest request = new CreatePayInRequest();
        request.setAmount(new BigDecimal("99.99"));
        request.setCurrency("USD");
        request.setCustomerId("CUST001");
        request.setPaymentMethodId("PM001");

        PayIn createdPayIn = PayIn.builder()
                .id(UUID.randomUUID())
                .transactionId("TXN123456")
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .customerId(request.getCustomerId())
                .paymentMethodId(request.getPaymentMethodId())
                .status(PayIn.PayInStatus.VALIDATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(payInUseCase.createPayIn(any(PayIn.class))).thenReturn(createdPayIn);

        // When & Then
        mockMvc.perform(post("/api/v1/payins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(99.99));

        verify(payInUseCase, times(1)).createPayIn(any(PayIn.class));
    }
}
