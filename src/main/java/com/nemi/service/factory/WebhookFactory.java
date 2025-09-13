package com.nemi.service.factory;

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
                .filter(service -> service.supports(webhookType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Webhook type not supported: " + webhookType)));
    }
}
