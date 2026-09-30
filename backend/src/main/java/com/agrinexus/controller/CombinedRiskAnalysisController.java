package com.agrinexus.controller;

import com.agrinexus.dto.CombinedRiskAnalysisResponse;
import com.agrinexus.entity.CombinedRiskAnalysis;
import com.agrinexus.mapper.CombinedRiskAnalysisMapper;
import com.agrinexus.repository.CombinedRiskAnalysisRepository;
import com.agrinexus.service.CombinedRiskAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/combined-risk")
@RequiredArgsConstructor
public class CombinedRiskAnalysisController {

    private final CombinedRiskAnalysisService combinedRiskAnalysisService;
    private final CombinedRiskAnalysisRepository combinedRiskAnalysisRepository;

    @PostMapping("/farm/{farmId}/analyze")
    public ResponseEntity<CombinedRiskAnalysisResponse> analyzeFarmRisk(
            @PathVariable UUID farmId
    ) {

        CombinedRiskAnalysis analysis =
                combinedRiskAnalysisService.analyzeFarmRisk(farmId);

        CombinedRiskAnalysisResponse response =
                CombinedRiskAnalysisMapper.toResponse(analysis);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<CombinedRiskAnalysisResponse>>
    getFarmCombinedRiskAnalyses(
            @PathVariable UUID farmId
    ) {

        List<CombinedRiskAnalysisResponse> responses =
                combinedRiskAnalysisRepository
                        .findByFarmIdOrderByAnalyzedAtDesc(farmId)
                        .stream()
                        .map(CombinedRiskAnalysisMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(responses);
    }
}