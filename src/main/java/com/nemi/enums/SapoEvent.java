package com.nemi.enums;

import com.nemi.constant.WebhookConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum SapoEvent {
    PRODUCT_ADD("products/create", WebhookConstants.SyncType.PRODUCT, WebhookConstants.EventType.ADD),
    PRODUCT_UPDATE("products/update", WebhookConstants.SyncType.PRODUCT, WebhookConstants.EventType.UPDATE),
    PRODUCT_DELETE("products/delete", WebhookConstants.SyncType.PRODUCT, WebhookConstants.EventType.DELETE),
    ORDER_ADD("orders/create", WebhookConstants.SyncType.ORDER, WebhookConstants.EventType.ADD),
    ORDER_UPDATE("orders/update", WebhookConstants.SyncType.ORDER, WebhookConstants.EventType.UPDATE),
    ORDER_DELETE("orders/delete", WebhookConstants.SyncType.ORDER, WebhookConstants.EventType.DELETE);

    private final String value;
    private final String syncType;
    private final String eventType;
    private static final Map<String, SapoEvent> CONSTANTS = new HashMap<>();

    static {
        for (SapoEvent e : SapoEvent.values()) {
            CONSTANTS.put(e.value, e);
        }
    }

    public static SapoEvent fromValue(String value) {
        return CONSTANTS.get(value);
    }
}