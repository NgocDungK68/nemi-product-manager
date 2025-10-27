package com.nemi.repository;

import com.nemi.entity.PosEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PosRepository extends JpaRepository<PosEntity, String> {
    List<PosEntity> findByUserId(String userId);

    Optional<PosEntity> findByIdAndUserId(String id, String userId);

    PosEntity findByUserIdAndPosName(String userId, String posName);

}
