package com.nemi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "product_manager", name = "pos")
@EqualsAndHashCode(callSuper = true)
public class PosEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;  // PK

    @Column(name = "user_id", nullable = false)
    private String userId;  // FK sang bảng user (nếu có quan hệ thì dùng @ManyToOne)

    @Column(name = "company_id", nullable = false)
    private String companyId;

    @Column(name = "pos_name", nullable = false, length = 100)
    // ten doi tac
    private String posName;

    //    @Lob uncoment when json is a long text
    @Column(name = "config", columnDefinition = "TEXT")
    private String config; // có thể để dạng JSON string

    @Column(name = "access_token", length = 255)
    private String accessToken;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "expired_time")
    private LocalDateTime expiredTime;

    @Column(name = "webhook_token")
    private String webhookToken;
}
