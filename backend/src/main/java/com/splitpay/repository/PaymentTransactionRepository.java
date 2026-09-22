package com.splitpay.repository;

import com.splitpay.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    List<PaymentTransaction> findByPaymentReference(String paymentReference);
    Optional<PaymentTransaction> findByProviderTransactionId(String providerTransactionId);
    boolean existsByPaymentReferenceAndProviderTransactionId(String paymentReference, String providerTransactionId);
}
