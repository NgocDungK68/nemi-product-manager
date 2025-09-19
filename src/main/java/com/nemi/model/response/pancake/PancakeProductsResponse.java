package com.nemi.model.response.pancake;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PancakeProductsResponse {
    private List<Product> data;

    @JsonProperty("page_number")
    private Integer pageNumber;

    @JsonProperty("page_size")
    private Integer pageSize;

    private Boolean success;

    @JsonProperty("total_entries")
    private Integer totalEntries;

    @JsonProperty("total_pages")
    private Integer totalPages;

    @Data
    public static class Product {
        private String id;

        @JsonProperty("custom_id")
        private String customId;

        private String name;

        private String description;

        @JsonProperty("display_id")
        private Integer displayId;

        @JsonProperty("shop_id")
        private Long shopId;

        @JsonProperty("category_ids")
        private List<Long> categoryIds;

        @JsonProperty("brand_id")
        private String brandId;

        private String keyword;

        @JsonProperty("is_published")
        private Boolean isPublished;

        @JsonProperty("inserted_at")
        private LocalDateTime insertedAt;

        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;

        private Boolean removed;

        private String image;

        private List<Variation> variations;

        @JsonProperty("product_attributes")
        private List<ProductAttribute> productAttributes;
    }

    @Data
    public static class Variation {
        private String id;

        @JsonProperty("display_id")
        private String displayId;

        @JsonProperty("product_id")
        private String productId;

        private String barcode;

        @JsonProperty("retail_price")
        private Double retailPrice;

        @JsonProperty("price_at_counter")
        private Double priceAtCounter;

        @JsonProperty("remain_quantity")
        private Integer remainQuantity;

        private List<String> images;

        private List<Field> fields;

        @JsonProperty("inserted_at")
        private LocalDateTime insertedAt;

        @JsonProperty("is_hidden")
        private Boolean isHidden;

        private Double weight;

        @JsonProperty("wholesale_price")
        private List<Object> wholesalePrice;
    }

    @Data
    public static class Field {
        private String id;
        private String name;
        private String value;

        @JsonProperty("keyValue")
        private String keyValue;
    }

    @Data
    public static class ProductAttribute {
        private String id;
        private String name;
        private List<String> values;
        private List<Keyword> keyword;
    }

    @Data
    public static class Keyword {
        @JsonProperty("keyValue")
        private String keyValue;
        private String value;
    }
}