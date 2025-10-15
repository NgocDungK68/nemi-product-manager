package com.nemi.model.request.nhanhvn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nemi.configuration.NhanhvnConfig;
import com.nemi.constant.NhanhvnConstants;
import com.nemi.utils.PosUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
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

        long updatedAtTo = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long updateAtFrom = LocalDateTime.now().minusDays(NhanhvnConfig.getRecentDaysStatic()).toEpochSecond(ZoneOffset.UTC);
        Map<String, Object> filters = new HashMap<>();
        filters.put(NhanhvnConstants.UPDATED_AT_FROM, updateAtFrom);
        filters.put(NhanhvnConstants.UPDATED_AT_TO, updatedAtTo);

        return NhanhvnRequest.builder()
                .appId(appId)
                .businessId(businessId)
                .accessToken(accessToken)
                .filters(filters)
                .build();
    }
}
