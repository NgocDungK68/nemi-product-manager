package com.nemi.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nemi.entity.PosEntity;
import com.nemi.enums.PosStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PosConnectionResponse {
    private String id;
    private String posName;
    private String status;
    private String reAuthLink;
    private LocalDateTime expiredTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PosConnectionResponse toPosConnectionResponse(PosEntity pos) {
        if (pos == null) {
            return null;
        }

        return PosConnectionResponse.builder()
                .id(pos.getId())
                .posName(pos.getPosName())
                .status(pos.getStatus())
                .expiredTime(pos.getExpiredTime())
                .createdAt(pos.getCreatedAt())
                .updatedAt(pos.getUpdatedAt())
                .build();
    }

    public static PosConnectionResponse expired(PosEntity entity, String reAuthLink) {
        return PosConnectionResponse.builder()
                .id(entity.getId())
                .posName(entity.getPosName())
                .status(PosStatus.EXPIRED.name())
                .reAuthLink(reAuthLink)
                .expiredTime(entity.getExpiredTime())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
