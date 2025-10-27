package com.nemi.service.factory;

import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.service.ReAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ReAuthPosFactory {
    private final Set<ReAuthService> reAuthServices;

    public ReAuthService getReAuthService(String posName) {
        return Objects.requireNonNull(this.reAuthServices.stream()
                .filter(service -> service.getPosName().equals(posName))
                .findFirst()
                .orElseThrow(() -> new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR))));
    }
}
