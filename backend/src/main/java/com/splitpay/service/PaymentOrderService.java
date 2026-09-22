package com.splitpay.service;

import com.splitpay.dto.payment.*;
import com.splitpay.entity.*;
import com.splitpay.exception.ResourceNotFoundException;
import com.splitpay.qr.QrPaymentResult;
import com.splitpay.qr.QrPaymentService;
import com.splitpay.repository.PaymentOrderRepository;
import com.splitpay.repository.PaymentPartRepository;
import com.splitpay.repository.UserRepository;
import com.splitpay.split.PaymentSplitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrderService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentPartRepository paymentPartRepository;
    private final UserRepository userRepository;
    private final PaymentSplitService paymentSplitService;
    private final QrPaymentService qrPaymentService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public PaymentOrderDto createPaymentOrder(CreatePaymentOrderRequest request, UUID userId) {
        // 1. Validate UPI ID
        qrPaymentService.validateUpiId(request.getUpiId());

        // 2. Resolve Strategy and Calculate Split Parts
        SplittingStrategyType strategyType = request.getSplittingStrategy() != null
                ? request.getSplittingStrategy()
                : paymentSplitService.getDefaultStrategy();

        BigDecimal effectiveMaxPartAmount = request.getMaxPartAmount() != null
                ? request.getMaxPartAmount()
                : paymentSplitService.getDefaultMaxPartAmount();

        List<BigDecimal> partAmounts = paymentSplitService.split(
                request.getTotalAmount(),
                strategyType,
                effectiveMaxPartAmount,
                request.getCustomParts()
        );

        // 3. User Resolution (optional for guest / merchant)
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        // 4. Generate Order Reference
        String orderRef = generateOrderReference();

        // 5. Create PaymentOrder
        PaymentOrder order = PaymentOrder.builder()
                .orderReference(orderRef)
                .user(user)
                .recipientName(request.getRecipientName().trim())
                .upiId(request.getUpiId().trim())
                .totalAmount(request.getTotalAmount())
                .currency("INR")
                .status(PaymentOrderStatus.PENDING)
                .splittingStrategy(strategyType)
                .maxPartAmount(effectiveMaxPartAmount)
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build();

        // 6. Generate PaymentParts with unique payment references and QR payloads
        int partNumber = 1;
        for (BigDecimal partAmount : partAmounts) {
            String partRef = generatePartReference(orderRef, partNumber);
            String note = String.format("SplitPay %s Part %d/%d", orderRef, partNumber, partAmounts.size());

            QrPaymentResult qrResult = qrPaymentService.generatePaymentRequest(
                    order.getUpiId(),
                    order.getRecipientName(),
                    partAmount,
                    partRef,
                    note
            );

            PaymentPart part = PaymentPart.builder()
                    .partNumber(partNumber)
                    .paymentReference(partRef)
                    .amount(partAmount)
                    .currency("INR")
                    .status(PaymentPartStatus.PENDING) // Strictly PENDING on creation!
                    .upiUri(qrResult.getUpiUri())
                    .qrPayload(qrResult.getQrDataUri())
                    .build();

            order.addPart(part);
            partNumber++;
        }

        PaymentOrder savedOrder = paymentOrderRepository.save(order);
        log.info("Created payment order: [ref={}, total=₹{}, parts={}]",
                savedOrder.getOrderReference(), savedOrder.getTotalAmount(), savedOrder.getParts().size());

        return toDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public PaymentPlanPreviewResponse previewPlan(PaymentPlanPreviewRequest request) {
        SplittingStrategyType strategyType = request.getSplittingStrategy() != null
                ? request.getSplittingStrategy()
                : paymentSplitService.getDefaultStrategy();

        BigDecimal effectiveMaxPartAmount = request.getMaxPartAmount() != null
                ? request.getMaxPartAmount()
                : paymentSplitService.getDefaultMaxPartAmount();

        List<BigDecimal> parts = paymentSplitService.split(
                request.getTotalAmount(),
                strategyType,
                effectiveMaxPartAmount,
                request.getCustomParts()
        );

        return PaymentPlanPreviewResponse.builder()
                .totalAmount(request.getTotalAmount())
                .partCount(parts.size())
                .parts(parts)
                .splittingStrategy(strategyType)
                .maxPartAmount(effectiveMaxPartAmount)
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentOrderDto getOrderById(UUID id) {
        PaymentOrder order = paymentOrderRepository.findByIdWithParts(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", "id", id));
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public PaymentOrderDto getOrderByReference(String reference) {
        PaymentOrder order = paymentOrderRepository.findByOrderReferenceWithParts(reference)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", "orderReference", reference));
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public Page<PaymentOrderDto> getOrdersForUser(UUID userId, Pageable pageable) {
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<PaymentOrderDto> getAllOrders(Pageable pageable) {
        return paymentOrderRepository.findAll(pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<PaymentPartDto> getPartsForOrder(UUID orderId) {
        return paymentPartRepository.findByPaymentOrderIdOrderByPartNumberAsc(orderId).stream()
                .map(this::toPartDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentPartDto getPartById(UUID partId) {
        PaymentPart part = paymentPartRepository.findById(partId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentPart", "id", partId));
        return toPartDto(part);
    }

    private PaymentOrderDto toDto(PaymentOrder order) {
        List<PaymentPartDto> partDtos = order.getParts().stream()
                .map(this::toPartDto)
                .toList();

        BigDecimal paidAmount = order.getParts().stream()
                .filter(p -> p.getStatus() == PaymentPartStatus.SUCCESS)
                .map(PaymentPart::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingAmount = order.getTotalAmount().subtract(paidAmount);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        return PaymentOrderDto.builder()
                .id(order.getId())
                .orderReference(order.getOrderReference())
                .recipientName(order.getRecipientName())
                .upiId(order.getUpiId())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .splittingStrategy(order.getSplittingStrategy())
                .maxPartAmount(order.getMaxPartAmount())
                .notes(order.getNotes())
                .paidAmount(paidAmount)
                .remainingAmount(remainingAmount)
                .partCount(order.getParts().size())
                .parts(partDtos)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private PaymentPartDto toPartDto(PaymentPart part) {
        return PaymentPartDto.builder()
                .id(part.getId())
                .paymentOrderId(part.getPaymentOrder() != null ? part.getPaymentOrder().getId() : null)
                .partNumber(part.getPartNumber())
                .paymentReference(part.getPaymentReference())
                .amount(part.getAmount())
                .currency(part.getCurrency())
                .status(part.getStatus())
                .upiUri(part.getUpiUri())
                .qrDataUri(part.getQrPayload())
                .createdAt(part.getCreatedAt())
                .updatedAt(part.getUpdatedAt())
                .build();
    }

    private String generateOrderReference() {
        long epochSecond = Instant.now().getEpochSecond();
        int randomDigits = 1000 + secureRandom.nextInt(9000);
        return String.format("ORD-%d-%d", epochSecond, randomDigits);
    }

    private String generatePartReference(String orderRef, int partNumber) {
        int randomSuffix = 100 + secureRandom.nextInt(900);
        return String.format("SP-%s-P%d-%d", orderRef.replace("ORD-", ""), partNumber, randomSuffix);
    }
}
