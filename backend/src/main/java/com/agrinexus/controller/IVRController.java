package com.agrinexus.controller;

import com.agrinexus.dto.IVRRequest;
import com.agrinexus.dto.IVRResponse;
import com.agrinexus.entity.IVRSession;
import com.agrinexus.service.IVRService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai/ivr")
@RequiredArgsConstructor
public class IVRController {

    private final IVRService service;

    /*
     * Start IVR session.
     */
    @PostMapping("/start")
    public ResponseEntity<IVRResponse> start(
            @RequestBody IVRRequest request
    ) {

        return ResponseEntity.ok(
                service.start(request)
        );
    }

    /*
     * Process an IVR digit/question.
     */
    @PostMapping("/interact")
    public ResponseEntity<IVRResponse> interact(
            @RequestBody IVRRequest request
    ) {

        return ResponseEntity.ok(
                service.interact(request)
        );
    }

    /*
     * Get session.
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<IVRSession> getSession(
            @PathVariable UUID sessionId
    ) {

        return ResponseEntity.ok(
                service.getSession(sessionId)
        );
    }

    /*
     * End IVR session.
     */
    @PostMapping("/session/{sessionId}/end")
    public ResponseEntity<IVRResponse> end(
            @PathVariable UUID sessionId
    ) {

        return ResponseEntity.ok(
                service.end(sessionId)
        );
    }
}