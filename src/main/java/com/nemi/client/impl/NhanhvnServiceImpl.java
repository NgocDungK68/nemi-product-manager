package com.nemi.client.impl;

import com.nemi.constant.enums.PosName;
import com.nemi.constant.enums.PosStatus;
import com.nemi.entity.PosEntity;
import com.nemi.model.config.NhanhvnConfig;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.nhanhvn.NhanhvnAccessTokenRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.repository.PosRepository;
import com.nemi.client.PosManagementService;
import com.nemi.client.AbstractPosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j

public class NhanhvnServiceImpl extends AbstractPosManagementService implements PosManagementService {
    private final NhanhvnConfig nhanhvnConfig;
    private final ClaimUtil claimUtil;
    private final RestTemplate restTemplate;

    public NhanhvnServiceImpl(PosRepository posRepository, NhanhvnConfig nhanhvnConfig, ClaimUtil claimUtil, RestTemplate restTemplate) {
        super(posRepository);
        this.nhanhvnConfig = nhanhvnConfig;
        this.claimUtil = claimUtil;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getPosName() {
        return PosName.NHANHVN.getValue();
    }

    @Override
    public PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest) {
        PosConnectionResponse posConnectionResponse = new PosConnectionResponse();
        try {
            String userId = claimUtil.getUserId();

            Map<String, String> configMap = new HashMap<>();
            configMap.put("secretId", posConnectionRequest.getAppSecret());
            configMap.put("appId", posConnectionRequest.getAppId());
            configMap.put("businessId", posConnectionRequest.getBusinessId());

            String url = nhanhvnConfig.getUrlAccessToken() + nhanhvnConfig.getApiVersion()
                    + "/app/getaccesstoken"
                    + "?appId=" + posConnectionRequest.getAppId()
                    + "&businessId=" + posConnectionRequest.getBusinessId();

            NhanhvnAccessTokenRequest requestBody = NhanhvnAccessTokenRequest.builder()
                    .accessCode(posConnectionRequest.getAccessCode())
                    .secretKey(posConnectionRequest.getAppSecret()).build();
            log.info("token respon: {}",JsonUtils.toJson(requestBody));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<NhanhvnAccessTokenRequest> entity = new HttpEntity<>(requestBody, headers);

            log.info("[NhanhvnAuthService.exchangeAccessToken] Request URL: {}", url);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.error("Nhanhvn exchange token failed, status: {}", resp.getStatusCode());

            }
            NhanhvnAccessTokenResponse tokenResponse =
                    JsonUtils.fromJson(resp.getBody(), NhanhvnAccessTokenResponse.class);

            if (tokenResponse.getData() == null || tokenResponse.getData().getAccessToken() == null) {
                log.error("Nhanhvn response does not contain accessToken: {}", resp.getBody());
            }

            LocalDateTime expiredTime = LocalDateTime.now().plusYears(1);
            //Jsonutil,map ?
            PosEntity posEntityBuilder = PosEntity.builder()
                    .posName(PosName.NHANHVN.name())
                    .userId(userId)
                    .status(PosStatus.ACTIVE.name())
                    .accessToken(tokenResponse.getData().getAccessToken())
                    .config(JsonUtils.toJson(configMap))
                    .expiredTime(expiredTime)
                    .companyId(String.valueOf(claimUtil.getCompanyId()))
                    .build();

            posRepository.save(posEntityBuilder);


        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);
        }
        return posConnectionResponse;
    }

    @Override
    public PosConnectionResponse registerPos(PosConnectionRequest posConnectionRequest) {
        String userId = claimUtil.getUserId();
        Map<String, String> configMap = new HashMap<>();
        configMap.put("secretId", posConnectionRequest.getAppSecret());
        configMap.put("appId", posConnectionRequest.getAppId());
        configMap.put("businessId", posConnectionRequest.getBusinessId());

        PosEntity posEntityBuilder = PosEntity.builder()
                .posName(PosName.NHANHVN.name())
                .userId(userId)
                .status(PosStatus.PENDING.name())
                .config(JsonUtils.toJson(configMap))
                .expiredTime(LocalDateTime.now().plusYears(1))
                .companyId(String.valueOf(claimUtil.getCompanyId()))
                .build();

        posRepository.save(posEntityBuilder);
        return PosConnectionResponse.toPosConnectionResponse(posEntityBuilder);
    }

}

