package com.nemi.repository;

import com.nemi.entity.ProductVariantEntity;
import com.nemi.entity.VariantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, VariantId> {
    
    List<ProductVariantEntity> findByProductId(String productId);

    Optional<ProductVariantEntity> findByVariantId(String variantId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProductVariantEntity v where v.productId = :productId and v.posId = :posId")
    int deleteAllByProductIdAndPosId(String productId, String posId);
}