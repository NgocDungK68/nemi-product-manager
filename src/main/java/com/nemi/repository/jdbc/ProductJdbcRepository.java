package com.nemi.repository.jdbc;

import com.nemi.entity.ProductEntity;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Repository
public class ProductJdbcRepository extends BaseBatchRepository<ProductEntity> {

    public ProductJdbcRepository(HikariDataSource dataSource) {
        super( dataSource);
    }

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;

    @Override
    protected String getSql() {
        return "INSERT INTO product_manager.products " +
                "(pos_id, product_id, code, name, description, brand, images, category, status, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (product_id, pos_id) DO NOTHING";
    }

    @Override
    protected void setValues(PreparedStatement ps, ProductEntity p) throws SQLException {
        ps.setString(1, p.getPosId());
        ps.setString(2, p.getProductId());
        ps.setString(3, p.getCode());
        ps.setString(4, p.getName());
        ps.setString(5, p.getDescription());
        ps.setString(6, p.getBrand());
        ps.setString(7, p.getImages());
        ps.setString(8, p.getCategory());
        ps.setString(9, p.getStatus());
        ps.setString(10, p.getCreatedBy());
    }

    public void insertProductsParallel(List<ProductEntity> products) {
        int poolSize = hikariDataSource.getMaximumPoolSize(); // số connection tối đa trong pool
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        List<List<ProductEntity>> partitions = createSubList(products, batchSize);

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
