package com.nemi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/pancake")
public class PancakeWebhookTestController {

    private static final Logger logger = LoggerFactory.getLogger(PancakeWebhookTestController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${pancake.x-api-key}")
    private String screetApiKey;

    @GetMapping("/test")
    public ResponseEntity<String> testWebhook() {
        logger.info("Pancake Webhook test endpoint called");
        return ResponseEntity.ok("Pancake Webhook service is working! Current time: " + java.time.LocalDateTime.now());
    }

    @PostMapping
    public ResponseEntity<String> receiveWebhook(@RequestHeader (value = "X-API-KEY") String apiKey,
                                                 @RequestBody String payload) {
        // 1. Verify API key
        if (!screetApiKey.equals(apiKey)) {
         return ResponseEntity.status(401).body("Invalid API key");
        }

        logger.info("=== Pancake Webhook received ===");
        logger.info("Payload body: {}", payload);

        // 2. Try parse JSON
        try {
            // 2. Parse JSON payload
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(payload);

            // 3. Check loại event
            String eventType = root.path("event").asText();
            JsonNode data = root.path("data");

            switch (eventType) {
                case "order.created":
                    handleOrderCreated(data);
                    break;
                case "order.updated":
                    handleOrderUpdated(data);
                    break;
                default:
                    System.out.println("Unhandled event: " + eventType);
            }

            // 4. Trả về 200
            return ResponseEntity.ok("Received");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook");
        }
    }

    private void handleOrderCreated(JsonNode data) {
        // Ví dụ: Lấy order_id, total_price
        String orderId = data.path("order_id").asText();
        double totalPrice = data.path("total_price").asDouble();
        System.out.printf("New order: %s - total: %.2f%n", orderId, totalPrice);

        // TODO: Lưu DB hoặc xử lý business logic
    }

    private void handleOrderUpdated(JsonNode data) {
        // Xử lý cập nhật đơn hàng
    }
}