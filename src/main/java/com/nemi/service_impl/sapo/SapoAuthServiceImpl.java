package com.nemi.service_impl.sapo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.model.config.SapoConfig;
import com.nemi.model.response.sapo.SapoAccessTokenResponse;
import com.nemi.service.sapo.SapoAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SapoAuthServiceImpl implements SapoAuthService {
    private final SapoConfig sapoConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<SapoAccessTokenResponse> getAccessToken(String code) {
        try {
            String url = sapoConfig.getUrl() + "admin/oauth/access_token"
                    + "?client_id=" + sapoConfig.getClientId()
                    + "&client_secret=" + sapoConfig.getClientSecret()
                    + "&code=" + code;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.info("[SapoAuthService.exchangeAccessToken] Request URL: {}", url);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.error("Sapo exchange token failed, status: {}", resp.getStatusCode());
                return Optional.empty();
            }

            SapoAccessTokenResponse tokenResponse =
                    objectMapper.readValue(resp.getBody(), SapoAccessTokenResponse.class);

            if (tokenResponse.getAccessToken() == null) {
                log.error("Sapo response does not contain accessToken: {}", resp.getBody());
                return Optional.empty();
            }

            log.info("Sapo AccessToken received: {}", tokenResponse.getAccessToken());
            return Optional.of(tokenResponse);

        } catch (Exception e) {
            log.error("Sapo exchange token failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}