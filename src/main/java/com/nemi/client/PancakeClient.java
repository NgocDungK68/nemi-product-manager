package com.nemi.client;

import com.nemi.configuration.PancakeConfig;
import com.nemi.model.request.pancake.PancakeRequest;
import com.nemi.model.response.pancake.PancakeProductResponse;
import com.nemi.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PancakeClient {

    private final RestTemplate restTemplate;
    private final PancakeConfig pancakeConfig;


    public Optional<PancakeProductResponse> getProducts(PancakeRequest request) {

        try {
            String url = pancakeConfig.getBaseUrl() + "/"
                    + "shops" + "/"
                    + request.getShopId() + "/"
                    + "products/variations";
            log.debug("[Pancake.getProducts] Calling URL: {}", url);

            String urlWithParams = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("api_key", request.getApiKey())
                    .queryParam("page_size", request.getPageSize())
                    .queryParam("page_number", request.getPageNumber())
                    .toUriString();
            log.debug("[Pancake.getProducts] Calling URL: {}", urlWithParams);

            // build request body

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(headers);
            log.debug("[NhanhvnClient.getProducts] Calling URL: {}", url);
            ResponseEntity<String> resp = restTemplate.exchange(urlWithParams, HttpMethod.GET, entity, String.class);
            String jsonResp = resp.getBody();
            log.debug("[PancakeClient.getProducts] resp {}", resp);

            if (jsonResp == null || jsonResp.isBlank()) {
                log.warn("[PancakeClient.getProducts] Empty response body (status: {})", resp.getStatusCode());
                return Optional.empty();
            }

            PancakeProductResponse productsResponse =
                    JsonUtils.fromJson(jsonResp, PancakeProductResponse.class);

            log.info("[PancakeClient.getProducts] Got {} products",
                    productsResponse.getData() != null ? productsResponse.getData().size() : 0);

            return Optional.of(productsResponse);

        } catch (Exception e) {
            log.error("[PancakeClient.getProducts] Failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
