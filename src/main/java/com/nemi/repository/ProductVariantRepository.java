package com.nemi.repository;

import com.nemi.entity.ProductVariantEntity;
import com.nemi.entity.ProductVariantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, ProductVariantId> {
    
    List<ProductVariantEntity> findByProductId(String productId);
    
    void deleteByProductId(String productId);
    
    Optional<ProductVariantEntity> findByVariantId(String variantId);
    
    void deleteByVariantId(String variantId);
}