package com.nemi.model.response.sapo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SapoProductResponse {

    private List<Product> products;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private Long id;
        private String name;
        private String alias;
        private String vendor;
        private String content;
        private String status;
        @JsonProperty("product_type")
        private String productType;
        private String createdAt;
        private String updatedAt;
        private String publishedAt;
        private String templateSuffix;
        private String publishedScope;
        @JsonProperty("modified_on")
        private String modifiedOn;
        @JsonProperty("created_on")
        private String createdOn;
        private String tags;

        private List<Variant> variants;
        private List<Option> options;
        private List<Image> images;
        private Image image;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Variant {
        private Long id;
        private Long productId;
        private String title;
        private Double price;
        private String sku;
        private Integer position;
        private String inventoryPolicy;
        private String compareAtPrice;
        private String fulfillmentService;
        private String inventoryManagement;
        private String option1;
        private String option2;
        private String option3;
        private String createdAt;
        private String updatedAt;
        private Boolean taxable;
        private String barcode;
        private Integer grams;
        private Long imageId;
        private Double weight;
        private String weightUnit;
        private Long inventoryItemId;
        @JsonProperty("inventory_quantity")
        private Integer inventoryQuantity;
        private Integer oldInventoryQuantity;
        private Boolean requiresShipping;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private Long id;
        private Long productId;
        private String name;
        private Integer position;
        private List<String> values;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Image {
        private Long id;
        private Long productId;
        private Integer position;
        private String createdAt;
        private String updatedAt;
        private String alt;
        private Integer width;
        private Integer height;
        private String src;
        private List<Long> variantIds;
    }
}
