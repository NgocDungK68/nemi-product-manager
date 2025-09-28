package com.nemi.model.response.pancake;

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
public class PancakeProductResponse {
    private List<ProductData> data;
    @JsonProperty("page_number")
    private int pageNumber;

    @JsonProperty("page_size")
    private int pageSize;

    private boolean success;

    @JsonProperty("total_entries")
    private int totalEntries;

    @JsonProperty("total_pages")
    private int totalPages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductData {
        private String id;                 // variation id
        @JsonProperty("product_id")
        private String productId;
        @JsonProperty("display_id")
        private String displayId;
        private String barcode;
        @JsonProperty("is_hidden")
        private Boolean isHidden;
        @JsonProperty("is_locked")
        private Boolean isLocked;
        @JsonProperty("is_sell_negative_variation")
        private Boolean isSellNegativeVariation;
        private List<Field> fields;
        private List<String> images;
        @JsonProperty("inserted_at")
        private String insertedAt;
        @JsonProperty("last_imported_price")
        private Double lastImportedPrice;
        @JsonProperty("price_at_counter")
        private Double priceAtCounter;
        @JsonProperty("remain_quantity")
        private Integer remainQuantity;
        @JsonProperty("retail_price")
        private Double retailPrice;
        @JsonProperty("total_purchase_price")
        private Double totalPurchasePrice;
        @JsonProperty("variations_warehouses")
        private List<VariationWarehouse> variationsWarehouses;
        @JsonProperty("wholesale_price")
        private List<Object> wholesalePrice;  // chưa rõ structure, để Object
        @JsonProperty("price_table")
        private List<PriceTable> priceTable;
        private Product product;              // nested product info
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Field {
        private String id;
        private String name;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        @JsonProperty("note_product")
        private String noteProduct;
        private List<Category> categories;
        @JsonProperty("display_id")
        private String displayId;
        private String image;
        @JsonProperty("inserted_at")
        private String insertedAt;
        private String name;
        private List<Tag> tags;
        @JsonProperty("is_published")
        private Boolean isPublished;
        @JsonProperty("manipulation_warehouses")
        private List<String> manipulationWarehouses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category {
        private String id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tag {
        private int id;
        private String note;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariationWarehouse {
        @JsonProperty("actual_remain_quantity")
        private Integer actualRemainQuantity;
        @JsonProperty("pending_quantity")
        private Integer pendingQuantity;
        @JsonProperty("remain_quantity")
        private Integer remainQuantity;
        @JsonProperty("returning_quantity")
        private Integer returningQuantity;
        @JsonProperty("selling_avg")
        private Double sellingAvg;
        @JsonProperty("total_quantity")
        private Integer totalQuantity;
        @JsonProperty("warehouse_id")
        private String warehouseId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceTable {
        private String id;
        private String name;
        private Double price;
    }
}
