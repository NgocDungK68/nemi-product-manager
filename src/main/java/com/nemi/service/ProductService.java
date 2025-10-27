package com.nemi.service;

import com.nemi.model.response.ProductBasicResponse;

import java.util.List;

public interface ProductService {
    List<ProductBasicResponse> searchProducts(Integer companyId, List<String> productIds,List<String> skus);
}
