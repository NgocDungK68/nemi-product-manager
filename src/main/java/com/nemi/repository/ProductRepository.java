package com.nemi.repository;

import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, ProductId> {
    Optional<ProductEntity> findByProductIdAndPosId(String productId, String posId);
}
