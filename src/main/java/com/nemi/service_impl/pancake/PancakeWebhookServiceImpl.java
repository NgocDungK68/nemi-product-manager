package com.nemi.service_impl.pancake;

import com.nemi.enums.PosName;
import com.nemi.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;

public class PancakeWebhookServiceImpl implements WebhookService {
    @Override
    public String getPosName() {
        return PosName.PANCAKE.getValue();
    }

    @Override
    public boolean processWebhook(String posId, HttpServletRequest request) {
        return false;
    }
}
