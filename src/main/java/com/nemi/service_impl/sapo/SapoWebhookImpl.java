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
import java.util.Optional;

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
                    case "products/create":
                        return processProductWebhook(posId, payload);
                    case "products/delete":
                        return processProductDeleteWebhook(posId, payload);
                    case "products/update":
                        return processProductUpdateWebhook(posId, payload);
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
            // Sapo webhook sends data directly in the root, not in "data" field
            JsonNode productData = payload;
            
            ProductEntity product = new ProductEntity();
            
            // Map Sapo product data to ProductEntity based on actual payload structure
            product.setProductId(String.valueOf(productData.path("id").asLong()));
            
            // Get SKU from first variant if available, otherwise use product ID
            JsonNode variants = productData.path("variants");
            if (!variants.isMissingNode() && variants.isArray() && variants.size() > 0) {
                String sku = variants.get(0).path("sku").asText();
                if (!sku.isEmpty()) {
                    product.setCode(sku);
                } else {
                    product.setCode("SAPO-" + productData.path("id").asLong());
                }
            } else {
                product.setCode("SAPO-" + productData.path("id").asLong());
            }
            
            product.setName(productData.path("name").asText());
            product.setDescription(productData.path("alias").asText()); // Using alias as description
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
            
            // Handle tags as additional info
            JsonNode tags = productData.path("tags");
            if (!tags.isMissingNode()) {
                String tagsStr = tags.asText();
                if (!tagsStr.isEmpty()) {
                    product.setDescription(product.getDescription() + " | Tags: " + tagsStr);
                }
            }
            
            // Set timestamps
            String createdOn = productData.path("created_on").asText();
            String modifiedOn = productData.path("modified_on").asText();
            
            try {
                if (!createdOn.isEmpty()) {
                    product.setCreatedDatetime(LocalDateTime.parse(createdOn.replace("Z", "")));
                } else {
                    product.setCreatedDatetime(LocalDateTime.now());
                }
                
                if (!modifiedOn.isEmpty()) {
                    product.setUpdatedDatetime(LocalDateTime.parse(modifiedOn.replace("Z", "")));
                } else {
                    product.setUpdatedDatetime(LocalDateTime.now());
                }
            } catch (Exception e) {
                log.warn("Failed to parse timestamps, using current time: {}", e.getMessage());
                product.setCreatedDatetime(LocalDateTime.now());
                product.setUpdatedDatetime(LocalDateTime.now());
            }

            // Save to database
            productRepository.save(product);
            log.info("Successfully processed Sapo product webhook for product: {} (SKU: {})", 
                    product.getProductId(), product.getCode());
            
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
            // Sapo delete webhook also sends data directly in the root
            Long productId = payload.path("id").asLong();
            
            if (productId == 0) {
                log.error("No product ID found in delete webhook payload");
                return false;
            }
            
            // Delete from database
            productRepository.deleteById(String.valueOf(productId));
            log.info("Successfully processed Sapo product delete webhook for product: {}", productId);
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to process product delete webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process product update webhook - only update changed fields
     */
    private boolean processProductUpdateWebhook(String posId, JsonNode payload) {
        try {
            // Sapo webhook sends data directly in the root
            JsonNode productData = payload;
            
            Long productId = productData.path("id").asLong();
            if (productId == 0) {
                log.error("No product ID found in update webhook payload");
                return false;
            }

            // Find existing product in database
            Optional<ProductEntity> existingProductOpt = productRepository.findById(String.valueOf(productId));
            if (existingProductOpt.isEmpty()) {
                log.warn("Product with ID {} not found in database, creating new product", productId);
                return processProductWebhook(posId, payload); // Create new product
            }

            ProductEntity existingProduct = existingProductOpt.get();
            boolean hasChanges = false;

            // Check and update name
            String newName = productData.path("name").asText();
            if (!newName.isEmpty() && !newName.equals(existingProduct.getName())) {
                log.info("Updating product name: {} -> {}", existingProduct.getName(), newName);
                existingProduct.setName(newName);
                hasChanges = true;
            }

            // Check and update alias (description)
            String newAlias = productData.path("alias").asText();
            if (!newAlias.isEmpty() && !newAlias.equals(existingProduct.getDescription())) {
                log.info("Updating product alias: {} -> {}", existingProduct.getDescription(), newAlias);
                existingProduct.setDescription(newAlias);
                hasChanges = true;
            }

            // Check and update status
            String newStatus = productData.path("status").asText();
            if (!newStatus.isEmpty() && !newStatus.equals(existingProduct.getStatus())) {
                log.info("Updating product status: {} -> {}", existingProduct.getStatus(), newStatus);
                existingProduct.setStatus(newStatus);
                hasChanges = true;
            }

            // Check and update product_type (category)
            String newProductType = productData.path("product_type").asText();
            if (!newProductType.isEmpty() && !newProductType.equals(existingProduct.getCategory())) {
                log.info("Updating product category: {} -> {}", existingProduct.getCategory(), newProductType);
                existingProduct.setCategory(newProductType);
                hasChanges = true;
            }

            // Check and update vendor (brand)
            String newVendor = productData.path("vendor").asText();
            if (!newVendor.isEmpty() && !newVendor.equals(existingProduct.getBrand())) {
                log.info("Updating product brand: {} -> {}", existingProduct.getBrand(), newVendor);
                existingProduct.setBrand(newVendor);
                hasChanges = true;
            }

            // Check and update tags
            String newTags = productData.path("tags").asText();
            String currentDescription = existingProduct.getDescription();
            String currentTags = "";
            if (currentDescription != null && currentDescription.contains(" | Tags: ")) {
                currentTags = currentDescription.substring(currentDescription.indexOf(" | Tags: ") + 9);
            }
            
            if (!newTags.isEmpty() && !newTags.equals(currentTags)) {
                log.info("Updating product tags: {} -> {}", currentTags, newTags);
                // Update description with new tags
                String baseDescription = currentDescription;
                if (baseDescription != null && baseDescription.contains(" | Tags: ")) {
                    baseDescription = baseDescription.substring(0, baseDescription.indexOf(" | Tags: "));
                } else if (baseDescription == null) {
                    baseDescription = "";
                }
                existingProduct.setDescription(baseDescription + " | Tags: " + newTags);
                hasChanges = true;
            }

            // Check and update SKU from variants
            JsonNode variants = productData.path("variants");
            if (!variants.isMissingNode() && variants.isArray() && variants.size() > 0) {
                String newSku = variants.get(0).path("sku").asText();
                if (!newSku.isEmpty() && !newSku.equals(existingProduct.getCode())) {
                    log.info("Updating product SKU: {} -> {}", existingProduct.getCode(), newSku);
                    existingProduct.setCode(newSku);
                    hasChanges = true;
                }
            }

            // Always update modified timestamp
            String modifiedOn = productData.path("modified_on").asText();
            if (!modifiedOn.isEmpty()) {
                try {
                    LocalDateTime newModifiedTime = LocalDateTime.parse(modifiedOn.replace("Z", ""));
                    if (!newModifiedTime.equals(existingProduct.getUpdatedDatetime())) {
                        log.info("Updating product modified time: {} -> {}", existingProduct.getUpdatedDatetime(), newModifiedTime);
                        existingProduct.setUpdatedDatetime(newModifiedTime);
                        hasChanges = true;
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse modified timestamp: {}", e.getMessage());
                    existingProduct.setUpdatedDatetime(LocalDateTime.now());
                    hasChanges = true;
                }
            }

            if (hasChanges) {
                // Save updated product to database
                productRepository.save(existingProduct);
                log.info("Successfully updated Sapo product: {} with {} changes", productId, 
                        (hasChanges ? "some" : "no"));
            } else {
                log.info("No changes detected for Sapo product: {}", productId);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to process product update webhook: {}", e.getMessage(), e);
            return false;
        }
    }
}
