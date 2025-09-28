package com.nemi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "product_variant", schema = "product_manager")
public class ProductVariantEntity {
    @Id
    @Column(name = "variant_id")
    private String variantId;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "sku")
    private String sku;

    @Column(name = "barcode")
    private String barcode;

    @Column(name = "price")
    private Double price;

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
