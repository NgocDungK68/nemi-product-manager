package com.nemi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Entity
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "product_manager", name = "orders")
public class OrderEntity extends BaseEntity {

    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty("order_id")
    private String  orderId;

    @JsonProperty("order_code")
    private String orderCode;

    @JsonProperty("pos_id")
    private String posId;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_phone")
    private String customerPhone;

    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("shipping_address")
    private String shippingAddress;

    @JsonProperty("status")
    private String status;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("shipping_method")
    private String shippingMethod;

    @JsonProperty("total_price")
    private BigDecimal totalPrice;

    @JsonProperty("shipping_fee")
    private BigDecimal shippingFee;

    @JsonProperty("discount_amount")
    private Double discountAmount;

}


