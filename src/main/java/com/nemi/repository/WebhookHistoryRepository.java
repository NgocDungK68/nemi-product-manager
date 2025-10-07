package com.nemi.repository;

import com.nemi.entity.WebhookHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookHistoryRepository extends JpaRepository<WebhookHistoryEntity, String> {
}
