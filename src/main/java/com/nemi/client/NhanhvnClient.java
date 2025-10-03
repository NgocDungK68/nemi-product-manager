package com.nemi.client;

import com.nemi.configuration.NhanhvnConfig;
import com.nemi.constant.NhanhvnConstants;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.nhanhvn.NhanhvnAccessTokenRequest;
import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.model.response.nhanhvn.NhanhvnOrderResponse;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class NhanhvnClient {
    @Resource(name = "nhanhvnRestTemplate")
    private final RestTemplate restTemplate;
    private final NhanhvnConfig nhanhvnConfig;

    public NhanhvnAccessTokenResponse getAccessToken(PosConnectionRequest posConnectionRequest) {
        String url = nhanhvnConfig.getBaseUrl() + "/"
                + nhanhvnConfig.getApiVersion() + "/"
                + nhanhvnConfig.getUrlAccessToken();

        String urlWithParams = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("appId", posConnectionRequest.getAppId())
                .queryParam("businessId", posConnectionRequest.getBusinessId())
                .toUriString();
        log.debug("[NhanhvnClient.getAccessToken] Request URL with params: {}", url);

        // 2. Request body chỉ chứa accessCode và secretKey
        NhanhvnAccessTokenRequest requestBody = NhanhvnAccessTokenRequest.builder()
                .accessCode(posConnectionRequest.getAccessCode())
                .secretKey(posConnectionRequest.getAppSecret())
                .build();
        log.debug("[NhanhvnClient.getAccessToken] Request body: {}", JsonUtils.toJson(requestBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<NhanhvnAccessTokenRequest> entity = new HttpEntity<>(requestBody, headers);
        log.info("[NhanhvnClient.getAccessToken] Request Entity: {}", headers);

        // 3. Gọi API với URL đã có query parameters
        ResponseEntity<String> resp = restTemplate.exchange(urlWithParams, HttpMethod.POST, entity, String.class);
        log.info("[NhanhvnClient.getAccessToken] response: {}", resp);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            log.error("[NhanhvnClient.getAccessToken] failed, status: {}", resp.getStatusCode());
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
        }
        return JsonUtils.fromJson(resp.getBody(), NhanhvnAccessTokenResponse.class);
    }

    public Optional<NhanhvnProductResponse> getProducts(NhanhvnRequest request) {
        log.debug("[NhanhvnClient.getProducts] paginator: {}", request.getPaginator());

        try {
            String relativeUri = UriComponentsBuilder.fromPath(nhanhvnConfig.getUrlProducts())
                    .queryParam(NhanhvnConstants.APP_ID, request.getAppId())
                    .queryParam(NhanhvnConstants.BUSINESS_ID, request.getBusinessId())
                    .toUriString();

            log.debug("[NhanhvnClient.getProducts] Calling URL: {}", restTemplate.getUriTemplateHandler().expand(relativeUri));

            // build request body
            Map<String, Object> requestBody = new HashMap<>();
            if (request.getPaginator() != null && !request.getPaginator().isEmpty()) {
                requestBody.put(NhanhvnConstants.PAGINATOR, request.getPaginator());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.AUTHORIZATION, request.getAccessToken());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> resp = restTemplate.exchange(relativeUri, HttpMethod.POST, entity, String.class);
            String jsonResp = resp.getBody();
            log.debug("[NhanhvnClient.getProducts] resp {}", resp);

            if (jsonResp == null || jsonResp.isBlank()) {
                log.warn("[NhanhvnClient.getProducts] Empty response body (status: {})", resp.getStatusCode());
                return Optional.empty();
            }

            NhanhvnProductResponse productsResponse =
                    JsonUtils.fromJson(jsonResp, NhanhvnProductResponse.class);

            log.info("[NhanhvnClient.getProducts] Got total {} products",
                    productsResponse.getData() != null ? productsResponse.getData().size() : 0);

            return Optional.of(productsResponse);

        } catch (Exception e) {
            log.error("[NhanhvnClient.getProducts] Failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
    public Optional<NhanhvnOrderResponse> getOrders(NhanhvnRequest request) {
        log.debug("[NhanhvnClient.getOrders] paginator: {}", request.getPaginator());

        try {
            String url = nhanhvnConfig.getBaseUrl() + "/"
                    + nhanhvnConfig.getApiVersion() + "/"
                    + nhanhvnConfig.getUrlOrders()
                    + "?appId=" + request.getAppId()
                    + "&businessId=" + request.getBusinessId();
            log.debug("[NhanhvnClient.getOrders] Calling URL: {}", url);

            // build request body
            Map<String, Object> requestBody = new HashMap<>();
            if (request.getPaginator() != null && !request.getPaginator().isEmpty()) {
                requestBody.put("paginator", request.getPaginator());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", request.getAccessToken());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            log.debug("[NhanhvnClient.getOrders] Calling URL: {}", url);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String jsonResp = resp.getBody();
            log.debug("[NhanhvnClient.getOrders] resp {}", resp);

            if (jsonResp == null || jsonResp.isBlank()) {
                log.warn("[NhanhvnClient.getOrders] Empty response body (status: {})", resp.getStatusCode());
                return Optional.empty();
            }

            NhanhvnOrderResponse orderResponse =
                    JsonUtils.fromJson(jsonResp, NhanhvnOrderResponse.class);

            log.info("[NhanhvnClient.getOrders] Got total {} orders",
                    orderResponse.getData() != null ? orderResponse.getData().size() : 0);

            return Optional.of(orderResponse);

        } catch (Exception e) {
            log.error("[NhanhvnClient.getOrders] Failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<NhanhvnProductResponse> getProductById(NhanhvnRequest request, String id) {
        log.debug("[NhanhvnClient.getProductById] id={}, appId={}, businessId={}", id, request.getAppId(), request.getBusinessId());

        try {
            String url = nhanhvnConfig.getBaseUrl() + "/"
                    + nhanhvnConfig.getApiVersion() + "/"
                    + nhanhvnConfig.getUrlProducts()
                    + "?appId=" + request.getAppId()
                    + "&businessId=" + request.getBusinessId();

            log.debug("[NhanhvnClient.getProductById] Calling URL: {}", url);

            // build request body
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, String> filters = new HashMap<>();
            filters.put("ids", id); // đưa id vào mảng
            requestBody.put("filters", filters);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", request.getAccessToken());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String jsonResp = resp.getBody();
            log.debug("[NhanhvnClient.getProductById] resp={}", resp);

            if (jsonResp == null || jsonResp.isBlank()) {
                log.warn("[NhanhvnClient.getProductById] Empty response body (status: {})", resp.getStatusCode());
                return Optional.empty();
            }

            NhanhvnProductResponse productResponse =
                    JsonUtils.fromJson(jsonResp, NhanhvnProductResponse.class);

            log.info("[NhanhvnClient.getProductById] Got {} product(s) for id={}",
                    productResponse.getData() != null ? productResponse.getData().size() : 0, id);

            return Optional.of(productResponse);

        } catch (Exception e) {
            log.error("[NhanhvnClient.getProductById] Failed for id {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
