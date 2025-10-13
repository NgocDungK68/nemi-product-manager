package com.nemi.repository.jdbc;

import com.nemi.entity.OrderEntity;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
public class OrderJdbcRepository extends BaseBatchRepository<OrderEntity> {

    public OrderJdbcRepository(HikariDataSource dataSource) {
        super( dataSource);
    }


    @Override
    protected String getSql() {
        return "INSERT INTO orders " +
                "(pos_id, order_id, order_code, customer_name, customer_phone, " +
                "shipping_address, payment_method, shipping_fee, total_price, status, created_by,discount_amount, customerEmail) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
        ps.setBigDecimal(12, BigDecimal.valueOf(o.getDiscountAmount()));
        ps.setString(13, o.getCustomerEmail());
    }
}
