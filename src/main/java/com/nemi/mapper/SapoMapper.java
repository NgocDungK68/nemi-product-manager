package com.nemi.mapper;

import com.nemi.configuration.SapoConfig;
import com.nemi.entity.OrderEntity;
import com.nemi.entity.OrderItemEntity;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.nemi.model.response.sapo.SapoOrderResponse;
import com.nemi.model.response.sapo.SapoProductResponse;
import com.nemi.util.JsonUtils;
import com.nemi.utils.PosUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SapoMapper {
    private final SapoConfig sapoConfig;

    public List<ProductEntity> convertToProductEntities(String posId, List<SapoProductResponse.Product> apiProducts, String username) {
        return apiProducts.stream()
                .map(apiProduct -> {
                    ProductEntity productEntity = convertToProductEntity(posId, apiProduct, username);
                    if (ObjectUtils.isNotEmpty(productEntity)) {
                        productEntity.setCreatedBy(username);
                        productEntity.setUpdatedBy(username);
                    }
                    return productEntity;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public ProductEntity convertToProductEntity(String posId, SapoProductResponse.Product apiProduct, String username) {
        ProductEntity product = new ProductEntity();

        product.setPosId(posId);
        product.setProductId(String.valueOf(apiProduct.getId()));
        // Set code from first variant's SKU
        product.setCode(null);
        product.setName(apiProduct.getName());
        product.setDescription(apiProduct.getContent());
        product.setBrand(apiProduct.getVendor());
        product.setCategory(apiProduct.getProductType());
        product.setStatus(apiProduct.getStatus().toUpperCase());
        product.setImages(JsonUtils.toJson(apiProduct.getImages().stream()
                .map(SapoProductResponse.Image::getSrc) // Dùng method reference
                .collect(Collectors.toList())));

        // Set timestamps - parse from string format
        if (ObjectUtils.isNotEmpty(apiProduct.getCreatedOn())) {
            product.setCreatedAt(PosUtils.parseDateTime(apiProduct.getCreatedOn()));
        }
        if (ObjectUtils.isNotEmpty(apiProduct.getModifiedOn())) {
            product.setUpdatedAt(PosUtils.parseDateTime(apiProduct.getModifiedOn()));
        }

        product.setCreatedBy(username);
        product.setUpdatedBy(username);

        return product;
    }

    public List<ProductVariantEntity> convertToVariantEntities(String posId, String productId, List<SapoProductResponse.Variant> apiVariants, String username) {
        return apiVariants.stream()
                .map(apiVariant -> convertToVariantEntity(posId, productId, apiVariant, username))
                .collect(Collectors.toList());
    }

    public ProductVariantEntity convertToVariantEntity(String posId, String productId, SapoProductResponse.Variant apiVariant, String username) {
        ProductVariantEntity variant = new ProductVariantEntity();

        // Required fields
        variant.setVariantId(String.valueOf(apiVariant.getId()));
        variant.setPosId(posId);
        variant.setProductId(productId);

        // Handle nullable fields with defaults
        variant.setSku(apiVariant.getSku());
        variant.setBarcode(apiVariant.getBarcode());

        // Parse price
        if (apiVariant.getPrice() != null) {
            try {
                variant.setPrice(BigDecimal.valueOf(apiVariant.getPrice()));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse price: {}", apiVariant.getPrice());
                variant.setPrice(BigDecimal.ZERO);
            }
        } else {
            variant.setPrice(BigDecimal.ZERO);
        }

        variant.setCcy("VND"); // Default currency
        variant.setInventoryQuantity(apiVariant.getInventoryQuantity() != null ? apiVariant.getInventoryQuantity() : 0);

        //        Cách tiếp cận chính xác (Nâng cao):
        //        Để tính toán chính xác fulfillable_quantity, bạn cần một logic phức tạp hơn:
        //        Lấy inventory_quantity từ API sản phẩm.
        //        Sử dụng API Đơn hàng (GET /admin/orders.json) để lấy danh sách các đơn hàng có trạng thái unfulfilled (chưa hoàn thành).
        //        Duyệt qua các đơn hàng đó, cộng tổng số lượng của sản phẩm (SKU) bạn đang xétt
        //        Lấy inventory_quantity trừ đi tổng số lượng vừa tính được để ra fulfillable_quantity.
        variant.setFulfillableQuantity(null);
        variant.setWeight(apiVariant.getWeight() != null ? apiVariant.getWeight() : 0.0);
        variant.setWeightUnit(apiVariant.getWeightUnit() != null ? apiVariant.getWeightUnit() : "kg");

        // Convert attributes to JSON
        Map<String, String> attributes = getStringStringMap(apiVariant);
        variant.setAttributes(JsonUtils.toJson(attributes));

        // Warehouse quantities - for now empty, can be extended later
        variant.setWarehouseQuantities("{}");

        variant.setCreatedBy(username);
        variant.setUpdatedBy(username);

        return variant;
    }

    private Map<String, String> getStringStringMap(SapoProductResponse.Variant apiVariant) {
        Map<String, String> attributes = new HashMap<>();
        if (ObjectUtils.isNotEmpty(apiVariant.getOption1())) {
            attributes.put("option1", apiVariant.getOption1());
        }
        if (ObjectUtils.isNotEmpty(apiVariant.getOption2())) {
            attributes.put("option2", apiVariant.getOption2());
        }
        if (ObjectUtils.isNotEmpty(apiVariant.getOption3())) {
            attributes.put("option3", apiVariant.getOption3());
        }
        return attributes;
    }

    public List<OrderEntity> convertToOrderEntities(String posId, List<SapoOrderResponse.Order> apiOrders, String username) {
        return apiOrders.stream()
                .map(orders -> convertToOrderEntity(posId, orders, username))
                .filter(Objects::nonNull)
                .toList();
    }

    public OrderEntity convertToOrderEntity(String posId, SapoOrderResponse.Order order, String username) {
        String status = sapoConfig.getStatusMapping(order.getStatus());

        // Lấy fulfillment và origin address một cách đơn giản
        SapoOrderResponse.Fulfillment fulfillment = getFirstFulfillment(order);
        SapoOrderResponse.OriginAddress origin = Optional.ofNullable(fulfillment)
                .map(SapoOrderResponse.Fulfillment::getOriginAddress)
                .orElse(null);

        return OrderEntity.builder()
                .posId(posId)
                .orderId(order.getId().toString())
                .orderCode(order.getName())
                .customerName(Optional.ofNullable(origin).map(SapoOrderResponse.OriginAddress::getName).orElse(null))
                .customerPhone(Optional.ofNullable(origin).map(SapoOrderResponse.OriginAddress::getPhone).orElse(null))
                .customerEmail(Optional.ofNullable(origin).map(SapoOrderResponse.OriginAddress::getEmail).orElse(null))
                .shippingAddress(buildShippingAddress(origin))
                .status(status.toUpperCase())
                .paymentMethod(getFirstPaymentMethod(order))
                .shippingMethod(Optional.ofNullable(fulfillment).map(SapoOrderResponse.Fulfillment::getDeliveryMethod).orElse(null))
                .totalPrice(order.getTotalPrice())
                .shippingFee(BigDecimal.ZERO)
                .discountAmount(order.getTotalDiscounts())
                .createdBy(username)
                .updatedBy(username)
                .build();
    }

    private SapoOrderResponse.Fulfillment getFirstFulfillment(SapoOrderResponse.Order order) {
        return order.getFulfillments() != null && !order.getFulfillments().isEmpty()
                ? order.getFulfillments().get(0)
                : null;
    }

    private String buildShippingAddress(SapoOrderResponse.OriginAddress origin) {
        if (origin == null) return null;

        StringBuilder address = new StringBuilder();
        if (ObjectUtils.isNotEmpty(origin.getAddress1())) {
            address.append(origin.getAddress1());
        }
        if (ObjectUtils.isNotEmpty(origin.getCity())) {
            if (address.length() > 0) address.append(", ");
            address.append(origin.getCity());
        }
        if (ObjectUtils.isNotEmpty(origin.getProvince())) {
            if (address.length() > 0) address.append(", ");
            address.append(origin.getProvince());
        }
        return address.toString();
    }

    private String getFirstPaymentMethod(SapoOrderResponse.Order order) {
        return Optional.ofNullable(order.getPaymentGatewayNames())
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);
    }

    public List<OrderItemEntity> convertToOrderItemEntities(List<SapoOrderResponse.Order> apiOrders, String username) {
        return apiOrders.stream()
                .flatMap(apiOrder -> convertToOrderItemEntity(apiOrder, username).stream())
                .peek(entity -> entity.setCreatedBy(username))
                .toList();
    }

    public List<OrderItemEntity> convertToOrderItemEntity(SapoOrderResponse.Order apiOrder, String username) {
        return apiOrder.getLineItems().stream()
                .map(lineItem -> OrderItemEntity.builder()
                        .orderItemId(String.valueOf(lineItem.getId()))
                        .orderId(apiOrder.getId().toString())
                        .sku(lineItem.getSku())
                        .variantName(lineItem.getVariantTitle())
                        .quantity(lineItem.getQuantity())
                        .price(lineItem.getPrice())
                        .totalPrice(lineItem.getPrice().multiply(BigDecimal.valueOf(lineItem.getQuantity())))
                        .productName(lineItem.getTitle())
                        .fulfillableQuantity(lineItem.getCurrentQuantity())
                        .createdBy(username)
                        .updatedBy(username)
                        .build())
                .collect(Collectors.toList());
    }
}