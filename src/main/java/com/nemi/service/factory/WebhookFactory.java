package com.nemi.service.factory;

import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class WebhookFactory {
    private final Set<WebhookService> webhookServices;

    public WebhookService getWebhookService(String webhookType) {
        return Objects.requireNonNull(this.webhookServices.stream()
                .filter(service -> service.getPosName().equals(webhookType))
                .findFirst()
                .orElseThrow(() -> new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYSTEM_ERROR))));
    }
}
