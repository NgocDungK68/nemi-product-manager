package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.nemi.constant.enums.PosName;
import com.nemi.constant.enums.PosStatus;
import com.nemi.entity.PosEntity;
import com.nemi.model.auth.request.AuthPosRequest;
import com.nemi.model.config.NhanhvnConfig;
import com.nemi.model.request.nhanhvn.NhanhvnAccessTokenRequest;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.repository.PosRepository;
import com.nemi.service.WebhookService;
import com.nemi.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhvnWebhookServiceImpl implements WebhookService {

    private final NhanhvnConfig nhanhvnConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PosRepository posRepository;

    @Value("${nhanhvn.verify-token}")
    private String verifyToken;

    @Override
    public String getWebhookType() {
        return PosName.NHANHVN.name().toLowerCase();
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
                if (!verifyToken.equals(token)) {
                    log.warn("Invalid webhook token. Expected: {}, Received: {}",
                            verifyToken, token);
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


    @Override
    public boolean supports(String webhookType) {
        if (webhookType == null) {
            return false;
        }
        String type = webhookType.toLowerCase();
        return "nhanh".equals(type) || "nhanhvn".equals(type);
    }

    // lay accessToken, set status trong pos thanh active
    public void authPos(AuthPosRequest authPosRequest) {
        try {

            String appId = authPosRequest.getAppId();

            Optional<PosEntity> posOPt = posRepository.findByAppId(appId);
            if (posOPt.isEmpty()) {
                throw new RuntimeException("pos connection is not exist");
            }
            PosEntity pos = posOPt.get();

            String configJson = pos.getConfig();
            Map<String, Object> configMap = objectMapper.readValue(configJson, new TypeReference<HashMap<String, Object>>() {
            });

            String secretKey = (String) configMap.get("secret-key");
            String businessId = (String) configMap.get("business-id");
            log.info("secretkey : {}", secretKey);
            log.info("businessId : {}", businessId);


            String url = nhanhvnConfig.getUrlAccessToken() + nhanhvnConfig.getApiVersion()
                    + "/app/getaccesstoken"
                    + "?appId=" + appId
                    + "&businessId=" + businessId;

            NhanhvnAccessTokenRequest requestBody =
                    new NhanhvnAccessTokenRequest(authPosRequest.getAccessCode(), secretKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<NhanhvnAccessTokenRequest> entity = new HttpEntity<>(requestBody, headers);

            log.info("[NhanhvnAuthService.exchangeAccessToken] Request URL: {}", url);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.error("Nhanhvn exchange token failed, status: {}", resp.getStatusCode());

            }

            NhanhvnAccessTokenResponse tokenResponse =
                    JsonUtils.fromJson(resp.getBody(), NhanhvnAccessTokenResponse.class);

            if (tokenResponse.getData() == null || tokenResponse.getData().getAccessToken() == null) {
                log.error("Nhanhvn response does not contain accessToken: {}", resp.getBody());

            }

            log.info("Nhanhvn AccessToken received: {}", tokenResponse.getData().getAccessToken());

            pos.setAccessToken(tokenResponse.getData().getAccessToken());
            pos.setStatus(PosStatus.ACTIVE.name());
            posRepository.save(pos);
            log.info("lay duoc accessToken: {} cua appid: {}  ", pos.getAccessToken(), appId);


        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);

        }
    }

    public Optional<NhanhvnAccessTokenResponse> exchangeAccessToken(String accessCode, String appId, String businessId, String secretKey) {
        try {
            String url = nhanhvnConfig.getUrlAccessCode() + nhanhvnConfig.getApiVersion()
                    + "/app/getaccesstoken"
                    + "?appId=" + appId
                    + "&businessId=" + businessId;

            NhanhvnAccessTokenRequest requestBody =
                    new NhanhvnAccessTokenRequest(accessCode, secretKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<NhanhvnAccessTokenRequest> entity = new HttpEntity<>(requestBody, headers);

            log.info("[NhanhvnAuthService.exchangeAccessToken] Request URL: {}", url);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.error("Nhanhvn exchange token failed, status: {}", resp.getStatusCode());
                return Optional.empty();
            }

            NhanhvnAccessTokenResponse tokenResponse =
                    objectMapper.readValue(resp.getBody(), NhanhvnAccessTokenResponse.class);

            if (tokenResponse.getData() == null || tokenResponse.getData().getAccessToken() == null) {
                log.error("Nhanhvn response does not contain accessToken: {}", resp.getBody());
                return Optional.empty();
            }

            log.info("Nhanhvn AccessToken received: {}", tokenResponse.getData().getAccessToken());
            return Optional.of(tokenResponse);

        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }


//    @Override
//    public void authWebhook(String appId ) throws Exception {
//
//        Optional<PosEntity> posEntityOpt = posRepository.findByAppId(appId);
//
//        if(!posEntityOpt.isPresent()){
//            log.info("Appid {} doese not exist",appId);
//            throw new RuntimeException("App id not exist"); // udpate enum exception later
//        }
//        PosEntity pos = posEntityOpt.get();
//
//        getAccessCode(appId,pos.getUserId(),pos.getId());
//    }
//
//        private void getAccessCode(String appId,String userId,String posId) {
//            try {
//                String url = nhanhvnConfig.getUrlAccessCode() + "?"
//                        + "version" + nhanhvnConfig.getApiVersion()
//                        + "&appId=" + appId
//                        + "&returnLink=" + "https://nemi-dev-02.ecombase.net/nemi-product-manager/public-api/nhanhvn/auth";
//
//                restTemplate.getForEntity(url, String.class);
//
//                TransactionTempEntity transactionTempEntity = TransactionTempEntity.builder()
//                        .id(posId)
//                        .status(PosStatus.PENDING.name())
//                        .appId(appId)
//                        .createdBy(userId)
//                        .build();
//
//                transactionTempRepository.save(transactionTempEntity);
//
//            } catch (Exception e) {
//                log.error("Exchange token failed: {}", e.getMessage(), e);
//            }
//
//        }
}



