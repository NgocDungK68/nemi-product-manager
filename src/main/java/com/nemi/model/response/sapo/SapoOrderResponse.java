package com.nemi.model.response.sapo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SapoOrderResponse {

    private List<Order> orders;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Order {
        private Long id;
        @JsonProperty("buyer_accepts_marketing")
        private Boolean buyerAcceptsMarketing;
        @JsonProperty("cancel_reason")
        private String cancelReason;
        @JsonProperty("cancelled_on")
        private String cancelledOn;
        @JsonProperty("confirmed_on")
        private String confirmedOn;
        @JsonProperty("created_on")
        private String createdOn;
        private String currency;
        private String email;
        private String phone;
        @JsonProperty("user_id")
        private String userId;
        @JsonProperty("customer_group_id")
        private Long customerGroupId;
        @JsonProperty("fulfillment_status")
        private String fulfillmentStatus;
        @JsonProperty("financial_status")
        private String financialStatus;
        private String status;
        @JsonProperty("return_status")
        private String returnStatus;
        private String name;
        private String note;
        private Integer number;
        @JsonProperty("order_number")
        private Integer orderNumber;
        @JsonProperty("processed_on")
        private String processedOn;
        @JsonProperty("processing_method")
        private String processingMethod;
        @JsonProperty("source_name")
        private String sourceName;
        private String source;
        private String gateway;
        private String token;
        @JsonProperty("total_discounts")
        private BigDecimal totalDiscounts;
        @JsonProperty("shipping_lines")
        private List<ShippingLine> shippingLines;
        @JsonProperty("total_line_items_price")
        private BigDecimal totalLineItemsPrice;
        @JsonProperty("total_price")
        private BigDecimal totalPrice;
        @JsonProperty("total_weight")
        private BigDecimal totalWeight;
        private String tags;
        private User user;
        private User assignee;
        @JsonProperty("line_items")
        private List<LineItem> lineItems;
        private List<Fulfillment> fulfillments;
        @JsonProperty("payment_gateway_names")
        private List<String> paymentGatewayNames;
        @JsonProperty("total_shipping_price")
        private BigDecimal totalShippingPrice;
        @JsonProperty("total_tax")
        private BigDecimal totalTax;
        @JsonProperty("subtotal_price")
        private BigDecimal subtotalPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        private Long id;
        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("last_name")
        private String lastName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LineItem {
        private Long id;
        private BigDecimal price;
        @JsonProperty("total_discount")
        private BigDecimal totalDiscount;
        @JsonProperty("fulfillment_status")
        private String fulfillmentStatus;
        private Integer quantity;
        @JsonProperty("current_quantity")
        private Integer currentQuantity;
        @JsonProperty("product_id")
        private Long productId;
        @JsonProperty("variant_id")
        private Long variantId;
        private String name;
        private String title;
        @JsonProperty("variant_title")
        private String variantTitle;
        private String sku;
        private String vendor;
        @JsonProperty("discounted_unit_price")
        private BigDecimal discountedUnitPrice;
        @JsonProperty("discounted_total")
        private BigDecimal discountedTotal;
        @JsonProperty("original_total")
        private BigDecimal originalTotal;
        private Boolean taxable;
        private Boolean giftCard;
        private Boolean requiresShipping;
        private Boolean deleted;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fulfillment {
        private Long id;
        private String name;
        private String status;
        @JsonProperty("shipment_status")
        private String shipmentStatus;
        @JsonProperty("origin_address")
        private OriginAddress originAddress;
        @JsonProperty("tracking_info")
        private TrackingInfo trackingInfo;
        @JsonProperty("line_items")
        private List<FulfillmentLineItem> lineItems;
        @JsonProperty("delivery_method")
        private String deliveryMethod;
        @JsonProperty("shipping_label_slip_url")
        private String shippingLabelSlipUrl;
        @JsonProperty("package_category")
        private String packageCategory;
        @JsonProperty("shipment_category")
        private String shipmentCategory;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OriginAddress {
        private String name;
        private String email;
        private String phone;
        private String address1;
        private String province;
        private String city;
        private String country;
        @JsonProperty("country_code")
        private String countryCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackingInfo {
        @JsonProperty("tracking_company")
        private String trackingCompany;
        private String carrier;
        @JsonProperty("carrier_name")
        private String carrierName;
        @JsonProperty("tracking_number")
        private String trackingNumber;
        @JsonProperty("tracking_numbers")
        private List<String> trackingNumbers;
        @JsonProperty("tracking_url")
        private String trackingUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingLine {
        private String title; // shipping_method
        private BigDecimal price; //shipping_fee
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FulfillmentLineItem {
        private Long id;
        @JsonProperty("line_item_id")
        private Long lineItemId;
        private BigDecimal price;
        private BigDecimal totalDiscount;
        private String fulfillmentStatus;
        private Integer quantity;
        @JsonProperty("effective_quantity")
        private Integer effectiveQuantity;
        @JsonProperty("product_id")
        private Long productId;
        @JsonProperty("variant_id")
        private Long variantId;
        private String name;
        private String title;
        @JsonProperty("variant_title")
        private String variantTitle;
        @JsonProperty("product_title")
        private String productTitle;
        private String sku;
        private String vendor;
        @JsonProperty("discounted_unit_price")
        private BigDecimal discountedUnitPrice;
        @JsonProperty("discounted_total")
        private BigDecimal discountedTotal;
        @JsonProperty("original_total")
        private BigDecimal originalTotal;
        private Boolean taxable;
        private Boolean giftCard;
        @JsonProperty("requires_shipping")
        private Boolean requiresShipping;
        @JsonProperty("fulfillment_service")
        private String fulfillmentService;
    }
}
