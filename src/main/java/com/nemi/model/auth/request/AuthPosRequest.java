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
    private String businessId;
    // bo sung cac truong tu pos khac sau
}
