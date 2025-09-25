package com.nemi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.model.auth.request.AuthPosRequest;
import com.nemi.repository.PosRepository;
import com.nemi.repository.TransactionTempRepository;
import com.nemi.service.WebhookService;
import com.nemi.service.factory.WebhookFactory;
import com.nemi.service_impl.nhanhvn.NhanhvnWebhookServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/v1")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final NhanhvnWebhookServiceImpl nhanhvnWebhookService;
    private final TransactionTempRepository transactionTempRepository;
    private final PosRepository posRepository;
    private final ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private WebhookFactory webhookFactory;

    @GetMapping("/test")
    public ResponseEntity<String> testWebhook() {
        logger.info("Webhook test endpoint called");
        return ResponseEntity.ok("Unified Webhook service is working! Current time: " + java.time.LocalDateTime.now());
    }

    /**
     * Unified webhook endpoint for all webhook types
     * URL patterns:
     * - POST /webhook/nhanh -> webhookType = "nhanh"
     * - POST /webhook/pancake -> webhookType = "pancake"
     * - POST /webhook/sapo -> webhookType = "sapo"
     */
    @PostMapping("/{webhookType}")
    public ResponseEntity<String> receiveWebhook(
            @PathVariable String webhookType,
            HttpServletRequest request) {

        logger.info("=== UNIFIED WEBHOOK RECEIVED ===");
        logger.info("Webhook type: {}", webhookType);
        logger.info("Request URI: {}", request.getRequestURI());
        logger.info("Request method: {}", request.getMethod());

        // Get appropriate webhook service using factory
        WebhookService webhookService = webhookFactory.getWebhookService(webhookType);
        logger.info("Using webhook service: {}", webhookService.getClass().getSimpleName());

        // Process webhook using the appropriate service
        webhookService.processWebhook(request);

        logger.info("Webhook processed successfully by {}", webhookService.getClass().getSimpleName());
        return ResponseEntity.ok("OK");
    }

    /**
     * Legacy support for Nhanh webhook (specific endpoint)
     * This maintains backward compatibility with existing Nhanh webhook configuration
     */
    @PostMapping("/nhanh")
    public ResponseEntity<String> receiveNhanhWebhook(HttpServletRequest request) {
        return receiveWebhook("nhanh", request);
    }

    @GetMapping("/{posName}/auth") //set accesstoken + update status -> active
    public ResponseEntity<String> authWebhook(@PathVariable String posName,
                                              @RequestParam(required = false) String accessCode,
                                              @RequestParam(required = false) String code) {

        AuthPosRequest authPosRequest = AuthPosRequest.builder().accessCode(accessCode).build();
        WebhookService webhookService = webhookFactory.getWebhookService(posName);
        webhookService.authPos(authPosRequest);

        return ResponseEntity.ok("accessToken updated");
    }
}
