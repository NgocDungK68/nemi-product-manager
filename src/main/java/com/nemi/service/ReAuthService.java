package com.nemi.service;

import com.nemi.entity.PosEntity;

import java.util.Map;

public interface ReAuthService {
    String getPosName();


    public String getReAuthLink(String posId);
}
