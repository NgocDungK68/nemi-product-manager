package com.nemi.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PosConnectionRequest {
    private String appId;           //  clientId    (SAPO)
    private String businessId;
    private String appSecret;       // clientSecret (SAPO)
    private String shopId;
    private String accessCode ;
    private String code;
}