package com.nemi.service_impl.sapo;

import com.nemi.constant.enums.PosName;
import com.nemi.model.auth.request.AuthPosRequest;
import com.nemi.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

public class SapoWebhookServiceImpl implements WebhookService {
    @Override
    public String getWebhookType() {
        return PosName.SAPO.getValue().toLowerCase();
    }

    @Override
    public void processWebhook(HttpServletRequest request) {

    }

    @Override
    public boolean supports(String webhookType) {
        return false;
    }

    @Override
    public void authPos(AuthPosRequest authPosRequest) {

    }
}
