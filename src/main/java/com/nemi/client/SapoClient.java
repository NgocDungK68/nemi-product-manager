package com.nemi.client;

import com.nemi.constant.SapoConstants;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.sapo.SapoRequest;
import com.nemi.model.response.sapo.SapoAccessTokenResponse;
import com.nemi.model.response.sapo.SapoProductResponse;
import com.nemi.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class SapoClient {
    private final RestTemplate restTemplate;

    public SapoAccessTokenResponse getAccessToken(PosConnectionRequest posConnectionRequest) {
        try {
            // Build full URL dynamically because each merchant has different storeName
            String baseUrl = "https://" + posConnectionRequest.getStoreName() + ".mysapo.net";
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(SapoConstants.PATH_OAUTH_ACCESS_TOKEN)
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
                    .path(SapoConstants.PATH_PRODUCTS)
                    .toUriString();
            
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

            return Optional.of(productsResponse);

        } catch (Exception e) {
            log.error("[SapoClient.getProducts] Failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
