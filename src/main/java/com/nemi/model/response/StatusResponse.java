package com.nemi.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class StatusResponse {

    private List<StatusDetail> statusDetails;
    private String statusConnect;


    @Data
    public static class StatusDetail {
        private String status;
        private String type;
    }
}
