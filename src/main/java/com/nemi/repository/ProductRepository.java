package com.nemi.repository;

import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, ProductId> {
    Optional<ProductEntity> findByProductIdAndPosId(String productId, String posId);
}
