package com.agrinexus.controller;

import com.agrinexus.dto.AIFeedbackRequest;
import com.agrinexus.dto.AIFeedbackResponse;
import com.agrinexus.service.AIFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/feedback")
@RequiredArgsConstructor
public class AIFeedbackController {

    private final AIFeedbackService service;

    /*
     * Submit farmer feedback.
     */
    @PostMapping
    public ResponseEntity<AIFeedbackResponse> create(
            @RequestBody AIFeedbackRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.create(request)
                );
    }

    /*
     * Get feedback by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AIFeedbackResponse> getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                service.getById(id)
        );
    }

    /*
     * Get feedback for a farm.
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<AIFeedbackResponse>> getByFarm(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getByFarm(farmId)
        );
    }

    /*
     * Get feedback waiting for scientist review.
     */
    @GetMapping("/pending-review")
    public ResponseEntity<List<AIFeedbackResponse>>
    getPendingReview() {

        return ResponseEntity.ok(
                service.getPendingReview()
        );
    }

    /*
     * Update submitted feedback.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AIFeedbackResponse> update(
            @PathVariable UUID id,
            @RequestBody AIFeedbackRequest request
    ) {

        return ResponseEntity.ok(
                service.update(
                        id,
                        request
                )
        );
    }

    /*
     * Delete unreviewed feedback.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable UUID id
    ) {

        service.delete(id);

        return ResponseEntity.ok(
                "AI feedback deleted successfully"
        );
    }
}