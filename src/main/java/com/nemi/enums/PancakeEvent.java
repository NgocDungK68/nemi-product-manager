package com.nemi.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum PancakeEvent {
    INVENTORY_CHANGE("inventoryChange"),
    ORDER_ADD("create"),
    ORDER_UPDATE("update"),
    ORDER_DELETE("delete");
    private static final Map<String, PancakeEvent> CONSTANTS = new HashMap<>();

    private final String value;

    PancakeEvent(String value) {
        this.value = value;
    }

    static {
        for (PancakeEvent e : PancakeEvent.values()) {
            CONSTANTS.put(e.value, e);
        }
    }

    public static PancakeEvent fromValue(String value) {
        return CONSTANTS.get(value);
    }
}
