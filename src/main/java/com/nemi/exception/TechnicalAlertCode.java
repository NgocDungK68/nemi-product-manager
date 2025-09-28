package com.nemi.exception;

import com.nemi.exception.constant.AlertType;
import com.nemi.exception.pojo.AlertCode;
import com.nemi.exception.pojo.IAlertCode;

/**
 * Các loại lỗi kỹ thuật (Technical Errors)
 */
public enum TechnicalAlertCode implements IAlertCode {
    DATABASE_ERROR("500001", "Lỗi cơ sở dữ liệu", AlertType.ERROR),
    SYSTEM_ERROR("500002", "Lỗi hệ thống", AlertType.ERROR),
    KEYCLOAK_ERROR("500003", "Lỗi tích hợp Keycloak", AlertType.ERROR),
    POS_CONNECTION_FAILED("500004", "Lỗi kết nối POS", AlertType.ERROR),
    STATUS_NOT_EXIST("500005","status không hợp lệ",AlertType.ERROR),
    POS_CONNECTION_NOTFOUND("500006", "Không tìm thấy POS đã kết nối", AlertType.ERROR),
    POS_STATUS_NOTFOUND("500007", "Không tìm thấy POS đã kết nối", AlertType.ERROR),
    DATA_INVALID("500008", "Thông tin không hợp lệ", AlertType.ERROR),
    DATA_PERSISTENCE_ERROR("500009", "Lỗi lưu trữ dữ liệu", AlertType.ERROR)
    ;

    private final AlertCode alertCode;

    TechnicalAlertCode(final String code, final String label, final AlertType alertType) {
        alertCode = new AlertCode(code, label, alertType);
    }

    @Override
    public String getCode() {
        return alertCode.getCode();
    }

    @Override
    public String getLabel() {
        return alertCode.getLabel();
    }

    @Override
    public AlertType getType() {
        return alertCode.getType();
    }

    @Override
    public String getUserMessage() {
        return "";
    }

    @Override
    public String getErrorSubCode() {
        return "";
    }
}
