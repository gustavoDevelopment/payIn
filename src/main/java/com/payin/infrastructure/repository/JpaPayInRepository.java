package com.payin.infrastructure.repository;

import com.payin.infrastructure.entity.PayInEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaPayInRepository extends JpaRepository<PayInEntity, UUID> {
    Optional<PayInEntity> findByTransactionId(String transactionId);
}
