package com.nemi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "product_manager", name = "order_item")
public class OrderItemEntity extends BaseEntity {

    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty("order_item_id")
    private String orderItemId;   // ID của order_item (primary key)

    @JsonProperty("order_id")
    private String  orderId;       // FK tới Order

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("variant_name")
    private String variantName;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("total_price")
    private BigDecimal totalPrice;

    @JsonProperty("fulfillable_quantity")
    private Integer fulfillableQuantity;
}
