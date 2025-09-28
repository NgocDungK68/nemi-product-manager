package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.constant.enums.PosName;
import com.nemi.model.config.NhanhvnConfig;
import com.nemi.repository.PosRepository;
import com.nemi.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhvnWebhookServiceImpl implements WebhookService {

    private final NhanhvnConfig nhanhvnConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PosRepository posRepository;

    @Override
    public String getPosName() {
        return PosName.NHANHVN.getValue();
    }

    @Override
    public void processWebhook(HttpServletRequest request) {
        log.info("[NhanhvnWebhook] Received request: method={}, uri={}", request.getMethod(), request.getRequestURI());

        //log headers
        Map<String, String> headers = extractHeaders(request);
        log.info("====== Weehook headers: =======");
        headers.forEach((k, v) -> log.info("Header: {} = {}", k, v));

        // read body
        String body = readBody(request);
        log.info("Raw body : {}", body);

        // parse body to json node
        JsonNode root = null;
        try {
            root = objectMapper.readTree(body);
            log.info("parsed body successfully");
        } catch (IOException e) {
            log.error("Failed to parse body to JSON", e);
            throw new RuntimeException("Invalid JSON body");
        }

        if (root != null) {
            //log possible fields
            String eventType = root.has("event") ? root.get("event").asText() : "unknown";
            log.info("Processing event type: {}", eventType);

            if (root.has("businessId")) {
                log.info("Field businessId: {}", root.get("businessId").asText());
            }
            if (root.has("webhooksVerifyToken")) {
                String token = root.get("webhooksVerifyToken").asText();
                log.info("Field webhooksVerifyToken: {}", token);
                if (!nhanhvnConfig.getVerifyToken().equals(token)) {
                    log.warn("Invalid webhook token. Expected: {}, Received: {}",
                            nhanhvnConfig.getVerifyToken(), token);
                    throw new RuntimeException("Invalid token");
                }
            }
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
            log.error("Error reading request body", e);
        }
        return sb.toString();
    }
}



