package com.nemi.service;

import com.nemi.model.auth.request.AuthPosRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface WebhookService {

    /**
     * Lấy type của webhook service (nhanh, pancake, sapo)
     */
    String getWebhookType();

    /**
     * Xử lý webhook request từ HttpServletRequest
     */
    void processWebhook(HttpServletRequest request);

    /**
     * Kiểm tra xem có hỗ trợ loại webhook này không
     */
    boolean supports(String webhookType);


    void authPos(AuthPosRequest authPosRequest);


}
