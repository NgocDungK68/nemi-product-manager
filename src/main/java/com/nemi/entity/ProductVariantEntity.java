package com.nemi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "product_variant", schema = "product_manager")
@IdClass(VariantId.class)
public class ProductVariantEntity extends BaseEntity {
    @Id
    @Column(name = "variant_id")
    private String variantId;

    @Id
    @Column(name = "pos_id")
    private String posId;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "sku")
    private String sku;

    @Column(name = "barcode")
    private String barcode;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "ccy")
    private String ccy;

    @Column(name = "inventory_quantity")
    private Integer inventoryQuantity;

    @Column(name = "fulfillable_quantity")
    private Integer fulfillableQuantity;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "weight_unit")
    private String weightUnit;

    @Column(name = "attributes", columnDefinition = "TEXT")
    private String attributes;

    @Column(name = "warehouse_quantities", columnDefinition = "TEXT")
    private String warehouseQuantities;
}
