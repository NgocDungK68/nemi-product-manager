package com.nemi.service.factory;

import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.client.PosManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PosManagementFactory {
    private final Set<PosManagementService> posManagementService;

    public PosManagementService getPosName(String posName) {
        return Objects.requireNonNull(posManagementService.stream()
                .filter(service -> service.getPosName().equals(posName))
                .findFirst()
                .orElseThrow(() ->  new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR))));  // update enum exception sau
    }
}
