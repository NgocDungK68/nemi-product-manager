package com.nemi.service_impl.sapo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.constant.enums.PosName;
import com.nemi.entity.ProductEntity;
import com.nemi.repository.ProductRepository;
import com.nemi.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SapoWebhookImpl implements WebhookService {
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    @Override
    public String getPosName() {
        return PosName.SAPO.getValue();
    }

    @Override
    public boolean processWebhook(String posId, HttpServletRequest request) {
        try {
            // 1. Read headers
            Map<String, String> headers = extractHeaders(request);
            log.info("=== Sapo Webhook received for posId: {} ===", posId);
            log.info("Headers:");
            headers.forEach((k, v) -> log.info("  {} = {}", k, v));

            // 2. Read raw body
            String body = readBody(request);
            log.info("Payload body: {}", body);

            // 3. Parse JSON payload
            if (body != null && !body.trim().isEmpty()) {
                JsonNode payload = objectMapper.readTree(body);
                
                // 4. Process webhook data based on event type


                String topic = request.getHeader("x-sapo-topic");

                switch (topic.toLowerCase()) {
                    case "product/create":
                    case "product/update":
                        return processProductWebhook(posId, payload);
                    case "product/delete":
                        return processProductDeleteWebhook(posId, payload);
                    default:
                        log.warn("Unhandled webhook event type: {}", topic);
                        return true; // Return true for unhandled events to acknowledge receipt
                }
            } else {
                log.warn("Empty webhook body received");
                return false;
            }
            
        } catch (Exception e) {
            log.error("Process webhook failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String value = request.getHeader(name);
                map.put(name, value);
            }
        }
        return map;
    }

    private String readBody(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            log.error("Error reading body", e);
        }
        return sb.toString();
    }

    /**
     * Process product create/update webhook
     */
    private boolean processProductWebhook(String posId, JsonNode payload) {
        try {
            JsonNode productData = payload.path("data");
            if (productData.isMissingNode()) {
                log.error("No product data found in webhook payload");
                return false;
            }

            ProductEntity product = new ProductEntity();
            
            // Map Sapo product data to ProductEntity
            product.setProductId(String.valueOf(productData.path("id").asLong()));
            product.setCode(productData.path("sku").asText());
            product.setName(productData.path("title").asText());
            product.setDescription(productData.path("body_html").asText());
            product.setStatus(productData.path("status").asText());
            product.setPosId(posId);
            
            // Handle category
            JsonNode category = productData.path("product_type");
            if (!category.isMissingNode()) {
                product.setCategory(category.asText());
            }
            
            // Handle brand
            JsonNode vendor = productData.path("vendor");
            if (!vendor.isMissingNode()) {
                product.setBrand(vendor.asText());
            }
            
            // Handle images
            JsonNode images = productData.path("images");
            if (!images.isMissingNode() && images.isArray() && images.size() > 0) {
                StringBuilder imageUrls = new StringBuilder();
                for (JsonNode image : images) {
                    if (image.path("src").isTextual()) {
                        if (imageUrls.length() > 0) {
                            imageUrls.append(",");
                        }
                        imageUrls.append(image.path("src").asText());
                    }
                }
                product.setImages(imageUrls.toString());
            }
            
            // Set timestamps
            product.setCreatedDatetime(LocalDateTime.now());
            product.setUpdatedDatetime(LocalDateTime.now());

            // Save to database
            productRepository.save(product);
            log.info("Successfully processed product webhook for product: {}", product.getProductId());
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to process product webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process product delete webhook
     */
    private boolean processProductDeleteWebhook(String posId, JsonNode payload) {
        try {
            JsonNode productData = payload.path("data");
            if (productData.isMissingNode()) {
                log.error("No product data found in delete webhook payload");
                return false;
            }

            Long productId = productData.path("id").asLong();
            
            // Delete from database
            productRepository.deleteById(String.valueOf(productId));
            log.info("Successfully processed product delete webhook for product: {}", productId);
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to process product delete webhook: {}", e.getMessage(), e);
            return false;
        }
    }
}
