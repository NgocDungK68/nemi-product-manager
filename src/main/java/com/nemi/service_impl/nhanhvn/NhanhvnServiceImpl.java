package com.nemi.service_impl.nhanhvn;

import com.nemi.constant.enums.PosName;
import com.nemi.constant.enums.PosStatus;
import com.nemi.entity.PosEntity;
import com.nemi.entity.TransactionTempEntity;
import com.nemi.model.config.NhanhvnConfig;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.repository.PosRepository;
import com.nemi.repository.TransactionTempRepository;
import com.nemi.service.PosManagementService;
import com.nemi.service_impl.AbstractPosManagementService;
import com.nemi.util.ClaimUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhvnServiceImpl extends AbstractPosManagementService implements PosManagementService {
    private final ClaimUtil claimUtil;
    @Override
    public String getPosName() {
        return PosName.NHANHVN.name();
    }
    @Override
    public PosConnectionResponse registerPos(PosConnectionRequest posConnectionRequest) {
        return null;
    }

    @Override
    public PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest) {
        PosConnectionResponse posConnectionResponse = new PosConnectionResponse();

        try {
             String userId = claimUtil.getUserId();


            String config = String.format(
                    "{\"secretId\":\"%s\", \"appId\":\"%s\", \"businessId\":\"%s\"}",
                    posConnectionRequest.getAppSecret(), posConnectionRequest.getAppId(), posConnectionRequest.getBusinessId()
            );

            PosEntity posEntityBuilder = PosEntity.builder()
                    .posName(PosName.NHANHVN.name())
                    .userId(userId)
                    .status(PosStatus.PENDING.name())
                    .config(config)
                    .expiredTime(LocalDateTime.now().plusYears(1))
                    .build();

            posRepository.save(posEntityBuilder);

        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);
        }
        return posConnectionResponse;
    }

}

