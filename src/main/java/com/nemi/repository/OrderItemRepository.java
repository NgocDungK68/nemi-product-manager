package com.nemi.repository;

import com.nemi.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity,String> {
    
    List<OrderItemEntity> findByOrderId(String orderId);
    void deleteByOrderId(String orderId);
}
