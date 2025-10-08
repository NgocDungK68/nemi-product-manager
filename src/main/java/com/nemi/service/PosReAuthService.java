package com.nemi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.configuration.NhanhvnConfig;
import com.nemi.constant.NhanhvnConstants;
import com.nemi.entity.PosEntity;
import com.nemi.enums.PosStatus;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.repository.PosRepository;
import io.jsonwebtoken.lang.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosReAuthService {
    private final NhanhvnConfig nhanhvnConfig;
    private final ObjectMapper objectMapper;
    private final PosRepository posRepository;

    /**
     * Kiểm tra accessToken của POS đã hết hạn chưa
     */
    public boolean isAccessTokenExpired(PosEntity posEntity) {
        if (Objects.isEmpty(posEntity.getExpiredTime())) {
            return false;
        }
        return posEntity.getExpiredTime().isBefore(LocalDateTime.now());
    }

    public String buildReAuthLink(PosEntity posEntity) {
        try {
            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(),
                    new TypeReference<>() {
                    }
            );

            String appId = configMap.get(NhanhvnConstants.APP_ID);
            String businessId = configMap.get(NhanhvnConstants.BUSINESS_ID);
            if (ObjectUtils.isEmpty(appId) || ObjectUtils.isEmpty(businessId)) {
                log.warn("AppId or BusinessId is empty, AppId: {}, BusinessId: {}", appId, businessId);
                return null;
            }

            return UriComponentsBuilder.fromHttpUrl(nhanhvnConfig.getBaseUrl())
                    .pathSegment(nhanhvnConfig.getUrlOauth())
                    .queryParam(NhanhvnConstants.VERSION, nhanhvnConfig.getApiVersion())
                    .queryParam(NhanhvnConstants.APP_ID, appId)
                    .queryParam(NhanhvnConstants.BUSINESS_ID, businessId)
                    .queryParam(NhanhvnConstants.RETURN_LINK, nhanhvnConfig.getReturnLink())
                    .toUriString();
        } catch (Exception e) {
            log.error("Build Nhanh.vn reAuth link failed: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.JSON_PARSE_ERROR));
        }
    }
}
