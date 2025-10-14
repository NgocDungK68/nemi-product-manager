package com.nemi.model.response.nhanhvn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.nemi.configuration.deserializer.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NhanhvnProductResponse {
    private Integer code;
    private Paginator paginator;
    private List<ProductData> data;

    @Data
    public static class Paginator {
        private Object next;
    }

    @Data
    public static class ProductData {
        private Integer id;
        private Integer parentId;
        private String code;
        private String barcode;
        private String name;
        private String otherName;
        private Integer status;
        private Integer vat;

        @JsonDeserialize(using = CategoryDeserializer.class)
        private Category category;

        @JsonDeserialize(using = CategoryDeserializer.class)
        private Category internalCategory;

        private Prices prices;
        private Images images;

        @JsonDeserialize(using = WarrantyDeserializer.class)
        private Warranty warranty;

        @JsonDeserialize(using = BrandDeserializer.class)
        private Brand brand;

        @JsonDeserialize(using = TypeDeserializer.class)
        private Type type;

        private Shipping shipping;
        private String countryName;
        private Units units;
        private List<Combo> combos;
        private Inventory inventory;
        private List<Attribute> attributes;
        private Long updatedAt;
        private Long createdAt;
    }

    @Data
    public static class Category {
        private Integer id;
        private String name;
        private String code;
    }

    @Data
    public static class Prices {
        private Double retail;
        @JsonProperty("import")
        private Double importPrice; // đổi tên để tránh trùng keyword
        private Double old;
        private Double wholesale;
        private Double avgCost;
    }

    @Data
    public static class Images {
        private String avatar;
        private List<String> others;
    }

    @Data
    public static class Warranty {
        private Integer month;
        private String phone;
        private String address;
    }

    @Data
    public static class Brand {
        private Integer id;
        private String name;
    }

    @Data
    public static class Type {
        private Integer id;
        private String name;
    }

    @Data
    public static class Shipping {
        private Double width;
        private Double height;
        private Double length;
        private Double weight;
    }

    @Data
    public static class Units {
        private String name;
        private List<UnitItem> list;
    }

    @Data
    public static class UnitItem {
        private int id;
        private String name;
        private Double quantity;
        private UnitPrice price;
    }

    @Data
    public static class UnitPrice {
        private Double retail;
        private Double importPrice;
        private Double wholesale;
    }

    @Data
    public static class Combo {
        private Integer id;
        private String code;
        private String name;
        private Integer quantity;
    }

    @Data
    public static class Inventory {
        private Integer remain;
        private Integer shipping;
        private Integer damaged;
        private Integer holding;
        private Integer available;
        @JsonDeserialize(using = WarrantyInventoryDeserializer.class)
        private WarrantyInventory warranty;
        private List<Depot> depots;
    }

    @Data
    public static class WarrantyInventory {
        private Integer remain;
        private Integer holding;
    }

    @Data
    public static class Depot {
        private Integer id;
        private Integer remain;
        private Integer shipping;
        private Integer damaged;
        private Integer holding;
        private Integer available;

        @JsonDeserialize(using = WarrantyInventoryDeserializer.class)
        private WarrantyInventory warranty;
    }

    @Data
    public static class Attribute {
        private Integer id;
        private String name;
        private String value;
    }
}