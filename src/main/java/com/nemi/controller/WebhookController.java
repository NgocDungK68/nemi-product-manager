package com.nemi.controller;

import com.nemi.constant.WebhookConstants;
import com.nemi.service.WebhookService;
import com.nemi.service.factory.WebhookFactory;
import com.nemi.utils.PosUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhook/v1")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookFactory webhookFactory;
    private final HttpServletRequest httpServletRequest;

    /**
     * Unified webhook endpoint for all webhook types
     * URL patterns:
     * - POST /webhook/nhanh -> webhookType = "nhanh"
     * - POST /webhook/pancake -> webhookType = "pancake"
     * - POST /webhook/sapo -> webhookType = "sapo"
     */
    @PostMapping("/{posName}/{posId}")
    public void receiveWebhook(
            @PathVariable String posName,
            @PathVariable String posId,
            @RequestBody Object body) {

        log.info("=== UNIFIED WEBHOOK RECEIVED ===");
        log.info("Webhook type: {}, PosId: {}", posName, posId);

        Map<String, String> headers = PosUtils.extractHeaders(httpServletRequest);
        log.debug("Webhook headers:");
        headers.forEach((k, v) -> log.debug("  {} = {}", k, v));
        log.debug("Webhook body: {}", body);

        // Get appropriate webhook service using factory
        WebhookService webhookService = webhookFactory.getWebhookService(posName);
        log.info("Using webhook service: {}", webhookService.getClass().getSimpleName());

        // Process webhook using the appropriate service
        boolean isSuccess = webhookService.processWebhook(posId, headers, body);
        String webhookStatus = isSuccess ? WebhookConstants.Status.SUCCESS : WebhookConstants.Status.FAILED;

        log.info("Webhook processed by {} with result: {}",
                webhookService.getClass().getSimpleName(),
                webhookStatus);
    }

    @PostMapping("/pancake/test")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) throws IOException {
        // 🧾 Log headers
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        log.info("🟣 [TestWebhook] Headers: {}", headers);

        // 🔍 Log query parameters
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (!parameterMap.isEmpty()) {
            Map<String, Object> queryParams = new HashMap<>();
            parameterMap.forEach((k, v) -> queryParams.put(k, String.join(",", v)));
            log.info("🟢 [TestWebhook] Query Params: {}", queryParams);
        } else {
            log.info("🟢 [TestWebhook] No query parameters");
        }

        // 📦 Log raw body
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        log.info("🔵 [TestWebhook] Body: {}", body);

        return ResponseEntity.ok("Webhook received successfully");
    }

    // Optional: hỗ trợ GET để test nhanh trên trình duyệt
    @GetMapping
    public ResponseEntity<String> testGet(HttpServletRequest request) {
        log.info("✅ [TestWebhook] GET received with params: {}", request.getParameterMap());
        return ResponseEntity.ok("Test webhook GET ok");
    }

}
