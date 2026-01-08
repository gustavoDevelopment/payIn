package com.payin.domain.service;

import com.payin.domain.model.PayIn;
import com.payin.domain.ports.input.PayInUseCase;
import com.payin.domain.ports.output.PayInRepositoryPort;
import com.payin.domain.exceptions.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayInService implements PayInUseCase {
    
    private final PayInRepositoryPort payInRepository;
    
    @Override
    @Transactional
    public PayIn createPayIn(PayIn payIn) {
        // Set initial state
        payIn.setId(UUID.randomUUID());
        payIn.setStatus(PayIn.PayInStatus.CREATED);
        payIn.setTransactionId(generateTransactionId());
        payIn.setCreatedAt(java.time.LocalDateTime.now());
        payIn.setUpdatedAt(java.time.LocalDateTime.now());
        
        // Validate the payin
        payIn.validate();
        
        // Save to repository
        return payInRepository.save(payIn);
    }
    
    @Override
    @Transactional
    public PayIn processPayIn(UUID payInId) {
        // Find the payin
        PayIn payIn = payInRepository.findById(payInId)
                .orElseThrow(() -> new IllegalArgumentException("PayIn not found with id: " + payInId));
        
        // Process the payin
        try {
            payIn.process();
            // Here you would typically integrate with a payment gateway
            // For now, we'll just update the status
            return payInRepository.save(payIn);
        } catch (Exception e) {
            payIn.fail(e.getMessage());
            return payInRepository.save(payIn);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public PayIn getPayIn(UUID payInId) {
        return payInRepository.findById(payInId)
                .orElseThrow(() -> new IllegalArgumentException("PayIn not found with id: " + payInId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public PayIn getPayInByTransactionId(String transactionId) {
        return payInRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new DomainException("No se encontró el PayIn con transactionId: " + transactionId, "PAYIN-009"));
    }
    
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + "" + (int)(Math.random() * 1000);
    }
}
