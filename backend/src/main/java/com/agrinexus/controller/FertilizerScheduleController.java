package com.agrinexus.controller;

import com.agrinexus.dto.FertilizerScheduleRequest;
import com.agrinexus.dto.FertilizerScheduleResponse;
import com.agrinexus.service.FertilizerScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fertilizer-schedules")
@RequiredArgsConstructor
public class FertilizerScheduleController {

    private final FertilizerScheduleService fertilizerScheduleService;

    /*
     * Create a fertilizer schedule.
     *
     * POST /api/fertilizer-schedules
     */
    @PostMapping
    public ResponseEntity<FertilizerScheduleResponse> createSchedule(
            @RequestBody FertilizerScheduleRequest request
    ) {

        FertilizerScheduleResponse response =
                fertilizerScheduleService.createSchedule(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * Get schedule by ID.
     *
     * GET /api/fertilizer-schedules/{scheduleId}
     */
    @GetMapping("/{scheduleId}")
    public ResponseEntity<FertilizerScheduleResponse> getScheduleById(
            @PathVariable UUID scheduleId
    ) {

        return ResponseEntity.ok(
                fertilizerScheduleService.getScheduleById(
                        scheduleId
                )
        );
    }

    /*
     * Get all schedules for a farm.
     *
     * GET /api/fertilizer-schedules/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<FertilizerScheduleResponse>>
    getFarmSchedules(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                fertilizerScheduleService.getFarmSchedules(
                        farmId
                )
        );
    }

    /*
     * Get schedules for a crop.
     *
     * GET /api/fertilizer-schedules/farm/{farmId}/crop/{cropId}
     */
    @GetMapping("/farm/{farmId}/crop/{cropId}")
    public ResponseEntity<List<FertilizerScheduleResponse>>
    getCropSchedules(
            @PathVariable UUID farmId,
            @PathVariable UUID cropId
    ) {

        return ResponseEntity.ok(
                fertilizerScheduleService.getCropSchedules(
                        farmId,
                        cropId
                )
        );
    }

    /*
     * Get schedules by status.
     *
     * Example:
     * GET /api/fertilizer-schedules/farm/{farmId}/status/PLANNED
     */
    @GetMapping("/farm/{farmId}/status/{status}")
    public ResponseEntity<List<FertilizerScheduleResponse>>
    getSchedulesByStatus(
            @PathVariable UUID farmId,
            @PathVariable String status
    ) {

        return ResponseEntity.ok(
                fertilizerScheduleService.getSchedulesByStatus(
                        farmId,
                        status
                )
        );
    }

    /*
     * Get schedules within a date range.
     *
     * Example:
     * GET /api/fertilizer-schedules/farm/{farmId}/range
     * ?startDate=2026-10-01&endDate=2026-12-31
     */
    @GetMapping("/farm/{farmId}/range")
    public ResponseEntity<List<FertilizerScheduleResponse>>
    getSchedulesByDateRange(
            @PathVariable UUID farmId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {

        return ResponseEntity.ok(
                fertilizerScheduleService.getSchedulesByDateRange(
                        farmId,
                        startDate,
                        endDate
                )
        );
    }

    /*
     * Get schedules for a particular crop stage.
     *
     * Example:
     * GET /api/fertilizer-schedules/farm/{farmId}/crop/{cropId}/stage/BASAL
     */
    @GetMapping("/farm/{farmId}/crop/{cropId}/stage/{stage}")
    public ResponseEntity<List<FertilizerScheduleResponse>>
    getSchedulesByStage(
            @PathVariable UUID farmId,
            @PathVariable UUID cropId,
            @PathVariable String stage
    ) {

        return ResponseEntity.ok(
                fertilizerScheduleService.getSchedulesByStage(
                        farmId,
                        cropId,
                        stage
                )
        );
    }

    /*
     * Get latest schedule for a farm.
     *
     * GET /api/fertilizer-schedules/farm/{farmId}/latest
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<FertilizerScheduleResponse>
    getLatestSchedule(
            @PathVariable UUID farmId
    ) {

        FertilizerScheduleResponse response =
                fertilizerScheduleService.getLatestSchedule(
                        farmId
                );

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /*
     * Get next planned schedule.
     *
     * GET /api/fertilizer-schedules/farm/{farmId}/next
     * ?date=2026-08-28
     *
     * date is optional; today is used when omitted.
     */
    @GetMapping("/farm/{farmId}/next")
    public ResponseEntity<FertilizerScheduleResponse>
    getNextSchedule(
            @PathVariable UUID farmId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {

        FertilizerScheduleResponse response =
                fertilizerScheduleService.getNextSchedule(
                        farmId,
                        date
                );

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /*
     * Get schedules requiring reassessment.
     *
     * GET /api/fertilizer-schedules/farm/{farmId}/reassessment
     */
    @GetMapping("/farm/{farmId}/reassessment")
    public ResponseEntity<List<FertilizerScheduleResponse>>
    getReassessmentSchedules(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                fertilizerScheduleService
                        .getReassessmentSchedules(farmId)
        );
    }

    /*
     * Update schedule.
     *
     * PUT /api/fertilizer-schedules/{scheduleId}
     */
    @PutMapping("/{scheduleId}")
    public ResponseEntity<FertilizerScheduleResponse>
    updateSchedule(
            @PathVariable UUID scheduleId,
            @RequestBody FertilizerScheduleRequest request
    ) {

        return ResponseEntity.ok(
                fertilizerScheduleService.updateSchedule(
                        scheduleId,
                        request
                )
        );
    }

    /*
     * Delete schedule.
     *
     * DELETE /api/fertilizer-schedules/{scheduleId}
     */
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<String> deleteSchedule(
            @PathVariable UUID scheduleId
    ) {

        fertilizerScheduleService.deleteSchedule(
                scheduleId
        );

        return ResponseEntity.ok(
                "Fertilizer schedule deleted successfully"
        );
    }
}