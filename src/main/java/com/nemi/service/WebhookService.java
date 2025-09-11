package com.nemi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.model.request.webhook.OrderWebhook;
import com.nemi.model.request.webhook.WebhookRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Xử lý webhook request từ Nhanh.vn
     */
    public void processWebhook(WebhookRequest webhookRequest) {
        String eventType = webhookRequest.getEvent();
        Long businessId = webhookRequest.getBusinessId();

        logger.info("Processing webhook - Event: {}, BusinessId: {}", eventType, businessId);

        try {
            switch (eventType) {
                case "webhooksEnabled":
                    handleWebhooksEnabled(webhookRequest);
                    break;

                case "orderAdd":
                    handleOrderAdd(webhookRequest);
                    break;

                case "orderUpdate":
                    handleOrderUpdate(webhookRequest);
                    break;

                case "orderDelete":
                    handleOrderDelete(webhookRequest);
                    break;

                case "productAdd":
                    handleProductAdd(webhookRequest);
                    break;

                case "productUpdate":
                    handleProductUpdate(webhookRequest);
                    break;

                case "productDelete":
                    handleProductDelete(webhookRequest);
                    break;

                case "inventoryChange":
                    handleInventoryChange(webhookRequest);
                    break;

                case "paymentReceived":
                    handlePaymentReceived(webhookRequest);
                    break;

                default:
                    logger.warn("Unknown webhook event: {}", eventType);
                    handleUnknownEvent(webhookRequest);
            }

        } catch (Exception e) {
            logger.error("Error processing webhook event: {}", eventType, e);
            throw new RuntimeException("Failed to process webhook", e);
        }
    }

    /**
     * Xử lý khi webhooks được bật
     */
    private void handleWebhooksEnabled(WebhookRequest request) {
        logger.info("Webhooks enabled confirmation received");
        logger.debug("Webhook data: {}", request.getData());
    }

    /**
     * Xử lý đơn hàng mới - CHỈ ĐỂ TEST LOGGING
     */
    private void handleOrderAdd(WebhookRequest request) {
        logger.info("=== NEW ORDER WEBHOOK RECEIVED ===");
        logger.info("Processing new order for BusinessId: {}", request.getBusinessId());

        try {
            // Convert webhook data to OrderWebhook object
            OrderWebhook orderData = objectMapper.convertValue(request.getData(), OrderWebhook.class);

            logger.info("=== ORDER DETAILS ===");
            logger.info("OrderId: {}", orderData.getOrderId());
            logger.info("Customer Name: {}", orderData.getCustomerName());
            logger.info("Customer Mobile: {}", orderData.getCustomerMobile());
            logger.info("Customer Email: {}", orderData.getCustomerEmail());
            logger.info("Customer Address: {}", orderData.getCustomerAddress());
            logger.info("Order Status: {}", orderData.getStatus());
            logger.info("Total Price: {}", orderData.getTotalPrice());

            // Log product details
            if (orderData.getProducts() != null && !orderData.getProducts().isEmpty()) {
                logger.info("=== ORDER PRODUCTS ({} items) ===", orderData.getProducts().size());
                for (int i = 0; i < orderData.getProducts().size(); i++) {
                    OrderWebhook.ProductItem product = orderData.getProducts().get(i);
                    logger.info("Product {}: ID={}, Name={}, Quantity={}, Price={}",
                            i + 1, product.getProductId(), product.getProductName(),
                            product.getQuantity(), product.getPrice());
                }
            } else {
                logger.warn("No products found in order!");
            }

            logger.info("=== ORDER PROCESSING COMPLETED ===");
            logger.info("Order {} has been logged successfully for BusinessId: {}",
                    orderData.getOrderId(), request.getBusinessId());

        } catch (IllegalArgumentException e) {
            logger.error("Invalid order data format in webhook: {}", e.getMessage());
            logger.error("Raw webhook data: {}", request.getData());
            throw new RuntimeException("Invalid order data format", e);
        } catch (Exception e) {
            logger.error("Failed to process new order webhook for BusinessId: {}",
                    request.getBusinessId(), e);
            logger.error("Raw webhook data: {}", request.getData());
            throw new RuntimeException("Failed to process new order webhook", e);
        }
    }

    /**
     * Xử lý cập nhật đơn hàng
     */
    private void handleOrderUpdate(WebhookRequest request) {
        logger.info("Order updated for BusinessId: {}", request.getBusinessId());
        logger.debug("Order update data: {}", request.getData());
    }

    /**
     * Xử lý xóa đơn hàng
     */
    private void handleOrderDelete(WebhookRequest request) {
        logger.info("Order deleted for BusinessId: {}", request.getBusinessId());
        logger.debug("Order delete data: {}", request.getData());
    }

    /**
     * Xử lý sản phẩm mới
     */
    private void handleProductAdd(WebhookRequest request) {
        logger.info("New product added for BusinessId: {}", request.getBusinessId());
        logger.debug("Product add data: {}", request.getData());
    }

    /**
     * Xử lý cập nhật sản phẩm
     */
    private void handleProductUpdate(WebhookRequest request) {
        logger.info("Product updated for BusinessId: {}", request.getBusinessId());
        logger.debug("Product update data: {}", request.getData());
    }

    /**
     * Xử lý xóa sản phẩm
     */
    private void handleProductDelete(WebhookRequest request) {
        logger.info("Product deleted for BusinessId: {}", request.getBusinessId());
        logger.debug("Product delete data: {}", request.getData());
    }

    /**
     * Xử lý thay đổi tồn kho
     */
    private void handleInventoryChange(WebhookRequest request) {
        logger.info("Inventory changed for BusinessId: {}", request.getBusinessId());
        logger.debug("Inventory change data: {}", request.getData());
    }

    /**
     * Xử lý nhận thanh toán
     */
    private void handlePaymentReceived(WebhookRequest request) {
        logger.info("Payment received for BusinessId: {}", request.getBusinessId());
        logger.debug("Payment data: {}", request.getData());
    }

    /**
     * Xử lý event không xác định
     */
    private void handleUnknownEvent(WebhookRequest request) {
        logger.warn("Received unknown event: {} from BusinessId: {}",
                request.getEvent(), request.getBusinessId());
        logger.debug("Unknown event data: {}", request.getData());
    }
}