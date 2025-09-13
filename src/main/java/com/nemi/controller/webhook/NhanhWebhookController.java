//package com.nemi.controller.webhook;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.nemi.model.request.webhook.WebhookRequest;
//import com.nemi.service.webhook.NhanhWebhookService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/webhook")
//public class NhanhWebhookController {
//    private static final Logger logger = LoggerFactory.getLogger(NhanhWebhookController.class);
//
//    @Autowired
//    private NhanhWebhookService webhookService;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    // Thay YOUR_VERIFY_TOKEN bằng token bạn đặt trong app Nhanh.vn
//    private static final String VERIFY_TOKEN = "nemiWebhook123!@#";
//
//    @GetMapping("/test")
//    public ResponseEntity<String> testWebhook() {
//        logger.info("Webhook test endpoint called");
//        return ResponseEntity.ok("Webhook service is working! Current time: " + java.time.LocalDateTime.now());
//    }
//
//    @PostMapping("/nhanh")
//    public ResponseEntity<String> receiveWebhook(@RequestBody String rawBody) {
//        logger.info("=== RAW WEBHOOK RECEIVED ===");
//        logger.info("Raw webhook body: {}", rawBody);
//
//        try {
//            // Parse raw body to WebhookRequest
//            WebhookRequest webhookRequest = objectMapper.readValue(rawBody, WebhookRequest.class);
//
//            logger.info("=== WEBHOOK PARSED SUCCESSFULLY ===");
//            logger.info("Received webhook - Event: {}, BusinessId: {}",
//                    webhookRequest.getEvent(), webhookRequest.getBusinessId());
//            logger.info("Received webhook - Token: {}", webhookRequest.getWebhooksVerifyToken());
//            logger.info("Webhook data: {}", webhookRequest.getData());
//
//            // Verify token
//            if (!VERIFY_TOKEN.equals(webhookRequest.getWebhooksVerifyToken())) {
//                logger.warn("Invalid webhook token. Expected: {}, Received: {}",
//                        VERIFY_TOKEN, webhookRequest.getWebhooksVerifyToken());
//                return ResponseEntity.status(401).body("Invalid token");
//            }
//
//            // Sử dụng WebhookService để xử lý
//            webhookService.processWebhook(webhookRequest);
//
//            logger.info("Webhook processed successfully");
//            return ResponseEntity.ok("OK");
//
//        } catch (Exception e) {
//            logger.error("Error parsing or processing webhook", e);
//            logger.error("Raw body that caused error: {}", rawBody);
//            return ResponseEntity.status(500).body("Internal Server Error");
//        }
//    }
//}