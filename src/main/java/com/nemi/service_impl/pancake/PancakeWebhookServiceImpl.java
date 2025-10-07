package com.nemi.service_impl.pancake;

import com.nemi.enums.PosName;
import com.nemi.service.WebhookService;

import java.util.Map;

public class PancakeWebhookServiceImpl implements WebhookService {
    @Override
    public String getPosName() {
        return PosName.PANCAKE.getValue();
    }

    @Override
    public boolean processWebhook(String posId, Map<String, String> headers, String body) {
        return false;
    }
}
