package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.model.config.NhanhvnConfig;
import com.nemi.model.request.nhanhvn.NhanhvnAccessTokenRequest;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.service.nhanhvn.NhanhvnAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhvnAuthServiceImpl implements NhanhvnAuthService {
    private final NhanhvnConfig nhanhvnConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<NhanhvnAccessTokenResponse> exchangeAccessToken(String accessCode) {
        try {
            String url = nhanhvnConfig.getUrlAccessToken() + nhanhvnConfig.getApiVersion()
                    + "/app/getaccesstoken"
                    + "?appId=" + nhanhvnConfig.getAppId()
                    + "&businessId=" + nhanhvnConfig.getBusinessId();

            NhanhvnAccessTokenRequest requestBody =
                    new NhanhvnAccessTokenRequest(accessCode, nhanhvnConfig.getSecretKey());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<NhanhvnAccessTokenRequest> entity = new HttpEntity<>(requestBody, headers);

            log.info("[NhanhvnAuthService.exchangeAccessToken] Request URL: {}", url);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.error("Nhanhvn exchange token failed, status: {}", resp.getStatusCode());
                return Optional.empty();
            }

            NhanhvnAccessTokenResponse tokenResponse =
                    objectMapper.readValue(resp.getBody(), NhanhvnAccessTokenResponse.class);

            if (tokenResponse.getData() == null || tokenResponse.getData().getAccessToken() == null) {
                log.error("Nhanhvn response does not contain accessToken: {}", resp.getBody());
                return Optional.empty();
            }

            log.info("Nhanhvn AccessToken received: {}", tokenResponse.getData().getAccessToken());
            return Optional.of(tokenResponse);

        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}