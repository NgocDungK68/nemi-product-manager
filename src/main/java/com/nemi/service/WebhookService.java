package com.nemi.service;

import jakarta.servlet.http.HttpServletRequest;

public interface WebhookService {

    /**
     * Lấy type của webhook service (nhanh, pancake, sapo)
     */
    String getPosName();

    /**
     * Xử lý webhook request từ HttpServletRequest
     */
    void processWebhook(HttpServletRequest request);
}
