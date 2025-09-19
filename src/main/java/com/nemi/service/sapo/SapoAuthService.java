package com.nemi.service.sapo;

import com.nemi.model.response.sapo.SapoAccessTokenResponse;

import java.util.Optional;

public interface SapoAuthService {
    Optional<SapoAccessTokenResponse> getAccessToken(String code);
}