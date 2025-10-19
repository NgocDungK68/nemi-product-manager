package com.nemi.service_impl.pancake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.configuration.PancakeConfig;
import com.nemi.constant.WebhookConstants;
import com.nemi.entity.OrderEntity;
import com.nemi.entity.OrderItemEntity;
import com.nemi.entity.WebhookHistoryEntity;
import com.nemi.enums.PancakeEvent;
import com.nemi.enums.PosName;
import com.nemi.model.request.pancake.PancakeWebhookRequest;
import com.nemi.model.response.pancake.PancakeOrderResponse;
import com.nemi.repository.OrderItemRepository;
import com.nemi.repository.OrderRepository;
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

            // 2. Parse JSON về model
            PancakeWebhookRequest webhookRequest = JsonUtils.map(body, PancakeWebhookRequest.class);
            webhookHistory.setBody(JsonUtils.toJson(webhookRequest));

            if (Objects.isEmpty(webhookRequest) || Objects.isEmpty(webhookRequest.getEventType())) {
                log.error("[PancakeWebhookServiceImpl.processWebhook] Invalid webhook payload: {}", body);
                return false;
            }

            log.info("[PancakeWebhookServiceImpl.processWebhook] Parsed webhook: {}", webhookRequest.getType());

            // 3. Xử lý từng loại webhook
            boolean isSuccess = handleEvent(posId, webhookRequest, webhookHistory);
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

    private boolean handleEvent(String posId, PancakeWebhookRequest request, WebhookHistoryEntity webhookHistory) {
        PancakeEvent event = PancakeEvent.fromValue(request.getEventType());
        if (ObjectUtils.isEmpty(event.getEventType())) {
            log.warn("[PancakeWebhookServiceImpl.handleEvent] Unhandled webhook event: {}", request.getEventType());
            return false;
        }
        webhookHistory.setSyncType(event.getSyncType());
        webhookHistory.setEventType(event.getEventType());

        return switch (event) {
            case ORDER_ADD -> handleOrderWebhook(posId, request);
            case ORDER_UPDATE -> handleOrderWebhook(posId, request);
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
    private boolean handleOrderWebhook(String posId, PancakeWebhookRequest webhookResponse) {
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

        // if delete
        if (webhookResponse.getStatus() == 6 || webhookResponse.getStatus() == 7) {
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
