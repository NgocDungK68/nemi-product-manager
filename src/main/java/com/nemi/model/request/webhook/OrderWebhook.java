package com.nemi.model.request.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderWebhook {
    
    // Mapping theo cấu trúc thực tế từ Nhanh.vn
    private Long id;
    
    @JsonProperty("customerId")
    private Long customerId;
    
    @JsonProperty("customerName")
    private String customerName;
    
    @JsonProperty("customerMobile") 
    private String customerMobile;
    
    @JsonProperty("customerEmail")
    private String customerEmail;
    
    @JsonProperty("shippingAddress")
    private String shippingAddress;
    
    @JsonProperty("totalPrice")
    private Double totalPrice;
    
    @JsonProperty("status")
    private Integer status;
    
    @JsonProperty("statusText")
    private String statusText;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("createdDateTime")
    private String createdDateTime;
    
    @JsonProperty("updatedDateTime") 
    private String updatedDateTime;
    
    @JsonProperty("businessId")
    private Long businessId;
    
    @JsonProperty("depotId")
    private Long depotId;
    
    @JsonProperty("saleId")
    private Long saleId;
    
    @JsonProperty("products")
    private List<ProductItem> products;
    
    @JsonProperty("payments")
    private List<PaymentItem> payments;
    
    @JsonProperty("carrierInfo")
    private Map<String, Object> carrierInfo;
    
    // For backward compatibility
    public Long getOrderId() {
        return this.id;
    }
    
    public String getCustomerAddress() {
        return this.shippingAddress;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductItem {
        
        @JsonProperty("id")
        private Long productId;
        
        @JsonProperty("productId") 
        private Long productIdAlt;
        
        @JsonProperty("name")
        private String productName;
        
        @JsonProperty("quantity")
        private Integer quantity;
        
        @JsonProperty("price")
        private Double price;
        
        @JsonProperty("discount")
        private Double discount;
        
        @JsonProperty("vat")
        private Double vat;
        
        @JsonProperty("gifts")
        private List<GiftItem> gifts;
        
        // Get productId with fallback
        public Long getProductId() {
            return productId != null ? productId : productIdAlt;
        }
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GiftItem {
        
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("quantity")
        private Integer quantity;
        
        @JsonProperty("price")
        private Double price;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentItem {
        
        @JsonProperty("method")
        private String method;
        
        @JsonProperty("amount")
        private Double amount;
        
        @JsonProperty("accountId")
        private Long accountId;
    }
}