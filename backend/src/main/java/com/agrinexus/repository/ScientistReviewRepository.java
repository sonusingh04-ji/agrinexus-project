package com.agrinexus.repository;

import com.agrinexus.entity.ScientistReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScientistReviewRepository
        extends JpaRepository<ScientistReview, UUID> {

    List<ScientistReview>
    findByFarmIdOrderByReviewedAtDesc(
            UUID farmId
    );

    List<ScientistReview>
    findByFeedbackIdOrderByReviewedAtDesc(
            UUID feedbackId
    );

    List<ScientistReview>
    findByReviewDecisionOrderByReviewedAtDesc(
            String reviewDecision
    );

    List<ScientistReview>
    findByValidatedForLearningTrueOrderByReviewedAtDesc();
}