package com.nemi.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PosConnectionResponse {
    private String id;
    private String posName;
    private String config;
    private String status;
    private String expiredTime;
    private String createdAt;
    private String updatedAt;
}
