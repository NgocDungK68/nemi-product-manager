package com.nemi.repository;

import com.nemi.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity,String> {
    
    Optional<OrderEntity> findByOrderIdAndPosId(String orderId, String posId);
}
