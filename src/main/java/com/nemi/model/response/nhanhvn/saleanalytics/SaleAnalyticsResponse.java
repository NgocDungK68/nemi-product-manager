package com.nemi.model.response.nhanhvn.saleanalytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaleAnalyticsResponse {
    private List<DataItem> data;
    private Object pagination; // Có thể là null hoặc object khác, tùy dữ liệu thực tế
    private boolean success;
    private Summary summary;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataItem {
        private String timeDay;  // map từ "Time.day"
        private SaleData result;
        private SaleData returned;
        private SaleData success;

    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaleData {
        private int cod;
        private int discount;
        private int order_count;
        private int price;
        @JsonProperty("price_data")
        private int priceData;
        @JsonProperty("product_count")
        private double productCount;
        private int range;
        @JsonProperty("shipping_fee")
        private int shippingFee;
        private int surcharge;
        @JsonProperty("is_returned")
        private Boolean isReturned; // chỉ có trong returned, optional
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Summary {
        private int cod;
        private int discount;
        private int order_count;
        private int price;
        @JsonProperty("price_data")
        private int priceData;
        @JsonProperty("product_count")
        private int productCount;
        private int range;
        @JsonProperty("shipping_fee")
        private int shippingFee;
        private int surcharge;
    }
}