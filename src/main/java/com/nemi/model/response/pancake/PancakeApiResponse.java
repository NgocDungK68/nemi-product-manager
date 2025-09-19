package com.nemi.model.response.pancake;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class PancakeApiResponse {
    private boolean success;
    private OrderData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderData {
        private String id;
        @JsonProperty("system_id")
        private String systemId;
        @JsonProperty("status_name")
        private String statusName;
        @JsonProperty("total_price")
        private Double totalPrice;
        @JsonProperty("shipping_fee")
        private Double shippingFee;
        @JsonProperty("total_discount")
        private Double totalDiscount;
        @JsonProperty("inserted_at")
        private String insertedAt;
        @JsonProperty("updated_at")
        private String updatedAt;
        @JsonProperty("bill_phone_number")
        private String billPhoneNumber;
        private Customer customer;
        @JsonProperty("shipping_address")
        private ShippingAddress shippingAddress;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Customer {
        private String name;
        private java.util.List<String> emails;
        @JsonProperty("phone_numbers")
        private java.util.List<String> phoneNumbers;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ShippingAddress {
        @JsonProperty("full_address")
        private String fullAddress;
    }
}