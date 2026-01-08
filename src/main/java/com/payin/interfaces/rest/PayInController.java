package com.payin.interfaces.rest;

import com.payin.domain.model.PayIn;
import com.payin.domain.ports.input.PayInUseCase;
import com.payin.interfaces.rest.dto.CreatePayInRequest;
import com.payin.interfaces.rest.dto.PayInResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payins")
@RequiredArgsConstructor
public class PayInController {

    private final PayInUseCase payInUseCase;

    @PostMapping
    public ResponseEntity<PayInResponse> createPayIn(@Valid @RequestBody CreatePayInRequest request) {
        PayIn payIn = PayIn.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .customerId(request.getCustomerId())
                .paymentMethodId(request.getPaymentMethodId())
                .description(request.getDescription())
                .build();

        PayIn createdPayIn = payInUseCase.createPayIn(payIn);
        return new ResponseEntity<>(PayInResponse.fromDomain(createdPayIn), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<PayInResponse> processPayIn(@PathVariable UUID id) {
        PayIn processedPayIn = payInUseCase.processPayIn(id);
        return ResponseEntity.ok(PayInResponse.fromDomain(processedPayIn));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayInResponse> getPayIn(@PathVariable UUID id) {
        PayIn payIn = payInUseCase.getPayIn(id);
        return ResponseEntity.ok(PayInResponse.fromDomain(payIn));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PayInResponse> getPayInByTransactionId(@PathVariable String transactionId) {
        PayIn payIn = payInUseCase.getPayInByTransactionId(transactionId);
        return ResponseEntity.ok(PayInResponse.fromDomain(payIn));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<String> handleNotFoundException(IllegalArgumentException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
