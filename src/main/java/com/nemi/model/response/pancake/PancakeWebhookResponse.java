package com.nemi.model.response.pancake;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PancakeWebhookResponse {

    @JsonProperty("type")
    private String type;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("inserted_at")
    private String insertedAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("id")
    private String id;

    @JsonProperty("warehouse_id")
    private String warehouseId;

    @JsonProperty("variation_id")
    private String variationId;

    @JsonProperty("change_quantity")
    private Integer changeQuantity;

    @JsonProperty("remain_quantity")
    private Integer remainQuantity;

    @JsonProperty("actual_remain_quantity")
    private Integer actualRemainQuantity;

    @JsonProperty("is_actual_remain_quantity")
    private Boolean actualRemainQuantityFlag;

    @JsonProperty("status_name")
    private String statusName;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("total_price")
    private Double totalPrice;

    @JsonProperty("cod")
    private Double cod;

    @JsonProperty("shipping_fee")
    private Double shippingFee;

    @JsonProperty("total_discount")
    private Double totalDiscount;

    @JsonProperty("money_to_collect")
    private Double moneyToCollect;

    private PancakeOrderResponse.Partner partner;

    @JsonProperty("prepaid")
    private Double prepaid;

    @JsonProperty("bill_full_name")
    private String billFullName;

    @JsonProperty("bill_phone_number")
    private String billPhoneNumber;

    @JsonProperty("bill_email")
    private String billEmail;

    @JsonProperty("payment_purchase_histories")
    private List<PancakeOrderResponse.PaymentPurchaseHistory> paymentPurchaseHistories;

    @JsonProperty("note")
    private String note;

    @JsonProperty("note_print")
    private String notePrint;

    @JsonProperty("link")
    private String link;

    @JsonProperty("order_link")
    private String orderLink;

    private List<PancakeOrderResponse.Item> items;

    @JsonProperty("shipping_address")
    private PancakeOrderResponse.ShippingAddress shippingAddress;




}
