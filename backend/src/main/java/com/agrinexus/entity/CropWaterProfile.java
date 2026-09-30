package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "crop_water_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropWaterProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    /**
     * Geographic scope of the reference record.
     * Blank/null means a national/global fallback reference.
     */
    @Column(length = 100)
    private String stateName;

    @Column(length = 150)
    private String districtName;

    /**
     * Agricultural season for which the reference is intended.
     * Blank/null means season-independent fallback.
     */
    @Column(length = 50)
    private String season;

    private Double kcInitial;
    private Double kcDevelopment;
    private Double kcMid;
    private Double kcEnd;

    private Double rootDepthInitialCm;
    private Double rootDepthMidCm;
    private Double rootDepthEndCm;

    private Integer initialDays;
    private Integer developmentDays;
    private Integer midDays;
    private Integer lateDays;

    @Column(length = 100)
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
