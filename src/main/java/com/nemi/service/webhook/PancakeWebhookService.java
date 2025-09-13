package com.nemi.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
public class PancakeWebhookService implements WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(PancakeWebhookService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getWebhookType() {
        return "pancake";
    }

    @Override
    public boolean supports(String webhookType) {
        return "pancake".equalsIgnoreCase(webhookType);
    }

    @Override
    public void processWebhook(HttpServletRequest request) {
        // Log headers
        Map<String, String> headers = extractHeaders(request);
        logger.info("=== Pancake Webhook received ===");
        headers.forEach((k, v) -> logger.info("Header: {} = {}", k, v));

        // Read raw body
        String body = readBody(request);
        logger.info("Payload body: {}", body);

        // Try parse JSON
        JsonNode root = null;
        try {
            root = objectMapper.readTree(body);
        } catch (IOException e) {
            logger.error("Error parsing JSON body", e);
        }

        if (root != null) {
            // Log possible fields
            if (root.has("event")) {
                logger.info("Field event: {}", root.get("event").asText());
            }
            if (root.has("topic")) {
                logger.info("Field topic: {}", root.get("topic").asText());
            }
            if (root.has("data")) {
                logger.info("Field data: {}", root.get("data").toString());
            }
            if (root.has("order")) {
                logger.info("Field order: {}", root.get("order").toString());
            }
            if (root.has("customer")) {
                logger.info("Field customer: {}", root.get("customer").toString());
            }
            // Log full tree
            logger.info("Full JSON tree: {}", root.toPrettyString());
        }

        logger.info("Pancake webhook processed successfully");
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return Collections.emptyMap();
        }
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String value = request.getHeader(name);
            map.put(name, value);
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
            logger.error("Error reading request body", e);
        }
        return sb.toString();
    }
}
