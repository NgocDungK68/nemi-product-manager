package com.nemi.client;

import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;

public interface PosClient {
    NhanhvnAccessTokenResponse getAccessToken(PosConnectionRequest posConnectionRequest);

}