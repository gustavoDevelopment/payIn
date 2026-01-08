package com.payin.domain.ports.output;

import com.payin.domain.model.PayIn;

import java.util.Optional;
import java.util.UUID;

public interface PayInRepositoryPort {
    PayIn save(PayIn payIn);
    Optional<PayIn> findById(UUID id);
    Optional<PayIn> findByTransactionId(String transactionId);
}
