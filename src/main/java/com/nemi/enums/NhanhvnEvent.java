package com.nemi.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum NhanhvnEvent {
    WEBHOOKS_ENABLED("webhooksEnabled"),
    APP_UNINSTALLED("appUninstalled"),
    PRODUCT_ADD("productAdd"),
    PRODUCT_UPDATE("productUpdate"),
    PRODUCT_DELETE("productDelete"),
    INVENTORY_CHANGE("inventoryChange"),
    ORDER_ADD("orderAdd"),
    ORDER_UPDATE("orderUpdate"),
    ORDER_DELETE("orderDelete"),
    PAYMENT_RECEIVED("paymentReceived");

    private final String value;
    private static final Map<String, NhanhvnEvent> CONSTANTS = new HashMap<>();

    NhanhvnEvent(String value) {
        this.value = value;
    }

    static {
        for (NhanhvnEvent e : NhanhvnEvent.values()) {
            CONSTANTS.put(e.value, e);
        }
    }

    public static NhanhvnEvent fromValue(String value) {
        return CONSTANTS.get(value);
    }
}