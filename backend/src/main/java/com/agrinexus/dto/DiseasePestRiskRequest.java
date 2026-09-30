package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiseasePestRiskRequest {

    private UUID farmId;

    private UUID cropId;
}