package com.agrinexus.service;

import com.agrinexus.dto.SolarPumpResponse;
import com.agrinexus.entity.IrrigationTelemetry;
import com.agrinexus.entity.SolarPumpProfile;
import com.agrinexus.repository.IrrigationTelemetryRepository;
import com.agrinexus.repository.SolarPumpProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolarPumpService {

    private final SolarPumpProfileRepository profileRepository;
    private final IrrigationTelemetryRepository telemetryRepository;

    public SolarPumpResponse evaluate(
            UUID farmId
    ) {

        SolarPumpProfile profile =
                profileRepository.findByFarmId(farmId);

        IrrigationTelemetry telemetry =
                telemetryRepository
                        .findFirstByFarmIdOrderByRecordedAtDesc(
                                farmId
                        );

        if (profile == null) {

            return SolarPumpResponse.builder()
                    .farmId(farmId)
                    .decision("NO_SOLAR_PROFILE")
                    .recommendedWindow("UNKNOWN")
                    .recommendation(
                            "Solar pump configuration is not available."
                    )
                    .reason(
                            "Solar panel and pump parameters are required."
                    )
                    .dataStatus("NOT_AVAILABLE")
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        Double irradiance =
                telemetry != null
                        ? telemetry.getSolarIrradianceWm2()
                        : null;

        Double moisture =
                telemetry != null
                        ? telemetry.getVwc30cm()
                        : null;

        Double solarPower =
                telemetry != null
                        ? telemetry.getSolarPowerKw()
                        : null;

        Double battery =
                telemetry != null
                        ? telemetry.getBatterySocPercent()
                        : profile.getBatterySocPercent();

        Double tank =
                telemetry != null
                        ? telemetry.getStorageLevelPercent()
                        : null;

        /*
         * If solar irradiance is missing, the system cannot
         * confidently determine a current pumping window.
         */
        if (irradiance == null) {

            return SolarPumpResponse.builder()
                    .farmId(farmId)
                    .decision("INSUFFICIENT_DATA")
                    .recommendedWindow("UNKNOWN")
                    .currentMoisture(moisture)
                    .batterySoc(battery)
                    .tankLevel(tank)
                    .recommendation(
                            "Solar irradiance data is required."
                    )
                    .reason(
                            "The solar-pumping decision requires current or forecast solar availability."
                    )
                    .dataStatus(
                            resolveDataStatus(
                                    telemetry,
                                    profile
                            )
                    )
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        /*
         * Minimum acceptable solar irradiance.
         */
        Double threshold =
                profile.getMinimumSolarIrradianceWm2() != null
                        ? profile.getMinimumSolarIrradianceWm2()
                        : 200.0;

        boolean solarAvailable =
                irradiance >= threshold;

        /*
         * Current MVP demand check.
         *
         * This is only a prototype decision signal.
         */
        boolean irrigationLikelyNeeded =
                moisture == null || moisture < 30.0;

        String decision;

        String window;

        String recommendation;

        if (solarAvailable
                && irrigationLikelyNeeded) {

            decision = "PUMP_NOW";

            window = "CURRENT_SOLAR_WINDOW";

            recommendation =
                    "Current solar availability is suitable for evaluating pumping.";

        } else if (!solarAvailable
                && irrigationLikelyNeeded) {

            decision = "WAIT_FOR_SOLAR";

            window = "NEXT_HIGH_SOLAR_WINDOW";

            recommendation =
                    "Wait for a stronger solar window when irrigation is not urgent.";

        } else {

            decision = "DO_NOT_PUMP";

            window = "NOT_REQUIRED";

            recommendation =
                    "Do not pump solely because solar power is available; verify field demand first.";
        }

        return SolarPumpResponse.builder()
                .farmId(farmId)
                .decision(decision)
                .recommendedWindow(window)
                .solarIrradiance(irradiance)
                .solarPowerKw(solarPower)
                .pumpPowerKw(
                        profile.getPumpRatedPowerKw()
                )
                .currentMoisture(moisture)
                .batterySoc(battery)
                .tankLevel(tank)
                .recommendation(recommendation)
                .reason(
                        "Solar availability was evaluated against current field moisture and pump configuration."
                )
                .dataStatus(
                        resolveDataStatus(
                                telemetry,
                                profile
                        )
                )
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    /*
     * Resolve provenance from both telemetry and
     * solar-pump configuration.
     */
    private String resolveDataStatus(
            IrrigationTelemetry telemetry,
            SolarPumpProfile profile
    ) {

        boolean testTelemetry =
                telemetry != null
                        && (
                        "TEST".equalsIgnoreCase(
                                telemetry.getSensorStatus()
                        )
                                || "TEST_SENSOR".equalsIgnoreCase(
                                telemetry.getSource()
                        )
                );

        boolean testProfile =
                profile != null
                        && (
                        "TEST".equalsIgnoreCase(
                                profile.getDataStatus()
                        )
                                || "ESTIMATED".equalsIgnoreCase(
                                profile.getDataStatus()
                        )
                                || "TEST_CONFIGURATION".equalsIgnoreCase(
                                profile.getSource()
                        )
                );

        /*
         * Current demo/test scenario.
         */
        if (testTelemetry || testProfile) {
            return "TEST";
        }

        /*
         * Both real telemetry and reference/configuration
         * are being used.
         */
        if (telemetry != null
                && profile != null) {

            return "MIXED";
        }

        /*
         * Telemetry only.
         */
        if (telemetry != null) {

            if ("SENSOR".equalsIgnoreCase(
                    telemetry.getSource()
            )
                    || "IOT_SENSOR".equalsIgnoreCase(
                    telemetry.getSource()
            )) {

                return "MEASURED";
            }

            if ("CALCULATED".equalsIgnoreCase(
                    telemetry.getSource()
            )) {

                return "CALCULATED";
            }

            return "UNKNOWN";
        }

        /*
         * Configuration only.
         */
        if (profile != null) {

            return "REFERENCE";
        }

        return "UNKNOWN";
    }
}