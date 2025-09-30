package com.nemi.repository;

import com.nemi.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, String> {
    
    List<ProductVariantEntity> findByProductId(String productId);
    
    void deleteByProductId(String productId);
}