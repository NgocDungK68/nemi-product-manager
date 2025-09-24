package com.nemi.model.request.nhanhvn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NhanhvnAccessTokenRequest {
    private String accessCode;
    private String secretKey;
}