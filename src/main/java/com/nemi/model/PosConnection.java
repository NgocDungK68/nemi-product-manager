package com.nemi.model;

import com.nemi.constant.enums.Platform;
import com.nemi.constant.enums.PosStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pos_connection")
public class PosConnection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // PK

    @Column(name = "user_id", nullable = false)
    private Long userId;  // FK sang bảng user (nếu có quan hệ thì dùng @ManyToOne)


    @Column(name = "pos_name", nullable = false, length = 100)
    private String posName;

//    @Lob uncoment when json is a long text
    @Column(name = "config",columnDefinition = "TEXT")
    private String config; // có thể để dạng JSON string

    @Column(name = "access_token", length = 255)
    private String accessToken;


    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "expired_time")
    private LocalDateTime expiredTime;
}
