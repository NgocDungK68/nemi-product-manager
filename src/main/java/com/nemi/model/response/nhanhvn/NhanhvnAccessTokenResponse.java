package com.nemi.model.response.nhanhvn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NhanhvnAccessTokenResponse {
    private Integer code;
    private TokenData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenData {
        private String accessToken;
        private String version;
        private Long expiredAt;
        private Integer businessId;
        private String depotIds;
        private String pageIds;
        private String permissions;
    }
}