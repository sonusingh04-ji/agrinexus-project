package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VillageCooperativeRequest {

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
}