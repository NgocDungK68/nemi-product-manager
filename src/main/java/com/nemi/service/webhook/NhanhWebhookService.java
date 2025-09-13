package com.nemi.service.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.model.request.webhook.OrderWebhook;
import com.nemi.model.request.webhook.WebhookRequest;
import com.nemi.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.MappingMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
public class NhanhWebhookService implements WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(NhanhWebhookService.class);

    @Autowired
    private ObjectMapper objectMapper;
    private static final String VERIFY_TOKEN = "nemiWebhook123!@#22";

    @Override
    public String getWebhookType() {
        return "nhanh";
    }

    @Override
    public boolean supports(String webhookType) {
        return "nhanh".equalsIgnoreCase(webhookType);
    }

    @Override
    public void processWebhook(HttpServletRequest request) {
        //log headers
        Map<String, String> headers  =extractHeaders(request);
        logger.info("===Nhanhvn WEBHOOK HEADERS ===");
        headers.forEach((k,v)-> logger.info("{}: {}", k,v));

        //read raw body
        String body = readBody(request);
        logger.info("Payload body: {}", body);

        //try parse json
        JsonNode root = null;
        try {
            root = objectMapper.readTree(body);
        } catch (IOException e) {
            logger.error("Error parsing JSON body", e);
        }

        if (root != null) {
            //log possible fields
            if (root.has("event")) {
                logger.info("Field event: {}", root.get("event").asText());
            }
            if (root.has("businessId")) {
                logger.info("Field businessId: {}", root.get("businessId").asText());
            }
            if (root.has("webhooksVerifyToken")) {
                String token = root.get("webhooksVerifyToken").asText();
                logger.info("Field webhooksVerifyToken: {}", token);
                if (!VERIFY_TOKEN.equals(token)) {
                    logger.warn("Invalid webhook token. Expected: {}, Received: {}",
                            VERIFY_TOKEN, token);
                    throw new RuntimeException("Invalid token");
                }
            }
            if (root.has("data")) {
                logger.info("Field data: {}", root.get("data").toString());
            }
            //log the whole json tree
            logger.info("Full JSON tree: {}", root.toPrettyString());
        }

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