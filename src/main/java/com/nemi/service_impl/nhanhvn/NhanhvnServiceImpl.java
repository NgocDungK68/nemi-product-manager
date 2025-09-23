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
public class NhanhvnServiceImpl implements PosManagementService {
    private final PosRepository posRepository;
    private final NhanhvnConfig nhanhvnConfig;
    private final TransactionTempRepository transactionTempRepository;
    private final ClaimUtil claimUtil;
    @Override
    public String getPosName() {
        return PosName.NHANHVN.name();
    }

    /**
     * API: GET /client-api/v1/pos/{transaction-id}/auth/status
     */
    @Override
    public String getPosStatus(String transactionId) {
        Optional<PosEntity> entity = posRepository.findById(transactionId);
        return entity.map(PosEntity::getStatus).orElse("NOT_FOUND");
    }

    /**
     * API: PATCH /client-api/v1/pos/{pos-id}
     */
    @Override
    public String setPosStatus(String id, String status) {
        return null;
    }

    @Override
    public List<PosConnectionResponse> listPosConnection(String userId) {
        return null;
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
            PosEntity posEntity = posRepository.findByConfigContaining(config).orElseThrow(() -> new RuntimeException("error while finding entity"));


            TransactionTempEntity transactionTempEntity = TransactionTempEntity.builder()
                    .id(posEntity.getId())
                    .status(PosStatus.PENDING.name())
                    .appId(posConnectionRequest.getAppId())
                    .createdBy(userId)
                    .build();
            transactionTempRepository.save(transactionTempEntity);

             posConnectionResponse = PosConnectionResponse.builder()
                    .posName(PosName.NHANHVN.name())
                    .status(PosStatus.PENDING.name())
                    .config(config)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();


        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);
        }
        return posConnectionResponse;
    }

}

