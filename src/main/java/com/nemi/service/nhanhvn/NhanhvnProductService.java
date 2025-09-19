package com.nemi.service.nhanhvn;

import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;

import java.util.Map;
import java.util.Optional;

public interface NhanhvnProductService {
    Optional<NhanhvnProductResponse> getProducts(Map<String, Object> paginator);
}
