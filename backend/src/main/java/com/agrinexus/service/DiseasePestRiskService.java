package com.agrinexus.service;

import com.agrinexus.entity.Crop;
import com.agrinexus.entity.DiseasePestRisk;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.WeatherRecord;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.DiseasePestRiskRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.WeatherRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiseasePestRiskService {

    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final WeatherRecordRepository weatherRecordRepository;
    private final DiseasePestRiskRepository riskRepository;


    /*
     * ============================================================
     * EVALUATE CURRENT DISEASE / PEST ENVIRONMENTAL RISK
     * ============================================================
     *
     * Uses:
     * - Existing farm
     * - Existing crop
     * - Latest stored weather observation
     *
     * No artificial weather or crop records are created here.
     */
    @Transactional
    public List<DiseasePestRisk> evaluate(
            UUID farmId,
            UUID cropId
    ) {

        if (farmId == null) {
            throw new IllegalArgumentException(
                    "Farm ID is required"
            );
        }

        /*
         * --------------------------------------------------------
         * Find real farm
         * --------------------------------------------------------
         */
        Farm farm =
                farmRepository.findById(farmId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + farmId
                                )
                        );


        /*
         * --------------------------------------------------------
         * Find crop
         * --------------------------------------------------------
         *
         * Disease/pest evaluation should normally be crop-aware.
         */
        if (cropId == null) {

            throw new IllegalArgumentException(
                    "Crop ID is required for disease/pest evaluation"
            );
        }


        Crop crop =
                cropRepository.findById(cropId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Crop not found with ID: "
                                                + cropId
                                )
                        );


        /*
         * --------------------------------------------------------
         * Get latest REAL weather observation
         * --------------------------------------------------------
         */
        WeatherRecord weather =
                weatherRecordRepository
                        .findByFarmIdOrderByRecordedAtDesc(
                                farmId
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);


        /*
         * --------------------------------------------------------
         * Do not generate a risk without weather evidence.
         * --------------------------------------------------------
         */
        if (weather == null) {

            throw new RuntimeException(
                    "No weather record available for risk evaluation"
            );
        }


        List<DiseasePestRisk> risks =
                new ArrayList<>();


        /*
         * --------------------------------------------------------
         * Extract actual weather observations
         * --------------------------------------------------------
         */
        Double temperature =
                weather.getTemperature();

        Integer humidity =
                weather.getHumidity();

        Double rainfall =
                weather.getRainfall();

        Double windSpeed =
                weather.getWindSpeed();


        /*
         * --------------------------------------------------------
         * Evaluate fungal environmental conditions
         * --------------------------------------------------------
         */
        DiseasePestRisk fungalRisk =
                evaluateFungalRisk(
                        farm,
                        crop,
                        temperature,
                        humidity,
                        rainfall
                );

        if (fungalRisk != null) {

            risks.add(fungalRisk);
        }


        /*
         * --------------------------------------------------------
         * Evaluate insect environmental conditions
         * --------------------------------------------------------
         */
        DiseasePestRisk insectRisk =
                evaluateInsectRisk(
                        farm,
                        crop,
                        temperature,
                        humidity,
                        rainfall,
                        windSpeed
                );

        if (insectRisk != null) {

            risks.add(insectRisk);
        }


        /*
         * --------------------------------------------------------
         * Evaluate extreme heat crop stress.
         * --------------------------------------------------------
         *
         * This is NOT a disease diagnosis.
         */
        DiseasePestRisk heatRisk =
                evaluateHeatStressRisk(
                        farm,
                        crop,
                        temperature
                );

        if (heatRisk != null) {

            risks.add(heatRisk);
        }


        /*
         * --------------------------------------------------------
         * If there is no detected signal, return an empty list.
         *
         * We do NOT create a fake "LOW RISK" database record.
         * --------------------------------------------------------
         */
        if (risks.isEmpty()) {

            return List.of();
        }


        /*
         * --------------------------------------------------------
         * Save calculated risk signals.
         * --------------------------------------------------------
         */
        return riskRepository.saveAll(risks);
    }


    /*
     * ============================================================
     * FUNGAL ENVIRONMENTAL SCREENING
     * ============================================================
     *
     * IMPORTANT:
     *
     * This is a generic environmental screening signal.
     *
     * It does NOT identify a particular fungal disease.
     */
    private DiseasePestRisk evaluateFungalRisk(
            Farm farm,
            Crop crop,
            Double temperature,
            Integer humidity,
            Double rainfall
    ) {

        if (humidity == null
                && temperature == null
                && rainfall == null) {

            return null;
        }


        int score = 0;

        List<String> factors =
                new ArrayList<>();


        /*
         * --------------------------------------------------------
         * Humidity contribution
         * --------------------------------------------------------
         */
        if (humidity != null) {

            if (humidity >= 85) {

                score += 50;

                factors.add(
                        "Relative humidity is "
                                + humidity
                                + "%"
                );

            } else if (humidity >= 75) {

                score += 35;

                factors.add(
                        "Relative humidity is "
                                + humidity
                                + "%"
                );
            }
        }


        /*
         * --------------------------------------------------------
         * Temperature contribution
         * --------------------------------------------------------
         */
        if (temperature != null) {

            if (temperature >= 18
                    && temperature <= 32) {

                score += 25;

                factors.add(
                        "Temperature is "
                                + temperature
                                + "°C"
                );
            }
        }


        /*
         * --------------------------------------------------------
         * Rainfall contribution
         * --------------------------------------------------------
         */
        if (rainfall != null) {

            if (rainfall >= 10) {

                score += 25;

                factors.add(
                        "Recorded rainfall is "
                                + rainfall
                                + " mm"
                );

            } else if (rainfall >= 5) {

                score += 15;

                factors.add(
                        "Recorded rainfall is "
                                + rainfall
                                + " mm"
                );
            }
        }


        /*
         * --------------------------------------------------------
         * No meaningful environmental signal.
         * --------------------------------------------------------
         */
        if (score < 35) {

            return null;
        }


        /*
         * --------------------------------------------------------
         * Include crop context.
         * --------------------------------------------------------
         */
        String cropContext =
                buildCropContext(crop);

        if (cropContext != null) {

            factors.add(cropContext);
        }


        return DiseasePestRisk.builder()

                .farm(farm)

                .crop(crop)

                .riskCategory(
                        "FUNGAL_DISEASE"
                )

                .riskName(
                        "Fungal Disease Environmental Risk"
                )

                .riskLevel(
                        riskLevel(score)
                )

                .riskScore(
                        normalizedScore(score)
                )

                .reason(
                        "Current environmental observations "
                                + "show conditions that may be "
                                + "favorable for fungal disease "
                                + "development."
                )

                .recommendation(
                        "Inspect the crop for disease symptoms "
                                + "and follow locally recommended "
                                + "integrated pest management practices. "
                                + "Do not apply treatment solely from "
                                + "this environmental signal."
                )

                .scoutingAdvice(
                        "Inspect representative crop areas, "
                                + "especially dense or humid zones, "
                                + "for visible symptoms before taking "
                                + "control decisions."
                )

                .contributingFactors(
                        String.join(
                                "; ",
                                factors
                        )
                )

                .source(
                        "RULE_ENGINE"
                )

                .dataStatus(
                        "CALCULATED"
                )

                .uncertaintyNote(
                        "Environmental screening only. "
                                + "This result does not confirm a "
                                + "specific fungal disease. Crop variety, "
                                + "growth stage, field observations and "
                                + "local surveillance should be considered."
                )

                .forecastBased(
                        false
                )

                .evaluatedAt(
                        LocalDateTime.now()
                )

                .validUntil(
                        LocalDateTime.now()
                                .plusHours(24)
                )

                .build();
    }


    /*
     * ============================================================
     * INSECT PEST ENVIRONMENTAL SCREENING
     * ============================================================
     *
     * This does NOT identify a particular pest species.
     */
    private DiseasePestRisk evaluateInsectRisk(
            Farm farm,
            Crop crop,
            Double temperature,
            Integer humidity,
            Double rainfall,
            Double windSpeed
    ) {

        if (temperature == null
                && humidity == null
                && rainfall == null) {

            return null;
        }


        int score = 0;

        List<String> factors =
                new ArrayList<>();


        /*
         * --------------------------------------------------------
         * Temperature contribution
         * --------------------------------------------------------
         */
        if (temperature != null) {

            if (temperature >= 20
                    && temperature <= 35) {

                score += 35;

                factors.add(
                        "Temperature is "
                                + temperature
                                + "°C"
                );
            }
        }


        /*
         * --------------------------------------------------------
         * Humidity contribution
         * --------------------------------------------------------
         */
        if (humidity != null) {

            if (humidity >= 60) {

                score += 20;

                factors.add(
                        "Relative humidity is "
                                + humidity
                                + "%"
                );
            }
        }


        /*
         * --------------------------------------------------------
         * Rainfall contribution
         * --------------------------------------------------------
         */
        if (rainfall != null) {

            if (rainfall >= 10) {

                score += 20;

                factors.add(
                        "Recorded rainfall is "
                                + rainfall
                                + " mm"
                );
            }
        }


        /*
         * --------------------------------------------------------
         * Wind contribution
         * --------------------------------------------------------
         */
        if (windSpeed != null
                && windSpeed > 30) {

            score -= 10;

            factors.add(
                    "High wind speed of "
                            + windSpeed
                            + " may affect insect activity "
                            + "and observation conditions"
            );
        }


        /*
         * --------------------------------------------------------
         * Keep score inside valid range.
         * --------------------------------------------------------
         */
        score =
                Math.max(
                        0,
                        Math.min(
                                score,
                                100
                        )
                );


        /*
         * --------------------------------------------------------
         * No meaningful signal.
         * --------------------------------------------------------
         */
        if (score < 30) {

            return null;
        }


        /*
         * --------------------------------------------------------
         * Crop context.
         * --------------------------------------------------------
         */
        String cropContext =
                buildCropContext(crop);

        if (cropContext != null) {

            factors.add(cropContext);
        }


        return DiseasePestRisk.builder()

                .farm(farm)

                .crop(crop)

                .riskCategory(
                        "INSECT_PEST"
                )

                .riskName(
                        "General Insect Pest Environmental Risk"
                )

                .riskLevel(
                        riskLevel(score)
                )

                .riskScore(
                        normalizedScore(score)
                )

                .reason(
                        "Current environmental observations "
                                + "may be compatible with increased "
                                + "insect-pest activity."
                )

                .recommendation(
                        "Scout the crop for actual pest presence "
                                + "or feeding damage before considering "
                                + "any control measure."
                )

                .scoutingAdvice(
                        "Inspect leaves, shoots and other crop parts "
                                + "for visible insects, feeding damage "
                                + "or unusual plant symptoms."
                )

                .contributingFactors(
                        String.join(
                                "; ",
                                factors
                        )
                )

                .source(
                        "RULE_ENGINE"
                )

                .dataStatus(
                        "CALCULATED"
                )

                .uncertaintyNote(
                        "General environmental screening only. "
                                + "This result does not identify a "
                                + "specific pest species or confirm "
                                + "an infestation. Field scouting and "
                                + "local surveillance are required."
                )

                .forecastBased(
                        false
                )

                .evaluatedAt(
                        LocalDateTime.now()
                )

                .validUntil(
                        LocalDateTime.now()
                                .plusHours(24)
                )

                .build();
    }


    /*
     * ============================================================
     * EXTREME HEAT CROP-STRESS SCREENING
     * ============================================================
     *
     * This is environmental crop stress.
     *
     * It must NOT be interpreted as a disease diagnosis.
     */
    private DiseasePestRisk evaluateHeatStressRisk(
            Farm farm,
            Crop crop,
            Double temperature
    ) {

        if (temperature == null) {

            return null;
        }


        /*
         * --------------------------------------------------------
         * No extreme heat signal.
         * --------------------------------------------------------
         */
        if (temperature < 40) {

            return null;
        }


        int score;


        if (temperature >= 45) {

            score = 85;

        } else {

            score = 70;
        }


        List<String> factors =
                new ArrayList<>();

        factors.add(
                "Temperature is "
                        + temperature
                        + "°C"
        );


        String cropContext =
                buildCropContext(crop);

        if (cropContext != null) {

            factors.add(cropContext);
        }


        return DiseasePestRisk.builder()

                .farm(farm)

                .crop(crop)

                .riskCategory(
                        "ENVIRONMENTAL_STRESS"
                )

                .riskName(
                        "Extreme Heat Crop Stress Risk"
                )

                .riskLevel(
                        riskLevel(score)
                )

                .riskScore(
                        normalizedScore(score)
                )

                .reason(
                        "Extreme heat may stress the crop "
                                + "and affect normal crop growth "
                                + "and health."
                )

                .recommendation(
                        "Monitor crop condition closely and "
                                + "follow validated crop-specific "
                                + "heat-stress and irrigation guidance."
                )

                .scoutingAdvice(
                        "Inspect the field for visible heat-related "
                                + "crop stress and unusual symptoms."
                )

                .contributingFactors(
                        String.join(
                                "; ",
                                factors
                        )
                )

                .source(
                        "RULE_ENGINE"
                )

                .dataStatus(
                        "CALCULATED"
                )

                .uncertaintyNote(
                        "This is an environmental stress warning "
                                + "and is not evidence of a specific "
                                + "pest or disease."
                )

                .forecastBased(
                        false
                )

                .evaluatedAt(
                        LocalDateTime.now()
                )

                .validUntil(
                        LocalDateTime.now()
                                .plusHours(24)
                )

                .build();
    }


    /*
     * ============================================================
     * CROP CONTEXT
     * ============================================================
     *
     * Uses information already stored against the real crop.
     */
    private String buildCropContext(
            Crop crop
    ) {

        if (crop == null) {

            return null;
        }


        List<String> cropDetails =
                new ArrayList<>();


        if (crop.getName() != null
                && !crop.getName().isBlank()) {

            cropDetails.add(
                    "Crop=" + crop.getName()
            );
        }


        if (crop.getCategory() != null
                && !crop.getCategory().isBlank()) {

            cropDetails.add(
                    "Category=" + crop.getCategory()
            );
        }


        if (crop.getSeason() != null
                && !crop.getSeason().isBlank()) {

            cropDetails.add(
                    "Season=" + crop.getSeason()
            );
        }


        if (crop.getIdealTemperatureMin() != null
                && crop.getIdealTemperatureMax() != null) {

            cropDetails.add(
                    "Stored crop temperature range="
                            + crop.getIdealTemperatureMin()
                            + "–"
                            + crop.getIdealTemperatureMax()
                            + "°C"
            );
        }


        if (cropDetails.isEmpty()) {

            return null;
        }


        return String.join(
                ", ",
                cropDetails
        );
    }


    /*
     * ============================================================
     * RISK LEVEL
     * ============================================================
     */
    private String riskLevel(
            int score
    ) {

        if (score >= 70) {

            return "HIGH";
        }


        if (score >= 40) {

            return "MEDIUM";
        }


        return "LOW";
    }


    /*
     * ============================================================
     * NORMALIZE SCORE
     * ============================================================
     */
    private double normalizedScore(
            int score
    ) {

        if (score < 0) {

            return 0.0;
        }


        if (score > 100) {

            return 100.0;
        }


        return score;
    }


    /*
     * ============================================================
     * GET RISK BY ID
     * ============================================================
     */
    public DiseasePestRisk getById(
            UUID id
    ) {

        return riskRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Disease/pest risk not found with ID: "
                                        + id
                        )
                );
    }


    /*
     * ============================================================
     * GET ALL RISKS FOR FARM
     * ============================================================
     */
    public List<DiseasePestRisk> getByFarm(
            UUID farmId
    ) {

        if (!farmRepository.existsById(
                farmId
        )) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }


        return riskRepository
                .findByFarmIdOrderByEvaluatedAtDesc(
                        farmId
                );
    }


    /*
     * ============================================================
     * GET HIGH-RISK RESULTS
     * ============================================================
     */
    public List<DiseasePestRisk> getHighRisk(
            UUID farmId
    ) {

        if (!farmRepository.existsById(
                farmId
        )) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }


        return riskRepository
                .findByFarmIdAndRiskLevelOrderByEvaluatedAtDesc(
                        farmId,
                        "HIGH"
                );
    }


    /*
     * ============================================================
     * GET FORECAST-BASED RESULTS
     * ============================================================
     */
    public List<DiseasePestRisk> getForecastRisks(
            UUID farmId
    ) {

        if (!farmRepository.existsById(
                farmId
        )) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }


        return riskRepository
                .findByFarmIdAndForecastBasedOrderByEvaluatedAtDesc(
                        farmId,
                        true
                );
    }
}