package com.nemi.service.nhanhvn;

import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;

import java.util.Map;
import java.util.Optional;

public interface NhanhvnProductService {
    Optional<NhanhvnProductResponse> getProducts(NhanhvnRequest request);
}
