package com.nemi.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum PancakEvent {

    INVENTORY_CHANGE("inventoryChange"),
    ORDER_ADD("create"),
    ORDER_UPDATE("update"),
    ORDER_DELETE("delete");
    private static final Map<String, PancakEvent> CONSTANTS = new HashMap<>();

    private final String value;

    PancakEvent(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }

    static {
        for (PancakEvent e : PancakEvent.values()) {
            CONSTANTS.put(e.value, e);
        }
    }

    public static PancakEvent fromValue(String value) {
        return CONSTANTS.get(value);
    }
}
