package com.nemi.repository.jdbc;

import com.nemi.entity.OrderItemEntity;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
public class OrderItemJdbcRepository extends BaseBatchRepository<OrderItemEntity> {

    public OrderItemJdbcRepository(HikariDataSource dataSource) {
        super( dataSource);
    }

    @Override
    protected String getSql() {
        return "INSERT INTO order_item " +
                "(order_item_id, order_id, sku, variant_name, quantity, price, total_price, product_name, created_by,fulfillableQuantity) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?,?)";
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
}

