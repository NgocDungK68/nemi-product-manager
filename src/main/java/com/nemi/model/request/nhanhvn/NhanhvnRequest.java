package com.nemi.model.request.nhanhvn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nemi.constant.NhanhvnConstants;
import com.nemi.utils.PosUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NhanhvnRequest {
    private String appId;
    private String businessId;
    private String accessToken;
    private Paginator paginator;
    private Map<String, Object> filters;

    @Data
    public static class Paginator {
        private int size;
        private Object sort;
        private Object next;
    }

    public static NhanhvnRequest buildRequest(String config, String accessToken) {
        Map<String, String> configMap = PosUtils.convertToConfigMap(config);
        String appId = configMap.get(NhanhvnConstants.APP_ID);
        String businessId = configMap.get(NhanhvnConstants.BUSINESS_ID);

        return NhanhvnRequest.builder()
                .appId(appId)
                .businessId(businessId)
                .accessToken(accessToken)
                .build();
    }
}
