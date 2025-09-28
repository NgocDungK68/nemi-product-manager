package com.nemi.constant.enums;

public enum SyncErrorMessage {
    MISSING_CONFIG("Missing required configuration for POS"),
    CONNECTION_FAILED("Failed to connect to POS"),
    TECHNICAL_ERROR("Technical error");

    private final String message;

    SyncErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
