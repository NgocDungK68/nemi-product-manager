package com.nemi.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosConnectionResponse {
    private String id;
    private String posName;
    private String config;
    private String status;
    private String expiredTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
