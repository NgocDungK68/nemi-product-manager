package com.nemi.service_impl.pancake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.configuration.PancakeConfig;
import com.nemi.constant.WebhookConstants;
import com.nemi.entity.OrderEntity;
import com.nemi.entity.OrderItemEntity;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.nemi.entity.WebhookHistoryEntity;
import com.nemi.enums.PancakeEvent;
import com.nemi.enums.PosName;
import com.nemi.enums.Status;
import com.nemi.enums.WeightUnit;
import com.nemi.model.request.pancake.PancakeProductWebhookRequest;
import com.nemi.model.request.pancake.PancakeWebhookRequest;
import com.nemi.model.response.pancake.PancakeOrderResponse;
import com.nemi.model.response.pancake.PancakeProductResponse;
import com.nemi.repository.OrderItemRepository;
import com.nemi.repository.OrderRepository;
import com.nemi.repository.ProductRepository;
import com.nemi.repository.ProductVariantRepository;
import com.nemi.repository.WebhookHistoryRepository;
import com.nemi.service.WebhookService;
import com.nemi.util.JsonUtils;
import io.jsonwebtoken.lang.Objects;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Xử lý webhook gửi từ Pancake POS
 * Dựa theo cấu trúc webhook thực tế (type=orders, type=variations_warehouses)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PancakeWebhookServiceImpl implements WebhookService {

    private final PancakeConfig pancakeConfig;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WebhookHistoryRepository webhookHistoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;


    @Override
    public String getPosName() {
        return PosName.PANCAKE.getValue();
    }

    @Override
    @Transactional
    @PreAuthorize("@pancakeAuth.checkWebhookToken(#headers, #posId)")
    public boolean processWebhook(String posId, String posName, Map<String, String> headers, Object body) {
        WebhookHistoryEntity webhookHistory = WebhookHistoryEntity.builder()
                .header(JsonUtils.toJson(headers))
                .status(WebhookConstants.Status.FAILED)
                .posId(posId)
                .posName(posName)
                .build();
        try {
            log.info("day la body{}", body);
            // 2. Parse JSON về model
            PancakeWebhookRequest webhookRequest = JsonUtils.map(body, PancakeWebhookRequest.class);
            webhookHistory.setBody(JsonUtils.toJson(webhookRequest));

            if (Objects.isEmpty(webhookRequest) || Objects.isEmpty(webhookRequest.getType())) {
                log.error("[PancakeWebhookServiceImpl.processWebhook] Invalid webhook payload: {}", body);
                return false;
            }

            log.info("[PancakeWebhookServiceImpl.processWebhook] Parsed webhook: {}", webhookRequest.getType());

            // 3. Xử lý từng loại webhook
            boolean isSuccess = handleEvent(posId, body, webhookHistory, webhookRequest);
            String webhookStatus = isSuccess ? WebhookConstants.Status.SUCCESS : WebhookConstants.Status.FAILED;
            webhookHistory.setStatus(webhookStatus);
            webhookHistory.setCreatedBy(WebhookConstants.WEBHOOK);
            webhookHistory.setUpdatedBy(WebhookConstants.WEBHOOK);
            return isSuccess;
        } catch (Exception e) {
            log.error("[PancakeWebhookServiceImpl.processWebhook] Process webhook failed: {}", e.getMessage(), e);
            return false;
        } finally {
            webhookHistoryRepository.save(webhookHistory);
        }
    }

    private boolean handleEvent(String posId, Object body, WebhookHistoryEntity webhookHistory, PancakeWebhookRequest request) {
        PancakeEvent event = PancakeEvent.fromValue(request.getType());
        if (ObjectUtils.isEmpty(request.getType())) {
            log.warn("[PancakeWebhookServiceImpl.handleEvent] Unhandled webhook event: {}", request);
            return false;
        }
        webhookHistory.setSyncType(request.getType());
        Optional.ofNullable(request.getEventType()).ifPresent(webhookHistory::setEventType);

        return switch (event) {
            case ORDER -> handleOrderWebhook(posId, body);
            case PRODUCT -> handleProductWebhook(posId, body);
//            case ORDER_DELETE -> handleOrderWebhook(posId, request); luc nao cx update
            default -> {
                log.warn("[PancakeWebhookServiceImpl.handleEvent] Unsupported webhook type: {}", event);
                yield false;
            }
        };
    }

    /**
     * Xử lý webhook type=orders
     * - Khi status đổi (add/update/delete)
     * - Có thể gồm nhiều history/status_history
     */
    private boolean handleOrderWebhook(String posId, Object webhookResponse) {

//        PancakeOrderWebhookRequest pancakeOrderWebhookRequest = JsonUtils.map(webhookResponse, PancakeOrderWebhookRequest.class);
//        log.info("[PancakeWebhookServiceImpl.handleOrderUpdate] Received Order Webhook: {}", pancakeOrderWebhookRequest);

        PancakeOrderResponse.DataItem orderData = objectMapper.convertValue(
                webhookResponse, PancakeOrderResponse.DataItem.class
        );
        log.info("[PancakeWebhookServiceImpl.handleOrderUpdate] Update OrderData: {}", orderData);

        if (Objects.isEmpty(orderData) || Objects.isEmpty(orderData.getId())) {
            log.error("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Failed to update order data: {}", orderData);
            return false;
        }

        // Convert OrderEntity
        OrderEntity orderEntity = convertToOrderEntity(posId, orderData);
        if (ObjectUtils.isEmpty(orderEntity)) {
            log.error("[PancakeWebhookServiceImpl.handleOrderUpdate] Failed to convert orderData={} to OrderEntity", orderData.getId());
            return false;
        }

        // Save (insert/update)
        orderRepository.save(orderEntity);

        // 6 va 7 la ma stattus huy cua pancake
        if (orderData.getStatus() == 6 || orderData.getStatus() == 7) {
            return true;
        }
        log.info("[PancakeWebhookServiceImpl.handleOrderUpdate] Successfully updated OrderEntity with id={} and code={}",
                orderEntity.getOrderId(), orderEntity.getOrderCode());

        // Sync OrderItems
        List<OrderItemEntity> orderItemEntities = convertToOrderItemEntity(orderData);
        if (CollectionUtils.isEmpty(orderItemEntities)) {
            log.warn("[PancakeWebhookServiceImpl.handleOrderUpdate] Order id={} has no products", orderEntity.getOrderId());
            return false;
        }
        // Xóa items cũ để tránh dữ liệu thừa
        orderItemRepository.deleteByOrderId(orderEntity.getOrderId());

        // Save lại items mới
        orderItemRepository.saveAll(orderItemEntities);
        log.info("[PancakeWebhookServiceImpl.handleOrderUpdate] Successfully synced {} OrderItemEntities for orderId={}",
                orderItemEntities.size(), orderEntity.getOrderId());

        return true;
    }

    private boolean handleProductWebhook(String posId, Object webhookResponse) {

        PancakeProductWebhookRequest pancakeProductWebhookRequest = JsonUtils.map(webhookResponse, PancakeProductWebhookRequest.class);
        log.info("[PancakeWebhookServiceImpl.handleOrderUpdate] Received Product Webhook: {}", pancakeProductWebhookRequest);

        // Convert OrderEntity

        ProductEntity product = convertToProductEntity(posId, pancakeProductWebhookRequest);

        if (ObjectUtils.isEmpty(product)) {
            log.error("[PancakeWebhookServiceImpl.handleOrderUpdate] Failed to convert orderData={} to OrderEntity", pancakeProductWebhookRequest.getId());
            return false;
        }

        // Save (insert/update)
        productRepository.save(product);

        // xoa product
        if (java.util.Objects.equals(product.getStatus(), Status.INACTIVE.getValue())) {
            // de xoa mem
            return true;
        }
        log.info("[PancakeWebhookServiceImpl.handleOrderUpdate] Successfully updated ProductEntity with id={} and code={}",
                product.getProductId(), product.getCode());

        // Sync variant
        List<ProductVariantEntity> productVariantEntities = convertToVariantEntities(posId, pancakeProductWebhookRequest.getVariations());


        if (CollectionUtils.isEmpty(productVariantEntities)) {
            log.error("[PancakeWebhookServiceImpl.handleOrderUpdate] Failed to update order data: {}", pancakeProductWebhookRequest);
            return false;
        }

        productVariantRepository.deleteByProductId(pancakeProductWebhookRequest.getId());

        productVariantRepository.saveAll(productVariantEntities);

        return true;
    }

    public ProductEntity convertToProductEntity(String posId, PancakeProductWebhookRequest pancakeOrderWebhookRequest) {

        String images = Optional.ofNullable(pancakeOrderWebhookRequest.getImages())
                .map(list -> list.stream()
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.joining(",")))
                .orElse(null);


        ProductEntity product = ProductEntity.builder()
                .posId(posId)
                .productId(pancakeOrderWebhookRequest.getId())
                .code(pancakeOrderWebhookRequest.getDisplayId())
                .name(pancakeOrderWebhookRequest.getName())
                .description(pancakeOrderWebhookRequest.getNoteProduct())
                .build();

        Boolean isRemoved = pancakeOrderWebhookRequest.getIsRemoved();

        if (isRemoved == null) {
            product.setStatus(null);
        } else if (isRemoved) {
            product.setStatus(Status.INACTIVE.getValue());
        } else {
            product.setStatus(Status.ACTIVE.getValue());
        }


        return product;
    }

    public List<ProductVariantEntity> convertToVariantEntities(
            String posId,
            List<PancakeProductResponse.ProductData> apiProducts
    ) {
        return apiProducts.stream()  // can nhac paralle stream
                .map(apiProduct -> convertToVariantEntity(posId, apiProduct))
                .collect(Collectors.toList());
    }


    public ProductVariantEntity convertToVariantEntity(String posId, PancakeProductResponse.ProductData apiProduct) {

        Integer remainQuantity = Optional.ofNullable(apiProduct.getVariationsWarehouses())
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(PancakeProductResponse.VariationWarehouse::getRemainQuantity)
                .orElse(null);


        return ProductVariantEntity.builder()
                .variantId(apiProduct.getId())
                .posId(posId)
                .productId(String.valueOf(apiProduct.getProductId()))
                .sku(apiProduct.getDisplayId())
                .barcode(apiProduct.getBarcode())
                .price(BigDecimal.valueOf(apiProduct.getRetailPrice()))
                .inventoryQuantity(apiProduct.getRemainQuantity())
                .fulfillableQuantity(remainQuantity)
                .weight(apiProduct.getWeight())
                .weightUnit(WeightUnit.GAM.getValue())
                .build();
    }


    public OrderEntity convertToOrderEntity(String posId, PancakeOrderResponse.DataItem apiOrders) {

        String status = pancakeConfig.getStatusMapping(apiOrders.getStatus(), apiOrders.getStatusName());
        log.info("status of orderId {} is {}", apiOrders.getId(), status);

        String paymentMethod = Optional.ofNullable(apiOrders.getPaymentPurchaseHistories())
                .filter(histories -> !histories.isEmpty())
                .map(histories -> histories.get(0).getType())
                .orElse(null);

        String orderCode = Optional.ofNullable(apiOrders.getPartner())
                .map(PancakeOrderResponse.Partner::getExtendCode)
                .orElse(null);

        return OrderEntity.builder()
                .posId(posId)
                .orderId(String.valueOf(apiOrders.getId()))
                .orderCode(orderCode)
                .customerName(apiOrders.getShippingAddress().getFullName())
                .customerPhone(apiOrders.getShippingAddress().getPhoneNumber())
                .shippingAddress(apiOrders.getShippingAddress().getFullAddress())
                .paymentMethod(paymentMethod)
                .shippingFee(apiOrders.getShippingFee())
                .totalPrice(apiOrders.getTotalPrice())
                .status(status)
                .customerEmail(apiOrders.getBillEmail())
                .discountAmount(apiOrders.getTotalDiscount())
                .updatedBy(PosName.WEBHOOK.getValue())
                .build();
    }

    public List<OrderItemEntity> convertToOrderItemEntity(PancakeOrderResponse.DataItem apiOrder) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for (PancakeOrderResponse.Item product : apiOrder.getItems()) {
            BigDecimal quantity = BigDecimal.valueOf(product.getQuantity());
            orderItemEntities.add(OrderItemEntity.builder()
                    .orderItemId(String.valueOf(product.getId()))
                    .orderId(String.valueOf(apiOrder.getId()))
                    .quantity(product.getQuantity())
                    .sku(product.getVariationInfo().getDisplayId())
                    .variantName(product.getVariationInfo().getName())
                    .price(product.getVariationInfo().getRetailPrice())
                    .totalPrice(product.getVariationInfo().getRetailPrice().multiply(quantity))
                    .productName(product.getVariationInfo().getName())
                    .updatedBy(PosName.WEBHOOK.getValue())
                    .build());
        }
        return orderItemEntities;
    }


    /**
     * Xử lý webhook type=variations_warehouses
     * - Cập nhật tồn kho
     */
}
