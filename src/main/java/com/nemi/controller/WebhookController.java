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

    @GetMapping("/debug/nhanh")
    public ResponseEntity<String> debugNhanhWebhook() {
        logger.info("Nhanh webhook debug endpoint called");
        
        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("=== NHANH WEBHOOK DEBUG INFO ===\n");
        debugInfo.append("Current time: ").append(java.time.LocalDateTime.now()).append("\n");
        debugInfo.append("Webhook URL: ").append("POST /webhook/nhanh").append("\n");
        debugInfo.append("Expected verify token: nemiWebhook123!@#22\n");
        debugInfo.append("\n=== SUPPORTED EVENTS ===\n");
        debugInfo.append("- addOrder: New order created\n");
        debugInfo.append("- updateOrder: Order status updated\n");
        debugInfo.append("- addProduct: New product added\n");
        debugInfo.append("- updateProduct: Product information updated\n");
        debugInfo.append("- addCustomer: New customer registered\n");
        debugInfo.append("- updateCustomer: Customer information updated\n");
        debugInfo.append("- addCategory: New category created\n");
        debugInfo.append("- updateCategory: Category updated\n");
        debugInfo.append("- webhooksEnabled: Webhook configuration enabled\n");
        debugInfo.append("\n=== TROUBLESHOOTING ===\n");
        debugInfo.append("1. Check Nhanh.vn webhook configuration\n");
        debugInfo.append("2. Verify webhook URL is publicly accessible\n");
        debugInfo.append("3. Ensure events are enabled in Nhanh.vn admin panel\n");
        debugInfo.append("4. Check webhook verification token\n");
        debugInfo.append("5. Monitor application logs for incoming webhooks\n");
        debugInfo.append("\n=== EXPECTED PAYLOAD FORMAT ===\n");
        debugInfo.append("{\n");
        debugInfo.append("  \"event\": \"addOrder\",\n");
        debugInfo.append("  \"businessId\": \"123456\",\n");
        debugInfo.append("  \"webhooksVerifyToken\": \"nemiWebhook123!@#22\",\n");
        debugInfo.append("  \"data\": {\n");
        debugInfo.append("    // Event specific data\n");
        debugInfo.append("  }\n");
        debugInfo.append("}\n");
        
        return ResponseEntity.ok(debugInfo.toString());
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
}
