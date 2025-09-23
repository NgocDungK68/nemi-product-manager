package com.nemi.repository;

import com.nemi.entity.PosEntity;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PosRepository extends JpaRepository<PosEntity, String> {
    List<PosEntity> findByUserId(String userId);

    @Query(value = "SELECT * FROM pos p WHERE CAST(p.config AS JSONB) ->> 'app-id' = :appId",
            nativeQuery = true)
    Optional<PosEntity> findByAppId(@Param("appId") String appId);
}
