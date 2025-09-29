package com.nemi.service_impl.sapo;

import com.fasterxml.jackson.databind.JsonNode;
import com.nemi.configuration.SapoConfig;
import com.nemi.constant.enums.PosName;
import com.nemi.repository.ProductRepository;
import com.nemi.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.awt.desktop.AboutEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SapoWebhookImpl implements WebhookService {
    private final SapoConfig sapoConfig;
    private final ProductRepository productRepository;
    @Override
    public String getPosName() {
        return PosName.SAPO.getValue();
    }

    @Override
    public boolean processWebhook(String posId, HttpServletRequest request) {
        try {
            // 1. Read headers
            Map<String, String> headers = extractHeaders(request);
            log.info("=== Sapo Webhook received ===");
            log.info("Headers:");
            headers.forEach((k, v) -> log.info("  {} = {}", k, v));

            // 2. Read raw body
            String body = readBody(request);
            log.info("Payload body: {}", body);
            log.info("Sapo webhook processed successfully");
            return true;
        }catch (Exception e){
            log.error("Process webhook failed: {}", e.getMessage(), e);
            return false;
        }
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
            log.error("Error reading body", e);
        }
        return sb.toString();
    }
}
