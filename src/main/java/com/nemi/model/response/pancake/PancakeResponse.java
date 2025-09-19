package com.nemi.model.response.pancake;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PancakeResponse {
    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("order_code")
    private String orderCode;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_phone")
    private String customerPhone;

    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("shipping_address")
    private String shippingAddress;

    private String status;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("shipping_method")
    private String shippingMethod;

    @JsonProperty("total_price")
    private Double totalPrice;

    @JsonProperty("shipping_fee")
    private Double shippingFee;

    @JsonProperty("discount_amount")
    private Double discountAmount;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}