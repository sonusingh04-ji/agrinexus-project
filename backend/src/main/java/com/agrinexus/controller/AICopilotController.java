package com.agrinexus.controller;

import com.agrinexus.dto.AICopilotRequest;
import com.agrinexus.dto.AICopilotResponse;
import com.agrinexus.service.AICopilotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/copilot")
@RequiredArgsConstructor
public class AICopilotController {

    private final AICopilotService service;

    /*
     * Ask the AI Farm Copilot.
     *
     * POST /api/ai/copilot/ask
     */
    @PostMapping("/ask")
    public ResponseEntity<AICopilotResponse> ask(
            @RequestBody AICopilotRequest request
    ) {

        return ResponseEntity.ok(
                service.ask(request)
        );
    }
}