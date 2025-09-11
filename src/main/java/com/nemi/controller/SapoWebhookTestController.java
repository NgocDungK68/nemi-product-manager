package com.nemi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhook/sapo")
public class SapoWebhookTestController {

    private static final Logger logger = LoggerFactory.getLogger(SapoWebhookTestController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<String> receiveWebhook(HttpServletRequest request) {
        // 1. Read headers
        Map<String, String> headers = extractHeaders(request);
        logger.info("=== Sapo Webhook received ===");
        logger.info("Headers:");
        headers.forEach((k, v) -> logger.info("  {} = {}", k, v));

        // 2. Read raw body
        String body = readBody(request);
        logger.info("Payload body: {}", body);

        // 3. Try parse JSON
        JsonNode root = null;
        try {
            root = objectMapper.readTree(body);
        } catch (IOException e) {
            logger.error("Failed to parse JSON body", e);
        }

        // 4. Log possible fields
        if (root != null) {
            if (root.has("topic")) {
                logger.info("Field topic: {}", root.get("topic").asText());
            }
            if (root.has("event")) {
                logger.info("Field event: {}", root.get("event").asText());
            }
            if (root.has("order")) {
                logger.info("Has order: {}", root.get("order").toString());
            }
            if (root.has("product")) {
                logger.info("Has product: {}", root.get("product").toString());
            }
            if (root.has("customer")) {
                logger.info("Has customer: {}", root.get("customer").toString());
            }
            // Log the whole JSON tree
            logger.info("Full JSON tree: {}", root.toPrettyString());
        }

        // 5. Respond 200 OK
        return ResponseEntity.ok("OK");
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String value = request.getHeader(name);
                map.put(name, value);
            }
        }
        return map;
    }

    private String readBody(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            logger.error("Error reading body", e);
        }
        return sb.toString();
    }
}