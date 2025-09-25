package com.nemi.client.impl;

import com.nemi.client.AbstractPosManagementService;
import com.nemi.constant.enums.PosName;
import com.nemi.constant.enums.PosStatus;
import com.nemi.entity.PosEntity;
import com.nemi.model.config.SapoConfig;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.nhanhvn.NhanhvnAccessTokenRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.client.PosManagementService;
import com.nemi.model.response.StatusResponse;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.model.response.sapo.SapoAccessTokenResponse;
import com.nemi.repository.PosRepository;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SapoServiceImpl extends AbstractPosManagementService implements PosManagementService {
    private SapoConfig sapoConfig;
    private ClaimUtil claimUtil;
    private RestTemplate restTemplate;

    public SapoServiceImpl(PosRepository posRepository, ClaimUtil claimUtil, SapoConfig sapoConfig, RestTemplate restTemplate) {
        super(posRepository);
        this.claimUtil = claimUtil;
        this.sapoConfig = sapoConfig;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getPosName() {
        return PosName.SAPO.getValue();
    }

    @Override
    public PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest) {
        PosConnectionResponse posConnectionResponse = new PosConnectionResponse();
        try {
            String userId = claimUtil.getUserId();

            Map<String, String> configMap = new HashMap<>();
            configMap.put("clientId", posConnectionRequest.getClientId());
            configMap.put("clientSecret", posConnectionRequest.getClientSecret());
            configMap.put("storeName", posConnectionRequest.getStoreName());

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

            PosEntity posEntityBuilder = PosEntity.builder()
                    .posName(PosName.SAPO.name())
                    .userId(userId)
                    .status(PosStatus.ACTIVE.name())
                    .config(JsonUtils.toJson(configMap))
                    .companyId(String.valueOf(claimUtil.getCompanyId()))
                    .build();

            posRepository.save(posEntityBuilder);
        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);
        }
        return posConnectionResponse;
    }
}
