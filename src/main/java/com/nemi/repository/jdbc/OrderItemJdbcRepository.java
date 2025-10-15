package com.nemi.repository.jdbc;

import com.nemi.entity.OrderItemEntity;
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

@Repository
@Slf4j
public class OrderItemJdbcRepository extends BaseBatchRepository<OrderItemEntity> {

    public OrderItemJdbcRepository(HikariDataSource dataSource) {
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
                INSERT INTO product_manager.order_item (
                    order_item_id,
                    order_id,
                    sku,
                    variant_name,
                    quantity,
                    price,
                    total_price,
                    product_name,
                    created_by,
                    fulfillable_quantity
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (order_item_id)
                DO UPDATE SET
                    sku                 = EXCLUDED.sku,
                    variant_name        = EXCLUDED.variant_name,
                    quantity            = EXCLUDED.quantity,
                    price               = EXCLUDED.price,
                    total_price         = EXCLUDED.total_price,
                    product_name        = EXCLUDED.product_name,
                    created_by          = EXCLUDED.created_by,
                    fulfillable_quantity = EXCLUDED.fulfillable_quantity;
                """;
    }

    @Override
    protected void setValues(PreparedStatement ps, OrderItemEntity item) throws SQLException {
        ps.setString(1, item.getOrderItemId());
        ps.setString(2, item.getOrderId());
        ps.setString(3, item.getSku());
        ps.setString(4, item.getVariantName());
        ps.setObject(5, item.getQuantity());
        ps.setBigDecimal(6, item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);
        ps.setBigDecimal(7, item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
        ps.setString(8, item.getProductName());
        ps.setString(9, item.getCreatedBy());
        ps.setObject(10, item.getFulfillableQuantity());
    }

    public void insertOrderItemParallel(List<OrderItemEntity> orderItemEntities) {

        List<List<OrderItemEntity>> partitions = createSubList(orderItemEntities, batchSize);

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

//        int poolSize = hikariDataSource.getMaximumPoolSize(); // số connection tối đa trong pool
//        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
//
//        List<List<OrderItemEntity>> partitions = createSubList(orderItemEntities, batchSize);
//
//        // tạo danh sách các task Callable<Void> hoặc tương tự
//        List<Callable<Void>> callables = partitions.stream().map(sublist ->
//                (Callable<Void>) () -> {
//                    batchInsert(sublist, batchSize);
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



