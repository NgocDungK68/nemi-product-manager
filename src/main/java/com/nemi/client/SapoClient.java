package com.nemi.client;

import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.model.response.sapo.SapoAccessTokenResponse;
import com.nemi.util.ClaimUtil;
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

    public Optional<NhanhvnProductResponse> getProducts(NhanhvnRequest request) {
        return Optional.empty();
    }
}
