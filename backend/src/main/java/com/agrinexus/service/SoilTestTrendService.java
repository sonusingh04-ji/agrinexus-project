package com.agrinexus.service;

import com.agrinexus.dto.SoilTestTrendResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.SoilSample;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.SoilSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SoilTestTrendService {

    private final FarmRepository farmRepository;

    private final SoilSampleRepository soilSampleRepository;

    /*
     * Default reminder interval = 12 months.
     *
     * You can override this in application.properties:
     *
     * agrinexus.soil-test.reminder-months=12
     */
    @Value("${agrinexus.soil-test.reminder-months:12}")
    private long reminderMonths;


    /*
     * =========================================================
     * Main method
     * =========================================================
     */

    public SoilTestTrendResponse getSoilTestTrend(
            UUID farmId
    ) {

        Farm farm =
                farmRepository.findById(farmId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + farmId
                                )
                        );


        /*
         * Retrieve all soil samples, newest first.
         */
        List<SoilSample> allSamples =
                soilSampleRepository
                        .findByFarmIdOrderBySampleDateDesc(
                                farmId
                        );


        if (allSamples == null) {
            allSamples = List.of();
        }


        /*
         * Only dated samples can be used for
         * chronological trend analysis.
         */
        List<SoilSample> datedSamples =
                allSamples.stream()
                        .filter(sample ->
                                sample.getSampleDate() != null
                        )
                        .sorted(
                                Comparator.comparing(
                                        SoilSample::getSampleDate
                                ).reversed()
                        )
                        .toList();


        /*
         * Latest dated soil sample.
         */
        SoilSample latestSample =
                datedSamples.isEmpty()
                        ? null
                        : datedSamples.get(0);


        /*
         * Previous dated soil sample.
         */
        SoilSample previousSample =
                datedSamples.size() < 2
                        ? null
                        : datedSamples.get(1);


        /*
         * =========================================================
         * Reminder calculation
         * =========================================================
         */

        LocalDate nextSuggestedTestDate = null;

        String reminderStatus;


        if (latestSample == null) {

            reminderStatus =
                    "NO_TEST_DATE";

        } else {

            nextSuggestedTestDate =
                    latestSample
                            .getSampleDate()
                            .plusMonths(
                                    Math.max(
                                            1,
                                            reminderMonths
                                    )
                            );

            reminderStatus =
                    determineReminderStatus(
                            nextSuggestedTestDate
                    );
        }


        /*
         * =========================================================
         * Indicator trends
         * =========================================================
         */

        List<SoilTestTrendResponse.IndicatorTrend>
                indicators =
                new ArrayList<>();


        addIndicator(
                indicators,
                "pH",
                previousSample != null
                        ? previousSample.getPH()
                        : null,
                latestSample != null
                        ? latestSample.getPH()
                        : null
        );


        addIndicator(
                indicators,
                "Electrical Conductivity",
                previousSample != null
                        ? previousSample.getElectricalConductivity()
                        : null,
                latestSample != null
                        ? latestSample.getElectricalConductivity()
                        : null
        );


        addIndicator(
                indicators,
                "Organic Carbon",
                previousSample != null
                        ? previousSample.getOrganicCarbon()
                        : null,
                latestSample != null
                        ? latestSample.getOrganicCarbon()
                        : null
        );


        addIndicator(
                indicators,
                "Nitrogen",
                previousSample != null
                        ? previousSample.getNitrogen()
                        : null,
                latestSample != null
                        ? latestSample.getNitrogen()
                        : null
        );


        addIndicator(
                indicators,
                "Phosphorus",
                previousSample != null
                        ? previousSample.getPhosphorus()
                        : null,
                latestSample != null
                        ? latestSample.getPhosphorus()
                        : null
        );


        addIndicator(
                indicators,
                "Potassium",
                previousSample != null
                        ? previousSample.getPotassium()
                        : null,
                latestSample != null
                        ? latestSample.getPotassium()
                        : null
        );


        addIndicator(
                indicators,
                "Zinc",
                previousSample != null
                        ? previousSample.getZinc()
                        : null,
                latestSample != null
                        ? latestSample.getZinc()
                        : null
        );


        addIndicator(
                indicators,
                "Sulphur",
                previousSample != null
                        ? previousSample.getSulphur()
                        : null,
                latestSample != null
                        ? latestSample.getSulphur()
                        : null
        );


        addIndicator(
                indicators,
                "Iron",
                previousSample != null
                        ? previousSample.getIron()
                        : null,
                latestSample != null
                        ? latestSample.getIron()
                        : null
        );


        addIndicator(
                indicators,
                "Manganese",
                previousSample != null
                        ? previousSample.getManganese()
                        : null,
                latestSample != null
                        ? latestSample.getManganese()
                        : null
        );


        addIndicator(
                indicators,
                "Copper",
                previousSample != null
                        ? previousSample.getCopper()
                        : null,
                latestSample != null
                        ? latestSample.getCopper()
                        : null
        );


        /*
         * =========================================================
         * Overall trend
         * =========================================================
         */

        String overallTrend =
                determineOverallTrend(
                        indicators
                );


        /*
         * =========================================================
         * Findings
         * =========================================================
         */

        List<String> findings =
                buildFindings(
                        latestSample,
                        previousSample,
                        indicators,
                        reminderStatus
                );


        /*
         * =========================================================
         * Recommended action
         * =========================================================
         */

        String recommendedAction =
                buildRecommendedAction(
                        latestSample,
                        previousSample,
                        reminderStatus,
                        overallTrend,
                        indicators
                );


        /*
         * =========================================================
         * Data status
         * =========================================================
         */

        String dataStatus =
                determineDataStatus(
                        latestSample,
                        previousSample
                );


        /*
         * =========================================================
         * Source
         * =========================================================
         */

        String source =
                determineSource(
                        latestSample
                );


        /*
         * =========================================================
         * Uncertainty
         * =========================================================
         */

        String uncertaintyNote =
                buildUncertaintyNote(
                        latestSample,
                        previousSample,
                        indicators
                );


        return SoilTestTrendResponse.builder()

                .farmId(
                        farm.getId()
                )

                .farmName(
                        farm.getName()
                )

                .latestTestDate(
                        latestSample != null
                                ? latestSample.getSampleDate()
                                : null
                )

                .nextSuggestedTestDate(
                        nextSuggestedTestDate
                )

                .testReminderStatus(
                        reminderStatus
                )

                .overallTrend(
                        overallTrend
                )

                .totalSoilSamples(
                        allSamples.size()
                )

                .measuredSoilSamples(
                        datedSamples.size()
                )

                .indicators(
                        indicators
                )

                .findings(
                        findings
                )

                .recommendedAction(
                        recommendedAction
                )

                .dataStatus(
                        dataStatus
                )

                .source(
                        source
                )

                .uncertaintyNote(
                        uncertaintyNote
                )

                .build();
    }


    /*
     * =========================================================
     * Add indicator
     * =========================================================
     */

    private void addIndicator(
            List<SoilTestTrendResponse.IndicatorTrend> indicators,
            String indicator,
            Double previousValue,
            Double latestValue
    ) {

        /*
         * We cannot calculate a change with missing
         * historical/current values.
         */
        if (previousValue == null
                || latestValue == null) {

            indicators.add(
                    SoilTestTrendResponse.IndicatorTrend
                            .builder()
                            .indicator(indicator)
                            .previousValue(previousValue)
                            .latestValue(latestValue)
                            .change(null)
                            .direction("INSUFFICIENT_DATA")
                            .dataStatus(
                                    latestValue != null
                                            ? "MEASURED_CURRENT_ONLY"
                                            : "INSUFFICIENT_DATA"
                            )
                            .build()
            );

            return;
        }


        double change =
                latestValue - previousValue;


        /*
         * Small numerical differences should not
         * automatically be treated as a trend.
         */
        double tolerance =
                Math.max(
                        0.000001,
                        Math.abs(previousValue) * 0.01
                );


        String direction;

        if (change > tolerance) {

            direction = "INCREASING";

        } else if (change < -tolerance) {

            direction = "DECREASING";

        } else {

            direction = "STABLE";
        }


        indicators.add(
                SoilTestTrendResponse.IndicatorTrend
                        .builder()
                        .indicator(indicator)
                        .previousValue(previousValue)
                        .latestValue(latestValue)
                        .change(round(change))
                        .direction(direction)
                        .dataStatus("MEASURED")
                        .build()
        );
    }


    /*
     * =========================================================
     * Reminder status
     * =========================================================
     */

    private String determineReminderStatus(
            LocalDate nextTestDate
    ) {

        if (nextTestDate == null) {
            return "NO_TEST_DATE";
        }


        LocalDate today =
                LocalDate.now();


        if (today.isAfter(nextTestDate)) {

            return "OVERDUE";
        }


        long daysUntilTest =
                ChronoUnit.DAYS.between(
                        today,
                        nextTestDate
                );


        /*
         * 30-day reminder window.
         */
        if (daysUntilTest <= 30) {

            return "DUE_SOON";
        }


        return "NOT_DUE";
    }


    /*
     * =========================================================
     * Overall trend
     * =========================================================
     *
     * Important:
     *
     * This describes observed numerical direction,
     * not agronomic "good/bad" classification.
     *
     * For example, an increasing pH is not automatically
     * an improvement.
     * =========================================================
     */

    private String determineOverallTrend(
            List<SoilTestTrendResponse.IndicatorTrend>
                    indicators
    ) {

        int increasing = 0;
        int decreasing = 0;
        int stable = 0;


        for (
                SoilTestTrendResponse.IndicatorTrend indicator
                : indicators
        ) {

            if ("INCREASING".equals(
                    indicator.getDirection()
            )) {

                increasing++;

            } else if ("DECREASING".equals(
                    indicator.getDirection()
            )) {

                decreasing++;

            } else if ("STABLE".equals(
                    indicator.getDirection()
            )) {

                stable++;
            }
        }


        int measured =
                increasing
                        + decreasing
                        + stable;


        if (measured == 0) {

            return "INSUFFICIENT_DATA";
        }


        if (increasing > 0
                && decreasing > 0) {

            return "MIXED";
        }


        if (increasing > decreasing
                && increasing > stable) {

            return "IMPROVING";
        }


        if (decreasing > increasing
                && decreasing > stable) {

            return "DECLINING";
        }


        return "STABLE";
    }


    /*
     * =========================================================
     * Findings
     * =========================================================
     */

    private List<String> buildFindings(
            SoilSample latestSample,
            SoilSample previousSample,
            List<SoilTestTrendResponse.IndicatorTrend>
                    indicators,
            String reminderStatus
    ) {

        List<String> findings =
                new ArrayList<>();


        if (latestSample == null) {

            findings.add(
                    "No dated soil test is currently available."
            );

            findings.add(
                    "A future soil test date cannot be determined "
                            + "until a dated soil sample is recorded."
            );

            return findings;
        }


        findings.add(
                "Latest dated soil test: "
                        + latestSample.getSampleDate()
        );


        if (previousSample == null) {

            findings.add(
                    "Only one dated soil test is available, "
                            + "so historical trend comparison "
                            + "is limited."
            );

        } else {

            findings.add(
                    "Historical comparison is available "
                            + "against the previous dated soil test "
                            + "from "
                            + previousSample.getSampleDate()
            );
        }


        for (
                SoilTestTrendResponse.IndicatorTrend indicator
                : indicators
        ) {

            if ("INSUFFICIENT_DATA".equals(
                    indicator.getDirection()
            )) {

                continue;
            }


            if ("MEASURED_CURRENT_ONLY".equals(
                    indicator.getDataStatus()
            )) {

                continue;
            }


            findings.add(
                    indicator.getIndicator()
                            + " is "
                            + indicator.getDirection()
                            + " by "
                            + indicator.getChange()
                            + " between the two latest "
                            + "dated measurements."
            );
        }


        switch (reminderStatus) {

            case "OVERDUE" ->
                    findings.add(
                            "The configured soil-test reminder "
                                    + "date has passed."
                    );

            case "DUE_SOON" ->
                    findings.add(
                            "The next configured soil-test "
                                    + "reminder is due soon."
                    );

            case "NOT_DUE" ->
                    findings.add(
                            "The latest dated soil test is "
                                    + "not yet due for the next "
                                    + "configured reminder."
                    );

            case "NO_TEST_DATE" ->
                    findings.add(
                            "No valid dated soil test is "
                                    + "available for scheduling."
                    );

            default -> {
            }
        }


        return findings;
    }


    /*
     * =========================================================
     * Recommended action
     * =========================================================
     */

    private String buildRecommendedAction(
            SoilSample latestSample,
            SoilSample previousSample,
            String reminderStatus,
            String overallTrend,
            List<SoilTestTrendResponse.IndicatorTrend>
                    indicators
    ) {

        if (latestSample == null) {

            return
                    "Record a dated laboratory soil test before "
                            + "using historical trend detection.";
        }


        if ("OVERDUE".equals(reminderStatus)) {

            return
                    "Arrange the next appropriate soil test "
                            + "according to the farm's validated "
                            + "soil-testing program.";
        }


        if ("DUE_SOON".equals(reminderStatus)) {

            return
                    "Plan the next soil test according to the "
                            + "farm's validated soil-testing schedule.";
        }


        if (previousSample == null) {

            return
                    "Keep the latest soil-test record and obtain "
                            + "another dated test when appropriate "
                            + "so a historical trend can be established.";
        }


        boolean hasDecline =
                indicators.stream()
                        .anyMatch(
                                indicator ->
                                        "DECREASING".equals(
                                                indicator.getDirection()
                                        )
                        );


        if (hasDecline) {

            return
                    "Review the declining soil indicators with "
                            + "the next appropriate soil test and "
                            + "qualified local agronomic guidance.";
        }


        if ("IMPROVING".equals(
                overallTrend
        )) {

            return
                    "Continue monitoring the measured soil "
                            + "indicators and compare them with "
                            + "future dated soil tests.";
        }


        return
                "Continue periodic soil testing and monitor "
                        + "the historical indicators over time.";
    }


    /*
     * =========================================================
     * Data status
     * =========================================================
     */

    private String determineDataStatus(
            SoilSample latestSample,
            SoilSample previousSample
    ) {

        if (latestSample == null) {

            return "NO_MEASURED_SOIL_TEST";
        }


        String latestSource =
                latestSample.getSource();


        if (previousSample == null) {

            return normalizeStatus(
                    latestSource,
                    "MEASURED_CURRENT_ONLY"
            );
        }


        String previousSource =
                previousSample.getSource();


        if (latestSource == null
                && previousSource == null) {

            return "MEASURED_RECORDS_SOURCE_UNSPECIFIED";
        }


        return "MIXED_MEASURED_HISTORY";
    }


    /*
     * =========================================================
     * Source
     * =========================================================
     */

    private String determineSource(
            SoilSample latestSample
    ) {

        if (latestSample == null) {

            return "NO_SOIL_TEST";
        }


        if (latestSample.getSource() == null
                || latestSample.getSource().isBlank()) {

            return "SOIL_SAMPLE_RECORD";
        }


        return latestSample.getSource();
    }


    /*
     * =========================================================
     * Uncertainty
     * =========================================================
     */

    private String buildUncertaintyNote(
            SoilSample latestSample,
            SoilSample previousSample,
            List<SoilTestTrendResponse.IndicatorTrend>
                    indicators
    ) {

        StringBuilder note =
                new StringBuilder();


        note.append(
                "Trend detection compares stored dated soil "
                        + "measurements and does not invent missing "
                        + "values."
        );


        if (latestSample == null) {

            note.append(
                    " No dated soil test is available."
            );

            return note.toString();
        }


        if (previousSample == null) {

            note.append(
                    " Only one dated soil test is available, "
                            + "so historical comparison is limited."
            );
        }


        boolean missingIndicators =
                indicators.stream()
                        .anyMatch(
                                indicator ->
                                        "INSUFFICIENT_DATA".equals(
                                                indicator.getDirection()
                                        )
                        );


        if (missingIndicators) {

            note.append(
                    " One or more soil indicators lack "
                            + "complete historical/current values."
            );
        }


        note.append(
                " Numerical direction such as increasing or "
                        + "decreasing does not by itself establish "
                        + "that a soil condition is agronomically "
                        + "better or worse."
        );


        note.append(
                " Soil-test frequency should follow the farm's "
                        + "validated agronomic program or local guidance."
        );


        return note.toString();
    }


    /*
     * =========================================================
     * Utility
     * =========================================================
     */

    private String normalizeStatus(
            String source,
            String fallback
    ) {

        if (source == null
                || source.isBlank()) {

            return fallback;
        }


        if ("LABORATORY".equalsIgnoreCase(source)
                || "SOIL_TEST".equalsIgnoreCase(source)) {

            return "MEASURED";
        }


        return source.toUpperCase();
    }


    private double round(
            double value
    ) {

        return Math.round(
                value * 10000.0
        ) / 10000.0;
    }
}