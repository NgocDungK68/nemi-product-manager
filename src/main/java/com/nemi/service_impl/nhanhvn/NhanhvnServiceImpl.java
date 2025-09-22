package com.nemi.service_impl.nhanhvn;

import com.nemi.constant.enums.PosName;
import com.nemi.entity.PosEntity;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.repository.PosRepository;
import com.nemi.service.PosManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhvnServiceImpl implements PosManagementService {
    private final PosRepository posRepository;

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
    public PosConnectionResponse connectPos(String accessCode) {
        return null;
    }
}
