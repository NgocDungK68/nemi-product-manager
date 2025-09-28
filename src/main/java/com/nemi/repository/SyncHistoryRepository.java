package com.nemi.repository;

import com.nemi.entity.SyncHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncHistoryRepository extends JpaRepository<SyncHistoryEntity, String> {
    Optional<SyncHistoryEntity> findByPosId(String s);
}
