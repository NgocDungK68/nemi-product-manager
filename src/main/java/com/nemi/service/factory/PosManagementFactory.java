package com.nemi.service.factory;

import com.nemi.exception.ValidationException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.service.PosManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PosManagementFactory {

    private final Set<PosManagementService> posManagementService;

    public PosManagementService getPosType(String adsType) {
        return Objects.requireNonNull(posManagementService.stream()
                .filter(service -> service.getPosManagementType().equals(adsType))
                .findFirst()
                .orElseThrow(() ->  new RuntimeException("Pos type not supported " + adsType)));  // update enum exception sau
    }

}
