package com.nemi.service;

import java.util.Map;

public interface WebhookService {
    String getPosName();
    boolean processWebhook(String posId, Map<String, String> headers, String body);
}