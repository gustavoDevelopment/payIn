package com.payin.domain.ports.input;

import com.payin.domain.model.PayIn;

import java.util.UUID;

public interface PayInUseCase {
    PayIn createPayIn(PayIn payIn);
    PayIn processPayIn(UUID payInId);
    PayIn getPayIn(UUID payInId);
    PayIn getPayInByTransactionId(String transactionId);
}
