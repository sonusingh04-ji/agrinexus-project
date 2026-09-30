package com.agrinexus.service;

import com.agrinexus.dto.AICopilotRequest;
import com.agrinexus.dto.AICopilotResponse;
import com.agrinexus.dto.VoiceRequest;
import com.agrinexus.dto.VoiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoiceAssistantService {

    private final AICopilotService copilotService;

    public VoiceResponse process(
            VoiceRequest request
    ) {

        validate(request);

        String language =
                normalizeLanguage(
                        request.getLanguage()
                );

        String channel =
                normalizeChannel(
                        request.getChannel()
                );

        /*
         * Reuse the existing Copilot.
         *
         * Do not duplicate decision logic.
         */
        AICopilotResponse copilotResponse =
                copilotService.ask(
                        AICopilotRequest.builder()
                                .farmId(
                                        request.getFarmId()
                                )
                                .conversationId(
                                        request.getConversationId()
                                )
                                .question(
                                        request.getTranscript()
                                )
                                .language(
                                        language
                                )
                                .channel(
                                        channel
                                )
                                .build()
                );

        /*
         * The returned text is already suitable
         * for the next text-to-speech provider layer.
         */
        String spokenText =
                buildSpokenText(
                        copilotResponse.getAnswer(),
                        language
                );

        return VoiceResponse.builder()
                .farmId(
                        copilotResponse.getFarmId()
                )
                .conversationId(
                        copilotResponse.getConversationId()
                )
                .messageId(
                        copilotResponse.getMessageId()
                )
                .contextSnapshotId(
                        copilotResponse.getContextSnapshotId()
                )
                .language(
                        language
                )
                .transcript(
                        request.getTranscript()
                )
                .answer(
                        copilotResponse.getAnswer()
                )
                .spokenText(
                        spokenText
                )
                .confidenceLevel(
                        copilotResponse.getConfidenceLevel()
                )
                .confidence(
                        copilotResponse.getConfidence()
                )
                .decisionContextUsed(
                        copilotResponse.getDecisionContextUsed()
                )
                .requiresReview(
                        copilotResponse.getRequiresReview()
                )
                .dataStatus(
                        copilotResponse.getDataStatus()
                )
                .audioGenerated(false)
                .respondedAt(
                        LocalDateTime.now()
                )
                .build();
    }

    /*
     * Prepare text for speech.
     */
    private String buildSpokenText(
            String answer,
            String language
    ) {

        if (answer == null
                || answer.isBlank()) {

            return "No clear answer is available right now.";
        }

        String clean =
                answer
                        .replace("\n", " ")
                        .replace("  ", " ")
                        .trim();

        /*
         * Keep voice answers concise.
         */
        if (clean.length() > 500) {

            clean =
                    clean.substring(
                            0,
                            497
                    )
                            + "...";
        }

        /*
         * Future provider-specific SSML or
         * pronunciation preparation can be added here.
         */
        return clean;
    }

    private void validate(
            VoiceRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Voice request cannot be null"
            );
        }

        if (request.getFarmId() == null) {

            throw new RuntimeException(
                    "Farm ID is required"
            );
        }

        if (request.getTranscript() == null
                || request.getTranscript().isBlank()) {

            throw new RuntimeException(
                    "Voice transcript is required"
            );
        }

        if (request.getTranscript().length() > 5000) {

            throw new RuntimeException(
                    "Voice transcript cannot exceed 5000 characters"
            );
        }
    }

    private String normalizeLanguage(
            String language
    ) {

        if (language == null
                || language.isBlank()) {

            return "HI";
        }

        String value =
                language.trim()
                        .toUpperCase();

        return "EN".equals(value)
                ? "EN"
                : "HI";
    }

    private String normalizeChannel(
            String channel
    ) {

        if (channel == null
                || channel.isBlank()) {

            return "VOICE";
        }

        return channel.trim()
                .toUpperCase();
    }
}