package com.nemi.mapper;

import com.nemi.configuration.NhanhvnConfig;
import com.nemi.constant.NhanhvnConstants;
import com.nemi.entity.OrderEntity;
import com.nemi.entity.OrderItemEntity;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.nemi.enums.WeightUnit;
import com.nemi.model.request.nhanhvn.NhanhvnOrderWebhookRequest;
import com.nemi.model.response.nhanhvn.NhanhvnOrderResponse;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class NhanhvnMapper {
    private final NhanhvnConfig nhanhvnConfig;

    public List<ProductEntity> convertToProductEntities(String posId, List<NhanhvnProductResponse.ProductData> apiProducts, String username) {
        return apiProducts.stream()
                .map(apiProduct -> {
                    ProductEntity productEntity = convertToProductEntity(posId, apiProduct,  username);
                    if (ObjectUtils.isNotEmpty(productEntity)) productEntity.setCreatedBy(username);
                    return productEntity;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public ProductEntity convertToProductEntity(String posId, NhanhvnProductResponse.ProductData apiProduct, String username) {
        if (!(apiProduct.getParentId()).equals(NhanhvnConstants.PARENT_PRODUCT)) return null;

        String status = nhanhvnConfig.getProductStatusMapping(apiProduct.getStatus());

        return ProductEntity.builder()
                .posId(posId)
                .productId(String.valueOf(apiProduct.getId()))
                .code(apiProduct.getCode())
                .name(apiProduct.getName())
                .status(status)
                .updatedBy(username)
                .build();
    }

    public List<ProductVariantEntity> convertToVariantEntities(String posId,
                                                                List<NhanhvnProductResponse.ProductData> apiProducts,
                                                                String username) {
        return apiProducts.stream()
                .map(apiProduct -> {
                    ProductVariantEntity variantEntity = convertToVariantEntity(posId, apiProduct,  username);
                    if (ObjectUtils.isNotEmpty(variantEntity)) variantEntity.setCreatedBy(username);
                    return variantEntity;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public ProductVariantEntity convertToVariantEntity(String posId,
                                                       NhanhvnProductResponse.ProductData apiProduct,
                                                       String username) {
        if ((apiProduct.getParentId()).equals(NhanhvnConstants.PARENT_PRODUCT)) return null;
        return ProductVariantEntity.builder()
                .variantId(String.valueOf(apiProduct.getId()))
                .posId(posId)
                .productId(String.valueOf(apiProduct.getParentId()))
                .sku(apiProduct.getCode())
                .barcode(apiProduct.getBarcode())
                .price(BigDecimal.valueOf(apiProduct.getPrices().getRetail()))
                .inventoryQuantity(apiProduct.getInventory().getRemain())
                .fulfillableQuantity(apiProduct.getInventory().getAvailable())
                .weight(apiProduct.getShipping().getWeight())
                .weightUnit(WeightUnit.GAM.getValue())
                .updatedBy(username)
                .build();
    }

    public List<OrderEntity> convertToOrderEntities(String posId,
                                                     List<NhanhvnOrderResponse.OrderData> apiOrders,
                                                     String username) {
        return apiOrders.stream()
                .map(apiOrder -> {
                    OrderEntity entity = convertToOrderEntity(posId, apiOrder, username);
                    entity.setCreatedBy(username);
                    return entity;
                })
                .toList();
    }

    public OrderEntity convertToOrderEntity(String posId, NhanhvnOrderResponse.OrderData apiOrder, String username) {
        String status = nhanhvnConfig.getOrderStatusMapping(apiOrder.getInfo().getStatus());

        return OrderEntity.builder()
                .posId(posId)
                .orderId(String.valueOf(apiOrder.getInfo().getId()))
                .orderCode(apiOrder.getCarrier().getCarrierCode())
                .customerName(apiOrder.getShippingAddress().getName())
                .customerEmail(apiOrder.getShippingAddress().getEmail())
                .customerPhone(apiOrder.getShippingAddress().getMobile())// khi user co du thi them custemer phone va email
                .shippingAddress(apiOrder.getShippingAddress().getAddress())
                .shippingMethod(apiOrder.getCarrier().getName())
                .paymentMethod(apiOrder.getPayment().getBusinessPayment().toString())
                .shippingFee(apiOrder.getCarrier().getShipFee())
                .totalPrice(totalProductPrice(apiOrder))
                .status(status)
                .saleId(String.valueOf(apiOrder.getInfo().getSaleId()))
                .updatedBy(username)
                .build();
    }

    public List<OrderItemEntity> convertToOrderItemEntities(List<NhanhvnOrderResponse.OrderData> apiOrders, String username) {
        return apiOrders.stream()
                .flatMap(apiOrder -> convertToOrderItemEntity(apiOrder, username).stream())
                .peek(entity -> entity.setCreatedBy(username))
                .toList();
    }

    public List<OrderItemEntity> convertToOrderItemEntity(NhanhvnOrderResponse.OrderData apiOrder, String username) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for (NhanhvnOrderResponse.Product product : apiOrder.getProducts()) {
            BigDecimal quantity = BigDecimal.valueOf(product.getQuantity());
            orderItemEntities.add(OrderItemEntity.builder()
                    .orderItemId(String.valueOf(product.getId()))
                    .orderId(apiOrder.getChannel().getAppOrderId())
                    .quantity(product.getQuantity())
                    .sku(product.getImeiId())
                    .price(product.getPrice())
                    .totalPrice(product.getPrice().multiply(quantity))
                    .productName(product.getName())
                    .updatedBy(username)
                    .build());
        }

        return orderItemEntities;
    }

    //---------------webhook order request---------------------
    public OrderEntity convertToOrderEntity(String posId, NhanhvnOrderWebhookRequest orderWebhookRequest, String username) {
        String status = nhanhvnConfig.getOrderStatusMapping(orderWebhookRequest.getInfo().getStatus());

        return OrderEntity.builder()
                .posId(posId)
                .orderId(String.valueOf(orderWebhookRequest.getInfo().getId()))
                .orderCode(orderWebhookRequest.getCarrier().getCarrierCode())
                .customerName(orderWebhookRequest.getCustomer().getName())
                .customerEmail(orderWebhookRequest.getCustomer().getEmail())
                .customerPhone(orderWebhookRequest.getCustomer().getMobile())// khi user co du thi them custemer phone va email
                .shippingAddress(orderWebhookRequest.getCustomer().getAddress())
                .shippingMethod(orderWebhookRequest.getCarrier().getName())
                .paymentMethod(orderWebhookRequest.getInfo().getPaymentMethod().toString())
                .totalPrice(totalProductPrice(orderWebhookRequest))
                .shippingFee(orderWebhookRequest.getCarrier().getShipFee())
                .discountAmount(orderWebhookRequest.getInfo().getDiscount())
                .status(status)
                .updatedBy(username)
                .build();
    }

    public List<OrderItemEntity> convertToOrderItemEntity(NhanhvnOrderWebhookRequest orderWebhookRequest, String username) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for (NhanhvnOrderWebhookRequest.Product product : orderWebhookRequest.getProducts()) {
            BigDecimal quantity = BigDecimal.valueOf(product.getQuantity());
            orderItemEntities.add(OrderItemEntity.builder()
                    .orderItemId(String.valueOf(product.getId()))
                    .orderId(String.valueOf(orderWebhookRequest.getInfo().getId()))
                    .quantity(product.getQuantity())
                    .sku(product.getCode())
                    .price(product.getPrice())
                    .totalPrice(product.getPrice().multiply(quantity))
                    .productName(product.getName())
                    .updatedBy(username)
                    .build());
        }

        return orderItemEntities;
    }


    private BigDecimal totalProductPrice(NhanhvnOrderResponse.OrderData apiOrders) {
        BigDecimal totalPrice = BigDecimal.valueOf(0);
        for (NhanhvnOrderResponse.Product product : apiOrders.getProducts()) {
            BigDecimal price = product.getPrice(); // BigDecimal
            BigDecimal vat = product.getVat().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal quantity = BigDecimal.valueOf(product.getQuantity());
            BigDecimal discount = product.getDiscount();

            BigDecimal lineTotal = price
                    .multiply(BigDecimal.ONE.add(vat))
                    .multiply(quantity)
                    .subtract(ObjectUtils.isNotEmpty(discount) ? discount : BigDecimal.ZERO);

            totalPrice = totalPrice.add(lineTotal);
        }
        return totalPrice.add(apiOrders.getCarrier().getShipFee());
    }

    private BigDecimal totalProductPrice(NhanhvnOrderWebhookRequest order) {
        BigDecimal totalPrice = BigDecimal.ZERO;

        if (ObjectUtils.isEmpty(order.getProducts())) {
            return totalPrice;
        }

        for (NhanhvnOrderWebhookRequest.Product product : order.getProducts()) {
            // Lấy giá, số lượng
            BigDecimal price = safeValue(product.getPrice());
            BigDecimal quantity = BigDecimal.valueOf(product.getQuantity());

            // Lấy thuế VAT (%)
            BigDecimal vatPercent = ObjectUtils.isNotEmpty(product.getVat()) ?
                    safeValue(product.getVat().getPercent()) : BigDecimal.ZERO;
            BigDecimal vatRate = vatPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            // Lấy discount amount (ưu tiên amount hơn percent)
            BigDecimal discountAmount = BigDecimal.ZERO;
            if (ObjectUtils.isNotEmpty(product.getDiscount())) {
                if (ObjectUtils.isNotEmpty(product.getDiscount().getAmount())) {
                    discountAmount = safeValue(product.getDiscount().getAmount());
                } else if (ObjectUtils.isNotEmpty(product.getDiscount().getPercent())) {
                    BigDecimal percent = safeValue(product.getDiscount().getPercent());
                    discountAmount = price.multiply(percent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                }
            }

            // Tổng cho từng dòng
            BigDecimal lineTotal = price
                    .multiply(BigDecimal.ONE.add(vatRate)) // áp VAT
                    .multiply(quantity)                     // nhân số lượng
                    .subtract(discountAmount)               // trừ giảm giá
                    .setScale(4, RoundingMode.HALF_UP);     // giữ 4 chữ số sau dấu phẩy

            totalPrice = totalPrice.add(lineTotal);
        }

        // Thêm phí vận chuyển (nếu có)
        if (ObjectUtils.isNotEmpty(order.getCarrier()) && ObjectUtils.isNotEmpty(order.getCarrier().getShipFee())) {
            totalPrice = totalPrice.add(safeValue(order.getCarrier().getShipFee()));
        }

        return totalPrice.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Tránh NullPointerException khi xử lý BigDecimal.
     */
    private BigDecimal safeValue(BigDecimal value) {
        return ObjectUtils.isNotEmpty(value) ?
                value.setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }
}
