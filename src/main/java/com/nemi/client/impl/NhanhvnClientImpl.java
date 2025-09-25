package com.nemi.client.impl;

import com.nemi.client.PosClient;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.nhanhvn.NhanhvnAccessTokenRequest;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
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

@RequiredArgsConstructor
@Slf4j
@Service
public class NhanhvnClientImpl implements PosClient {
    private final RestTemplate restTemplate;

    @Override
    public NhanhvnAccessTokenResponse getAccessToken(PosConnectionRequest posConnectionRequest) {
        String url = "https://pos.open.nhanh.vn/v3.0/app/getaccesstoken";

        String urlWithParams = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("appId", posConnectionRequest.getAppId())
                .queryParam("businessId", posConnectionRequest.getBusinessId())
                .toUriString();

        // 2. Request body chỉ chứa accessCode và secretKey
        NhanhvnAccessTokenRequest requestBody = NhanhvnAccessTokenRequest.builder()
                .accessCode(posConnectionRequest.getAccessCode())
                .secretKey(posConnectionRequest.getAppSecret())
                .build();

        log.info("Request body: {}", JsonUtils.toJson(requestBody));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<NhanhvnAccessTokenRequest> entity = new HttpEntity<>(requestBody, headers);

        log.info("[NhanhvnAuthService.exchangeAccessToken] Request URL with params: {}", urlWithParams);

        // 3. Gọi API với URL đã có query parameters
        ResponseEntity<String> resp = restTemplate.exchange(urlWithParams, HttpMethod.POST, entity, String.class);
        log.info("[NhanhvnAuthService.exchangeAccessToken] Request URL: {}", url);


        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            log.error("Nhanhvn exchange token failed, status: {}", resp.getStatusCode());
        }
        return JsonUtils.fromJson(resp.getBody(), NhanhvnAccessTokenResponse.class);
    }

}

