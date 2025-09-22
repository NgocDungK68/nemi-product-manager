package com.nemi.service;

import com.nemi.model.PosConnection;

import java.util.List;

public interface PosManagementService {

    String getPosManagementType();

    String setPosStatus(String status);

    List<PosConnection> listPosConnection(String posName,Long userId);








}
