package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.model.config.NhanhvnConfig;
import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.service.nhanhvn.NhanhvnProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
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
    public Optional<NhanhvnProductResponse> getProducts(NhanhvnRequest request) {
        log.info("[NhanhvnServiceImpl.getProducts] paginator: {}", request.getPaginator());

        try {
            String url = nhanhvnConfig.getBaseUrl() + "/"
                    + nhanhvnConfig.getApiVersion()
                    + "/product/list"
                    + "?appId=" + request.getAppId()
                    + "&businessId=" + request.getBusinessId();

            // build request body
            Map<String, Object> requestBody = new HashMap<>();
            if (request.getPaginator() != null && !request.getPaginator().isEmpty()) {
                requestBody.put("paginator", request.getPaginator());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", request.getAccessToken());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("[NhanhvnServiceImpl.getProducts] Calling URL: {}", url);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String jsonResp = resp.getBody();

            if (jsonResp == null || jsonResp.isBlank()) {
                log.warn("[NhanhvnServiceImpl.getProducts] Empty response body (status: {})", resp.getStatusCode());
                return Optional.empty();
            }

            NhanhvnProductResponse productsResponse =
                    objectMapper.readValue(jsonResp, NhanhvnProductResponse.class);

            log.info("[NhanhvnServiceImpl.getProducts] Got {} products",
                    productsResponse.getData() != null ? productsResponse.getData().size() : 0);

            return Optional.of(productsResponse);

        } catch (Exception e) {
            log.error("[NhanhvnServiceImpl.getProducts] Failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}