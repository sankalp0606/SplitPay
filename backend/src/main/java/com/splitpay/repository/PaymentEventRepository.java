package com.splitpay.repository;

import com.splitpay.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {
    Optional<PaymentEvent> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
}
