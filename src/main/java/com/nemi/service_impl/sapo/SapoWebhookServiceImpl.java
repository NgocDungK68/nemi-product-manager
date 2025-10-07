package com.nemi.service_impl.sapo;

import com.nemi.configuration.SapoConfig;
import com.nemi.constant.WebhookConstants;
import com.nemi.entity.*;
import com.nemi.enums.NhanhvnEvent;
import com.nemi.enums.PosName;
import com.nemi.enums.SapoEvent;
import com.nemi.model.response.sapo.SapoOrderResponse;
import com.nemi.model.response.sapo.SapoProductResponse;
import com.nemi.repository.*;
import com.nemi.service.WebhookService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import com.nemi.utils.PosUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SapoWebhookServiceImpl implements WebhookService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SapoConfig sapoConfig;
    private final ClaimUtil claimUtil;
    private final WebhookHistoryRepository webhookHistoryRepository;

    @Override
    public String getPosName() {
        return PosName.SAPO.getValue();
    }

    @Override
    @Transactional
    public boolean processWebhook(String posId, Map<String, String> headers, Object body) {
        WebhookHistoryEntity webhookHistory = WebhookHistoryEntity.builder()
                .header(JsonUtils.toJson(headers))
                .status(WebhookConstants.Status.FAILED)
                .createdBy(WebhookConstants.UNKNOWN)
                .updatedBy(WebhookConstants.UNKNOWN)
                .build();
        try {
            String topic = headers.get("x-sapo-topic");

            SapoProductResponse.Product payloadProduct = null;
            SapoOrderResponse.Order payloadOrder = null;

            // 1️⃣ Parse payload theo loại topic
            if (topic.startsWith("products")) {
                payloadProduct = JsonUtils.map(body, SapoProductResponse.Product.class);
                if (payloadProduct == null) {
                    log.error("Failed to parse Sapo product webhook payload");
                    return false;
                }
            } else if (topic.startsWith("orders")) {
                payloadOrder = JsonUtils.map(body, SapoOrderResponse.Order.class);
                if (payloadOrder == null) {
                    log.error("Failed to parse Sapo order webhook payload");
                    return false;
                }
            } else {
                log.warn("Unknown webhook topic: {}", topic);
                return true; // acknowledge unknown topics
            }

            // 5. Process webhook data based on event type
            SapoEvent event = SapoEvent.fromValue(topic);
            return switch (event) {
                case PRODUCT_ADD -> processProductWebhook(posId, payloadProduct);
                case PRODUCT_DELETE -> processProductDeleteWebhook(posId, payloadProduct);
                case PRODUCT_UPDATE -> processProductUpdateWebhook(posId, payloadProduct);
                case ORDER_ADD -> processOrderCreateWebhook(posId, payloadOrder);
                case ORDER_DELETE -> processOrderDeleteWebhook(posId, payloadOrder);
                case ORDER_UPDATE -> processOrderUpdateWebhook(posId, payloadOrder);
            };
        } catch (Exception e) {
            log.error("Failed to process Sapo webhook: {}", e.getMessage(), e);
            return false;
        } finally {
            webhookHistoryRepository.save(webhookHistory);
        }
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
                    ProductVariantEntity variantEntity = convertToVariantEntity(posId, variant, payload.getId());
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

            // Delete variants first (foreign key constraint) - bulk delete by productId + posId
            try {
                productVariantRepository.deleteAllByProductIdAndPosId(String.valueOf(productId), posId);
                log.info("Deleted variants for product: {}", productId);
            } catch (Exception ex) {
                log.warn("Delete variants skipped for product {} (possibly already deleted): {}", productId, ex.getMessage());
            }

            // Delete product
            try {
                productRepository.deleteById(new ProductId(String.valueOf(productId), posId));
            } catch (Exception ex) {
                log.warn("Delete product skipped for product {} (possibly already deleted): {}", productId, ex.getMessage());
            }
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
            Optional<ProductEntity> existingProductOpt = productRepository.findById(new ProductId(String.valueOf(productId), posId));
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
                assert newImages != null;
                if (!newImages.equals(existingProduct.getImages())) {
                    log.info("Updating product images");
                    existingProduct.setImages(newImages);
                    hasChanges = true;
                }
            }

            // Check and update modified timestamp
            LocalDateTime newModifiedTime = PosUtils.parseDateTime(payload.getModifiedOn());

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
                productVariantRepository.deleteAllByProductIdAndPosId(String.valueOf(productId), posId);

                // Save new variants
                for (SapoProductResponse.Variant variant : payload.getVariants()) {
                    ProductVariantEntity variantEntity = convertToVariantEntity(posId, variant, productId);
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
        product.setStatus(payload.getStatus().toUpperCase());
        product.setPosId(posId);
        product.setCategory(payload.getProductType());
        product.setBrand(payload.getVendor());
        // Handle null images
        if (payload.getImages() != null && !payload.getImages().isEmpty()) {
            product.setImages(JsonUtils.toJson(payload.getImages().stream()
                    .map(SapoProductResponse.Image::getSrc) // Dùng method reference
                    .collect(Collectors.toList())));
        } else {
            product.setImages(null);
        }

        // Set timestamps - parse from string format
        product.setCreatedAt(PosUtils.parseDateTime(payload.getCreatedOn()));
        product.setUpdatedAt(PosUtils.parseDateTime(payload.getModifiedOn()));

        return product;
    }

    /**
     * Convert Sapo variant to ProductVariantEntity
     */
    private ProductVariantEntity convertToVariantEntity(String posId, SapoProductResponse.Variant variant, Long productId) {
        return ProductVariantEntity.builder()
                .variantId(String.valueOf(variant.getId()))
                .posId(posId)
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

    /**
     * Process order create webhook
     */
    private boolean processOrderCreateWebhook(String posId, SapoOrderResponse.Order payload) {
        try {
            // Convert and save order
            OrderEntity order = convertToOrderEntity(posId, payload);
            orderRepository.save(order);
            log.info("Successfully processed Sapo order webhook for order: {} (Code: {})",
                    order.getOrderId(), order.getOrderCode());

            // Process order items if any
            if (payload.getLineItems() != null && !payload.getLineItems().isEmpty()) {
                for (SapoOrderResponse.LineItem lineItem : payload.getLineItems()) {
                    OrderItemEntity orderItem = convertToOrderItemEntity(lineItem, order.getOrderId());
                    orderItemRepository.save(orderItem);
                    log.info("Successfully saved order item: {} for order: {}",
                            orderItem.getOrderItemId(), order.getOrderId());
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Failed to process order create webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process order delete webhook
     */
    private boolean processOrderDeleteWebhook(String posId, SapoOrderResponse.Order payload) {
        try {
            // Find and delete order by external ID
            Optional<OrderEntity> existingOrderOpt = orderRepository.findByOrderIdAndPosId(payload.getId().toString(), posId);
            if (existingOrderOpt.isPresent()) {
                OrderEntity existingOrder = existingOrderOpt.get();
                // Delete order items first
                List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(existingOrder.getOrderId());
                orderItemRepository.deleteAll(orderItems);

                // Delete order
                orderRepository.delete(existingOrder);
                log.info("Successfully deleted Sapo order: {} (Code: {})",
                        existingOrder.getOrderId(), existingOrder.getOrderCode());
            } else {
                log.warn("Order not found for deletion: {} in posId: {}", payload.getId(), posId);
            }
            return true;

        } catch (Exception e) {
            log.error("Failed to process order delete webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process order update webhook
     */
    private boolean processOrderUpdateWebhook(String posId, SapoOrderResponse.Order payload) {
        try {
            // Find existing order
            Optional<OrderEntity> existingOrderOpt = orderRepository.findByOrderIdAndPosId(payload.getId().toString(), posId);
            if (existingOrderOpt.isPresent()) {
                OrderEntity existingOrder = existingOrderOpt.get();
                // Update existing order
                OrderEntity updatedOrder = convertToOrderEntity(posId, payload);
                updatedOrder.setOrderId(existingOrder.getOrderId()); // Keep the same internal ID
                orderRepository.save(updatedOrder);

                // Update order items - delete existing and create new ones
                List<OrderItemEntity> existingOrderItems = orderItemRepository.findByOrderId(existingOrder.getOrderId());
                orderItemRepository.deleteAll(existingOrderItems);

                if (payload.getLineItems() != null && !payload.getLineItems().isEmpty()) {
                    for (SapoOrderResponse.LineItem lineItem : payload.getLineItems()) {
                        OrderItemEntity orderItem = convertToOrderItemEntity(lineItem, updatedOrder.getOrderId());
                        orderItemRepository.save(orderItem);
                    }
                }

                log.info("Successfully updated Sapo order: {} (Code: {})",
                        updatedOrder.getOrderId(), updatedOrder.getOrderCode());
            } else {
                // Create new order if not found
                return processOrderCreateWebhook(posId, payload);
            }
            return true;

        } catch (Exception e) {
            log.error("Failed to process order update webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Convert SapoOrder to OrderEntity
     */
    private OrderEntity convertToOrderEntity(String posId, SapoOrderResponse.Order sapoOrder) {
        SapoOrderResponse.OriginAddress originAddress = extractOriginAddress(sapoOrder);

        Map<String, String> mapping = sapoConfig.getOrder().getStatus().getMapping();
        String status = mapping.getOrDefault(sapoOrder.getStatus(), "unknown");

        log.info("Converting SapoOrder to OrderEntity - ID: {}, Name: {}",
                sapoOrder.getId(), sapoOrder.getName());

        return OrderEntity.builder()
                .orderId(sapoOrder.getId() != null ? sapoOrder.getId().toString() : "UNKNOWN")
                .orderCode(sapoOrder.getName())
                .posId(posId)
                .customerName(extractOriginAddressName(originAddress))
                .customerPhone(extractOriginAddressPhone(originAddress))
                .customerEmail(sapoOrder.getEmail())
                .shippingAddress(extractOriginAddressAddress(originAddress))
                .status(status.toUpperCase())
                .paymentMethod(extractPaymentMethods(sapoOrder))
                .shippingMethod(extractShippingMethod(sapoOrder))
                .totalPrice(sapoOrder.getTotalPrice())
                .shippingFee(extractShippingFee(sapoOrder))
                .discountAmount(extractDiscountAmount(sapoOrder))
                .createdAt(PosUtils.parseDateTime(sapoOrder.getCreatedOn()))
                .updatedAt(PosUtils.parseDateTime(sapoOrder.getCancelledOn()))
                .build();
    }

    /**
     * Convert SapoLineItem to OrderItemEntity
     */
    private OrderItemEntity convertToOrderItemEntity(SapoOrderResponse.LineItem sapoLineItem, String orderId) {
        BigDecimal price = sapoLineItem.getPrice() != null ? sapoLineItem.getPrice() : BigDecimal.ZERO;
        int quantity = sapoLineItem.getQuantity() != null ? sapoLineItem.getQuantity() : 0;
        BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(quantity));

        return OrderItemEntity.builder()
                .orderId(orderId)
                .sku(sapoLineItem.getSku())
                .productName(sapoLineItem.getTitle() != null ? sapoLineItem.getTitle() : null)
                .variantName(sapoLineItem.getVariantTitle() != null ? sapoLineItem.getVariantTitle() : null)
                .quantity(quantity)
                .price(price)
                .totalPrice(totalPrice)
                .fulfillableQuantity(sapoLineItem.getCurrentQuantity() != null ? sapoLineItem.getCurrentQuantity() : 0)
                .createdAt(LocalDateTime.now())
                .createdBy(claimUtil.getUserName() != null ? claimUtil.getUserName() : "system")
                .build();
    }

    // Extract origin address from fulfillments
    private SapoOrderResponse.OriginAddress extractOriginAddress(SapoOrderResponse.Order sapoOrder) {
        return Optional.ofNullable(sapoOrder.getFulfillments())
                .filter(fulfillments -> !fulfillments.isEmpty())
                .map(fulfillments -> fulfillments.get(0).getOriginAddress())
                .orElse(null);
    }

    // Extract fields from OriginAddress
    private String extractOriginAddressName(SapoOrderResponse.OriginAddress originAddress) {
        return Optional.ofNullable(originAddress)
                .map(SapoOrderResponse.OriginAddress::getName)
                .orElse(null);
    }

    private String extractOriginAddressPhone(SapoOrderResponse.OriginAddress originAddress) {
        return Optional.ofNullable(originAddress)
                .map(SapoOrderResponse.OriginAddress::getPhone)
                .orElse(null);
    }

    private String extractOriginAddressAddress(SapoOrderResponse.OriginAddress originAddress) {
        return Optional.ofNullable(originAddress)
                .map(SapoOrderResponse.OriginAddress::getAddress1)
                .orElse(null);
    }

    // Extract payment methods
    private String extractPaymentMethods(SapoOrderResponse.Order sapoOrder) {
        return Optional.ofNullable(sapoOrder.getPaymentGatewayNames())
                .filter(methods -> !methods.isEmpty())
                .map(methods -> String.join(", ", methods))
                .orElse(null);
    }

    // Extract shipping method
    private String extractShippingMethod(SapoOrderResponse.Order sapoOrder) {
        return Optional.ofNullable(sapoOrder.getShippingLines())
                .filter(lines -> !lines.isEmpty())
                .map(lines -> lines.get(0).getTitle())
                .orElse(null);
    }

    // Extract shipping fee
    private BigDecimal extractShippingFee(SapoOrderResponse.Order sapoOrder) {
        return Optional.ofNullable(sapoOrder.getShippingLines())
                .filter(lines -> !lines.isEmpty())
                .map(lines -> lines.get(0))
                .map(SapoOrderResponse.ShippingLine::getPrice)
                .orElse(null);
    }

    // Extract discount amount
    private Double extractDiscountAmount(SapoOrderResponse.Order sapoOrder) {
        return Optional.ofNullable(sapoOrder.getTotalDiscounts())
                .map(BigDecimal::doubleValue)
                .orElse(0.0);
    }

    // Helper methods for extracting data from SapoOrder

}
