package com.nemi.exception;


import com.nemi.exception.constant.AlertType;
import com.nemi.exception.pojo.AlertCode;
import com.nemi.exception.pojo.IAlertCode;

/**
 * Các loại lỗi, thuộc về Validation
 */
public enum ValidationAlertCode implements IAlertCode {
    POS_ALREADY_CONNECTED("400001", "Pos đã được connect", AlertType.ERROR),
    ARGUMENT_FIELD_INVALID("400002", "Tham số không hợp lệ", AlertType.ERROR),
    DATA_INVALID("400003", "Giá trị đầu vào không hợp lệ", AlertType.ERROR),
    EMAIL_EXISTED("400004", "Email đã tồn tại", AlertType.ERROR),
    PERMISSION_DENIED("400005", "Bạn không có quyền thực hiện hành động này", AlertType.ERROR),
    PASSWORD_NOT_VALID("400006", "Mật khẩu không hợp lệ", AlertType.ERROR),
    FULL_NAME_EXISTED("400007", "Tên người dùng đã tồn tại", AlertType.ERROR),
    DEPARTMENT_NAME_EXISTED("400008", "Tên phòng ban đã tồn tại trong công ty", AlertType.ERROR),
    DEPARTMENT_NOT_FOUND("400009", "Không tìm thấy phòng ban", AlertType.ERROR),
    DEPARTMENT_HAS_USERS("400010", "Không thể xóa phòng ban có nhân viên", AlertType.ERROR),
    ROLE_NAME_EXISTED("400011", "Tên vai trò đã tồn tại trong công ty", AlertType.ERROR),
    ROLE_NOT_FOUND("400012", "Không tìm thấy vai trò", AlertType.ERROR),
    ROLE_HAS_USERS("400013", "Không thể xóa vai trò có người dùng", AlertType.ERROR),
    COMPANY_NAME_EXISTED("400014", "Tên công ty đã tồn tại", AlertType.ERROR),
    COMPANY_NOT_FOUND("400015", "Không tìm thấy công ty", AlertType.ERROR),
    RESOURCE_CODE_NOT_FOUND("400016", "Mã tài nguyên không tồn tại", AlertType.ERROR),
    FUNCTION_CODE_NOT_FOUND("400017", "Mã chức năng không tồn tại hoặc không thuộc tài nguyên", AlertType.ERROR),
    PRIVILEGE_CODE_NOT_FOUND("400018", "Mã quyền không tồn tại hoặc không thuộc chức năng", AlertType.ERROR),
    ROLE_NOT_EXISTS("400019", "Vai trò không tồn tại", AlertType.ERROR),
    ROLE_NOT_BELONG_TO_COMPANY("400020", "Vai trò không thuộc về công ty", AlertType.ERROR),
    USER_NOT_FOUND("400021", "Không tìm thấy người dùng", AlertType.ERROR),
    EMAIL_ALREADY_EXISTS("400022", "Địa chỉ email đã tồn tại", AlertType.ERROR),
    ACCESS_DENIED("400023", "Truy cập bị từ chối", AlertType.ERROR),
    INVALID_PERMISSION_RESOURCE("400024", "Tài nguyên quyền không hợp lệ", AlertType.ERROR),
    INVALID_PERMISSION_FUNCTION("400025", "Chức năng quyền không hợp lệ", AlertType.ERROR),
    INVALID_PERMISSION_PRIVILEGE("400026", "Đặc quyền không hợp lệ", AlertType.ERROR),
    ROLE_NOT_ASSIGNED_TO_DEPARTMENT("400027", "Vai trò không được gán cho phòng ban", AlertType.ERROR),
    CURRENT_USER_ROLE_NOT_FOUND("400028", "Không tìm thấy vai trò người dùng hiện tại", AlertType.ERROR),
    INSUFFICIENT_ROLE_LEVEL("400029", "Không đủ quyền cấp vai trò", AlertType.ERROR),
    DEPARTMENT_NOT_BELONG_TO_COMPANY("400030", "Phòng ban không thuộc về công ty", AlertType.ERROR),
    ;

    private final AlertCode alertCode;

    ValidationAlertCode(final String code, final String label, final AlertType alertType) {
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
