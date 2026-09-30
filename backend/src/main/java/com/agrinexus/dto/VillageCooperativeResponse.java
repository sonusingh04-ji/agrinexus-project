package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VillageCooperativeResponse {

    private UUID id;

    private String name;

    private String village;

    private String district;

    private String state;

    private String contactInfo;

    private UUID coordinatorFarmerId;

    private String status;

    private String dataStatus;

    private String source;

    private String notes;

    private LocalDateTime createdAt;

    private Integer memberCount;

    private Integer resourceCount;
}