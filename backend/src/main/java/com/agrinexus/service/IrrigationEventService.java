package com.agrinexus.service;

import com.agrinexus.dto.IrrigationEventRequest;
import com.agrinexus.dto.IrrigationEventResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.IrrigationEvent;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.IrrigationEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IrrigationEventService {

    private final IrrigationEventRepository irrigationEventRepository;
    private final FarmRepository farmRepository;

    public IrrigationEventResponse createEvent(
            IrrigationEventRequest request
    ) {

        validate(request);

        Farm farm =
                farmRepository.findById(request.getFarmId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + request.getFarmId()
                                )
                        );

        Double duration =
                request.getDurationMinutes();

        if (duration == null
                && request.getStartTime() != null
                && request.getEndTime() != null) {

            duration = Duration.between(
                    request.getStartTime(),
                    request.getEndTime()
            ).toSeconds() / 60.0;
        }

        IrrigationEvent event =
                IrrigationEvent.builder()
                        .farm(farm)
                        .deviceId(request.getDeviceId())
                        .startTime(request.getStartTime())
                        .endTime(request.getEndTime())
                        .durationMinutes(duration)
                        .volumeLiters(request.getVolumeLiters())
                        .triggerReason(request.getTriggerReason())
                        .completionStatus(
                                request.getCompletionStatus() != null
                                        ? request.getCompletionStatus()
                                        : "RECORDED"
                        )
                        .moistureBefore(request.getMoistureBefore())
                        .moistureAfter(request.getMoistureAfter())
                        .targetMoisture(request.getTargetMoisture())
                        .pumpWasOn(request.getPumpWasOn())
                        .notes(request.getNotes())
                        .build();

        return toResponse(
                irrigationEventRepository.save(event)
        );
    }

    public IrrigationEventResponse getById(UUID id) {

        return toResponse(
                irrigationEventRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Irrigation event not found"
                                )
                        )
        );
    }

    public List<IrrigationEventResponse> getFarmEvents(
            UUID farmId
    ) {

        verifyFarm(farmId);

        return irrigationEventRepository
                .findByFarmIdOrderByStartTimeDesc(farmId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<IrrigationEventResponse> getRange(
            UUID farmId,
            LocalDateTime start,
            LocalDateTime end
    ) {

        verifyFarm(farmId);

        return irrigationEventRepository
                .findByFarmIdAndStartTimeBetweenOrderByStartTimeAsc(
                        farmId,
                        start,
                        end
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validate(
            IrrigationEventRequest request
    ) {

        if (request == null) {
            throw new RuntimeException("Request cannot be null");
        }

        if (request.getFarmId() == null) {
            throw new RuntimeException("Farm ID is required");
        }

        if (request.getStartTime() == null) {
            throw new RuntimeException("Start time is required");
        }

        if (request.getEndTime() != null
                && request.getEndTime()
                .isBefore(request.getStartTime())) {

            throw new RuntimeException(
                    "End time cannot be before start time"
            );
        }
    }

    private void verifyFarm(UUID farmId) {

        if (!farmRepository.existsById(farmId)) {
            throw new RuntimeException(
                    "Farm not found with ID: " + farmId
            );
        }
    }

    private IrrigationEventResponse toResponse(
            IrrigationEvent event
    ) {

        return IrrigationEventResponse.builder()
                .id(event.getId())
                .farmId(event.getFarm().getId())
                .deviceId(event.getDeviceId())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .durationMinutes(event.getDurationMinutes())
                .volumeLiters(event.getVolumeLiters())
                .triggerReason(event.getTriggerReason())
                .completionStatus(event.getCompletionStatus())
                .moistureBefore(event.getMoistureBefore())
                .moistureAfter(event.getMoistureAfter())
                .targetMoisture(event.getTargetMoisture())
                .pumpWasOn(event.getPumpWasOn())
                .notes(event.getNotes())
                .build();
    }
}