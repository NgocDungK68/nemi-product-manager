package com.nemi.model.request.sapo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nemi.configuration.SapoConfig;
import com.nemi.constant.SapoConstants;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.utils.PosUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
public class SapoRequest {
    private String storeName;
    private String clientId;
    private String clientSecret;
    private String accessToken;
    private Paginator paginator;
    private Map<String, Object> filters;

    @Data
    public static class Paginator {
        private int limit;
        private int page;
    }

    public static SapoRequest buildRequest(String config, String accessToken, long posCreatedAt, int recentDays) {
        try {

            Map<String, String> configMap = PosUtils.convertToConfigMap(config);

            String clientId = configMap.get(SapoConstants.CLIENT_ID);
            String clientSecret = configMap.get(SapoConstants.CLIENT_SECRET);
            String storeName = configMap.get(SapoConstants.STORE_NAME);

            // Calculate the time range for recent days
            long updateAtFrom = posCreatedAt - (long) recentDays * 86400L;

            // Decrypt access token when using for API calls
            Map<String, Object> filters = new HashMap<>();
            filters.put(SapoConstants.CREATE_ON_MIN, updateAtFrom); // lấy order tạo sau thời điểm này (epoch seconds)
            filters.put(SapoConstants.CREATE_ON_MAX, posCreatedAt); // lấy order tạo trước thời điểm này (epoch seconds)

            return SapoRequest.builder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .storeName(storeName)
                    .accessToken(accessToken)
                    .filters(filters)
                    .build();
        } catch (Exception e) {
            log.error("Failed request SapoRequest - {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.JSON_PARSE_ERROR));
        }
    }
}
