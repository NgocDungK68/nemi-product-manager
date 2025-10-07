package com.nemi.enums;

import com.nemi.constant.WebhookConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum NhanhvnEvent {
    WEBHOOKS_ENABLED("webhooksEnabled", WebhookConstants.SyncType.SYSTEM, WebhookConstants.EventType.ENABLED),
    APP_UNINSTALLED("appUninstalled", WebhookConstants.SyncType.SYSTEM, WebhookConstants.EventType.UNINSTALLED),

    PRODUCT_ADD("productAdd", WebhookConstants.SyncType.PRODUCT, WebhookConstants.EventType.ADD),
    PRODUCT_UPDATE("productUpdate", WebhookConstants.SyncType.PRODUCT, WebhookConstants.EventType.UPDATE),
    PRODUCT_DELETE("productDelete", WebhookConstants.SyncType.PRODUCT, WebhookConstants.EventType.DELETE),
    INVENTORY_CHANGE("inventoryChange", WebhookConstants.SyncType.PRODUCT, WebhookConstants.EventType.INVENTORY_CHANGE),

    ORDER_ADD("orderAdd", WebhookConstants.SyncType.ORDER, WebhookConstants.EventType.ADD),
    ORDER_UPDATE("orderUpdate", WebhookConstants.SyncType.ORDER, WebhookConstants.EventType.UPDATE),
    ORDER_DELETE("orderDelete", WebhookConstants.SyncType.ORDER, WebhookConstants.EventType.DELETE),
    PAYMENT_RECEIVED("paymentReceived", WebhookConstants.SyncType.ORDER, WebhookConstants.EventType.PAYMENT_RECEIVED);

    private final String value;
    private final String syncType;
    private final String eventType;
    private static final Map<String, NhanhvnEvent> CONSTANTS = new HashMap<>();

    static {
        for (NhanhvnEvent e : NhanhvnEvent.values()) {
            CONSTANTS.put(e.value, e);
        }
    }

    public static NhanhvnEvent fromValue(String value) {
        return CONSTANTS.get(value);
    }
}