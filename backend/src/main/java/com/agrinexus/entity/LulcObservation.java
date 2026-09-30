package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "lulc_observations",
        indexes = {
                @Index(
                        name = "idx_lulc_farm_id",
                        columnList = "farm_id"
                ),
                @Index(
                        name = "idx_lulc_analyzed_at",
                        columnList = "analyzed_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LulcObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "farm_id",
            nullable = false
    )
    private Farm farm;

    // ---------------------------------------------------------
    // DATA SOURCE
    // ---------------------------------------------------------

    @Column(name = "data_source", nullable = false)
    private String dataSource;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "collection_id", nullable = false)
    private String collectionId;

    @Column(name = "base_year", nullable = false)
    private Integer baseYear;

    @Column(name = "spatial_resolution_meters")
    private Integer spatialResolutionMeters;

    @Column(name = "aoi_radius_meters")
    private Double aoiRadiusMeters;

    // ---------------------------------------------------------
    // LULC CLASS PERCENTAGES
    // ---------------------------------------------------------

    @Column(name = "tree_cover_percent")
    private Double treeCoverPercent;

    @Column(name = "shrubland_percent")
    private Double shrublandPercent;

    @Column(name = "grassland_percent")
    private Double grasslandPercent;

    @Column(name = "cropland_percent")
    private Double croplandPercent;

    @Column(name = "herbaceous_wetland_percent")
    private Double herbaceousWetlandPercent;

    @Column(name = "mangrove_percent")
    private Double mangrovePercent;

    @Column(name = "moss_lichen_percent")
    private Double mossLichenPercent;

    @Column(name = "bare_sparse_vegetation_percent")
    private Double bareSparseVegetationPercent;

    @Column(name = "built_up_percent")
    private Double builtUpPercent;

    @Column(name = "water_percent")
    private Double waterPercent;

    @Column(name = "snow_ice_percent")
    private Double snowIcePercent;

    @Column(name = "unclassified_percent")
    private Double unclassifiedPercent;

    // ---------------------------------------------------------
    // DERIVED VALUES
    // ---------------------------------------------------------

    @Column(name = "dominant_class")
    private String dominantClass;

    @Column(name = "natural_vegetation_percent")
    private Double naturalVegetationPercent;

    @Column(name = "classified_coverage_percent")
    private Double classifiedCoveragePercent;

    // ---------------------------------------------------------
    // ANALYSIS METADATA
    // ---------------------------------------------------------

    @Column(
            name = "analysis_note",
            length = 2000
    )
    private String analysisNote;

    @Column(
            name = "analyzed_at",
            nullable = false
    )
    private LocalDateTime analyzedAt;

    // ---------------------------------------------------------
    // JPA
    // ---------------------------------------------------------

    @PrePersist
    protected void onCreate() {

        if (analyzedAt == null) {
            analyzedAt = LocalDateTime.now();
        }
    }
}