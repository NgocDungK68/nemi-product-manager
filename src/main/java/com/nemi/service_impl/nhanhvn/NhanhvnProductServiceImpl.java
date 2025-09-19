package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.model.config.NhanhvnConfig;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.service.nhanhvn.NhanhvnProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NhanhvnProductServiceImpl implements NhanhvnProductService {
    private final RestTemplate restTemplate;
    private final NhanhvnConfig nhanhvnConfig;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<NhanhvnProductResponse> getProducts(Map<String, Object> paginator) {
        log.info("[NhanhvnServiceImpl.getProducts] paginator: {}", paginator);

        try {
            // step1: setup request URL
            String url = nhanhvnConfig.getUrl() + nhanhvnConfig.getApiVersion()
                    + "/product/list"
                    + "?appId=" + nhanhvnConfig.getAppId()
                    + "&businessId=" + nhanhvnConfig.getBusinessId();

            // step2: build request body
            Map<String, Object> requestBody = new HashMap<>();
            if (paginator != null && !paginator.isEmpty()) {
                requestBody.put("paginator", paginator);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", nhanhvnConfig.getAccessToken());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("[NhanhvnServiceImpl.getProducts] Calling URL: {}", url);

            // step3: call API
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String jsonResp = resp.getBody();

            if (jsonResp == null || jsonResp.isBlank()) {
                log.warn("[NhanhvnServiceImpl.getProducts] Empty response body from Nhanhvn API (status: {})",
                        resp.getStatusCode());
                return Optional.empty();
            }

            log.info("[NhanhvnServiceImpl.getProducts] Raw response: {}", jsonResp);

            // step4: parse response
            NhanhvnProductResponse productsResponse =
                    objectMapper.readValue(jsonResp, NhanhvnProductResponse.class);

            log.info("[NhanhvnServiceImpl.getProducts] Parsed response: {} products",
                    productsResponse.getData() != null ? productsResponse.getData().size() : 0);

            return Optional.of(productsResponse);

        } catch (Exception e) {
            log.error("[NhanhvnServiceImpl.getProducts] Get products failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}