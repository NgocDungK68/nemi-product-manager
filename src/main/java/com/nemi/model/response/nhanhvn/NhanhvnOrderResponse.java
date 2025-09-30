package com.nemi.model.response.nhanhvn;

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
public class NhanhvnOrderResponse {
    private int code;
    private Paginator paginator;
    private List<OrderData> data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Paginator {
        private Object next;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Next {
            private long id;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderData {
        private Depot depot;
        private Info info;
        private Channel channel;
        private ShippingAddress shippingAddress;
        private List<Product> products;
        private Carrier carrier;
        private Payment payment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Depot {
        private long id;
        private Pickup pickup;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Pickup {
            private long id;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Info {
        private long id;
        private int type;
        private int mode;
        private int exportType;
        private long originalOrderId;
        private long returnFromId;
        private long idReturn;
        private long createdById;
        private long saleId;
        private long technicalStaffId;
        private long packedById;
        private long customerCareId;
        private long createdAt;
        private long updatedAt;
        private long confirmedAt;
        private long packedAt;
        private String description;
        private int orderIndex;
        private int status;
        private int reason;
        private List<Long> handoverIds;
        private List<String> tags;
        private String trackingUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Channel {
        private String appOrderId;
        private int saleChannel;
        private String trafficSource;
        private long fbAdsId;
        private long fbPsid;
        private String shopId;
        private Affiliate affiliate;
        private List<Object> marketing;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Affiliate {
            private BigDecimal value;
            private BigDecimal bonus;
            private BigDecimal bonusRate;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddress {
        private long id;
        private String name;
        private String mobile;
        private String address;
        private int cityId;
        private int districtId;
        private int wardId;
        private String location;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private long id;
        private int typeId;
        private String name;
        private BigDecimal weight;
        private Integer quantity;
        private BigDecimal originalQuantity;
        private BigDecimal originalPrice;
        private BigDecimal price;
        private BigDecimal discount;
        private BigDecimal avgCost;
        private BigDecimal vat;
        private BigDecimal vatAmount;
        private String imeiId;
        private BigDecimal transactionFee;
        private int paymentStatus;
        private int partialReturnStatus;
        private BigDecimal usedPoints;
        private BigDecimal usedPointAmount;
        private List<Object> batch;
        private List<Object> unit;
        private List<Object> gifts;
        private List<Object> combos;
        private String shippingWeight;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Carrier {
        private long id;
        private String name;
        private Service service;
        private String carrierCode;
        private int checkGoods;
        private int isPartDelivery;
        private int isDeclaredFee;
        private int sendCarrierType;
        private long sendCarrierAt;
        private long deliveryAt;
        private BigDecimal declaredValue;
        private BigDecimal declaredFee;
        private BigDecimal weight;
        private BigDecimal shipFee;
        private BigDecimal codFee;
        private BigDecimal ecomFee;
        private BigDecimal overWeightShipFee;
        private BigDecimal returnFee;
        private BigDecimal customerShipFee;
        private int carrierPaymentStatus;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Service {
            private long id;
            private String code;
            private int type;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payment {
        private UsedPoints usedPoints;
        private Vat vat;
        private Discount discount;
        private Deposit deposit;
        private Transfer transfer;
        private Credit credit;
        private BigDecimal codAmount;
        private BigDecimal businessPayment;
        private int businessPaymentStatus;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class UsedPoints {
            private BigDecimal points;
            private BigDecimal amount;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Vat {
            private BigDecimal amount;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Discount {
            private BigDecimal amount;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Deposit {
            private BigDecimal amount;
            private long accountId;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Transfer {
            private BigDecimal amount;
            private long accountId;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Credit {
            private BigDecimal amount;
            private long accountId;
        }
    }
}