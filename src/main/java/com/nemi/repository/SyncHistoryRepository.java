package com.nemi.repository;

import com.nemi.entity.SyncHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncHistoryRepository extends JpaRepository<SyncHistoryEntity, String> {
}
