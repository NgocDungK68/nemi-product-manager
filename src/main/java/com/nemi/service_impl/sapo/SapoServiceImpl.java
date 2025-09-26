package com.nemi.service_impl.sapo;

import com.nemi.client.SapoClient;
import com.nemi.constant.enums.PosName;
import com.nemi.constant.enums.PosStatus;
import com.nemi.entity.PosEntity;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.sapo.SapoAccessTokenResponse;
import com.nemi.repository.PosRepository;
import com.nemi.repository.ProductRepository;
import com.nemi.service.AbstractPosManagementService;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SapoServiceImpl extends AbstractPosManagementService implements PosManagementService {

    private final ClaimUtil claimUtil;
    private final SapoClient sapoClient;
    private final ProductRepository productRepository;

    public SapoServiceImpl(PosRepository posRepository, ClaimUtil claimUtil, ClaimUtil claimUtil1, SapoClient sapoClient, ProductRepository productRepository) {
        super(posRepository, claimUtil);
        this.claimUtil = claimUtil1;
        this.sapoClient = sapoClient;
        this.productRepository = productRepository;
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

            SapoAccessTokenResponse tokenResponse = sapoClient.getAccessToken(posConnectionRequest);
            if (tokenResponse.getAccessToken() == null) {
                log.error("Sapo response does not contain accessToken: {}", tokenResponse);
            }

            PosEntity posEntityBuilder = PosEntity.builder()
                    .posName(PosName.SAPO.name())
                    .userId(userId)
                    .accessToken(tokenResponse.getAccessToken())
                    .status(PosStatus.ACTIVE.name())
                    .config(JsonUtils.toJson(configMap))
                    .companyId(String.valueOf(claimUtil.getCompanyId()))
                    .createdBy(claimUtil.getUserName())
                    .build();

            posRepository.save(posEntityBuilder);
        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);
        }
        return posConnectionResponse;
    }

    @Override
    public boolean syncData(String posId) {
        // sync product from Sapo

        // sync order from Sapo
        return false;
    }
}
