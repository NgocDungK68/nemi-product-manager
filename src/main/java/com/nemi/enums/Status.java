package com.nemi.enums;

import lombok.Getter;

@Getter
public enum Status {
    NEW("new"),
    PROCESSING("processing"),
    READY_TO_SHIP("ready_to_ship"),
    SHIPPING("shipping"),
    DELIVERED("delivered"),
    CANCELLED("cancelled"),
    FAILED("failed"),
    RETURNED("returned"),
    UNKNOWN("unknown");


    private final String value;

    Status(String value) {
        this.value = value;
    }

}
