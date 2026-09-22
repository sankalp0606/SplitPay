package com.splitpay.repository;

import com.splitpay.entity.PaymentPart;
import com.splitpay.entity.PaymentPartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentPartRepository extends JpaRepository<PaymentPart, UUID> {
    Optional<PaymentPart> findByPaymentReference(String paymentReference);

    List<PaymentPart> findByPaymentOrderIdOrderByPartNumberAsc(UUID paymentOrderId);

    long countByPaymentOrderIdAndStatus(UUID paymentOrderId, PaymentPartStatus status);
}
