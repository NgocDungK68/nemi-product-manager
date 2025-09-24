package com.nemi.model.auth.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthPosRequest {
    private String accessCode;
    private String secretkey;
    private String businessId; // kha nang k can truong nay, pancake hay sapo k can thi xoa di nhe
    private String appId;
    // bo sung cac truong tu pos khac sau
}
