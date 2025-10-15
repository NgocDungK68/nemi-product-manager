package com.nemi.controller;

import com.nemi.constant.WebhookConstants;
import com.nemi.service.WebhookService;
import com.nemi.service.factory.WebhookFactory;
import com.nemi.utils.PosUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        boolean isSuccess = webhookService.processWebhook(posId, posName, headers, body);
        String webhookStatus = isSuccess ? WebhookConstants.Status.SUCCESS : WebhookConstants.Status.FAILED;

        log.info("Webhook processed by {} with result: {}",
                webhookService.getClass().getSimpleName(),
                webhookStatus);
    }
}
