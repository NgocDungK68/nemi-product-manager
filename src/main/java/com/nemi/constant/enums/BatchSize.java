package com.nemi.constant.enums;

public enum BatchSize {
    PRODUCT(50),
    PRODUCT_VARIATION(100),
    ORDER(50),
    ORDER_ITEM(100),
    PAGE_NUMBER(1);

    private final int size;

    BatchSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
