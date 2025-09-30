package com.nemi.constant.enums;

public enum OrderStatus {
    NEW("new"),
    PROCESSING("processing"),
    READY_TO_SHIP("ready_to_ship"),
    SHIPPING("shipping"),
    DELIVERED("delivered"),
    CANCELLED("cancelled"),
    FAILED("failed"),
    RETURNED("returned");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
