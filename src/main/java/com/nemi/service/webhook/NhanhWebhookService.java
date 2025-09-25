//package com.nemi.service.webhook;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.nemi.model.request.webhook.OrderWebhook;
//import com.nemi.model.request.webhook.WebhookRequest;
//import com.nemi.client.WebhookService;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.MappingMatch;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.util.Collections;
//import java.util.Enumeration;
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//public class NhanhWebhookService implements WebhookService {
//
//    private static final Logger logger = LoggerFactory.getLogger(NhanhWebhookService.class);
//
//    @Autowired
//    private ObjectMapper objectMapper;
//    private static final String VERIFY_TOKEN = "nemiWebhook123!@#";
//
//    @Override
//    public String getWebhookType() {
//        return "nhanh";
//    }
//
//    @Override
//    public boolean supports(String webhookType) {
//        return "nhanh".equalsIgnoreCase(webhookType);
//    }
//
//    @Override
//    public void processWebhook(HttpServletRequest request) {
//        logger.info("=== NHANH.VN WEBHOOK PROCESSING START ===");
//
//        // Log headers for debugging
//        Map<String, String> headers = extractHeaders(request);
//        logger.info("=== WEBHOOK HEADERS ===");
//        headers.forEach((k,v)-> logger.info("Header: {} = {}", k, v));
//
//        // Read raw body
//        String body = readBody(request);
//        logger.info("Raw payload body: {}", body);
//
//        // Parse JSON
//        JsonNode root = null;
//        try {
//            root = objectMapper.readTree(body);
//            logger.info("JSON parsing successful");
//        } catch (IOException e) {
//            logger.error("Error parsing JSON body", e);
//            throw new RuntimeException("Invalid JSON payload", e);
//        }
//
//        if (root != null) {
//            //log possible fields
//            String eventType = root.has("event") ? root.get("event").asText() : "unknown";
//            logger.info("Processing event type: {}", eventType);
//
//            if (root.has("businessId")) {
//                logger.info("Field businessId: {}", root.get("businessId").asText());
//            }
//            if (root.has("webhooksVerifyToken")) {
//                String token = root.get("webhooksVerifyToken").asText();
//                logger.info("Field webhooksVerifyToken: {}", token);
//                if (!VERIFY_TOKEN.equals(token)) {
//                    logger.warn("Invalid webhook token. Expected: {}, Received: {}",
//                            VERIFY_TOKEN, token);
//                    throw new RuntimeException("Invalid token");
//                }
//            }
//            JsonNode data = root.get("data");
//            switch (eventType) {
//                case "webhooksEnabled":
//                    handleWebhooksEnabled(data);
//                    break;
//                case "productAdd":
//                    handleProductAdd(data);
//                    break;
//                case "productUpdate":
//                    handleProductUpdate(data);
//                    break;
//            }
//            logger.info("=== NHANH.VN WEBHOOK PROCESSING COMPLETE ===");
//        }
//    }
//
//    private Map<String, String> extractHeaders(HttpServletRequest request) {
//        Map<String, String> map = new HashMap<>();
//        Enumeration<String> names = request.getHeaderNames();
//        if (names == null) {
//            return Collections.emptyMap();
//        }
//        while (names.hasMoreElements()) {
//            String name = names.nextElement();
//            String value = request.getHeader(name);
//            map.put(name, value);
//        }
//        return map;
//    }
//
//    private String readBody(HttpServletRequest request) {
//        StringBuilder sb = new StringBuilder();
//        try (BufferedReader reader = request.getReader()) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                sb.append(line);
//            }
//        } catch (IOException e) {
//            logger.error("Error reading request body", e);
//        }
//        return sb.toString();
//    }
//
//    private void handleWebhooksEnabled(JsonNode data) {
//        logger.info("Handling webhooksEnabled event");
//        // Implement your logic here
//        if (data != null && data.has("registeredEvents")) {
//            logger.info("Registered events: {}", data.get("registeredEvents").toString());
//        }
//    }
//
//    private void handleProductAdd(JsonNode data) {
//        logger.info("Handling productAdd event");
//        // Implement your logic here
//        try {
//            if (data != null) {
//                logger.info("Product added: {}", data.toPrettyString());
//            }
//        } catch (Exception e) {
//            logger.error("Error processing productAdd event", e);
//        }
//    }
//
//    private void handleProductUpdate(JsonNode data) {
//    logger.info("Handling productUpdate event");
//        // Implement your logic here
//        try {
//            if (data != null) {
//                logger.info("Product updated: {}", data.toPrettyString());
//            }
//        } catch (Exception e) {
//            logger.error("Error processing productUpdate event", e);
//        }
//    }
//}