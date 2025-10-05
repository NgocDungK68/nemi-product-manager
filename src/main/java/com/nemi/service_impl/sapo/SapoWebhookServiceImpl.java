package com.nemi.service_impl.sapo;

import com.nemi.constant.enums.PosName;
import com.nemi.util.JsonUtils;
import com.nemi.entity.OrderEntity;
import com.nemi.entity.OrderItemEntity;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.nemi.model.response.sapo.SapoOrderResponse;
import com.nemi.model.response.sapo.SapoProductResponse;
import com.nemi.repository.OrderItemRepository;
import com.nemi.repository.OrderRepository;
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
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.HashMap;
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
                SapoProductResponse.Product payloadProduct = JsonUtils.fromJson(body, SapoProductResponse.Product.class);
                SapoOrderResponse.Order payloadOrder = JsonUtils.fromJson(body, SapoOrderResponse.Order.class);
                if (payloadProduct == null || payloadOrder == null) {
                    log.error("Failed to parse webhook payload ");
                    return false;
                }

                // 4. Process webhook data based on event type
                String topic = request.getHeader("x-sapo-topic");
                switch (topic) {
                    case "products/create":
                        return processProductWebhook(posId, payloadProduct);
                    case "products/delete":
                        return processProductDeleteWebhook(posId, payloadProduct);
                    case "products/update":
                        return processProductUpdateWebhook(posId, payloadProduct);
                    case "orders/create":
                        return processOrderCreateWebhook(posId, payloadOrder);
                    case "orders/delete":
                        return processOrderDeleteWebhook(posId, payloadOrder);
                    case "orders/update":
                        return processOrderUpdateWebhook(posId, payloadOrder);
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
        // Handle null images
        if (payload.getImages() != null && !payload.getImages().isEmpty()) {
            product.setImages(JsonUtils.toJson(payload.getImages().stream()
                    .map(SapoProductResponse.Image::getSrc) // Dùng method reference
                    .collect(Collectors.toList())));
        } else {
            product.setImages(null);
        }

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
        SapoOrderResponse.OriginAddress billing = sapoOrder.getFulfillments() != null && !sapoOrder.getFulfillments().isEmpty()
                ? sapoOrder.getFulfillments().get(0).getOriginAddress()
                : null;


        // Debug logging to understand the issue
        log.info("Converting SapoOrder to OrderEntity - ID: {}, Name: {}", sapoOrder.getId(), sapoOrder.getName());

        return OrderEntity.builder()
                .orderId(sapoOrder.getId() != null ? sapoOrder.getId().toString() : "UNKNOWN")
                .orderCode(sapoOrder.getName())
                .posId(posId)
                .customerName(billing != null ? billing.getName() : null)
                .customerPhone(billing != null ? billing.getPhone() : null)
                .customerEmail(sapoOrder.getEmail())
                .shippingAddress(billing !=null ? billing.getAddress1() : null)
                .status(null)
                .paymentMethod(sapoOrder.getPaymentGatewayNames() != null && !sapoOrder.getPaymentGatewayNames().isEmpty()
                        ? String.join(", ", sapoOrder.getPaymentGatewayNames()) : null)
                .shippingMethod(sapoOrder.getShippingLines() != null ? sapoOrder.getShippingLines().getTitle() : null)
                .totalPrice(sapoOrder.getTotalPrice())
                .shippingFee(sapoOrder.getShippingLines() != null && sapoOrder.getShippingLines().getPrice() != null ? sapoOrder.getShippingLines().getPrice() : null)
                .discountAmount(sapoOrder.getTotalDiscounts() != null ? sapoOrder.getTotalDiscounts().doubleValue() : 0.0)
                .build();
    }

    /**
     * Convert SapoLineItem to OrderItemEntity
     */
    private OrderItemEntity convertToOrderItemEntity(SapoOrderResponse.LineItem sapoLineItem, String orderId) {
        BigDecimal totalPrice = sapoLineItem.getPrice().multiply(BigDecimal.valueOf(sapoLineItem.getQuantity()));

        return OrderItemEntity.builder()
                .orderId(orderId)
                .sku(sapoLineItem.getSku())
                .productName(sapoLineItem.getTitle())
                .variantName(sapoLineItem.getVariantTitle())
                .quantity(sapoLineItem.getQuantity())
                .price(sapoLineItem.getPrice())
                .totalPrice(totalPrice)
                .fulfillableQuantity(sapoLineItem.getCurrentQuantity())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // Helper methods for extracting data from SapoOrder

}
