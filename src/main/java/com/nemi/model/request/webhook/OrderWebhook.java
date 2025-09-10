package com.nemi.model.request.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderWebhook {
    private String customerName;
    private String customerMobile;
    private String customerEmail;
    private String customerAddress;
    private List<ProductItem> products;
    private Double totalPrice;
    private String status;
    private Long orderId;

    // Constructors, Getters, Setters...

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductItem {
        private Long productId;
        private String productName;
        private Integer quantity;
        private Double price;
    }
}
