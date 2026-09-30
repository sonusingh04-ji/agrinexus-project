package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LulcResponse {

    private UUID id;

    private UUID farmId;

    private String farmName;

    private String location;

    private Double latitude;

    private Double longitude;

    // ---------------------------------------------------------
    // DATA SOURCE
    // ---------------------------------------------------------

    private String dataSource;

    private String productName;

    private String collectionId;

    private Integer baseYear;

    private Integer spatialResolutionMeters;

    private Double aoiRadiusMeters;

    // ---------------------------------------------------------
    // LULC CLASS PERCENTAGES
    // ---------------------------------------------------------

    private Double treeCoverPercent;

    private Double shrublandPercent;

    private Double grasslandPercent;

    private Double croplandPercent;

    private Double herbaceousWetlandPercent;

    private Double mangrovePercent;

    private Double mossLichenPercent;

    private Double bareSparseVegetationPercent;

    private Double builtUpPercent;

    private Double waterPercent;

    private Double snowIcePercent;

    private Double unclassifiedPercent;

    // ---------------------------------------------------------
    // DERIVED VALUES
    // ---------------------------------------------------------

    private String dominantClass;

    private Double naturalVegetationPercent;

    private Double classifiedCoveragePercent;

    // ---------------------------------------------------------
    // ANALYSIS
    // ---------------------------------------------------------

    private String analysisNote;

    private LocalDateTime analyzedAt;
}