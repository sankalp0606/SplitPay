package com.splitpay.controller;

import com.splitpay.audit.AuditService;
import com.splitpay.dto.payment.PaymentOrderDto;
import com.splitpay.entity.AuditLog;
import com.splitpay.entity.PaymentEvent;
import com.splitpay.entity.PaymentOrderStatus;
import com.splitpay.entity.PaymentTransaction;
import com.splitpay.repository.PaymentEventRepository;
import com.splitpay.repository.PaymentOrderRepository;
import com.splitpay.repository.PaymentTransactionRepository;
import com.splitpay.service.PaymentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Observability", description = "Operational visibility for orders, transactions, events, and audit logs")
public class AdminController {

    private final PaymentOrderService paymentOrderService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuditService auditService;

    @GetMapping("/metrics")
    @Operation(summary = "Get high-level payment status counts and platform metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        long pendingOrders = paymentOrderRepository.countByStatus(PaymentOrderStatus.PENDING);
        long completedOrders = paymentOrderRepository.countByStatus(PaymentOrderStatus.COMPLETED);
        long partiallyPaidOrders = paymentOrderRepository.countByStatus(PaymentOrderStatus.PARTIALLY_PAID);
        long failedOrders = paymentOrderRepository.countByStatus(PaymentOrderStatus.FAILED);
        long totalOrders = paymentOrderRepository.count();

        return ResponseEntity.ok(Map.of(
                "totalOrders", totalOrders,
                "pendingOrders", pendingOrders,
                "completedOrders", completedOrders,
                "partiallyPaidOrders", partiallyPaidOrders,
                "failedOrders", failedOrders
        ));
    }

    @GetMapping("/orders")
    @Operation(summary = "List all platform payment orders (paginated)")
    public ResponseEntity<Page<PaymentOrderDto>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(paymentOrderService.getAllOrders(pageable));
    }

    @GetMapping("/events")
    @Operation(summary = "List all inbound provider webhook events")
    public ResponseEntity<Page<PaymentEvent>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(paymentEventRepository.findAll(pageable));
    }

    @GetMapping("/transactions")
    @Operation(summary = "List all settled provider payment transactions")
    public ResponseEntity<Page<PaymentTransaction>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(paymentTransactionRepository.findAll(pageable));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "List security and administrative audit trail logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(auditService.getAllAuditLogs(pageable));
    }
}
