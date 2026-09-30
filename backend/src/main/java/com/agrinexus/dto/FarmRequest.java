package com.agrinexus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FarmRequest {

    @NotNull(message = "Farmer ID is required")
    private UUID farmerId;

    @NotBlank(message = "Farm name is required")
    private String name;

    /*
     * Farm area in acres.
     */
    private Double areaAcres;

    private String location;

    private String stateName;

    private String districtName;

    private String subDistrictName;

    private Double latitude;

    private Double longitude;
}