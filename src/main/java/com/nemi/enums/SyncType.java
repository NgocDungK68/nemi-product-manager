package com.nemi.enums;

import lombok.Getter;

@Getter
public enum SyncType {
    ORDER("order and order_item"),
    PRODUCT("product and product_variant")
    ;

    private final String value;

    SyncType(String message) {
        this.value = message;
    }
}
