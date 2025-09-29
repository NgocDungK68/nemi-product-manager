package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.configuration.NhanhvnConfig;
import com.nemi.constant.enums.NhanhvnEvent;
import com.nemi.constant.enums.PosName;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.model.response.nhanhvn.NhanhvnWebhookResponse;
import com.nemi.repository.ProductRepository;
import com.nemi.repository.ProductVariantRepository;
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
    private final ProductVariantRepository variantRepository;
    private final NhanhvnServiceImpl nhanhvnService;
    private final ObjectMapper objectMapper;

    @Override
    public String getPosName() {
        return PosName.NHANHVN.getValue();
    }

    @Override
    public boolean processWebhook(String posId, HttpServletRequest request) {
        try {
            String verifyToken = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (verifyToken == null || verifyToken.isEmpty() || !verifyToken.equals(nhanhvnConfig.getVerifyToken())) {
                log.error("Invalid verify token: {}", verifyToken);
                return false;
            }

            String body = readBody(request);
            log.info("Body: {}", body);

            NhanhvnWebhookResponse webhookResponse = JsonUtils.fromJson(body, NhanhvnWebhookResponse.class);
            log.info("Webhook response convert from Body: {}", webhookResponse);
            if (webhookResponse == null || webhookResponse.getEvent() == null) {
                log.error("Invalid webhook payload: {}", body);
                return false;
            }

            return handleEvent(posId, webhookResponse);
        } catch (Exception e) {
            log.error("Process webhook failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean handleEvent(String posId, NhanhvnWebhookResponse webhookResponse) {
        NhanhvnEvent event = NhanhvnEvent.fromValue(webhookResponse.getEvent());
        Object data = webhookResponse.getData();

        if (event == null) {
            log.warn("Unhandled webhook event: {}", webhookResponse.getEvent());
            return false;
        }

        switch (event) {
            case WEBHOOKS_ENABLED:
                return handleWebhooksEnabled(data);
            case PRODUCT_ADD, PRODUCT_UPDATE:
                return handleProductAddAndUpdate(posId, data);
            default:
                log.warn("Unhandled event: {}", event);
                return false;
        }
    }

    private boolean handleWebhooksEnabled(Object data) {
        // logic
        return true;
    }

    private boolean handleProductAddAndUpdate(String posId, Object data) {
        NhanhvnProductResponse.ProductData productData = objectMapper.convertValue(
                data, NhanhvnProductResponse.ProductData.class
        );
        log.info("ProductData: {}", productData);

        if (productData == null) {
            log.error("Failed to parse product data: {}", data);
            return false;
        }

        if (productData.getParentId() == -2) {
            ProductEntity productEntity = nhanhvnService.convertToProductEntity(posId, productData);
            productRepository.save(productEntity);
            log.info("Successfully add 1 product");
        } else {
            ProductVariantEntity variantEntity = nhanhvnService.convertToVariantEntity(productData);
            variantRepository.save(variantEntity);
            log.info("Successfully add 1 product variant");
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



