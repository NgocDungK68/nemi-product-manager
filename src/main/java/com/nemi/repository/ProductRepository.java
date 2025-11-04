package com.nemi.repository;

import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, ProductId> {

    @Query("SELECT DISTINCT p FROM ProductEntity p " +
            "LEFT JOIN PosEntity pos on p.posId = pos.id " +
            "LEFT JOIN ProductVariantEntity v ON p.productId = v.productId AND p.posId = v.posId " +
            "WHERE pos.companyId = :companyId " +
            "AND (:productIds IS NULL OR p.productId IN :productIds) " +
            "AND (:skus IS NULL OR v.sku IN :skus)")
    List<ProductEntity> searchByProductIdsOrSkus(@Param("companyId") Integer companyId,
                                                 @Param("productIds") List<String> productIds,
                                                 @Param("skus") List<String> skus);

    @Query("select p from ProductEntity p join PosEntity pos on p.posId = pos.id " +
            "where pos.departmentId = :departmentId " +
            "and (p.productId like concat('%', :search,'%') or (p.name like concat('%', :search,'%'))) ")
    Page<ProductEntity> clientSearch(@Param("search") String search, @Param("departmentId") String departmentId, Pageable pageable);
}
