//package com.nemi.service_impl.nhanhvn;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.nemi.constant.enums.PosName;
//import com.nemi.entity.PosEntity;
//import com.nemi.model.config.NhanhvnConfig;
//import com.nemi.model.request.PosConnectionRequest;
//import com.nemi.model.request.nhanhvn.NhanhvnAccessTokenRequest;
//import com.nemi.model.response.PosConnectionResponse;
//import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
//import com.nemi.repository.PosRepository;
//import com.nemi.repository.TransactionTempRepository;
//import com.nemi.service.WebhookService;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.parameters.P;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class NhanhvnWebhookServiceImpl implements WebhookService {
//
//    private final NhanhvnConfig nhanhvnConfig;
//    private final RestTemplate restTemplate;
//    private final ObjectMapper objectMapper;
//    private final PosRepository posRepository;
//    private final TransactionTempRepository transactionTempRepository;
//    @Override
//    public String getWebhookType() {
//        return PosName.NHANHVN.name().toLowerCase();
//    }
//
//    @Override
//    public void processWebhook(HttpServletRequest request) {
//
//    }
//
//    @Override
//    public boolean supports(String webhookType) {
//        return false;
//    }
//
//    @Override
//    public PosConnectionResponse authorizeUser(PosConnectionRequest posConnectionRequest) throws Exception {
//
//        String appId = posConnectionRequest.getAppId();
//
//        Optional<PosEntity> posEntityOpt = posRepository.findByAppId(appId);
//
//        if(!posEntityOpt.isPresent()){
//            log.info("Appid {} doese not exist",appId);
//            throw new RuntimeException("App id not exist"); // udpate enum exception later
//        }
//        PosEntity pos = posEntityOpt.get();
//
//        String configJson = pos.getConfig();
//        Map<String, Object> configMap = objectMapper.readValue(configJson, new TypeReference <HashMap<String, Object>>() {});
//
//        String secretKey = (String) configMap.get("secret-key");
//
//
//
//
//        Optional<NhanhvnAccessTokenResponse> nhanhvnAccessTokenResponse = exchangeAccessToken();
//
//        return null;
//    }
//
//    public Optional<NhanhvnAccessTokenResponse> exchangeAccessToken(String accessCode,String appId,String businessId) {
//        try {
//            String url = nhanhvnConfig.getUrlAccessCode() + nhanhvnConfig.getApiVersion()
//                    + "/app/getaccesstoken"
//                    + "?appId=" + appId
//                    + "&businessId=" + businessId;
//
//            NhanhvnAccessTokenRequest requestBody =
//                    new NhanhvnAccessTokenRequest(accessCode, nhanhvnConfig.getSecretKey());
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            HttpEntity<NhanhvnAccessTokenRequest> entity = new HttpEntity<>(requestBody, headers);
//
//            log.info("[NhanhvnAuthService.exchangeAccessToken] Request URL: {}", url);
//
//            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
//
//            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
//                log.error("Nhanhvn exchange token failed, status: {}", resp.getStatusCode());
//                return Optional.empty();
//            }
//
//            NhanhvnAccessTokenResponse tokenResponse =
//                    objectMapper.readValue(resp.getBody(), NhanhvnAccessTokenResponse.class);
//
//            if (tokenResponse.getData() == null || tokenResponse.getData().getAccessToken() == null) {
//                log.error("Nhanhvn response does not contain accessToken: {}", resp.getBody());
//                return Optional.empty();
//            }
//
//            log.info("Nhanhvn AccessToken received: {}", tokenResponse.getData().getAccessToken());
//            return Optional.of(tokenResponse);
//
//        } catch (Exception e) {
//            log.error("Exchange token failed: {}", e.getMessage(), e);
//            return Optional.empty();
//        }
//
//        private void getAccessCode(String appId,String businessId) {
//            try {
//                String url = nhanhvnConfig.getUrlAccessCode() + "?"
//                        + "version" + nhanhvnConfig.getApiVersion()
//                        + "&appId=" + appId
//                        + "&returnLink=" + "https://nemi-dev-02.ecombase.net/nemi-product-manager/public-api/nhanhvn/auth";
//
//                restTemplate.getForEntity(url,String.class);
//
//                NhanhvnAccessTokenRequest requestBody =
//                        new NhanhvnAccessTokenRequest(accessCode, nhanhvnConfig.getSecretKey());
//
//                HttpHeaders headers = new HttpHeaders();
//                headers.setContentType(MediaType.APPLICATION_JSON);
//
//                HttpEntity<NhanhvnAccessTokenRequest> entity = new HttpEntity<>(requestBody, headers);
//
//                log.info("[NhanhvnAuthService.exchangeAccessToken] Request URL: {}", url);
//
//                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
//
//                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
//                    log.error("Nhanhvn exchange token failed, status: {}", resp.getStatusCode());
//                    return Optional.empty();
//                }
//
//                NhanhvnAccessTokenResponse tokenResponse =
//                        objectMapper.readValue(resp.getBody(), NhanhvnAccessTokenResponse.class);
//
//                if (tokenResponse.getData() == null || tokenResponse.getData().getAccessToken() == null) {
//                    log.error("Nhanhvn response does not contain accessToken: {}", resp.getBody());
//                    return Optional.empty();
//                }
//
//                log.info("Nhanhvn AccessToken received: {}", tokenResponse.getData().getAccessToken());
//                return Optional.of(tokenResponse);
//
//            } catch (Exception e) {
//                log.error("Exchange token failed: {}", e.getMessage(), e);
//                return Optional.empty();
//            }
//
//
//}
