package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.configuration.NhanhvnConfig;
import com.nemi.constant.NhanhvnConstants;
import com.nemi.entity.PosEntity;
import com.nemi.enums.PosName;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.repository.PosRepository;
import com.nemi.service.ReAuthService;
import io.jsonwebtoken.lang.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhvnReAuthServiceImpl implements ReAuthService {
    private final PosRepository posRepository;

    @Override
    public String getPosName() {
        return PosName.NHANHVN.getValue();
    }


    public String getReAuthLink(String posId) {
        PosEntity pos = posRepository.findById(posId).orElseThrow(() ->
                new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_NOTFOUND, posId))
        );
        return pos.getUrlConnect();
    }

}
