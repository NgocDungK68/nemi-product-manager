package com.nemi.client;

import com.nemi.configuration.PancakeConfig;
import com.nemi.constant.PancakeConstatns;
import com.nemi.model.request.pancake.PancakeRequest;
import com.nemi.model.response.pancake.PancakeOrderResponse;
import com.nemi.model.response.pancake.PancakeProductResponse;
import com.nemi.util.JsonUtils;
import jakarta.annotation.Resource;
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
    @Resource(name = "pancakeRestTemplate")
    private final RestTemplate restTemplate;
    private final PancakeConfig pancakeConfig;


    public Optional<PancakeProductResponse> getProducts(PancakeRequest request) {

        try {


            String relativeUri = UriComponentsBuilder.fromPath(request.getShopId() + "/products/variations")
                    .queryParam(PancakeConstatns.API_KEY, request.getApiKey())
                    .queryParam(PancakeConstatns.PAGE_SIZE, request.getPageSize())
                    .queryParam(PancakeConstatns.PAGE_NUMBER, request.getPageNumber())
                    .toUriString();

            log.debug("[Pancake.getProducts] Calling relative URI: {}", relativeUri);
            // build request body

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(headers);
            log.debug("[NhanhvnClient.getProducts] Calling URL: {}", relativeUri);
            ResponseEntity<String> resp = restTemplate.exchange(relativeUri, HttpMethod.GET, entity, String.class);
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

    public Optional<PancakeOrderResponse> getOrders(PancakeRequest request) {
        log.debug("[Pancake.getOrders] with pagesize {} and page number", request.getPageSize(),request.getPageNumber());

        try {

            String relativeUri = UriComponentsBuilder.fromPath(request.getShopId() + "/orders")
                    .queryParam(PancakeConstatns.API_KEY, request.getApiKey())
                    .queryParam(PancakeConstatns.PAGE_SIZE, request.getPageSize())
                    .queryParam(PancakeConstatns.PAGE_NUMBER, request.getPageNumber())
                    .toUriString();

            log.debug("[Pancake.getProducts] Calling relative URI: {}", relativeUri);

            // build request body
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(headers);
            log.debug("[PancakeClient.getProducts] Calling URL: {}", relativeUri);
            ResponseEntity<String> resp = restTemplate.exchange(relativeUri, HttpMethod.GET, entity, String.class);
            String jsonResp = resp.getBody();
            log.debug("[PancakeClient.getProducts] resp {}", resp);

            if (jsonResp == null || jsonResp.isBlank()) {
                log.warn("[PancakeClient.getProducts] Empty response body (status: {})", resp.getStatusCode());
                return Optional.empty();
            }

            PancakeOrderResponse productsResponse =
                    JsonUtils.fromJson(jsonResp, PancakeOrderResponse.class);

            log.info("[PancakeClient.getProducts] Got {} products",
                    productsResponse.getData() != null ? productsResponse.getData().size() : 0);

            return Optional.of(productsResponse);

        } catch (Exception e) {
            log.error("[Pancakeclient.getOrders] Failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
