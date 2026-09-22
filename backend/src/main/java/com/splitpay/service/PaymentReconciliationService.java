package com.splitpay.service;

import com.splitpay.entity.PaymentOrder;
import com.splitpay.entity.PaymentOrderStatus;
import com.splitpay.entity.PaymentPart;
import com.splitpay.entity.PaymentPartStatus;
import com.splitpay.repository.PaymentOrderRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final PaymentOrderRepository paymentOrderRepository;

    @Data
    @Builder
    @AllArgsConstructor
    public static class ReconciliationResult {
        private BigDecimal expectedAmount;
        private BigDecimal paidAmount;
        private BigDecimal remainingAmount;
        private BigDecimal overpaidAmount;
        private PaymentOrderStatus determinedOrderStatus;
        private int totalParts;
        private int successfulParts;
        private int failedParts;
        private int pendingParts;
    }

    @Transactional
    public ReconciliationResult reconcileOrder(PaymentOrder order) {
        List<PaymentPart> parts = order.getParts();

        BigDecimal expectedAmount = order.getTotalAmount();
        BigDecimal paidAmount = BigDecimal.ZERO;
        int successfulParts = 0;
        int failedParts = 0;
        int expiredParts = 0;
        int pendingParts = 0;

        for (PaymentPart part : parts) {
            if (part.getStatus() == PaymentPartStatus.SUCCESS) {
                paidAmount = paidAmount.add(part.getAmount());
                successfulParts++;
            } else if (part.getStatus() == PaymentPartStatus.FAILED) {
                failedParts++;
            } else if (part.getStatus() == PaymentPartStatus.EXPIRED) {
                expiredParts++;
            } else {
                pendingParts++;
            }
        }

        BigDecimal remainingAmount = expectedAmount.subtract(paidAmount);
        BigDecimal overpaidAmount = BigDecimal.ZERO;

        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            overpaidAmount = remainingAmount.abs();
            remainingAmount = BigDecimal.ZERO;
        }

        PaymentOrderStatus newStatus;
        if (paidAmount.compareTo(expectedAmount) >= 0) {
            newStatus = PaymentOrderStatus.COMPLETED;
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            newStatus = PaymentOrderStatus.PARTIALLY_PAID;
        } else if (failedParts > 0 && failedParts == parts.size()) {
            newStatus = PaymentOrderStatus.FAILED;
        } else if (expiredParts > 0 && expiredParts == parts.size()) {
            newStatus = PaymentOrderStatus.EXPIRED;
        } else {
            newStatus = PaymentOrderStatus.PENDING;
        }

        if (order.getStatus() != newStatus) {
            log.info("Order [{}] status transitioning from {} -> {}", order.getOrderReference(), order.getStatus(), newStatus);
            order.setStatus(newStatus);
            paymentOrderRepository.save(order);
        }

        return ReconciliationResult.builder()
                .expectedAmount(expectedAmount)
                .paidAmount(paidAmount)
                .remainingAmount(remainingAmount)
                .overpaidAmount(overpaidAmount)
                .determinedOrderStatus(newStatus)
                .totalParts(parts.size())
                .successfulParts(successfulParts)
                .failedParts(failedParts)
                .pendingParts(pendingParts)
                .build();
    }
}
