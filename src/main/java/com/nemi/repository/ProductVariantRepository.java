package com.nemi.repository;

import com.nemi.entity.ProductVariantEntity;
import com.nemi.entity.VariantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, VariantId> {
    
    List<ProductVariantEntity> findByProductId(String productId);
    
    void deleteByProductId(String productId);
}