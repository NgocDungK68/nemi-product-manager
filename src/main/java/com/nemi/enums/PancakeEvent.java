package com.nemi.enums;

import com.nemi.constant.WebhookConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum PancakeEvent {
    INVENTORY_CHANGE("inventoryChange", WebhookConstants.SyncType.SYSTEM, WebhookConstants.EventType.INVENTORY_CHANGE),
    ORDER_ADD("create", WebhookConstants.SyncType.ORDER, WebhookConstants.EventType.ADD),
    ORDER_UPDATE("update", WebhookConstants.SyncType.ORDER, WebhookConstants.EventType.UPDATE),
    ORDER_DELETE("delete", WebhookConstants.SyncType.ORDER, WebhookConstants.EventType.DELETE);
    private static final Map<String, PancakeEvent> CONSTANTS = new HashMap<>();

    private final String value;
    private final String syncType;
    private final String eventType;


    static {
        for (PancakeEvent e : PancakeEvent.values()) {
            CONSTANTS.put(e.value, e);
        }
    }

    public static PancakeEvent fromValue(String value) {
        return CONSTANTS.get(value);
    }
}
