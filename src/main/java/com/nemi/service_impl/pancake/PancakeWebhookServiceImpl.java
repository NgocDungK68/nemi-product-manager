package com.nemi.service_impl.pancake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.configuration.PancakeConfig;
import com.nemi.entity.OrderEntity;
import com.nemi.entity.OrderItemEntity;
import com.nemi.enums.PancakeEvent;
import com.nemi.enums.PosName;
import com.nemi.enums.Status;
import com.nemi.model.response.pancake.PancakeOrderResponse;
import com.nemi.model.response.pancake.PancakeWebhookResponse;
import com.nemi.repository.OrderItemRepository;
import com.nemi.repository.OrderRepository;
import com.nemi.repository.ProductRepository;
import com.nemi.repository.ProductVariantRepository;
import com.nemi.service.WebhookService;
import com.nemi.util.JsonUtils;
import io.jsonwebtoken.lang.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


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
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PancakeServiceImpl pancakeService;

    @Override
    public String getPosName() {
        return PosName.PANCAKE.getValue();
    }

    @Override
    public boolean processWebhook(String posId, Map<String, String> headers, Object body) {
        try {
            // 1. Xác thực header x-api-key
            String apiKey = headers.get("x-api-key");
            if (Objects.isEmpty(apiKey) || !apiKey.equals(pancakeConfig.getXApiKey())) { //sau nay de thg user nhap rong connect post- regiset webhook gi do...
                log.error("[PancakeWebhookServiceImpl.processWebhook] Invalid x-api-key: {}", apiKey);
                return false;
            }

            // 2. Parse JSON về model
            PancakeWebhookResponse webhookResponse = JsonUtils.map(body, PancakeWebhookResponse.class);
            if (Objects.isEmpty(webhookResponse) || Objects.isEmpty(webhookResponse.getEventType())) {
                log.error("[PancakeWebhookServiceImpl.processWebhook] Invalid webhook payload: {}", body);
                return false;
            }

            log.info("[PancakeWebhookServiceImpl.processWebhook] Parsed webhook: {}", webhookResponse.getType());

            // 3. Xử lý từng loại webhook
            return handleEvent(posId, webhookResponse);

        } catch (Exception e) {
            log.error("[PancakeWebhookServiceImpl.processWebhook] Process webhook failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean handleEvent(String posId, PancakeWebhookResponse response) {
        PancakeEvent event = PancakeEvent.fromValue(response.getEventType());

        return switch (event) {
            case ORDER_ADD -> handleOrderWebhook(posId, response);
            case ORDER_UPDATE -> handleOrderWebhook(posId, response);
//            case ORDER_DELETE -> handleOrderWebhook(posId, response); luc nao cx update
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
    private boolean handleOrderWebhook(String posId, PancakeWebhookResponse webhookResponse) {
        PancakeOrderResponse.DataItem orderData = objectMapper.convertValue(
                webhookResponse, PancakeOrderResponse.DataItem.class
        );
        log.info("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Update OrderData: {}", orderData);

        if (Objects.isEmpty(orderData) || Objects.isEmpty(orderData.getId())) {
            log.error("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Failed to update order data: {}", orderData);
            return false;
        }

        // Convert OrderEntity
        OrderEntity orderEntity = convertToOrderEntity(posId, orderData);
        if (Objects.isEmpty(orderEntity)) {
            log.error("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Failed to convert orderData={} to OrderEntity", orderData.getId());
            return false;
        }

        // Save (insert/update)
        orderRepository.save(orderEntity);

        // if delete
        if(webhookResponse.getStatus()==6 || webhookResponse.getStatus()==7){
            return true;
        }
        log.info("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Successfully updated OrderEntity with id={} and code={}",
                orderEntity.getOrderId(), orderEntity.getOrderCode());

        // Sync OrderItems
        List<OrderItemEntity> orderItemEntities = convertToOrderItemEntity(orderData);
        if (orderItemEntities.isEmpty()) {
            log.warn("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Order id={} has no products", orderEntity.getOrderId());
            return false;
        } else {
            // Xóa items cũ để tránh dữ liệu thừa
            orderItemRepository.deleteByOrderId(orderEntity.getOrderId());

            // Save lại items mới
            orderItemRepository.saveAll(orderItemEntities);
            log.info("[NhanhvnWebhookServiceImpl.handleOrderUpdate] Successfully synced {} OrderItemEntities for orderId={}",
                    orderItemEntities.size(), orderEntity.getOrderId());
        }

        return true;
    }


    public OrderEntity convertToOrderEntity(String posId, PancakeOrderResponse.DataItem apiOrders) {
        String status = Optional.ofNullable(apiOrders.getStatus())
                .map(code -> pancakeConfig.getOrder().getStatus().getMapping()
                        .getOrDefault(code, Status.UNKNOWN.getValue()))
                .orElse(apiOrders.getStatusName());

        String paymentMethod = Optional.ofNullable(apiOrders.getPaymentPurchaseHistories())
                .filter(histories -> !histories.isEmpty())
                .map(histories -> histories.get(0).getType())
                .orElse(Status.UNKNOWN.getValue());

        String orderCode = Optional.ofNullable(apiOrders.getPartner())
                .map(PancakeOrderResponse.Partner::getExtendCode)
                .orElse(Status.UNKNOWN.getValue());

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
                .status(status.toUpperCase())
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
                    .sku(product.getVariationId())
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
