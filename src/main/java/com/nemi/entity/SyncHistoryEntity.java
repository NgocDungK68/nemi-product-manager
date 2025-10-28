package com.nemi.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_history", schema = "product_manager")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "pos_id", nullable = false, length = 36)
    private String posId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "sync_status", nullable = false, length = 50)
    private String syncStatus;

    @Column(name = "error_message", length = 100)
    private String errorMessage;

    @Column(name = "sync_type",length = 50)
    private String syncType;

    @Column(name = "pos_name")
    private String posName;


}
