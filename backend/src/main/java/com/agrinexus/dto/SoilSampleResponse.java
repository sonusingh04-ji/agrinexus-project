package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilSampleResponse {

    private UUID id;

    private UUID farmId;

    private LocalDate sampleDate;

    private String soilType;

    private Double pH;

    private Double electricalConductivity;

    private Double organicCarbon;

    private Double nitrogen;

    private Double phosphorus;

    private Double potassium;

    private Double zinc;

    private Double sulphur;

    private Double iron;

    private Double manganese;

    private Double copper;

    private String source;

    private String notes;
}