package com.nemi.repository;

import com.nemi.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, String> {
    
    Optional<ProductEntity> findByProductIdAndPosId(String productId, String posId);
}
