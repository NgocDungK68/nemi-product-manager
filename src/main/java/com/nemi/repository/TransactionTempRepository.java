package com.nemi.repository;

import com.nemi.entity.TransactionTempEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionTempRepository extends JpaRepository<TransactionTempEntity,String> {

}
