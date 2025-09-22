package com.nemi.service.factory;

import com.nemi.service.PosManagementService;
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
                .orElseThrow(() ->  new RuntimeException("Pos type not supported " + posName)));  // update enum exception sau
    }
}
