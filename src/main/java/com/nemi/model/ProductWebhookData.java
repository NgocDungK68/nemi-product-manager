package com.nemi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductWebhookData {
    private Integer productId;
    private String shopProductId;
    private Integer categoryId;
    private Integer brandId;
    private Integer parentId;
    private String code;
    private String barcode;
    private String name;
    private Double price;
    private Integer vat;
    private String image;
    private List<String> images;
    private String status;
    private String description;
    private String content;
    private Float length;
    private Float width;
    private Float height;
    private Double weight;
    private String createdDateTime;
    private Integer createdById;
    private List<Inventory> inventory;
    private List<Attribute> attributes;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Inventory {
        private Integer shopId;
        private Integer quantity;
        private Integer available;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Attribute {
        private String attributeName;
        private Integer id;
        private String name;
        private String content;
    }
}

