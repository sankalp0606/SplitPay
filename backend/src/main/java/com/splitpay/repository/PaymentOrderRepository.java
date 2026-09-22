package com.splitpay.repository;

import com.splitpay.entity.PaymentOrder;
import com.splitpay.entity.PaymentOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {
    Optional<PaymentOrder> findByOrderReference(String orderReference);

    Page<PaymentOrder> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT po FROM PaymentOrder po LEFT JOIN FETCH po.parts WHERE po.id = :id")
    Optional<PaymentOrder> findByIdWithParts(@Param("id") UUID id);

    @Query("SELECT po FROM PaymentOrder po LEFT JOIN FETCH po.parts WHERE po.orderReference = :reference")
    Optional<PaymentOrder> findByOrderReferenceWithParts(@Param("reference") String reference);

    long countByStatus(PaymentOrderStatus status);
}
