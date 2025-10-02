package com.nemi.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "products", schema = "product_manager")
public class ProductEntity extends BaseEntity{
    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(name = "posId")
    private String posId;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @Column(name = "brand")
    private String brand;

    @Column(name = "category")
    private String category;

    @Column(name = "status")
    private String status;

    @Column(name = "images", columnDefinition = "TEXT")
    private String images;

}
