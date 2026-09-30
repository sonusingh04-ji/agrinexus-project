package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CooperativeMemberResponse {

    private UUID id;

    private UUID cooperativeId;

    private UUID farmerId;

    private UUID farmId;

    private String farmName;

    private String role;

    private String status;

    private LocalDateTime joinedAt;

    private String notes;
}