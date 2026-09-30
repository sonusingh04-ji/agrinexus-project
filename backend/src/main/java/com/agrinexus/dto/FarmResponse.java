package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmResponse {

    private UUID id;

    private UUID farmerId;

    private String name;

    private Double areaAcres;

    private String location;

    private String stateName;

    private String districtName;

    private String subDistrictName;

    private Double latitude;

    private Double longitude;

    private LocalDateTime createdAt;
}