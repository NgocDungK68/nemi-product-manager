package com.nemi.repository;

import com.nemi.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, String> {

    /**
     * Find product by ID
     */
    Optional<ProductEntity> findById(String id);

    /**
     * Find products by category ID
     */
    List<ProductEntity> findByCategoryId(String categoryId);

    /**
     * Find products by status
     */
    List<ProductEntity> findByStatus(Integer status);

    /**
     * Find products by brand name
     */
    List<ProductEntity> findByBrandName(String brandName);

    /**
     * Check if product exists by ID
     */
    boolean existsById(String id);

    /**
     * Find products by title containing keyword (case insensitive)
     */
    @Query("SELECT p FROM ProductEntity p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<ProductEntity> findByTitleContainingIgnoreCase(@Param("keyword") String keyword);

    /**
     * Find all products with pagination support
     */
    @Query("SELECT p FROM ProductEntity p ORDER BY p.updatedAt DESC")
    List<ProductEntity> findAllOrderByUpdatedAtDesc();

    /**
     * Delete all products (for full sync)
     */
    void deleteAll();

    /**
     * Batch save products
     */
    @Override
    <S extends ProductEntity> List<S> saveAll(Iterable<S> entities);
}