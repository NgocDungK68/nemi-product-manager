package com.nemi.service.factory;

import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.service.sync_data.PosSyncDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PosSyncDataFactory {
    private final Set<PosSyncDataService> posSyncDataServices;

    public PosSyncDataService getPosName(String posName) {
        return Objects.requireNonNull(posSyncDataServices.stream()
                .filter(service -> service.getPosName().equals(posName))
                .findFirst()
                .orElseThrow(() ->  new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR))));  // update enum exception sau
    }
}
