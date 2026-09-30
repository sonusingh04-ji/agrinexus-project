package com.agrinexus.controller;

import com.agrinexus.dto.AIRecommendationExplanationRequest;
import com.agrinexus.dto.AIRecommendationExplanationResponse;
import com.agrinexus.service.AIRecommendationExplanationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/explanations")
@RequiredArgsConstructor
public class AIRecommendationExplanationController {

    private final AIRecommendationExplanationService service;

    /*
     * Create explanation.
     */
    @PostMapping
    public ResponseEntity<AIRecommendationExplanationResponse>
    create(
            @RequestBody AIRecommendationExplanationRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.create(request)
                );
    }

    /*
     * Get explanation by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AIRecommendationExplanationResponse>
    getById(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                service.getById(id)
        );
    }

    /*
     * Get all explanations for a farm.
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<
            List<AIRecommendationExplanationResponse>
            > getByFarm(
            @PathVariable UUID farmId
    ) {

        return ResponseEntity.ok(
                service.getByFarm(farmId)
        );
    }

    /*
     * Get explanations for conversation.
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<
            List<AIRecommendationExplanationResponse>
            > getByConversation(
            @PathVariable UUID conversationId
    ) {

        return ResponseEntity.ok(
                service.getByConversation(
                        conversationId
                )
        );
    }

    /*
     * Get latest explanation for a farm.
     */
    @GetMapping("/farm/{farmId}/latest")
    public ResponseEntity<
            AIRecommendationExplanationResponse
            > getLatest(
            @PathVariable UUID farmId
    ) {

        AIRecommendationExplanationResponse response =
                service.getLatestByFarm(
                        farmId
                );

        if (response == null) {

            return ResponseEntity
                    .noContent()
                    .build();
        }

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * Delete explanation.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable UUID id
    ) {

        service.delete(id);

        return ResponseEntity.ok(
                "AI recommendation explanation deleted successfully"
        );
    }
}