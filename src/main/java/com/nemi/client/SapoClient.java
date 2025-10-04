package com.nemi.client;

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

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class SapoClient {
    private final RestTemplate restTemplate;

    public SapoAccessTokenResponse getAccessToken(PosConnectionRequest posConnectionRequest) {
        try {

            String url = "https://" + posConnectionRequest.getStoreName() + ".mysapo.net/admin/oauth/access_token"
                    + "?client_id=" + posConnectionRequest.getClientId()
                    + "&client_secret=" + posConnectionRequest.getClientSecret()
                    + "&code=" + posConnectionRequest.getCode();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            log.info("Sapo Request URL: {}", url);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.error("Sapo exchange token failed, status: {}", resp.getStatusCode());
            }
            SapoAccessTokenResponse tokenResponse =
                    JsonUtils.fromJson(resp.getBody(), SapoAccessTokenResponse.class);

            if (tokenResponse.getAccessToken() == null) {
                log.error("Sapo response does not contain accessToken: {}", resp.getBody());
            }
            return tokenResponse;
        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
        }
    }

    public Optional<SapoProductResponse> getProducts(SapoRequest request) {
        log.debug("[SapoClient.getProducts] paginator: {}", request.getPaginator());

        try {
            String url = "https://" + request.getStoreName() + ".mysapo.net/admin/products.json";
            log.debug("[SapoClient.getProducts] Calling URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Sapo-Access-Token", request.getAccessToken());

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
