package com.nemi.service.pancake;

import com.nemi.model.response.nhanhvn.managershop.ShopResponse;
import com.nemi.model.response.nhanhvn.saleanalytics.SaleAnalyticsResponse;
import com.nemi.model.response.pancake.PancakeProductsResponse;
import com.nemi.model.response.pancake.PancakeResponse;

import java.util.Map;
import java.util.Optional;

public interface PancakeService {
    Optional<PancakeResponse> getOrderDetail(String shopId, String orderId, Map<String, String> queryParams);
    Optional<ShopResponse> getShopInfo(Map<String, String> queryParams);
    Optional<SaleAnalyticsResponse> getSaleAnalytics(Map<String, String> queryParams);
    Optional<PancakeProductsResponse> getProducts(String shopId, Map<String, String> queryParams);
}
