package com.nemi.service_impl.nhanhvn;

import com.nemi.client.impl.NhanhvnClientImpl;
import com.nemi.constant.enums.PosName;
import com.nemi.constant.enums.PosStatus;
import com.nemi.entity.PosEntity;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.repository.PosRepository;
import com.nemi.service.AbstractPosManagementService;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j

public class NhanhvnServiceImpl extends AbstractPosManagementService implements PosManagementService {
    private final ClaimUtil claimUtil;
    private final NhanhvnClientImpl nhanhvnClient;

    public NhanhvnServiceImpl(PosRepository posRepository, ClaimUtil claimUtil, NhanhvnClientImpl nhanhvnClient) {
        super(posRepository, claimUtil);
        this.claimUtil = claimUtil;
        this.nhanhvnClient = nhanhvnClient;
    }

    @Override
    public String getPosName() {
        return PosName.NHANHVN.getValue();
    }

    @Override
    public PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest) {
        String userId = claimUtil.getUserId();

        Map<String, String> configMap = new HashMap<>();
        configMap.put("secretId", posConnectionRequest.getAppSecret());
        configMap.put("appId", posConnectionRequest.getAppId());
        configMap.put("businessId", posConnectionRequest.getBusinessId());

        NhanhvnAccessTokenResponse tokenResponse = nhanhvnClient.getAccessToken(posConnectionRequest);

        if (tokenResponse.getData() == null || tokenResponse.getData().getAccessToken() == null) {
            log.error("Nhanhvn response is null, stop persist to db {}", tokenResponse);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
        }

        LocalDateTime expiredTime = LocalDateTime.now().plusYears(1);
        PosEntity posEntityBuilder = PosEntity.builder()
                .posName(PosName.NHANHVN.name())
                .userId(userId)
                .status(PosStatus.ACTIVE.name())
                .accessToken(tokenResponse.getData().getAccessToken())
                .config(JsonUtils.toJson(configMap))
                .expiredTime(expiredTime)
                .companyId(String.valueOf(claimUtil.getCompanyId()))
                .createdBy(claimUtil.getUserName())
                .build();

        posRepository.save(posEntityBuilder);
        PosConnectionResponse posConnectionResponse = PosConnectionResponse.toPosConnectionResponse(posEntityBuilder);
        log.info("Nhanhvn response is {}", posConnectionResponse);

        return posConnectionResponse;
    }

}

