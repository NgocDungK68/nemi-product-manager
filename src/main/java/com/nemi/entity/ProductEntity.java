package com.nemi.entity;

import com.nemi.constant.enums.PosName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "products")
public class ProductEntity {
    // Pancake POS: id
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    // Pancake POS: sku (nếu không có biến thể)
    @Column(name = "sku", length = 100)
    private String sku;

    // Pancake POS: title
    @Column(name = "title", length = 300, nullable = false)
    private String title;

    // Pancake POS: description
    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Pancake POS: categoryid
    @Column(name = "category_id", length = 64)
    private String categoryId;

    // Pancake POS: status
    @Column(name = "status")
    private Integer status;

    // Sheet ghi brand có thể là custom field (tùy chọn)
    @Column(name = "brand_name", length = 200)
    private String brandName;

    // API metadata: created_at
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // API metadata: updated_at
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Pancake POS: images[] -> bảng con
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImageEntity> images = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 50, nullable = false)
    private PosName posName;
}