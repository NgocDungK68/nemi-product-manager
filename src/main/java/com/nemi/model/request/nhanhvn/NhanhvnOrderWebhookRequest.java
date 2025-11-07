package com.nemi.model.request.nhanhvn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NhanhvnOrderWebhookRequest {
    private Depot depot;
    private Sender sender;
    private Customer customer;
    private Info info;
    private List<Product> products;
    private Carrier carrier;
    private Channel channel;

    // ---------------- Depot ----------------
    @Data
    public static class Depot {
        private Integer id;
        private String name;
        private String mobile;
        private PickupDepot pickupDepot;
    }

    @Data
    public static class Sender {
        private Locations locations;
    }

    @Data
    public static class Locations {
        private Integer cityId;
        private Integer districtId;
        private Integer wardId;
    }

    // ---------------- Customer ----------------
    @Data
    public static class Customer {
        private Integer id;
        private String name;
        private String mobile;
        private String email;
        private String address;
        private Locations locations;

    }

    @Data
    public static class PickupDepot {
        private Integer id;
    }

    // ---------------- Info ----------------
    @Data
    public static class Info {
        private Integer id;
        private Integer type;
        private Integer returnFromOrderId;
        private Integer status;
        private BigDecimal discount;
        private UsedPoints usedPoints;
        private Vat vat;
        private Integer paymentMethod;
        private CreatedBy createdBy;
        private Sale sale;
        private TechnicalStaff technicalStaff;
        private CustomerCare customerCare;
        private Long createdAt;
        private Long updatedAt;
        private String description;
        private List<OrderPartDeliveries> orderPartDeliveries;
        private String trackingUrl;
    }

    @Data
    public static class UsedPoints {
        private BigDecimal points;
        private BigDecimal amount;
    }

    @Data
    public static class Vat {
        private BigDecimal percent;
        private BigDecimal amount;
    }

    @Data
    public static class CreatedBy {
        private Integer id;
        private String name;
    }

    @Data
    public static class Sale {
        private Integer id;
    }

    @Data
    public static class TechnicalStaff {
        private Integer id;
    }

    @Data
    public static class CustomerCare {
        private Integer id;
    }

    @Data
    public static class OrderPartDeliveries {
        private Integer id;
        private Integer type;
        private Integer totalQuantity;
        private BigDecimal totalProductAmount;
        private Integer status;
    }

    // ---------------- Product ----------------
    @Data
    public static class Product {
        private Integer id;
        private Integer typeId;
        private String code;                   // thuong null
        private String name;                   // thuong null
        private BigDecimal weight;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private Discount discount;
        private Integer available;
        private BigDecimal avgCost;
        private Vat vat;
        private Batch batch;
        private List<Combo> combos;
        private Unit unit;
        private List<Gift> gifts;
    }

    @Data
    public static class Discount {
        private BigDecimal percent;
        private BigDecimal amount;
    }

    @Data
    public static class Batch {
        private Integer id;
        private String name;
        private String manufactureDate;
        private String expiredDate;
    }

    @Data
    public static class Combo {
        private Integer id;
        private String code;
        private String name;
        private Integer quantity;
    }

    @Data
    public static class Unit {
        private Integer id;
        private String name;
        private Integer quantity;
        private Integer originalQuantity;
        private BigDecimal price;
    }

    @Data
    public static class Gift {
        private Integer id;
        private String name;
        private Integer quantity;
        private Integer originalQuantity;
    }

    // ---------------- Carrier ----------------
    @Data
    public static class Carrier {
        private Integer id;
        private String name;
        private OrderPackageSize orderPackageSize;
        private Service service;
        private String carrierCode;               // thuong null
        private Integer isPartDelivery;
        private SendCarrier sendCarrier;
        private String deliveryDate;
        private BigDecimal weight;
        private BigDecimal shipFee;
        private BigDecimal codFee;
        private BigDecimal declaredFee;
        private BigDecimal returnFee;
        private BigDecimal customerShipFee;
    }

    @Data
    public static class OrderPackageSize {
        private BigDecimal height;
        private BigDecimal merge;
        private BigDecimal length;
        private BigDecimal weight;
        private BigDecimal width;
    }

    @Data
    public static class Service {
        private Integer id;
        private String name;
        private String code;
        private Integer type;
    }

    @Data
    public static class SendCarrier {
        private Integer type;
    }

    // ---------------- Channel ----------------
    @Data
    public static class Channel {
        private String ecomOrderId;
        private Integer saleChannel;
        private TrafficSource trafficSource;
        private Facebook facebook;
    }

    @Data
    public static class TrafficSource {
        private Integer id;
        private String name;
    }

    @Data
    public static class Facebook {
        private Integer adId;
        private Integer postId;
    }
}
