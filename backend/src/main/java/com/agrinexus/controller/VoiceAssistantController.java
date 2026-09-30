package com.agrinexus.controller;

import com.agrinexus.dto.VoiceRequest;
import com.agrinexus.dto.VoiceResponse;
import com.agrinexus.service.VoiceAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/voice")
@RequiredArgsConstructor
public class VoiceAssistantController {

    private final VoiceAssistantService service;

    @PostMapping("/ask")
    public ResponseEntity<VoiceResponse> ask(
            @RequestBody VoiceRequest request
    ) {
        return ResponseEntity.ok(
                service.process(request)
        );
    }
}