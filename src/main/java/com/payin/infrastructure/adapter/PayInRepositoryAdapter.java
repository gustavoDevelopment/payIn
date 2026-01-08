package com.payin.infrastructure.adapter;

import com.payin.domain.model.PayIn;
import com.payin.domain.ports.output.PayInRepositoryPort;
import com.payin.infrastructure.entity.PayInEntity;
import com.payin.infrastructure.repository.JpaPayInRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PayInRepositoryAdapter implements PayInRepositoryPort {
    
    private final JpaPayInRepository jpaPayInRepository;
    
    @Override
    public PayIn save(PayIn payIn) {
        PayInEntity entity = PayInEntity.fromDomain(payIn);
        PayInEntity savedEntity = jpaPayInRepository.save(entity);
        return savedEntity.toDomain();
    }
    
    @Override
    public Optional<PayIn> findById(UUID id) {
        return jpaPayInRepository.findById(id)
                .map(PayInEntity::toDomain);
    }
    
    @Override
    public Optional<PayIn> findByTransactionId(String transactionId) {
        return jpaPayInRepository.findByTransactionId(transactionId)
                .map(PayInEntity::toDomain);
    }
}
