package com.nemi.controller.nhanhvn;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.constant.enums.PosStatus;
import com.nemi.entity.PosEntity;
import com.nemi.entity.TransactionTempEntity;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.repository.PosRepository;
import com.nemi.repository.TransactionTempRepository;
import com.nemi.service_impl.nhanhvn.NhanhvnWebhookServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/public-api/nhanhvn/auth")
@RequiredArgsConstructor
@Slf4j
public class NhanhRedirectController {
    private final NhanhvnWebhookServiceImpl nhanhvnWebhookService;
    private final TransactionTempRepository transactionTempRepository;
    private final PosRepository posRepository;
    private final ObjectMapper objectMapper;

    @GetMapping

    public ResponseEntity<?> getAccessToken(@RequestParam String accessCode) throws JsonProcessingException {
        Optional<TransactionTempEntity> transactionTempEntityOpt = transactionTempRepository.findTopByOrderByUpdatedAtDesc();
        if (transactionTempEntityOpt.isEmpty()) {
            throw new RuntimeException("Transcation is not exist");
        }
        TransactionTempEntity transactionTempEntity = transactionTempEntityOpt.get();
        String appId = transactionTempEntity.getAppId();
        Optional<PosEntity> posOPt = posRepository.findByAppId(appId);

        if (posOPt.isEmpty()) {
            throw new RuntimeException("pos connection is not exist");
        }
        PosEntity pos = posOPt.get();

        String configJson = pos.getConfig();
        Map<String, Object> configMap = objectMapper.readValue(configJson, new TypeReference<HashMap<String, Object>>() {
        });

        String secretKey = (String) configMap.get("secret-key");
        String businessId = (String) configMap.get("business-id");


        Optional<NhanhvnAccessTokenResponse> nhanhvnAccessTokenResponseOpt = nhanhvnWebhookService.exchangeAccessToken(accessCode, appId, businessId, secretKey);
        NhanhvnAccessTokenResponse nhanhvnAccessTokenResponse = nhanhvnAccessTokenResponseOpt.get();
        if (nhanhvnAccessTokenResponse == null && nhanhvnAccessTokenResponse.getCode() == 0) {
            throw new RuntimeException("can not get access token");
        }

        pos.setAccessToken(nhanhvnAccessTokenResponse.getData().getAccessToken());
        pos.setStatus(PosStatus.ACTIVE.name());
        posRepository.save(pos);

        return ResponseEntity.ok("accessToken updated");
    }
}
