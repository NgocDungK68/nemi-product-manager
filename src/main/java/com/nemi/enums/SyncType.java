package com.nemi.enums;

import lombok.Getter;

@Getter
public enum SyncType {
    ORDER("ORDER"),
    PRODUCT("PRODUCT")
    ;

    private final String value;

    SyncType(String message) {
        this.value = message;
    }
}
