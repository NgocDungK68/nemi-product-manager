package com.nemi.constant;

public final class WebhookConstants {
    public static final String CREATED_BY = "webhook";
    public static final String UNKNOWN = "unknown";

    public static final class Status {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILED = "FAILED";
    }

    public static final class SyncType {
        public static final String ORDER = "ORDER";
        public static final String PRODUCT = "PRODUCT";
        public static final String SYSTEM = "SYSTEM";
    }

    public static final class EventType {
        public static final String ADD = "ADD";
        public static final String UPDATE = "UPDATE";
        public static final String DELETE = "DELETE";
        public static final String ENABLED = "ENABLED";
        public static final String UNINSTALLED = "UNINSTALLED";
        public static final String INVENTORY_CHANGE = "INVENTORY_CHANGE";
        public static final String PAYMENT_RECEIVED = "PAYMENT_RECEIVED";
    }

    private WebhookConstants() {
    }
}
