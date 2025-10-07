package com.nemi.enums;

public enum PosName {
    NHANHVN("nhanhvn"),
    SAPO("sapo"),
    PANCAKE("pancake"),
    WEBHOOK("webhook");

    private final String value;

    PosName(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getKey() {
        return this.name(); // Trả về tên enum
    }
}
