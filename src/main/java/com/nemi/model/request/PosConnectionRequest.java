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
    private String accessCode;

    private String clientId;        // SAPO
    private String clientSecret;
    private String storeName;
    private String code;

    private String shopId;
    private String apiKey;    //PANCAKE

    private String posId;

    // aop preAuthorize

}

// mapping: nhanhvn: appId = appId
//          pancake: appId = shopId
// Khi co 1 nen tang moi => chi can thay doi mapping config trong backend
// Can 1 common request (lop iteration de mapping)
// key: mapping rule