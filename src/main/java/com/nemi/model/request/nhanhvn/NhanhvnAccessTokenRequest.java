package com.nemi.model.request.nhanhvn;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NhanhvnAccessTokenRequest {
    private String accessCode;
    private String secretKey;
}