package com.nemi.model.request.sapo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SapoRequest {
    private String storeName;
    private String clientId;
    private String clientSecret;
    private String accessToken;
    private int limit;
    private int page;
    private Map<String, Object> paginator;
}
