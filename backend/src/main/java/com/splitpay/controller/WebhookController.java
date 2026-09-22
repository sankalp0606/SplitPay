package com.splitpay.controller;

import com.splitpay.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Inbound payment provider webhook ingestion")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    @Operation(summary = "Ingest authoritative payment notification from payment provider")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String payload,
            HttpServletRequest request) {

        Map<String, String> headers = extractHeaders(request);
        String clientIp = getClientIp(request);

        webhookService.processWebhook(payload, headers, clientIp);

        return ResponseEntity.ok(Map.of(
                "status", "ACCEPTED",
                "message", "Webhook received and processed"
        ));
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                map.put(name, request.getHeader(name));
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
