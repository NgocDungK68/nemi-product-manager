package com.nemi.controller;

import com.nemi.service.factory.WebhookFactory;
import com.nemi.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
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
        
        try {
            // Get appropriate webhook service using factory
            WebhookService webhookService = webhookFactory.getWebhookService(webhookType);
            
            logger.info("Using webhook service: {}", webhookService.getClass().getSimpleName());
            
            // Process webhook using the appropriate service
            webhookService.processWebhook(request);
            
            logger.info("Webhook processed successfully by {}", webhookService.getClass().getSimpleName());
            return ResponseEntity.ok("OK");
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not supported")) {
                logger.warn("Unsupported webhook type: {}", webhookType);
                return ResponseEntity.status(400).body("Unsupported webhook type: " + webhookType);
            } else {
                logger.error("Error processing webhook of type: {}", webhookType, e);
                return ResponseEntity.status(500).body("Internal Server Error");
            }
        } catch (Exception e) {
            logger.error("Unexpected error processing webhook of type: {}", webhookType, e);
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }

    /**
     * Legacy support for Nhanh webhook (specific endpoint)
     * This maintains backward compatibility with existing Nhanh webhook configuration
     */
    @PostMapping("/nhanh")
    public ResponseEntity<String> receiveNhanhWebhook(HttpServletRequest request) {
        return receiveWebhook("nhanh", request);
    }
}
