package com.agrinexus.entity;

import com.agrinexus.enums.AgriculturalReferenceType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "agricultural_references")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgriculturalReference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgriculturalReferenceType referenceType;

    @Column(length = 100)
    private String stateName;

    @Column(length = 150)
    private String districtName;

    @Column(length = 150)
    private String subDistrictName;

    @Column(length = 50)
    private String season;

    @Column(length = 80)
    private String cropStage;

    /* Nutrient reference values. Units remain source-defined. */
    private Double nitrogen;
    private Double phosphorus;
    private Double potassium;
    private Double sulphur;
    private Double zinc;

    /* Crop-water reference values. */
    private Double kcInitial;
    private Double kcDevelopment;
    private Double kcMid;
    private Double kcEnd;

    private Double rootDepthInitialCm;
    private Double rootDepthMidCm;
    private Double rootDepthEndCm;

    /* Soil and climate reference ranges. */
    private Double soilPhMin;
    private Double soilPhMax;
    private Double temperatureMin;
    private Double temperatureMax;

    /* Planning/reference descriptors. */
    @Column(length = 500)
    private String waterNeed;

    @Column(length = 500)
    private String sowingWindow;

    @Column(length = 300)
    private String seedRate;

    @Column(length = 300)
    private String rowSpacing;

    @Column(length = 200)
    private String sowingDepth;

    @Column(length = 1000)
    private String criticalWaterSensitiveStages;

    @Column(length = 1000)
    private String applicableConditions;

    /* Provenance. */
    @Column(length = 150)
    private String source;

    @Column(length = 500)
    private String sourceDocument;

    @Column(length = 1000)
    private String sourceUrl;

    private Integer publicationYear;

    @Column(length = 30)
    private String dataStatus;

    @Column(length = 100)
    private String version;

    @Column(length = 30)
    private String validFrom;

    @Column(length = 30)
    private String validTo;

    @Column(length = 2000)
    private String notes;
}
