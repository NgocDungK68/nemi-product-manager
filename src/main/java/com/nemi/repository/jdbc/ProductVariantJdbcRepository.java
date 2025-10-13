package com.nemi.repository.jdbc;

import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Repository
public class ProductVariantJdbcRepository extends BaseBatchRepository<ProductVariantEntity> {

    public ProductVariantJdbcRepository(HikariDataSource dataSource) {
        super( dataSource);
    }

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;


    @Override
    protected String getSql() {
        return "INSERT INTO product_manager.product_variant " +
                "(variant_id, pos_id, product_id, sku, barcode, price, inventory_quantity, " +
                "fulfillable_quantity, weight, weight_unit,attributes, warehouse_quantities) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                "ON CONFLICT (variant_id, pos_id) DO NOTHING";
    }

    @Override
    protected void setValues(PreparedStatement ps, ProductVariantEntity v) throws SQLException {
        ps.setObject(1, v.getVariantId());
        ps.setString(2, v.getPosId());
        ps.setString(3, v.getProductId());
        ps.setString(4, v.getSku());
        ps.setString(5, v.getBarcode());
        ps.setBigDecimal(6, v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO);
        ps.setObject(7, v.getInventoryQuantity());
        ps.setObject(8, v.getFulfillableQuantity());
        ps.setObject(9, v.getWeight());
        ps.setString(10, v.getWeightUnit());
        ps.setString(11, v.getAttributes());
        ps.setString(12, v.getWarehouseQuantities());
    }

    public void insertProductsVariantParallel(List<ProductVariantEntity> products) {
        int poolSize = hikariDataSource.getMaximumPoolSize(); // số connection tối đa trong pool
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        List<List<ProductVariantEntity>> partitions = createSubList(products, poolSize);

        // tạo danh sách các task Callable<Void> hoặc tương tự
        List<Callable<Void>> callables = partitions.stream().map(sublist ->
                (Callable<Void>) () -> {
                    batchInsert(sublist,batchSize);
                    return null;
                }).collect(Collectors.toList());

        try {
            executor.invokeAll(callables);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
}
