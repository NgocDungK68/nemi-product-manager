package com.nemi.client;

import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;

import java.util.Optional;

public interface PosClient {
    NhanhvnAccessTokenResponse getAccessToken(PosConnectionRequest posConnectionRequest);
    Optional<NhanhvnProductResponse> getProducts(NhanhvnRequest request);
}