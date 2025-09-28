package com.nemi.client;

import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.configuration.NhanhvnConfig;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.nhanhvn.NhanhvnAccessTokenRequest;
import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
            String url = nhanhvnConfig.getBaseUrl() + "/"
                    + nhanhvnConfig.getApiVersion() + "/"
                    + nhanhvnConfig.getUrlProducts()
                    + "?appId=" + request.getAppId()
                    + "&businessId=" + request.getBusinessId();
            log.debug("[NhanhvnClient.getProducts] Calling URL: {}", url);

            // build request body
            Map<String, Object> requestBody = new HashMap<>();
            if (request.getPaginator() != null && !request.getPaginator().isEmpty()) {
                requestBody.put("paginator", request.getPaginator());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", request.getAccessToken());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            log.debug("[NhanhvnClient.getProducts] Calling URL: {}", url);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
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
}

