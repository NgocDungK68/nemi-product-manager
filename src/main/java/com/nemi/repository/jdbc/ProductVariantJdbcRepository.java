package com.nemi.repository.jdbc;

import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class ProductVariantJdbcRepository extends BaseBatchRepository<ProductVariantEntity> {

    public ProductVariantJdbcRepository(HikariDataSource dataSource) {
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
                INSERT INTO product_manager.product_variant (
                    variant_id,
                    pos_id,
                    product_id,
                    sku,
                    barcode,
                    price,
                    inventory_quantity,
                    fulfillable_quantity,
                    weight,
                    weight_unit,
                    attributes,
                    warehouse_quantities
                )
                VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                ON CONFLICT (variant_id, pos_id)
                DO UPDATE SET
                    product_id           = EXCLUDED.product_id,
                    sku                  = EXCLUDED.sku,
                    barcode              = EXCLUDED.barcode,
                    price                = EXCLUDED.price,
                    inventory_quantity   = EXCLUDED.inventory_quantity,
                    fulfillable_quantity = EXCLUDED.fulfillable_quantity,
                    weight               = EXCLUDED.weight,
                    weight_unit          = EXCLUDED.weight_unit,
                    attributes           = EXCLUDED.attributes,
                    warehouse_quantities = EXCLUDED.warehouse_quantities;
                """;
    }


    @Override
    protected void setValues(PreparedStatement ps, ProductVariantEntity productVariantEntity) throws SQLException {
        ps.setString(1, productVariantEntity.getVariantId());
        ps.setString(2, productVariantEntity.getPosId());
        ps.setString(3, productVariantEntity.getProductId());
        ps.setString(4, productVariantEntity.getSku());
        ps.setString(5, productVariantEntity.getBarcode());
        ps.setBigDecimal(6, productVariantEntity.getPrice() != null ? productVariantEntity.getPrice() : BigDecimal.ZERO);
        ps.setObject(7, productVariantEntity.getInventoryQuantity());
        ps.setObject(8, productVariantEntity.getFulfillableQuantity());
        ps.setObject(9, productVariantEntity.getWeight());
        ps.setString(10, productVariantEntity.getWeightUnit());
        ps.setString(11, productVariantEntity.getAttributes());
        ps.setString(12, productVariantEntity.getWarehouseQuantities());
    }

//    public void insertProductsVariantParallel(List<ProductVariantEntity> products) {
//        int poolSize = hikariDataSource.getMaximumPoolSize(); // số connection tối đa trong pool
//        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
//
//        List<List<ProductVariantEntity>> partitions = createSubList(products, batchSize);
//
//        // tạo danh sách các task Callable<Void> hoặc tương tự
//        List<Callable<Void>> callables = partitions.stream().map(sublist ->
//                (Callable<Void>) () -> {
//                    batchInsert(sublist,batchSize);
//                    return null;
//                }).collect(Collectors.toList());
//
//        try {
//            executor.invokeAll(callables);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } finally {
//            executor.shutdown();
//        }
//    }

    public void insertProductsVariantParallel(List<ProductVariantEntity> productVariantEntities) {
        List<List<ProductVariantEntity>> partitions = createSubList(productVariantEntities, batchSize);

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
