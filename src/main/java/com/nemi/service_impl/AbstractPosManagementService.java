package com.nemi.service_impl;

import com.nemi.entity.PosEntity;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.repository.PosRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RequiredArgsConstructor

public abstract class AbstractPosManagementService {
    protected PosRepository posRepository;


    public String getPosStatus(String posId) {
        Optional<PosEntity> entityOpt = posRepository.findById(posId);
        if(entityOpt.isEmpty()){
            throw new RuntimeException("pos does not exist");
        }
        return entityOpt.map(PosEntity::getStatus).orElse("NOT_FOUND");
    }

    /**
     * API: PATCH /client-api/v1/pos/{pos-id}
     */

    public String setPosStatus(String id, String status) {
        Optional<PosEntity> entityOpt = posRepository.findById(id);
        if(entityOpt.isEmpty()){
            throw new RuntimeException("pos does not exist");
        }
        PosEntity pos = entityOpt.get();
        pos.setStatus(status);
        posRepository.save(pos);

        return "set status succesfully";
    }


    public List<PosConnectionResponse> listPosConnection(String userId) {
        List<PosEntity> posEntities =  posRepository.findByUserId(userId);
        return posEntities.stream().map(PosConnectionResponse::toPosConnectionResponse).collect(Collectors.toList());
    }


}
