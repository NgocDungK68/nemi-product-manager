package com.nemi.service_impl.sapo;

import com.nemi.configuration.SapoConfig;
import com.nemi.constant.SapoConstants;
import com.nemi.constant.WebhookConstants;
import com.nemi.entity.*;
import com.nemi.enums.PosName;
import com.nemi.enums.SapoEvent;
import com.nemi.model.response.sapo.SapoOrderResponse;
import com.nemi.model.response.sapo.SapoProductResponse;
import com.nemi.repository.*;
import com.nemi.service.WebhookService;
import com.nemi.util.JsonUtils;
import com.nemi.utils.PosUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    private final WebhookHistoryRepository webhookHistoryRepository;

    @Override
    public String getPosName() {
        return PosName.SAPO.getValue();
    }

    @Override
    @Transactional
    public boolean processWebhook(String posId, String posName, Map<String, String> headers, Object body) {
        WebhookHistoryEntity webhookHistory = WebhookHistoryEntity.builder()
                .header(JsonUtils.toJson(headers))
                .status(WebhookConstants.Status.FAILED)
                .posId(posId)
                .posName(posName)
                .build();
        try {
            String topic = headers.get(SapoConstants.X_SAPO_TOPIC);

            SapoProductResponse.Product payloadProduct = null;
            SapoOrderResponse.Order payloadOrder = null;

            // Parse payload theo loại topic
            if (topic.startsWith(SapoConstants.TOPIC_PRODUCTS)) {
                payloadProduct = JsonUtils.map(body, SapoProductResponse.Product.class);
                if (payloadProduct == null) {
                    log.error("Failed to parse Sapo product webhook payload");
                    return false;
                }
                webhookHistory.setBody(JsonUtils.toJson(payloadProduct));
            } else if (topic.startsWith(SapoConstants.TOPIC_ORDERS)) {
                payloadOrder = JsonUtils.map(body, SapoOrderResponse.Order.class);
                if (payloadOrder == null) {
                    log.error("Failed to parse Sapo order webhook payload");
                    return false;
                }
                webhookHistory.setBody(JsonUtils.toJson(payloadOrder));
            } else {
                log.warn("Unknown webhook topic: {}", topic);
                return false; // acknowledge unknown topics
            }

            // 5. Process webhook data based on event type
            SapoEvent event = SapoEvent.fromValue(topic);
            if (ObjectUtils.isEmpty(event)) {
                log.warn("Unhandled webhook event: {}", event);
                return false;
            }
            webhookHistory.setSyncType(event.getSyncType());
            webhookHistory.setEventType(event.getEventType());

            boolean isSuccess = switch (event) {
                case PRODUCT_ADD -> processProductAddWebhook(posId, payloadProduct);
                case PRODUCT_UPDATE -> processProductUpdateWebhook(posId, payloadProduct);
                case PRODUCT_DELETE -> processProductDeleteWebhook(posId, payloadProduct);
                case ORDER_ADD -> processOrderAddWebhook(posId, payloadOrder);
                case ORDER_UPDATED -> processOrderUpdatedWebhook(posId, payloadOrder);
                case ORDER_FULFILLED -> false;
                case ORDER_UPDATE -> false;
                case ORDER_DELETE -> processOrderDeleteWebhook(posId, payloadOrder);
            };

            String webhookStatus = isSuccess ? WebhookConstants.Status.SUCCESS : WebhookConstants.Status.FAILED;
            webhookHistory.setStatus(webhookStatus);
            webhookHistory.setCreatedBy(WebhookConstants.WEBHOOK);
            webhookHistory.setUpdatedBy(WebhookConstants.WEBHOOK);
            return isSuccess;
        } catch (Exception e) {
            log.error("Failed to process Sapo webhook: {}", e.getMessage(), e);
            return false;
        } finally {
            webhookHistoryRepository.save(webhookHistory);
        }
    }

    /**
     * Process product create/update webhook with upsert logic
     */
    private boolean processProductUpsertWebhook(String posId, SapoProductResponse.Product payload) {
        try {
            Long productId = payload.getId();
            if (productId == null || productId == 0) {
                log.error("No product ID found in webhook payload");
                return false;
            }

            Optional<ProductEntity> existingProductOpt = productRepository.findById(new ProductId(String.valueOf(productId), posId));
            
            ProductEntity product;
            if (existingProductOpt.isPresent()) {
                product = existingProductOpt.get();
                updateProductFromPayload(product, payload);
                log.info("Updating existing Sapo product: {}", productId);
            } else {
                product = convertToProductEntity(posId, payload);
                log.info("Creating new Sapo product: {}", productId);
            }
            
            productRepository.save(product);

            if (!ObjectUtils.isEmpty(payload.getVariants())) {
                productVariantRepository.deleteAllByProductIdAndPosId(String.valueOf(productId), posId);
                for (SapoProductResponse.Variant variant : payload.getVariants()) {
                    ProductVariantEntity variantEntity = convertToVariantEntity(posId, variant, productId);
                    productVariantRepository.save(variantEntity);
                }
            }

            log.info("Successfully processed Sapo product: {} (SKU: {})", product.getProductId(), product.getCode());
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

            if (ObjectUtils.isEmpty(productId) || productId == 0) {
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
        if (!ObjectUtils.isEmpty(payload.getImages())) {
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
     * Update existing ProductEntity from payload
     */
    private void updateProductFromPayload(ProductEntity product, SapoProductResponse.Product payload) {
        product.setName(payload.getName());
        product.setDescription(payload.getContent());
        product.setStatus(payload.getStatus().toUpperCase());
        product.setCategory(payload.getProductType());
        product.setBrand(payload.getVendor());
        
        if (!ObjectUtils.isEmpty(payload.getImages())) {
            product.setImages(JsonUtils.toJson(payload.getImages().stream()
                    .map(SapoProductResponse.Image::getSrc)
                    .collect(Collectors.toList())));
        } else {
            product.setImages(null);
        }
        
        product.setUpdatedAt(PosUtils.parseDateTime(payload.getModifiedOn()));
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
                .price(variant.getPrice() != null ? BigDecimal.valueOf(variant.getPrice()) : null)
                .ccy("VND") // Default currency
                .inventoryQuantity(variant.getInventoryQuantity())
                .fulfillableQuantity(variant.getInventoryQuantity()) // Assume same as inventory
                .weight(variant.getWeight())
                .weightUnit(variant.getWeightUnit())
                .attributes(JsonUtils.toJson(variant)) // Store full variant data as JSON
                .build();
    }

    /**
     * Process order create webhook - prioritizes INSERT with race condition handling
     */
    private boolean processOrderAddWebhook(String posId, SapoOrderResponse.Order payload) {
        try {
            String externalOrderId = payload.getId() != null ? payload.getId().toString() : null;
            if (externalOrderId == null) {
                log.error("No order ID found in webhook payload");
                return false;
            }

            log.info("Processing Sapo orders/create webhook for order: {}", externalOrderId);

            // Upsert order (handles race condition and syncs items)
            boolean success = upsertOrderAndSyncItems(posId, externalOrderId, payload, payload.getLineItems());
            if (!success) {
                return false;
            }

            log.info("Successfully processed Sapo order create: {} (Code: {})", externalOrderId, payload.getName());
            return true;

        } catch (Exception e) {
            log.error("Failed to process order create webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Process order update webhook - prioritizes UPDATE with fallback to INSERT
     */
    private boolean processOrderUpdatedWebhook(String posId, SapoOrderResponse.Order payload) {
        try {
            String externalOrderId = payload.getId() != null ? payload.getId().toString() : null;
            if (externalOrderId == null) {
                log.error("No order ID found in webhook payload");
                return false;
            }

            log.info("Processing Sapo orders/updated webhook for order: {}", externalOrderId);

            // Upsert order (handles race condition and syncs items)
            boolean success = upsertOrderAndSyncItems(posId, externalOrderId, payload, payload.getLineItems());
            if (!success) {
                return false;
            }

            log.info("Successfully processed Sapo order update: {} (Code: {})", externalOrderId, payload.getName());
            return true;

        } catch (Exception e) {
            log.error("Failed to process order update webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Upsert order and sync items - handles both create and update with race condition
     */
    private boolean upsertOrderAndSyncItems(String posId, String externalOrderId, SapoOrderResponse.Order payload, List<SapoOrderResponse.LineItem> lineItems) {
        Optional<OrderEntity> existingOrderOpt = orderRepository.findById(externalOrderId);
        
        OrderEntity order;
        if (existingOrderOpt.isPresent()) {
            // UPDATE existing order
            order = existingOrderOpt.get();
            updateOrderFromPayload(order, posId, payload);
            log.info("Updating existing Sapo order: {}", externalOrderId);
            orderRepository.save(order);
            
            // Sync items in current transaction
            syncOrderItems(order.getOrderId(), lineItems);
        } else {
            // CREATE new order - handle race condition
            try {
                order = convertToOrderEntity(posId, payload);
                log.info("Creating new Sapo order: {}", externalOrderId);
                orderRepository.saveAndFlush(order);
                
                // Sync items in current transaction
                syncOrderItems(order.getOrderId(), lineItems);
            } catch (DataIntegrityViolationException e) {
                // Race condition: another thread inserted this order
                // Current transaction is aborted - must handle retry + sync in NEW transaction
                log.warn("Duplicate key detected for order {}, retrying in new transaction", externalOrderId);
                // Don't continue in this transaction - it's aborted
                // Return the result from the new transaction
                return retryUpdateAndSyncInNewTransaction(posId, externalOrderId, payload, lineItems);
            }
        }
        
        return true;
    }

    /**
     * Retry update AND sync items in a new transaction after duplicate key error
     * This is needed because the current transaction is aborted after DataIntegrityViolationException
     * Must sync items in same new transaction to avoid "transaction is aborted" error
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean retryUpdateAndSyncInNewTransaction(String posId, String externalOrderId, SapoOrderResponse.Order payload, List<SapoOrderResponse.LineItem> lineItems) {
        try {
            Optional<OrderEntity> existingOrderOpt = orderRepository.findById(externalOrderId);
            if (existingOrderOpt.isPresent()) {
                OrderEntity order = existingOrderOpt.get();
                updateOrderFromPayload(order, posId, payload);
                orderRepository.save(order);
                log.info("Successfully retried update for Sapo order: {}", externalOrderId);
                
                // Sync items in SAME new transaction
                syncOrderItems(order.getOrderId(), lineItems);
                
                return true;
            } else {
                log.error("Order {} not found during retry", externalOrderId);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to retry update for Sapo order {}: {}", externalOrderId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Sync order items - delete old and insert new
     */
    private void syncOrderItems(String orderId, List<SapoOrderResponse.LineItem> lineItems) {
        if (ObjectUtils.isEmpty(lineItems)) {
            log.warn("No line items to sync for orderId={}", orderId);
            return;
        }

        // Delete old items
        orderItemRepository.deleteByOrderId(orderId);
        
        // Insert new items
        for (SapoOrderResponse.LineItem lineItem : lineItems) {
            OrderItemEntity orderItem = convertToOrderItemEntity(lineItem, orderId);
            orderItemRepository.save(orderItem);
        }
        
        log.info("Successfully synced {} order items for orderId={}", lineItems.size(), orderId);
    }

    // Event-specific wrappers (kept separate for clarity and future custom logic per event)
    private boolean processProductAddWebhook(String posId, SapoProductResponse.Product payload) {
        return processProductUpsertWebhook(posId, payload);
    }

    private boolean processProductUpdateWebhook(String posId, SapoProductResponse.Product payload) {
        return processProductUpsertWebhook(posId, payload);
    }

    private boolean processOrderFulfilledWebhook(String posId, SapoOrderResponse.Order payload) {
        try {
            String externalOrderId = payload.getId() != null ? payload.getId().toString() : null;
            if (externalOrderId == null) {
                log.error("No order ID found in webhook payload");
                return false;
            }

            log.info("Processing Sapo orders/fulfilled webhook for order: {}", externalOrderId);

            // Upsert order (handles race condition and syncs items)
            boolean success = upsertOrderAndSyncItems(posId, externalOrderId, payload, payload.getLineItems());
            if (!success) {
                return false;
            }

            log.info("Successfully processed Sapo order fulfilled: {} (Code: {})", externalOrderId, payload.getName());
            return true;

        } catch (Exception e) {
            log.error("Failed to process order fulfilled webhook: {}", e.getMessage(), e);
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
                .orderItemId(sapoLineItem.getId() != null ? String.valueOf(sapoLineItem.getId()) : UUID.randomUUID().toString())
                .sku(sapoLineItem.getSku())
                .productName(sapoLineItem.getTitle() != null ? sapoLineItem.getTitle() : null)
                .variantName(sapoLineItem.getVariantTitle() != null ? sapoLineItem.getVariantTitle() : null)
                .quantity(quantity)
                .price(price)
                .totalPrice(totalPrice)
                .fulfillableQuantity(sapoLineItem.getCurrentQuantity() != null ? sapoLineItem.getCurrentQuantity() : 0)
                .createdAt(LocalDateTime.now())
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

    /**
     * Update existing OrderEntity from payload without creating new instance
     */
    private void updateOrderFromPayload(OrderEntity order, String posId, SapoOrderResponse.Order payload) {
        SapoOrderResponse.OriginAddress originAddress = extractOriginAddress(payload);
        Map<String, String> mapping = sapoConfig.getOrder().getStatus().getMapping();
        String status = mapping.getOrDefault(payload.getStatus(), "unknown");

        order.setOrderCode(payload.getName());
        order.setPosId(posId);
        order.setCustomerName(extractOriginAddressName(originAddress));
        order.setCustomerPhone(extractOriginAddressPhone(originAddress));
        order.setCustomerEmail(payload.getEmail());
        order.setShippingAddress(extractOriginAddressAddress(originAddress));
        order.setStatus(status.toUpperCase());
        order.setPaymentMethod(extractPaymentMethods(payload));
        order.setShippingMethod(extractShippingMethod(payload));
        order.setTotalPrice(payload.getTotalPrice());
        order.setShippingFee(extractShippingFee(payload));
        order.setDiscountAmount(extractDiscountAmount(payload));
        order.setCreatedAt(PosUtils.parseDateTime(payload.getCreatedOn()));
        order.setUpdatedAt(PosUtils.parseDateTime(payload.getCancelledOn()));
    }

}
