package com.nemi.repository.jdbc;

import com.nemi.entity.ProductEntity;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Repository
@Slf4j

public class ProductJdbcRepository extends BaseBatchRepository<ProductEntity> {


    public ProductJdbcRepository(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    @Autowired
    @Qualifier("batchExecutor")
    private ThreadPoolTaskExecutor batchExecutor;


    @Override
    protected String getSql() {
        return """
                INSERT INTO product_manager.products (
                    pos_id,
                    product_id,
                    code,
                    name,
                    description,
                    brand,
                    images,
                    category,
                    status,
                    created_by,
                    created_at,
                    updated_at,
                    department_id,
                    updated_by
             
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (product_id, pos_id)
                DO UPDATE
                SET
                    code         = EXCLUDED.code,
                    name         = EXCLUDED.name,
                    description  = EXCLUDED.description,
                    brand        = EXCLUDED.brand,
                    images       = EXCLUDED.images,
                    category     = EXCLUDED.category,
                    status       = EXCLUDED.status,
                    created_by   = EXCLUDED.created_by,
                    created_at   = EXCLUDED.created_at,
                    updated_at   = EXCLUDED.updated_at,
                    department_id = EXCLUDED.department_id,
                    updated_by   = EXCLUDED.updated_by;
                """;
    }


    @Override
    protected void setValues(PreparedStatement ps, ProductEntity productEntity) throws SQLException {
        ps.setString(1, productEntity.getPosId());
        ps.setString(2, productEntity.getProductId());
        ps.setString(3, productEntity.getCode());
        ps.setString(4, productEntity.getName());
        ps.setString(5, productEntity.getDescription());
        ps.setString(6, productEntity.getBrand());
        ps.setString(7, productEntity.getImages());
        ps.setString(8, productEntity.getCategory());
        ps.setString(9, productEntity.getStatus());
        ps.setString(10, productEntity.getCreatedBy());
        ps.setObject(11, productEntity.getCreatedAt());
        ps.setObject(12, productEntity.getUpdatedAt());
        ps.setString(13, productEntity.getDepartmentId());
        ps.setString(14, productEntity.getUpdatedBy());
    }

    public void insertProductsParallel(List<ProductEntity> products) {
        List<List<ProductEntity>> partitions = createSubList(products, batchSize);

        List<Future<Object>> futures = partitions.stream()
                .map(sublist -> batchExecutor.submit(() -> {
                    batchInsert(sublist, batchSize);
                    return null;
                }))
                .toList();

        // chờ tất cả batch hoàn thành và bắt lỗi
        for (Future<Object> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread was interrupted while waiting for batch", e);
            } catch (ExecutionException e) {
                log.error("Batch execution failed", e.getCause());
            }
        }

    }


}
