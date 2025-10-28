package com.nemi.repository.jdbc;

import com.nemi.entity.OrderEntity;
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
public class OrderJdbcRepository extends BaseBatchRepository<OrderEntity> {


    @Value("${spring.jpa.properties.hibernate.jdbc.order_batch_size}")
    private int batchSize;

    public OrderJdbcRepository(HikariDataSource dataSource) {
        super(dataSource);
    }

    @Autowired
    @Qualifier("batchExecutor")
    private ThreadPoolTaskExecutor batchExecutor;

    @Override
    protected String getSql() {
        return """
        INSERT INTO product_manager.orders (
            pos_id,
            order_id,
            order_code,
            customer_name,
            customer_phone,
            shipping_address,
            payment_method,
            shipping_fee,
            total_price,
            status,
            created_by,
            discount_amount,
            customer_email,
            sale_id,
            created_at,
            updated_at,
            department_id,
            updated_by
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (pos_id, order_id)
        DO UPDATE SET
            order_code       = EXCLUDED.order_code,
            customer_name    = EXCLUDED.customer_name,
            customer_phone   = EXCLUDED.customer_phone,
            shipping_address = EXCLUDED.shipping_address,
            payment_method   = EXCLUDED.payment_method,
            shipping_fee     = EXCLUDED.shipping_fee,
            total_price      = EXCLUDED.total_price,
            status           = EXCLUDED.status,
            created_by       = EXCLUDED.created_by,
            discount_amount  = EXCLUDED.discount_amount,
            customer_email   = EXCLUDED.customer_email,
            sale_id          = EXCLUDED.sale_id,
            created_at       = EXCLUDED.created_at,
            updated_at       = EXCLUDED.updated_at,
            department_id    = EXCLUDED.department_id,
            updated_by       = EXCLUDED.updated_by;
        """;
    }




    @Override
    protected void setValues(PreparedStatement ps, OrderEntity o) throws SQLException {
        ps.setString(1, o.getPosId());
        ps.setString(2, o.getOrderId());
        ps.setString(3, o.getOrderCode());
        ps.setString(4, o.getCustomerName());
        ps.setString(5, o.getCustomerPhone());
        ps.setString(6, o.getShippingAddress());
        ps.setString(7, o.getPaymentMethod());
        ps.setBigDecimal(8, o.getShippingFee());
        ps.setBigDecimal(9, o.getTotalPrice());
        ps.setString(10, o.getStatus());
        ps.setString(11, o.getCreatedBy());

        // Chỉ check null cho discountAmount
        if (o.getDiscountAmount() != null) {
            ps.setBigDecimal(12, o.getDiscountAmount());
        } else {
            ps.setBigDecimal(12, BigDecimal.ZERO); // hoặc ps.setNull(12, java.sql.Types.DECIMAL);
        }

        ps.setString(13, o.getCustomerEmail());
        ps.setString(14, o.getSaleId());
        ps.setObject(15, o.getCreatedAt());
        ps.setObject(16, o.getUpdatedAt());
        ps.setString(17, o.getDepartmentId());
        ps.setString(18, o.getUpdatedBy());
    }


    public void insertOrdersParallel(List<OrderEntity> orderEntities) {

        List<List<OrderEntity>> partitions = createSubList(orderEntities, batchSize);

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
//        List<List<OrderEntity>> partitions = createSubList(orderEntities, batchSize);
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
//
//        }


