package com.nemi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DepartmentRoleId implements Serializable {

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "role_id")
    private Long roleId;

    public DepartmentRoleId() {
    }

    public DepartmentRoleId(Long departmentId, Long roleId) {
        this.departmentId = departmentId;
        this.roleId = roleId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepartmentRoleId that = (DepartmentRoleId) o;
        return Objects.equals(departmentId, that.departmentId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(departmentId, roleId);
    }
}


