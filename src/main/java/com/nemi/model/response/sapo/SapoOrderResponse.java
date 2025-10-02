package com.nemi.model.response.sapo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SapoOrderResponse {
    
    @JsonProperty("orders")
    private List<SapoOrder> orders;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SapoOrder {
        private Long id; //orderID
        private LocalDateTime closedOn;
        @JsonProperty("created_on")
        private LocalDateTime createdOn; //created_at
        @JsonProperty("modified_on")
        private LocalDateTime modifiedOn; //updated_at
        @JsonProperty("total_price")
        private BigDecimal totalPrice;//total_price
        @JsonProperty("total_discounts")
        private BigDecimal totalDiscounts; //discount_amount
        private String name; //code
        private List<String> paymentGatewayNames; //payment_method(bang chua cong thanh toan)
        private String tags;
        private String contactEmail;
        @JsonProperty("line_items")
        private List<SapoLineItem> lineItems;
        @JsonProperty("shipping_lines")
        private ShippingLine shippingLines;
        @JsonProperty("billing_address")
        private SapoAddress billingAddress;
        @JsonProperty("shipping_address")
        private SapoAddress shippingAddress;
        private List<Fulfillment> fulfillments;
        private SapoCustomer customer;

    }

    // Supporting classes
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SapoLineItem {
        private Long id;
        private Long variantId;
        private String title;
        private Integer quantity;
        private BigDecimal price;
        private Integer grams;
        private String sku;
        private String variantTitle;
        private String vendor;
        private String fulfillmentService;
        private Long productId;
        private Boolean requiresShipping;
        private Boolean taxable;
        private Boolean giftCard;
        private String name;
        private String variantInventoryManagement;
        private List<Property> properties;
        private Boolean productExists;
        private Integer fulfillableQuantity;
        private BigDecimal totalDiscount;
        private String fulfillmentStatus;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SapoAddress {
        private String firstName;
        private String address1; //customer_address
        private String phone; //customer_phone
        private String city;
        private String zip;
        private String province;
        private String country;
        private String lastName;
        private String address2;
        private String company;
        private Double latitude;
        private Double longitude;
        private String name; //customer_name
        private String countryCode;
        private String provinceCode;
        private String countryName;
        private Boolean defaultAddress;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SapoCustomer {
        private Long id;
        private String email;
        private Boolean acceptsMarketing;
        private LocalDateTime createdOn;
        private LocalDateTime modifiedOn;
        private String firstName;
        private String lastName;
        private Integer ordersCount;
        private String state;
        private BigDecimal totalSpent;
        private Long lastOrderId;
        private String note;
        private Boolean verifiedEmail;
        private String multipassIdentifier;
        private Boolean taxExempt;
        private String tags;
        private String lastOrderName;
        private SapoAddress defaultAddress;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Property {
        private String name;
        private String value;

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
    public static class Fulfillment {
        private Long id;
        private Long orderId;
        private String status;
        private LocalDateTime createdOn;
        private String service;
        private LocalDateTime modifiedOn;
        private String trackingCompany;
        private String trackingNumber;
        private List<String> trackingNumbers;
        private String trackingUrl;
        private List<String> trackingUrls;
        private Receipt receipt;
        private List<SapoLineItem> lineItems;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Receipt {
        private Boolean testcase;
        private String authorization;

    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundLineItem {
        private Long id;
        private Integer quantity;
        private Long lineItemId;
        private SapoLineItem lineItem;

    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderAdjustment {
        private Long id;
        private Long orderId;
        private Long refundId;
        private BigDecimal amount;
        private BigDecimal taxAmount;
        private String kind;
        private String reason;
    }
}
