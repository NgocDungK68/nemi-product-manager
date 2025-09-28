package com.nemi.constant.enums;

import lombok.Getter;

@Getter
public enum WeightUnit {
    GAM("g"),
    KILOGRAM("kg");

    private final String value;

    WeightUnit(String value) {
        this.value = value;
    }
}
