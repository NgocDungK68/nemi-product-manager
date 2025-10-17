package com.nemi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.configuration.SapoConfig;
import com.nemi.constant.PancakeConstatns;
import com.nemi.constant.SapoConstants;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.sapo.SapoRequest;
import com.nemi.model.request.sapo.SapoWebhookRequest;
import com.nemi.model.response.sapo.SapoAccessTokenResponse;
import com.nemi.model.response.sapo.SapoOrderResponse;
import com.nemi.model.response.sapo.SapoProductResponse;
import com.nemi.model.response.sapo.SapoWebhookListResponse;
import com.nemi.model.response.sapo.SapoWebhookResponse;
import com.nemi.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class SapoClient {
    private final RestTemplate restTemplate;
    private final SapoConfig sapoConfig;
    private final ObjectMapper objectMapper;

    public SapoAccessTokenResponse getAccessToken(PosConnectionRequest posConnectionRequest) {
        try {
            // Build full URL dynamically because each merchant has different storeName
            String baseUrl = "https://" + posConnectionRequest.getStoreName() + ".mysapo.net";
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(sapoConfig.getPathOauthAccessToken())
                    .queryParam(SapoConstants.CLIENT_ID, posConnectionRequest.getClientId())
                    .queryParam(SapoConstants.CLIENT_SECRET, posConnectionRequest.getClientSecret())
                    .queryParam(SapoConstants.CODE, posConnectionRequest.getCode())
                    .toUriString();

            log.info("[SapoClient.getAccessToken] Calling URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.error("[SapoClient.getAccessToken] Failed, status: {}", resp.getStatusCode());
                throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
            }

            SapoAccessTokenResponse tokenResponse =
                    JsonUtils.fromJson(resp.getBody(), SapoAccessTokenResponse.class);

            assert tokenResponse != null;
            if (tokenResponse.getAccessToken() == null) {
                log.error("[SapoClient.getAccessToken] Response does not contain accessToken: {}", resp.getBody());
                throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
            }

            return tokenResponse;
        } catch (Exception e) {
            log.error("[SapoClient.getAccessToken] Failed: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
        }
    }

    public Optional<SapoProductResponse> getProducts(SapoRequest request) {
        log.debug("[SapoClient.getProducts] paginator: {}", request.getPaginator());

        try {
            String baseUrl = "https://" + request.getStoreName() + ".mysapo.net";
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(sapoConfig.getPathProducts())
                    .toUriString();

            if (Boolean.FALSE.equals(sapoConfig.getIsSyncAllProduct())) {
                url = UriComponentsBuilder.fromUriString(url)
                        .queryParam(SapoConstants.CREATE_ON_MIN, request.getFilters().get(SapoConstants.CREATE_ON_MIN))
                        .queryParam(SapoConstants.CREATE_ON_MAX, request.getFilters().get(SapoConstants.CREATE_ON_MAX))
                        .build().toUriString();
            }

            log.debug("[SapoClient.getProducts] Calling URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(SapoConstants.X_SAPO_ACCESS_TOKEN, request.getAccessToken());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            log.debug("[SapoClient.getProducts] Request headers: {}", headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String jsonResp = resp.getBody();
            log.debug("[SapoClient.getProducts] Response: {}", resp);

            if (jsonResp == null || jsonResp.isBlank()) {
                log.warn("[SapoClient.getProducts] Empty response body (status: {})", resp.getStatusCode());
                return Optional.empty();
            }

            SapoProductResponse productsResponse =
                    JsonUtils.fromJson(jsonResp, SapoProductResponse.class);

            log.info("[SapoClient.getProducts] Got products response successfully");

            assert productsResponse != null;
            return Optional.of(productsResponse);

        } catch (Exception e) {
            log.error("[SapoClient.getProducts] Failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<SapoOrderResponse> getOrders(SapoRequest request) {


        try {
            String baseUrl = "https://" + request.getStoreName() + ".mysapo.net";
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/admin/orders.json")
                    .toUriString();

            if (Boolean.FALSE.equals(sapoConfig.getIsSyncAllOrder())) {
                url = UriComponentsBuilder.fromUriString(url)
                        .queryParam(SapoConstants.CREATE_ON_MIN, request.getFilters().get(SapoConstants.CREATE_ON_MIN))
                        .queryParam(SapoConstants.CREATE_ON_MAX, request.getFilters().get(SapoConstants.CREATE_ON_MAX))
                        .build().toUriString();
            }

            log.debug("[SapoClient.getOrders] Calling URL: {}", url);

            String urlWithParams = url;

            log.debug("[SapoClient.getProducts] URL with params: {}", urlWithParams);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Sapo-Access-Token", request.getAccessToken());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            log.debug("[SapoClient.getOrders] Request headers: {}", headers);

            ResponseEntity<String> resp = restTemplate.exchange(urlWithParams, HttpMethod.GET, entity, String.class);
            String jsonResp = resp.getBody();
            log.debug("[SapoClient.getOrders] Response: {}", resp);

            if (ObjectUtils.isEmpty(jsonResp)) {
                log.warn("[SapoClient.getOrders] Empty response body (status: {})", resp.getStatusCode());
                return Optional.empty();
            }

            SapoOrderResponse productsResponse =
                    JsonUtils.fromJson(jsonResp, SapoOrderResponse.class);

            log.info("[SapoClient.getOrders] Got products response successfully");

            return Optional.of(productsResponse);

        } catch (Exception e) {
            log.error("[SapoClient.getOrders] Failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Register webhooks for a POS with automatic webhook URL generation
     *
     * @param storeName   The store name
     * @param accessToken The access token
     * @param posId       The POS ID for webhook URL
     * @return List of registered webhook responses
     */
    public List<SapoWebhookResponse> registerWebhook(String storeName, String accessToken, String posId) {
        // Build webhook URL automatically
        String webhookUrl = sapoConfig.getUrlRegisterWebhook() + posId;

        SapoWebhookRequest webhookRequest = SapoWebhookRequest.builder()
                .storeName(storeName)
                .accessToken(accessToken)
                .address(webhookUrl)
                .format(SapoConstants.JSON)
                .build();

        return registerWebhook(webhookRequest);
    }

    /**
     * Register webhooks for all configured topics
     *
     * @param webhookRequest The webhook registration request
     * @return List of registered webhook responses
     */
    public List<SapoWebhookResponse> registerWebhook(SapoWebhookRequest webhookRequest) {
        log.info("[SapoClient.registerWebhook] Starting webhook registration for store: {}", webhookRequest.getStoreName());

        // Get topics from configuration
        List<String> topics = sapoConfig.getWebhook().getTopic();
        if (ObjectUtils.isEmpty(topics)) {
            log.warn("[SapoClient.registerWebhook] No webhook topics configured");
            return List.of();
        }

        log.info("[SapoClient.registerWebhook] Found {} topics to register: {}", topics.size(), topics);

        List<SapoWebhookResponse> responses = new java.util.ArrayList<>();

        // Register webhook for each topic
        for (String topic : topics) {
            try {
                log.debug("[SapoClient.registerWebhook] Registering webhook for topic: {}", topic);

                SapoWebhookResponse response = registerSingleWebhook(webhookRequest, topic);
                if (ObjectUtils.isNotEmpty(response)) {
                    responses.add(response);
                    log.info("[SapoClient.registerWebhook] Successfully registered webhook for topic: {} with ID: {}",
                            topic, response.getWebhook().getId());
                } else {
                    log.error("[SapoClient.registerWebhook] Failed to register webhook for topic: {}", topic);
                }

            } catch (Exception e) {
                log.error("[SapoClient.registerWebhook] Error registering webhook for topic {}: {}", topic, e.getMessage(), e);
            }
        }

        log.info("[SapoClient.registerWebhook] Completed webhook registration. Successfully registered {}/{} webhooks",
                responses.size(), topics.size());

        return responses;
    }

    /**
     * Register a single webhook for a specific topic
     */
    private SapoWebhookResponse registerSingleWebhook(SapoWebhookRequest webhookRequest, String topic) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(buildBaseUrl(webhookRequest.getStoreName()))
                    .path(sapoConfig.getPathWebhooks())
                    .toUriString();

            log.debug("[SapoClient.registerSingleWebhook] Calling URL: {} for topic: {}", url, topic);

            // Build request body - cách 1: Sử dụng Map.of() cho immutable map
            Map<String, Object> webhookData = Map.of(
                    SapoConstants.TOPIC, topic,
                    SapoConstants.ADDRESS, webhookRequest.getAddress(),
                    SapoConstants.FORMAT, webhookRequest.getFormat() != null ? webhookRequest.getFormat() : SapoConstants.JSON
            );

            Map<String, Object> requestBody = Map.of(SapoConstants.WEBHOOK, webhookData);

            // Set headers
            HttpHeaders headers = buildHeaders(webhookRequest.getAccessToken());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            log.debug("[SapoClient.registerSingleWebhook] Request body: {}", JsonUtils.toJson(requestBody));

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String jsonResp = resp.getBody();
            log.debug("[SapoClient.registerSingleWebhook] Response: {}", resp);

            SapoWebhookResponse response = objectMapper.readValue(jsonResp, SapoWebhookResponse.class);
            if (ObjectUtils.isEmpty(response) || ObjectUtils.isEmpty(response.getWebhook())) {
                log.error("[SapoClient.registerSingleWebhook] Invalid response when registering webhook for topic {}", topic);
                return null;
            }
            return response;
        } catch (Exception e) {
            log.error("[SapoClient.registerSingleWebhook] Failed to register webhook for topic {}: {}", topic, e.getMessage(), e);
            return null;
        }
    }

    // ----------------- Helpers & additional endpoints (refactor per sample) --------------------------x`

    private String buildBaseUrl(String storeName) {
        return "https://" + storeName + ".mysapo.net";
    }

    private HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(SapoConstants.X_SAPO_ACCESS_TOKEN, accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * List all webhooks for store
     */
    public List<SapoWebhookResponse.Webhook> listWebhooks(String storeName, String accessToken) {

        String url = UriComponentsBuilder.fromHttpUrl(buildBaseUrl(storeName))
                .path(sapoConfig.getPathWebhooks())
                .toUriString();

        HttpEntity<String> entity = new HttpEntity<>(buildHeaders(accessToken));

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("[SapoClient.listWebhooks] Failed to list webhooks, status={}", resp.getStatusCode());
                return Collections.emptyList();
            }

            SapoWebhookListResponse webhookListResponse = JsonUtils.fromJson(resp.getBody(), SapoWebhookListResponse.class);

            return Optional.ofNullable(webhookListResponse)
                    .map(SapoWebhookListResponse::getWebhooks)
                    .orElse(List.of());

        } catch (Exception e) {
            log.error("[SapoClient.listWebhooks] Failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Delete webhook by id
     */
    public void deleteWebhook(String storeName, String accessToken, Long webhookId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(buildBaseUrl(storeName))
                    .path(sapoConfig.getPathWebhooks().replace(".json", "/" + webhookId + ".json"))
                    .toUriString();

            HttpEntity<String> entity = new HttpEntity<>(buildHeaders(accessToken));
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.warn("[SapoClient.deleteWebhook] Failed to delete webhook {}, status={}", webhookId, resp.getStatusCode());
            } else {
                log.info("[SapoClient.deleteWebhook] Deleted webhook id={}", webhookId);
            }
        } catch (Exception e) {
            log.error("[SapoClient.deleteWebhook] Failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Delete webhooks not matching the provided posId (signature aligned with registerWebhook)
     */
    public void deleteWebhook(String storeName, String accessToken, String posId) {
        try {
            List<SapoWebhookResponse.Webhook> current = listWebhooks(storeName, accessToken);
            if (ObjectUtils.isEmpty(current)) {
                log.debug("[SapoClient.deleteWebhook(posId)] No webhooks found for posId={}", posId);
                return;
            }

            current.stream()
                    .filter(webhook -> ObjectUtils.isNotEmpty(webhook)
                            && ObjectUtils.isNotEmpty(webhook.getId())
                            && ObjectUtils.isNotEmpty(webhook.getAddress()))
                    .filter(webhook -> !webhook.getAddress().contains(posId))
                    .forEach(webhook -> {
                        log.info("[SapoClient.deleteWebhook(posId)] Deleting webhook id={} not matching posId={}",
                                webhook.getId(), posId);
                        deleteWebhook(storeName, accessToken, webhook.getId());
                    });
        } catch (Exception e) {
            log.warn("[SapoClient.deleteWebhook(posId)] Failed: {}", e.getMessage());
        }
    }
}
