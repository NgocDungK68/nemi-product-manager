package com.nemi.model.response.pancake;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PancakeOrderResponse {
    private Aggs aggs;
    private List<DataItem> data;
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Aggs {
        private ValueWrapper cod;
        @JsonProperty("partner_fee")
        private ValueWrapper partnerFee;
        private ValueWrapper prepaid;
        @JsonProperty("shipping_fee")
        private ValueWrapper shippingFee;
        private StatusAgg status;
        private TagAgg tag;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ValueWrapper {
        private BigDecimal value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusAgg {
        private List<Bucket> buckets;
        @JsonProperty("doc_count_error_upper_bound")
        private Integer docCountErrorUpperBound;
        @JsonProperty("sum_other_doc_count")
        private Integer sumOtherDocCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TagAgg {
        private List<Bucket> buckets;
        @JsonProperty("doc_count")
        private Integer docCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bucket {
        @JsonProperty("doc_count")
        private Integer docCount;
        private String key;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataItem {
        private String id; // String để nhận cả số và text
        @JsonProperty("system_id")
        private Long systemId;
        private String note;
        private Integer status;
        @JsonProperty("status_name")
        private String statusName;
        @JsonProperty("total_quantity")
        private Integer totalQuantity;
        @JsonProperty("total_price")
        private BigDecimal totalPrice;
        @JsonProperty("total_price_after_sub_discount")
        private BigDecimal totalPriceAfterSubDiscount;
        private Customer customer;
        @JsonProperty("warehouse_info")
        private WarehouseInfo warehouseInfo;
        private List<Item> items;
        @JsonProperty("status_history")
        private List<StatusHistory> statusHistory;
        private Partner partner;

        @JsonProperty("bill_full_name")
        private String billFullName;
        @JsonProperty("bill_phone_number")
        private String billPhoneNumber;
        @JsonProperty("page_id")
        private String pageId;
        private Creator creator;
        @JsonProperty("inserted_at")
        private String insertedAt;
        @JsonProperty("updated_at")
        private String updatedAt;
        @JsonProperty("is_free_shipping")
        private Boolean isFreeShipping;
        @JsonProperty("is_livestream")
        private Boolean isLivestream;
        @JsonProperty("is_live_shopping")
        private Boolean isLiveShopping;
        @JsonProperty("is_smc")
        private Boolean isSmc;
        @JsonProperty("received_at_shop")
        private Boolean receivedAtShop;
        @JsonProperty("partner_fee")
        private BigDecimal partnerFee;
        @JsonProperty("post_id")
        private String postId;
        @JsonProperty("customer_pay_fee")
        private Boolean customerPayFee;
        @JsonProperty("sub_status")
        private Integer subStatus;
        @JsonProperty("note_print")
        private String notePrint;
        @JsonProperty("returned_reason")
        private Integer returnedReason;
        @JsonProperty("payment_purchase_histories")
        private List<PaymentPurchaseHistory> paymentPurchaseHistories;
        @JsonProperty("warehouse_id")
        private String warehouseId;
        @JsonProperty("shipping_address")
        private ShippingAddress shippingAddress;
        @JsonProperty("shipping_fee")
        private BigDecimal shippingFee;
        @JsonProperty("shop_id")
        private Long shopId;
        private List<Tag> tags;
        @JsonProperty("total_discount")
        private BigDecimal totalDiscount;
        @JsonProperty("shopify_abandon_checkout_id")
        private Long shopifyAbandonCheckoutId;
        @JsonProperty("link_confirm_order")
        private String linkConfirmOrder;
        @JsonProperty("pke_mkter")
        private String pkeMkter;
        private Marketer marketer;
        @JsonProperty("ads_source")
        private String adsSource;
        @JsonProperty("bank_payments")
        private Map<String, Object> bankPayments;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Customer {
        @JsonAlias({"id","customer_id"})
        private String id;
        private String name;
        @JsonProperty("phone_numbers")
        private List<String> phoneNumbers;
        @JsonProperty("shop_customer_addresses")
        private List<CustomerAddress> shopCustomerAddresses;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerAddress {
        private String id;
        private String fullName;
        private String fullAddress;
        @JsonProperty("phone_number")
        private String phoneNumber;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WarehouseInfo {
        private String name;
        private String address;
        @JsonProperty("full_address")
        private String fullAddress;
        @JsonProperty("phone_number")
        private String phoneNumber;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String id; // để string vì có khi trả text
        private Integer quantity;
        @JsonProperty("product_id")
        private String productId;
        @JsonProperty("variation_id")
        private String variationId;
        @JsonProperty("variation_info")
        private VariationInfo variationInfo;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VariationInfo {
        private String name;
        private String barcode;
        @JsonProperty("display_id")
        private String displayId;
        @JsonProperty("product_display_id")
        private String productDisplayId;
        private List<String> images;
        @JsonProperty("retail_price")
        private BigDecimal retailPrice;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusHistory {
        private Integer status;
        @JsonProperty("updated_at")
        private String updatedAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Partner {
        private BigDecimal cod;
        @JsonProperty("extend_code")
        private String extendCode;
        @JsonProperty("extend_update")
        private List<ExtendUpdate> extendUpdate;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExtendUpdate {
        private String key;
        private String note;
        private String status;
        @JsonProperty("tracking_id")
        private String trackingId;
        @JsonProperty("update_at")
        private String updateAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Creator {
        private String id;
        private String name;
        @JsonProperty("fb_id")
        private String fbId;
        @JsonProperty("avatar_url")
        private String avatarUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentPurchaseHistory {
        private BigDecimal amount;
        @JsonProperty("inserted_at")
        private String insertedAt;
        @JsonProperty("ipn_reply")
        private Boolean ipnReply;
        @JsonProperty("result_code")
        private String resultCode;
        private List<String> tags;
        private String type;
        @JsonProperty("voucher_amount")
        private BigDecimal voucherAmount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShippingAddress {
        private String address;
        @JsonProperty("commune_id")
        private String communeId;
        @JsonProperty("district_id")
        private String districtId;
        @JsonProperty("province_id")
        private String provinceId;
        @JsonProperty("full_name")
        private String fullName;
        @JsonProperty("phone_number")
        private String phoneNumber;
        @JsonProperty("full_address")
        private String fullAddress;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tag {
        private Long id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Marketer {
        private String id;
        private String name;
        private String email;
        @JsonProperty("fb_id")
        private String fbId;
        @JsonProperty("phone_number")
        private String phoneNumber;
        @JsonProperty("avatar_url")
        private String avatarUrl;
    }
}
