package com.nemi.service_impl.sapo;

import com.nemi.constant.enums.PosName;
import com.nemi.util.JsonUtils;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.nemi.model.response.sapo.SapoProductResponse;
import com.nemi.repository.ProductRepository;
import com.nemi.repository.ProductVariantRepository;
import com.nemi.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SapoWebhookImpl implements WebhookService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    @Override
    public String getPosName() {
        return PosName.SAPO.getValue();
    }

    @Override
    @Transactional
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

            // 3. Parse JSON payload using JsonUtils
            if (body != null && !body.trim().isEmpty()) {
                SapoProductResponse.Product payload = JsonUtils.fromJson(body, SapoProductResponse.Product.class);
                if (payload == null) {
                    log.error("Failed to parse webhook payload");
                    return false;
                }
                
                // 4. Process webhook data based on event type
                String topic = request.getHeader("x-sapo-topic");
                switch (topic) {
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

    private boolean processProductWebhook(String posId, SapoProductResponse.Product payload) {
        try {
            // Convert and save product
            ProductEntity product = convertToProductEntity(posId, payload);
            productRepository.save(product);
            log.info("Successfully processed Sapo product webhook for product: {} (SKU: {})", 
                    product.getProductId(), product.getCode());
            
            // Process variants if any
            if (payload.getVariants() != null && !payload.getVariants().isEmpty()) {
                for (SapoProductResponse.Variant variant : payload.getVariants()) {
                    ProductVariantEntity variantEntity = convertToVariantEntity(variant, payload.getId());
                    productVariantRepository.save(variantEntity);
                    log.info("Successfully saved variant: {} for product: {}", 
                            variantEntity.getVariantId(), product.getProductId());
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to process product webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process product delete webhook
     */

    public boolean processProductDeleteWebhook(String posId, SapoProductResponse.Product payload) {
        try {
            Long productId = payload.getId();
            
            if (productId == null || productId == 0) {
                log.error("No product ID found in delete webhook payload");
                return false;
            }
            
            // Delete variants first (foreign key constraint)
            productVariantRepository.deleteByProductId(String.valueOf(productId));
            log.info("Deleted variants for product: {}", productId);
            
            // Delete product
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

    private boolean processProductUpdateWebhook(String posId, SapoProductResponse.Product payload) {
        try {
            Long productId = payload.getId();
            if (productId == null || productId == 0) {
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
            if (payload.getName() != null && !payload.getName().equals(existingProduct.getName())) {
                log.info("Updating product name: {} -> {}", existingProduct.getName(), payload.getName());
                existingProduct.setName(payload.getName());
                hasChanges = true;
            }

            // Check and update content (description)
            if (payload.getContent() != null && !payload.getContent().equals(existingProduct.getDescription())) {
                log.info("Updating product description: {} -> {}", existingProduct.getDescription(), payload.getContent());
                existingProduct.setDescription(payload.getContent());
                hasChanges = true;
            }

            // Check and update status
            if (payload.getStatus() != null && !payload.getStatus().equals(existingProduct.getStatus())) {
                log.info("Updating product status: {} -> {}", existingProduct.getStatus(), payload.getStatus());
                existingProduct.setStatus(payload.getStatus());
                hasChanges = true;
            }

            // Check and update product_type (category)
            if (payload.getProductType() != null && !payload.getProductType().equals(existingProduct.getCategory())) {
                log.info("Updating product category: {} -> {}", existingProduct.getCategory(), payload.getProductType());
                existingProduct.setCategory(payload.getProductType());
                hasChanges = true;
            }

            // Check and update vendor (brand)
            if (payload.getVendor() != null && !payload.getVendor().equals(existingProduct.getBrand())) {
                log.info("Updating product brand: {} -> {}", existingProduct.getBrand(), payload.getVendor());
                existingProduct.setBrand(payload.getVendor());
                hasChanges = true;
            }

            // Check and update images
            if (payload.getImages() != null && !payload.getImages().isEmpty()) {
                String newImages = JsonUtils.toJson(payload.getImages().stream()
                        .map(SapoProductResponse.Image::getSrc)
                        .collect(Collectors.toList()));
                if (!newImages.equals(existingProduct.getImages())) {
                    log.info("Updating product images");
                    existingProduct.setImages(newImages);
                    hasChanges = true;
                }
            }

            // Check and update modified timestamp
            LocalDateTime newModifiedTime = parseSapoDateTime(payload.getModifiedOn());
            if (newModifiedTime != null && !newModifiedTime.equals(existingProduct.getUpdatedAt())) {
                log.info("Updating product modified time: {} -> {}", existingProduct.getUpdatedAt(), newModifiedTime);
                existingProduct.setUpdatedAt(newModifiedTime);
                hasChanges = true;
            }

            // Save updated product to database if there are changes
            if (hasChanges) {
                productRepository.save(existingProduct);
                log.info("Successfully updated Sapo product: {} with some changes", productId);
            } else {
                log.info("No changes detected for Sapo product: {}", productId);
            }

            // Update variants if any
            if (payload.getVariants() != null && !payload.getVariants().isEmpty()) {
                // Delete existing variants first
                productVariantRepository.deleteByProductId(String.valueOf(productId));

                // Save new variants
                for (SapoProductResponse.Variant variant : payload.getVariants()) {
                    ProductVariantEntity variantEntity = convertToVariantEntity(variant, productId);
                    productVariantRepository.save(variantEntity);
                    log.info("Updated variant: {} for product: {}", variantEntity.getVariantId(), productId);
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Failed to process product update webhook: {}", e.getMessage(), e);
            return false;
        }
    }


    /**
     * Convert Sapo product to ProductEntity
     */
    private ProductEntity convertToProductEntity(String posId, SapoProductResponse.Product payload) {
        ProductEntity product = new ProductEntity();
        
        // Map Sapo product data to ProductEntity
        product.setProductId(String.valueOf(payload.getId()));
        
        // Get SKU from first variant if available, otherwise use product ID
        product.setCode(null);
        product.setName(payload.getName());
        product.setDescription(payload.getContent()); // Using content as description
        product.setStatus(payload.getStatus());
        product.setPosId(posId);
        product.setCategory(payload.getProductType());
        product.setBrand(payload.getVendor());
        product.setImages(JsonUtils.toJson(payload.getImages().stream()
                .map(SapoProductResponse.Image::getSrc) // Dùng method reference
                .collect(Collectors.toList())));

        // Set timestamps - parse from string format
        product.setCreatedAt(parseSapoDateTime(payload.getCreatedOn()));
        product.setUpdatedAt(parseSapoDateTime(payload.getModifiedOn()));

        return product;
    }

    /**
     * Convert Sapo variant to ProductVariantEntity
     */
    private ProductVariantEntity convertToVariantEntity(SapoProductResponse.Variant variant, Long productId) {
        return ProductVariantEntity.builder()
                .variantId(String.valueOf(variant.getId()))
                .productId(String.valueOf(productId))
                .sku(variant.getSku())
                .barcode(variant.getBarcode())
                .price(variant.getPrice() != null ? java.math.BigDecimal.valueOf(variant.getPrice()) : null)
                .ccy("VND") // Default currency
                .inventoryQuantity(variant.getInventoryQuantity())
                .fulfillableQuantity(variant.getInventoryQuantity()) // Assume same as inventory
                .weight(variant.getWeight())
                .weightUnit(variant.getWeightUnit())
                .attributes(JsonUtils.toJson(variant)) // Store full variant data as JSON
                .build();
    }

    private LocalDateTime parseSapoDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }

        try {
            // 1. Dùng Instant để xử lý chuỗi ISO 8601 có 'Z' (Zulu/UTC)
            // Instant.parse() xử lý định dạng "yyyy-MM-ddTHH:mm:ssZ" hoặc có mili giây.
            Instant instant = Instant.parse(dateTimeString.trim());

            // 2. Chuyển Instant (UTC time) sang LocalDateTime (bỏ thông tin múi giờ)
            // Sử dụng ZoneOffset.UTC để đảm bảo chuyển đổi chính xác từ UTC.
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);

        } catch (Exception e) {
            // Ghi log chi tiết hơn để dễ debug
            log.warn("[SapoServiceImpl] Failed to parse date time '{}'. Error: {}", dateTimeString, e.getMessage());
            return null;
        }
    }
}
