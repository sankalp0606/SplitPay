package com.splitpay.controller;

import com.splitpay.dto.payment.CreatePaymentOrderRequest;
import com.splitpay.dto.payment.PaymentOrderDto;
import com.splitpay.dto.payment.PaymentPartDto;
import com.splitpay.dto.payment.PaymentPlanPreviewRequest;
import com.splitpay.dto.payment.PaymentPlanPreviewResponse;
import com.splitpay.security.UserPrincipal;
import com.splitpay.service.PaymentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-orders")
@RequiredArgsConstructor
@Tag(name = "Payment Orders", description = "UPI payment request creation and splitting APIs")
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;

    @PostMapping
    @Operation(summary = "Create a new multi-part UPI payment order")
    public ResponseEntity<PaymentOrderDto> createPaymentOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal != null ? principal.getId() : null;
        PaymentOrderDto response = paymentOrderService.createPaymentOrder(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview payment split parts without creating an order")
    public ResponseEntity<PaymentPlanPreviewResponse> previewPlan(
            @Valid @RequestBody PaymentPlanPreviewRequest request) {
        PaymentPlanPreviewResponse response = paymentOrderService.previewPlan(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List payment orders for the authenticated user")
    public ResponseEntity<Page<PaymentOrderDto>> getOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaymentOrderDto> orders = paymentOrderService.getOrdersForUser(principal.getId(), pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment order details and split parts by ID")
    public ResponseEntity<PaymentOrderDto> getOrderById(@PathVariable UUID id) {
        PaymentOrderDto order = paymentOrderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/ref/{reference}")
    @Operation(summary = "Get payment order details by public reference")
    public ResponseEntity<PaymentOrderDto> getOrderByReference(@PathVariable String reference) {
        PaymentOrderDto order = paymentOrderService.getOrderByReference(reference);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{id}/parts")
    @Operation(summary = "Get split payment parts for an order")
    public ResponseEntity<List<PaymentPartDto>> getPartsForOrder(@PathVariable UUID id) {
        List<PaymentPartDto> parts = paymentOrderService.getPartsForOrder(id);
        return ResponseEntity.ok(parts);
    }
}
