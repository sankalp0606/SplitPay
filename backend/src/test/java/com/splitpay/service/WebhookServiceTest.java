package com.splitpay.service;

import com.splitpay.dto.payment.CreatePaymentOrderRequest;
import com.splitpay.dto.payment.PaymentOrderDto;
import com.splitpay.entity.PaymentOrderStatus;
import com.splitpay.entity.PaymentPart;
import com.splitpay.entity.PaymentPartStatus;
import com.splitpay.repository.PaymentEventRepository;
import com.splitpay.repository.PaymentOrderRepository;
import com.splitpay.repository.PaymentPartRepository;
import com.splitpay.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WebhookServiceTest {

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private PaymentPartRepository paymentPartRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PaymentEventRepository paymentEventRepository;

    @BeforeEach
    void setUp() {
        paymentTransactionRepository.deleteAll();
        paymentEventRepository.deleteAll();
        paymentOrderRepository.deleteAll();
    }

    @Test
    @DisplayName("Webhook successfully settles Part 1 and reconciles order to PARTIALLY_PAID")
    void processWebhook_Part1Success_ReconcilesToPartiallyPaid() {
        // 1. Create order for ₹5,000
        CreatePaymentOrderRequest orderReq = CreatePaymentOrderRequest.builder()
                .recipientName("ABC Electronics")
                .upiId("abcelectronics@upi")
                .totalAmount(new BigDecimal("5000.00"))
                .build();
        PaymentOrderDto orderDto = paymentOrderService.createPaymentOrder(orderReq, null);
        String part1Ref = orderDto.getParts().get(0).getPaymentReference();

        // 2. Webhook payload for Part 1 (₹1,990)
        String payload = String.format("""
                {
                    "eventId": "EVT-MOCK-001",
                    "eventType": "PAYMENT.SUCCESS",
                    "paymentReference": "%s",
                    "providerTransactionId": "TXN-BANK-1001",
                    "amount": 1990.00,
                    "status": "SUCCESS"
                }
                """, part1Ref);

        webhookService.processWebhook(payload, Collections.emptyMap(), "127.0.0.1");

        // 3. Verify Part 1 is SUCCESS
        PaymentPart part1 = paymentPartRepository.findByPaymentReference(part1Ref).orElseThrow();
        assertThat(part1.getStatus()).isEqualTo(PaymentPartStatus.SUCCESS);

        // 4. Verify Order is PARTIALLY_PAID
        PaymentOrderDto updatedOrder = paymentOrderService.getOrderById(orderDto.getId());
        assertThat(updatedOrder.getStatus()).isEqualTo(PaymentOrderStatus.PARTIALLY_PAID);
        assertThat(updatedOrder.getPaidAmount()).isEqualByComparingTo("1990.00");
        assertThat(updatedOrder.getRemainingAmount()).isEqualByComparingTo("3010.00");
    }

    @Test
    @DisplayName("Idempotency: Duplicate webhook events are discarded without double crediting")
    void processWebhook_DuplicateEvent_DoesNotDoubleCredit() {
        CreatePaymentOrderRequest orderReq = CreatePaymentOrderRequest.builder()
                .recipientName("ABC Electronics")
                .upiId("abcelectronics@upi")
                .totalAmount(new BigDecimal("5000.00"))
                .build();
        PaymentOrderDto orderDto = paymentOrderService.createPaymentOrder(orderReq, null);
        String part1Ref = orderDto.getParts().get(0).getPaymentReference();

        String payload = String.format("""
                {
                    "eventId": "EVT-MOCK-DUPLICATE",
                    "eventType": "PAYMENT.SUCCESS",
                    "paymentReference": "%s",
                    "providerTransactionId": "TXN-BANK-DUPLICATE",
                    "amount": 1990.00,
                    "status": "SUCCESS"
                }
                """, part1Ref);

        // First delivery
        webhookService.processWebhook(payload, Collections.emptyMap(), "127.0.0.1");
        long txCountAfterFirst = paymentTransactionRepository.count();

        // Second delivery (duplicate event)
        webhookService.processWebhook(payload, Collections.emptyMap(), "127.0.0.1");
        long txCountAfterSecond = paymentTransactionRepository.count();

        assertThat(txCountAfterSecond).isEqualTo(txCountAfterFirst);

        // Verify balance was credited only ONCE
        PaymentOrderDto updatedOrder = paymentOrderService.getOrderById(orderDto.getId());
        assertThat(updatedOrder.getPaidAmount()).isEqualByComparingTo("1990.00");
        assertThat(updatedOrder.getRemainingAmount()).isEqualByComparingTo("3010.00");
    }

    @Test
    @DisplayName("Complete settlement of all parts transitions order to COMPLETED")
    void processWebhook_AllPartsSettled_TransitionsToCompleted() {
        CreatePaymentOrderRequest orderReq = CreatePaymentOrderRequest.builder()
                .recipientName("ABC Electronics")
                .upiId("abcelectronics@upi")
                .totalAmount(new BigDecimal("5000.00"))
                .build();
        PaymentOrderDto orderDto = paymentOrderService.createPaymentOrder(orderReq, null);

        // Settle Part 1
        String part1Ref = orderDto.getParts().get(0).getPaymentReference();
        webhookService.processWebhook(String.format("""
                {"eventId": "EVT-ALL-1", "paymentReference": "%s", "amount": 1990.00, "status": "SUCCESS"}
                """, part1Ref), Collections.emptyMap(), "127.0.0.1");

        // Settle Part 2
        String part2Ref = orderDto.getParts().get(1).getPaymentReference();
        webhookService.processWebhook(String.format("""
                {"eventId": "EVT-ALL-2", "paymentReference": "%s", "amount": 1990.00, "status": "SUCCESS"}
                """, part2Ref), Collections.emptyMap(), "127.0.0.1");

        // Settle Part 3
        String part3Ref = orderDto.getParts().get(2).getPaymentReference();
        webhookService.processWebhook(String.format("""
                {"eventId": "EVT-ALL-3", "paymentReference": "%s", "amount": 1020.00, "status": "SUCCESS"}
                """, part3Ref), Collections.emptyMap(), "127.0.0.1");

        // Order must now be COMPLETED
        PaymentOrderDto finalOrder = paymentOrderService.getOrderById(orderDto.getId());
        assertThat(finalOrder.getStatus()).isEqualTo(PaymentOrderStatus.COMPLETED);
        assertThat(finalOrder.getPaidAmount()).isEqualByComparingTo("5000.00");
        assertThat(finalOrder.getRemainingAmount()).isEqualByComparingTo("0.00");
    }
}
