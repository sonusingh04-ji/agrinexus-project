package com.agrinexus.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowingAdvisorRequest {

    /*
     * Farm for which the sowing decision
     * is required.
     */
    private UUID farmId;

    /*
     * Crop that the farmer wants to sow.
     */
    private UUID cropId;

    /*
     * Optional assessment date.
     *
     * If not provided, the service can use
     * the current date.
     */
    private LocalDate assessmentDate;
}