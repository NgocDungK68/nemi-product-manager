package com.nemi.controller;

import com.nemi.service.WebhookService;
import com.nemi.service.factory.WebhookFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/v1")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookFactory webhookFactory;

    /**
     * Unified webhook endpoint for all webhook types
     * URL patterns:
     * - POST /webhook/nhanh -> webhookType = "nhanh"
     * - POST /webhook/pancake -> webhookType = "pancake"
     * - POST /webhook/sapo -> webhookType = "sapo"
     */
    @PostMapping("/{webhookType}/{posId}")
    public ResponseEntity<String> receiveWebhook(
            @PathVariable String webhookType,
            @PathVariable String posId,
            HttpServletRequest request) {

        log.info("=== UNIFIED WEBHOOK RECEIVED ===");
        log.info("Webhook type: {}, PosId: {}, Request URI: {}, Request method: {}", webhookType, posId, request.getRequestURI(), request.getMethod());

        // Get appropriate webhook service using factory
        WebhookService webhookService = webhookFactory.getWebhookService(webhookType);
        log.info("Using webhook service: {}", webhookService.getClass().getSimpleName());

        // Process webhook using the appropriate service
        webhookService.processWebhook(request);

        log.info("Webhook processed successfully by {}", webhookService.getClass().getSimpleName());
        return ResponseEntity.ok("OK");
    }
}
