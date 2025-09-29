package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.List;
import java.util.Optional;

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

        return switch (event) {
            case WEBHOOKS_ENABLED -> handleWebhooksEnabled(posId, data);
            case PRODUCT_ADD -> handleProductAdd(posId, data);
            case PRODUCT_UPDATE -> handleProductUpdate(posId, data);
            case PRODUCT_DELETE -> handleProductDelete(posId, data);
            default -> {
                log.warn("Unhandled event: {}", event);
                yield false;
            }
        };
    }

    private boolean handleWebhooksEnabled(String posId, Object data) {
        // logic
        return true;
    }

    private boolean handleProductAdd(String posId, Object data) {
        NhanhvnProductResponse.ProductData productData = objectMapper.convertValue(
                data, NhanhvnProductResponse.ProductData.class
        );
        log.info("Add ProductData: {}", productData);

        if (productData == null) {
            log.error("Failed to add product data: {}", data);
            return false;
        }

        if (productData.getParentId() == -2) {
            ProductEntity productEntity = nhanhvnService.convertToProductEntity(posId, productData);
            productRepository.save(productEntity);
            log.info("Successfully add 1 product with id={}", productEntity.getProductId());
        } else {
            ProductVariantEntity variantEntity = nhanhvnService.convertToVariantEntity(productData);
            variantRepository.save(variantEntity);
            log.info("Successfully add 1 product variant with id={}", variantEntity.getVariantId());
        }

        return true;
    }

    private boolean handleProductUpdate(String posId, Object data) {
        NhanhvnProductResponse.ProductData productData = objectMapper.convertValue(
                data, NhanhvnProductResponse.ProductData.class
        );
        log.info("Update ProductData: {}", productData);

        if (productData == null) {
            log.error("Failed to update product data: {}", data);
            return false;
        }

        if (productData.getParentId() == -2) {
            Optional<ProductVariantEntity> variantEntity = variantRepository.findById(String.valueOf(productData.getId()));
            if (variantEntity.isPresent()) {
                variantRepository.deleteById(String.valueOf(productData.getId()));
                log.info("Deleted variant with id={} because it is now a parent product", variantEntity.get().getVariantId());
            }

            ProductEntity productEntity = nhanhvnService.convertToProductEntity(posId, productData);
            productRepository.save(productEntity);
            log.info("Successfully update 1 product with id={}", productEntity.getProductId());
        } else {
            Optional<ProductEntity> productEntity = productRepository.findById(String.valueOf(productData.getId()));
            if (productEntity.isPresent()) {
                productRepository.deleteById(String.valueOf(productData.getId()));
                log.info("Deleted product with id={} because it is now a variant", productEntity.get().getProductId());
            }

            ProductVariantEntity variantEntity = nhanhvnService.convertToVariantEntity(productData);
            variantRepository.save(variantEntity);
            log.info("Successfully update 1 product variant with id={}", variantEntity.getVariantId());
        }

        return true;
    }

    /**
     * body: {"event":"productDelete","businessId":215487,"data":["15"]}
     */
    private boolean handleProductDelete(String posId, Object data) {
        List<String> ids = objectMapper.convertValue(data, new TypeReference<>() {});
        if (ids.isEmpty()) {
            log.warn("No product ID provided for deletion");
            return false;
        }

        String productId = ids.get(0);

        if (variantRepository.existsById(productId)) {
            variantRepository.deleteById(productId);
            log.info("Deleted variant with id={} from posId={}", productId, posId);
            return true;
        }

        if (productRepository.existsById(productId)) {
            productRepository.deleteById(productId);
            log.info("Deleted product with id={} from posId={}", productId, posId);
            return true;
        }

        log.warn("No product or variant found with id={} for posId={}", productId, posId);
        return false;
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



