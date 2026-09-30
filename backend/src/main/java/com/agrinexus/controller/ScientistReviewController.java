package com.agrinexus.controller;

import com.agrinexus.dto.ScientistReviewRequest;
import com.agrinexus.dto.ScientistReviewResponse;
import com.agrinexus.entity.ScientistReview;
import com.agrinexus.service.ScientistReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/scientist-reviews")
@RequiredArgsConstructor
public class ScientistReviewController {

    private final ScientistReviewService service;

    /*
     * Create scientist review.
     */
    @PostMapping
    public ResponseEntity<ScientistReviewResponse> review(
            @RequestBody ScientistReviewRequest request
    ) {

        ScientistReview review =
                service.review(
                        request.getFarmId(),
                        request.getFeedbackId(),
                        request.getReviewerId(),
                        request.getReviewerName(),
                        request.getReviewDecision(),
                        request.getReviewComments(),
                        request.getValidatedForLearning(),
                        request.getEvidenceReference(),
                        request.getNotes()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(review));
    }

    /*
     * Get review by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScientistReviewResponse> getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                toResponse(
                        service.getById(id)
                )
        );
    }

    /*
     * Get reviews for a farm.
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<ScientistReviewResponse>> getByFarm(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getByFarm(farmId)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    /*
     * Get reviews for one farmer-feedback record.
     */
    @GetMapping("/feedback/{feedbackId}")
    public ResponseEntity<List<ScientistReviewResponse>> getByFeedback(
            @PathVariable UUID feedbackId
    ) {

        return ResponseEntity.ok(
                service.getByFeedback(feedbackId)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    /*
     * Get reviews that are validated for learning.
     */
    @GetMapping("/validated-for-learning")
    public ResponseEntity<List<ScientistReviewResponse>>
    getValidatedForLearning() {

        return ResponseEntity.ok(
                service.getValidatedForLearning()
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    /*
     * Get reviews by decision.
     */
    @GetMapping("/decision/{decision}")
    public ResponseEntity<List<ScientistReviewResponse>>
    getByDecision(
            @PathVariable String decision
    ) {

        return ResponseEntity.ok(
                service.getByDecision(
                                decision
                        )
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    /*
     * Move feedback into UNDER_REVIEW state.
     */
    @PutMapping("/feedback/{feedbackId}/start-review")
    public ResponseEntity<String> startReview(
            @PathVariable UUID feedbackId
    ) {

        service.markFeedbackUnderReview(
                feedbackId
        );

        return ResponseEntity.ok(
                "Feedback moved to UNDER_REVIEW"
        );
    }

    /*
     * Entity → DTO.
     */
    private ScientistReviewResponse toResponse(
            ScientistReview review
    ) {

        return ScientistReviewResponse.builder()
                .id(review.getId())
                .farmId(
                        review.getFarm() != null
                                ? review.getFarm().getId()
                                : null
                )
                .feedbackId(
                        review.getFeedback() != null
                                ? review.getFeedback().getId()
                                : null
                )
                .reviewerId(
                        review.getReviewerId()
                )
                .reviewerName(
                        review.getReviewerName()
                )
                .reviewDecision(
                        review.getReviewDecision()
                )
                .reviewComments(
                        review.getReviewComments()
                )
                .validatedForLearning(
                        review.getValidatedForLearning()
                )
                .evidenceReference(
                        review.getEvidenceReference()
                )
                .status(
                        review.getStatus()
                )
                .reviewedAt(
                        review.getReviewedAt()
                )
                .notes(
                        review.getNotes()
                )
                .build();
    }
}