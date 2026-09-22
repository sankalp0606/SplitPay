package com.splitpay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitpay.dto.auth.RegisterRequest;
import com.splitpay.dto.payment.CreatePaymentOrderRequest;
import com.splitpay.dto.payment.PaymentPlanPreviewRequest;
import com.splitpay.entity.PaymentOrderStatus;
import com.splitpay.entity.PaymentPartStatus;
import com.splitpay.repository.PaymentOrderRepository;
import com.splitpay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private UserRepository userRepository;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        paymentOrderRepository.deleteAll();
        userRepository.deleteAll();

        // Register and get auth token
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("testuser@splitpay.in")
                .password("TestPassword123!")
                .fullName("Test Merchant")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        authToken = objectMapper.readTree(regResult.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("Create Payment Order for ABC Electronics ₹5,000 splits into ₹1,990, ₹1,990, ₹1,020 all PENDING")
    void createPaymentOrder_ABC_Electronics_5000_Success() throws Exception {
        CreatePaymentOrderRequest request = CreatePaymentOrderRequest.builder()
                .recipientName("ABC Electronics")
                .upiId("abcelectronics@upi")
                .totalAmount(new BigDecimal("5000.00"))
                .notes("Store invoice #8492")
                .build();

        mockMvc.perform(post("/api/payment-orders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.orderReference", startsWith("ORD-")))
                .andExpect(jsonPath("$.recipientName", is("ABC Electronics")))
                .andExpect(jsonPath("$.upiId", is("abcelectronics@upi")))
                .andExpect(jsonPath("$.totalAmount", is(5000.00)))
                .andExpect(jsonPath("$.status", is(PaymentOrderStatus.PENDING.name())))
                .andExpect(jsonPath("$.partCount", is(3)))
                .andExpect(jsonPath("$.paidAmount", is(0)))
                .andExpect(jsonPath("$.remainingAmount", is(5000.00)))
                // Verify Part 1
                .andExpect(jsonPath("$.parts[0].partNumber", is(1)))
                .andExpect(jsonPath("$.parts[0].amount", is(1990.00)))
                .andExpect(jsonPath("$.parts[0].status", is(PaymentPartStatus.PENDING.name())))
                .andExpect(jsonPath("$.parts[0].paymentReference", startsWith("SP-")))
                .andExpect(jsonPath("$.parts[0].upiUri", containsString("pa=abcelectronics@upi")))
                .andExpect(jsonPath("$.parts[0].qrDataUri", startsWith("data:image/png;base64,")))
                // Verify Part 2
                .andExpect(jsonPath("$.parts[1].partNumber", is(2)))
                .andExpect(jsonPath("$.parts[1].amount", is(1990.00)))
                .andExpect(jsonPath("$.parts[1].status", is(PaymentPartStatus.PENDING.name())))
                // Verify Part 3
                .andExpect(jsonPath("$.parts[2].partNumber", is(3)))
                .andExpect(jsonPath("$.parts[2].amount", is(1020.00)))
                .andExpect(jsonPath("$.parts[2].status", is(PaymentPartStatus.PENDING.name())));
    }

    @Test
    @DisplayName("Preview plan returns split parts without persisting order")
    void previewPlan_Success() throws Exception {
        PaymentPlanPreviewRequest previewReq = PaymentPlanPreviewRequest.builder()
                .totalAmount(new BigDecimal("7500.00"))
                .build();

        mockMvc.perform(post("/api/payment-orders/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partCount", is(4)))
                .andExpect(jsonPath("$.parts[0]", is(1990.00)))
                .andExpect(jsonPath("$.parts[1]", is(1990.00)))
                .andExpect(jsonPath("$.parts[2]", is(1990.00)))
                .andExpect(jsonPath("$.parts[3]", is(1530.00)));
    }

    @Test
    @DisplayName("Rejects invalid UPI ID format with 400")
    void createPaymentOrder_InvalidUpi_Fails() throws Exception {
        CreatePaymentOrderRequest request = CreatePaymentOrderRequest.builder()
                .recipientName("ABC Electronics")
                .upiId("invalid-upi-without-at")
                .totalAmount(new BigDecimal("5000.00"))
                .build();

        mockMvc.perform(post("/api/payment-orders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Fetch payment part QR code image returns PNG format")
    void getPartQrImage_ReturnsPng() throws Exception {
        // Create an order first
        CreatePaymentOrderRequest request = CreatePaymentOrderRequest.builder()
                .recipientName("Test Store")
                .upiId("teststore@upi")
                .totalAmount(new BigDecimal("1000.00"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/payment-orders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String partId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("parts").get(0).get("id").asText();

        // Public QR PNG download
        mockMvc.perform(get("/api/payment-parts/" + partId + "/qr"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }
}
