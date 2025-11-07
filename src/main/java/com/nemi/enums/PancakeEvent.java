package com.nemi.enums;

import com.nemi.constant.WebhookConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum PancakeEvent {

    ORDER("orders", WebhookConstants.SyncType.ORDER),
    PRODUCT("products", WebhookConstants.SyncType.PRODUCT);
    private static final Map<String, PancakeEvent> CONSTANTS = new HashMap<>();

    private final String value;
    private final String syncType;
    static {
        for (PancakeEvent e : PancakeEvent.values()) {
            CONSTANTS.put(e.value, e);
        }
    }

    public static PancakeEvent fromValue(String value) {
        return CONSTANTS.get(value);
    }
}
