package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "irrigation_telemetry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrrigationTelemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * Farm associated with this telemetry record.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Optional device/node that produced the reading.
     */
    @Column(length = 64)
    private String deviceId;

    /*
     * UTC timestamp supplied by the device.
     */
    @Column(nullable = false)
    private LocalDateTime recordedAt;

    /*
     * ---------------------------------------------------------
     * Soil moisture / VWC
     * ---------------------------------------------------------
     *
     * Values are percentages.
     *
     * Member 2 specifies multi-depth VWC at:
     * 15 cm / 30 cm / 60 cm
     */
    private Double vwc15cm;

    private Double vwc30cm;

    private Double vwc60cm;

    /*
     * ---------------------------------------------------------
     * Pump / hydraulic telemetry
     * ---------------------------------------------------------
     */

    /*
     * 0 = OFF
     * 1 = ON
     */
    private Integer pumpState;

    /*
     * Flow rate in litres/minute.
     */
    private Double flowRateLpm;

    /*
     * Line pressure in bar.
     */
    private Double linePressureBar;

    /*
     * Water-table / observation-pipe depth in metres.
     */
    private Double waterTableDepthM;

    /*
     * Pump runtime represented by this telemetry sample,
     * when supplied by the device.
     */
    private Double pumpRuntimeMinutes;

    /*
     * ---------------------------------------------------------
     * Rainfall
     * ---------------------------------------------------------
     */

    /*
     * Local rain-gauge reading in mm for the
     * relevant telemetry interval.
     */
    private Double rainfallMm;

    /*
     * ---------------------------------------------------------
     * Weather / ET
     * ---------------------------------------------------------
     */

    /*
     * Reference evapotranspiration, mm/day.
     */
    private Double et0Mm;

    /*
     * Crop evapotranspiration, mm/day.
     *
     * ETc = Kc × ET0
     */
    private Double etcMm;

    /*
     * ---------------------------------------------------------
     * Solar
     * ---------------------------------------------------------
     */

    /*
     * Solar irradiance in W/m².
     */
    private Double solarIrradianceWm2;

    /*
     * Real-time solar panel power output in kW.
     */
    private Double solarPowerKw;

    /*
     * Optional cloud-cover percentage.
     */
    private Double cloudCoverPercent;

    /*
     * ---------------------------------------------------------
     * Storage / battery
     * ---------------------------------------------------------
     */

    /*
     * Tank or storage fill level.
     */
    private Double storageLevelPercent;

    /*
     * Battery state of charge, when a battery exists.
     */
    private Double batterySocPercent;

    /*
     * ---------------------------------------------------------
     * Crop-state context
     * ---------------------------------------------------------
     */

    /*
     * Crop growth stage at the time of this reading.
     */
    @Column(length = 50)
    private String cropStage;

    /*
     * Crop coefficient used for ETc calculation.
     */
    private Double kc;

    /*
     * ---------------------------------------------------------
     * Data/provenance
     * ---------------------------------------------------------
     */

    /*
     * Examples:
     * SENSOR
     * WEATHER_API
     * FARMER_ENTERED
     * ESTIMATED
     * CALCULATED
     */
    @Column(length = 40)
    private String source;

    /*
     * Optional sensor quality/status.
     */
    @Column(length = 40)
    private String sensorStatus;

    /*
     * Optional raw payload/reference identifier.
     */
    @Column(length = 100)
    private String payloadReference;

    /*
     * Notes and diagnostics.
     */
    @Column(length = 2000)
    private String notes;
}