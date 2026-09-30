package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoilTestTrendResponse {

    private UUID farmId;

    private String farmName;

    private LocalDate latestTestDate;

    private LocalDate nextSuggestedTestDate;

    /*
     * NOT_DUE
     * DUE_SOON
     * OVERDUE
     * NO_TEST_DATE
     */
    private String testReminderStatus;

    /*
     * Overall historical direction.
     *
     * IMPROVING
     * DECLINING
     * STABLE
     * MIXED
     * INSUFFICIENT_DATA
     */
    private String overallTrend;

    private Integer totalSoilSamples;

    private Integer measuredSoilSamples;

    private List<IndicatorTrend> indicators;

    private List<String> findings;

    private String recommendedAction;

    private String dataStatus;

    private String source;

    private String uncertaintyNote;


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndicatorTrend {

        private String indicator;

        private Double previousValue;

        private Double latestValue;

        private Double change;

        private String direction;

        /*
         * MEASURED
         * INSUFFICIENT_DATA
         */
        private String dataStatus;
    }
}