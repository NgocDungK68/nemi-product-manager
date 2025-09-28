package com.nemi.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosConnectionRequest {
    private String appId;
    private String businessId;
    private String appSecret;
    private String shopId;
    private String accessCode;

    private String clientId;        // SAPO
    private String clientSecret;
    private String storeName;
    private String code;

    private String apiKey;    //PANCAKE
}