package com.nemi.service_impl.nhanhvn;

import com.nemi.constant.enums.NhanhvnEvent;
import com.nemi.constant.enums.PosName;
import com.nemi.model.config.NhanhvnConfig;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.model.response.nhanhvn.NhanhvnWebhookResponse;
import com.nemi.repository.ProductRepository;
import com.nemi.service.WebhookService;
import com.nemi.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhvnWebhookServiceImpl implements WebhookService {
    private final NhanhvnConfig nhanhvnConfig;
    private final ProductRepository productRepository;

    @Override
    public String getPosName() {
        return PosName.NHANHVN.getValue();
    }

    @Override
    public boolean processWebhook(HttpServletRequest request) {
        try {
            String verifyToken = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (verifyToken == null || verifyToken.isEmpty() || !verifyToken.equals(nhanhvnConfig.getVerifyToken())) {
                log.error("Invalid verify token: {}", verifyToken);
                return false;
            }

            String body = readBody(request);
            log.info("Body: {}", body);

            NhanhvnWebhookResponse webhookResponse = JsonUtils.fromJson(body, NhanhvnWebhookResponse.class);
            if (webhookResponse == null || webhookResponse.getEvent() == null) {
                log.error("Invalid webhook payload: {}", body);
                return false;
            }

            return handleEvent(webhookResponse);
        } catch (Exception e) {
            log.error("Process webhook failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean handleEvent(NhanhvnWebhookResponse webhookResponse) {
        NhanhvnEvent event = NhanhvnEvent.fromValue(webhookResponse.getEvent());
        String data = webhookResponse.getData();

        if (event == null) {
            log.warn("Unhandled webhook event: {}", webhookResponse.getEvent());
            return false;
        }

        switch (event) {
            case PRODUCT_ADD:
                return handleProductAdd(data);
            case PRODUCT_UPDATE:
                return handleProductUpdate(data);

            default:
                log.warn("Unhandled event: {}", event);
                return false;
        }
    }

    private boolean handleProductAdd(String data) {
        NhanhvnProductResponse.ProductData productData = JsonUtils.fromJson(data, NhanhvnProductResponse.ProductData.class);
        if (productData == null) {
            log.error("Failed to parse product data: {}", data);
            return false;
        }

        // logic ...
        return true;
    }

    private boolean handleProductUpdate(String data) {
        NhanhvnProductResponse.ProductData productData = JsonUtils.fromJson(data, NhanhvnProductResponse.ProductData.class);
        if (productData == null) {
            log.error("Failed to parse product data: {}", data);
            return false;
        }

        return true;
    }

    private String readBody(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            log.error("Error reading request body", e);
        }
        return sb.toString();
    }
}



