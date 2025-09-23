package com.nemi.controller;

import com.nemi.model.response.PosConnectionResponse;
import com.nemi.service.factory.WebhookFactory;
import com.nemi.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/v1")
public class WebhookController {
    
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

    @GetMapping("/{posName}/auth")
    public PosConnectionResponse authWebhook(@PathVariable String posName,
                                             @RequestParam String accessCode,
                                             @RequestParam String code) {
        WebhookService webhookService = webhookFactory.getWebhookService(posName);


        return null;
    }
}
