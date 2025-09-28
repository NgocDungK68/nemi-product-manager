package com.nemi.service;

import jakarta.servlet.http.HttpServletRequest;

public interface WebhookService {
    String getPosName();
    boolean processWebhook(HttpServletRequest request);
}