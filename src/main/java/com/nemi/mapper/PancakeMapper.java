package com.nemi.mapper;

import com.nemi.configuration.PancakeConfig;
import com.nemi.entity.OrderEntity;
import com.nemi.entity.OrderItemEntity;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.nemi.enums.Status;
import com.nemi.enums.WeightUnit;
import com.nemi.model.response.pancake.PancakeOrderResponse;
import com.nemi.model.response.pancake.PancakeProductResponse;
import com.nemi.service.GeneralPosService;
import com.nemi.utils.PosUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PancakeMapper {
    private final PancakeConfig pancakeConfig;

    public List<ProductEntity> convertToProductEntities( // return btg
                                                         String posId,
                                                         List<PancakeProductResponse.ProductData> apiProducts, String userName, String departmentId
    ) {
        return apiProducts.stream()  // hoặc parallelStream()
                .map(apiProduct -> convertToProductEntity(posId, apiProduct, userName, departmentId))
                .collect(Collectors.toList());
    }

    public ProductEntity convertToProductEntity(String posId, PancakeProductResponse.ProductData apiProducts, String userName, String departmentId) {

        String images = Optional.ofNullable(apiProducts.getImages())
                .map(list -> list.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(",")))
                .orElse(null);

        String categories = Optional.ofNullable(apiProducts.getProduct())
                .map(PancakeProductResponse.Product::getCategories)
                .map(list -> list.stream()
                        .filter(Objects::nonNull)
                        .map(PancakeProductResponse.Category::getName)
                        .collect(Collectors.joining(",")))
                .orElse(null);


        LocalDateTime insertedAt = PosUtils.pancakeParseTime(apiProducts.getInsertedAt());


        ProductEntity product = ProductEntity.builder()
                .posId(posId)
                .code(apiProducts.getProduct().getDisplayId())
                .name(apiProducts.getProduct().getName())
                .productId(apiProducts.getProductId())
                .description(apiProducts.getProduct().getNoteProduct())
                .images(images)
                .category(categories)
                .createdBy(userName)
                .departmentId(departmentId)
                .createdAt(insertedAt)
                .build();
        if (Boolean.TRUE.equals(apiProducts.getIsLocked())) {
            product.setStatus(Status.INACTIVE.getValue());
        } else {
            product.setStatus(Status.ACTIVE.getValue());
        }
        return product;
    }

    public List<ProductVariantEntity> convertToVariantEntities(
            String posId,
            List<PancakeProductResponse.ProductData> apiProducts, String userName
    ) {
        return apiProducts.stream()  // can nhac paralle stream
                .map(apiProduct -> convertToVariantEntity(posId, apiProduct, userName))
                .collect(Collectors.toList());
    }


    public ProductVariantEntity convertToVariantEntity(String posId, PancakeProductResponse.ProductData apiProduct, String userName) {

        Integer remainQuantity = Optional.ofNullable(apiProduct.getVariationsWarehouses())
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(PancakeProductResponse.VariationWarehouse::getRemainQuantity)
                .orElse(null);

        LocalDateTime insertedAt = PosUtils.pancakeParseTime(apiProduct.getInsertedAt());


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
                .createdAt(insertedAt)
                .createdBy(userName)
                .build();
    }

    public List<OrderEntity> convertToOrderEntities(String posId, List<PancakeOrderResponse.DataItem> apiOrders, String userName, String departmentId) {
        return apiOrders.stream()
                .map(orders -> convertToOrderEntity(posId, orders, userName, departmentId))
                .filter(Objects::nonNull)
                .toList();
    }

    public OrderEntity convertToOrderEntity(String posId, PancakeOrderResponse.DataItem apiOrders, String userName, String departmentId) {
        String status = pancakeConfig.getStatusMapping(apiOrders.getStatus(), apiOrders.getStatusName());
        log.info("status of orderId {} is {}", apiOrders.getId(), status);

        String paymentMethod = Optional.ofNullable(apiOrders.getPaymentPurchaseHistories())
                .filter(histories -> !histories.isEmpty())
                .map(histories -> histories.get(0).getType())
                .orElse(null);

        String orderCode = Optional.ofNullable(apiOrders.getPartner())
                .map(PancakeOrderResponse.Partner::getExtendCode)
                .orElse(null);

        String saleId = Optional.ofNullable(apiOrders.getMarketer())
                .map(marketer -> String.valueOf(marketer.getId()))
                .orElse(null);

        LocalDateTime insertedAt =  PosUtils.pancakeParseTime(apiOrders.getInsertedAt());
        LocalDateTime updatedAt =  PosUtils.pancakeParseTime(apiOrders.getUpdatedAt());


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
                .customerEmail(apiOrders.getBillEmail())
                .discountAmount(apiOrders.getTotalDiscount())
                .status(status)
                .saleId(saleId)
                .createdAt(insertedAt)
                .updatedAt(updatedAt)
                .createdBy(userName)
                .departmentId(departmentId)
                .build();
    }

    public List<OrderItemEntity> convertToOrderItemEntities(List<PancakeOrderResponse.DataItem> apiOrders, String userName) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for (PancakeOrderResponse.DataItem orderData : apiOrders) {
            orderItemEntities.addAll(convertToOrderItemEntity(orderData, userName));
        }
        return orderItemEntities;
    }

    public List<OrderItemEntity> convertToOrderItemEntity(PancakeOrderResponse.DataItem apiOrder, String userName) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for (PancakeOrderResponse.Item product : apiOrder.getItems()) {

            LocalDateTime insertedAt =  PosUtils.pancakeParseTime(apiOrder.getInsertedAt());
            LocalDateTime updatedAt =  PosUtils.pancakeParseTime(apiOrder.getUpdatedAt());

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
                    .createdBy(userName)
                    .createdAt(insertedAt)
                    .updatedAt(updatedAt)
                    .build());
        }
        return orderItemEntities;
    }
}
