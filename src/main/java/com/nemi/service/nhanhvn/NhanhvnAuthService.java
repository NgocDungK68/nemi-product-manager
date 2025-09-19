package com.nemi.service.nhanhvn;

import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;

import java.util.Optional;

public interface NhanhvnAuthService {
    public Optional<NhanhvnAccessTokenResponse> exchangeAccessToken(String accessCode);
}