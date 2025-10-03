package com.nemi.enums;

public enum SyncErrorMessage {
    MISSING_CONFIG("Missing required configuration for POS"),
    CONNECTION_FAILED("Failed to connect to POS"),
    TECHNICAL_ERROR("Technical error"),

    INVALID_CREDENTIAL("Invalid credentials: the provided token or third-party ID is not correct"),


    PRODUCT_CONNECTION_FAILED("No response from Pancake API when fetching products"),
    PRODUCT_INVALID_CREDENTIAL("Invalid API key or shopId when fetching products"),
    PRODUCT_TECHNICAL_ERROR("Unexpected system error while syncing products"),


    ORDER_CONNECTION_FAILED("No response from Pancake API when fetching orders"),
    ORDER_INVALID_CREDENTIAL("Invalid API key or shopId when fetching orders"),
    ORDER_TECHNICAL_ERROR("Unexpected system error while syncing orders");

    private final String message;

    SyncErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
