package com.nemi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK về sản phẩm
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    // URL ảnh từ images[]
    @Column(name = "url", length = 1000, nullable = false)
    private String url;

    // Thứ tự hiển thị (tùy chọn)
    @Column(name = "sort_order")
    private Integer sortOrder;
}
