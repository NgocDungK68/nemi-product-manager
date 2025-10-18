package com.nemi.utils.security;

import com.nemi.configuration.PancakeConfig;
import com.nemi.constant.PancakeConstatns;
import com.nemi.entity.PosEntity;
import com.nemi.repository.PosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("pancakeAuth")
@RequiredArgsConstructor
@Slf4j
public class PancakeAuthChecker {

    private final PosRepository posRepository;
    public boolean checkXApiKey(Map<String, String> headers, String posId) {


        String apiKey = headers.get(PancakeConstatns.X_API_KEY);
        if (apiKey == null){
            log.warn("Missing X-API-KEY (posId={})", posId);
            return false;
        }
        log.info("Checking X-API-KEY for posId={}", posId);
        return posRepository.findById(posId)
                .map(PosEntity::getAccessToken)
                .map(apiKey::equals)
                .orElse(false);

    }
}
