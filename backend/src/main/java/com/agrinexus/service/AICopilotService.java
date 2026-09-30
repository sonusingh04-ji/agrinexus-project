package com.agrinexus.service;

import com.agrinexus.dto.AIContextRequest;
import com.agrinexus.dto.AIContextResponse;
import com.agrinexus.dto.AICopilotRequest;
import com.agrinexus.dto.AICopilotResponse;
import com.agrinexus.dto.AIRecommendationExplanationRequest;
import com.agrinexus.dto.AIRecommendationExplanationResponse;
import com.agrinexus.entity.AIConversation;
import com.agrinexus.entity.AIContextSnapshot;
import com.agrinexus.entity.AIMessage;
import com.agrinexus.entity.Farm;
import com.agrinexus.enums.SupportedLanguage;
import com.agrinexus.repository.AIConversationRepository;
import com.agrinexus.repository.AIContextSnapshotRepository;
import com.agrinexus.repository.AIMessageRepository;
import com.agrinexus.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AICopilotService {

    private final FarmRepository farmRepository;

    private final AIConversationRepository conversationRepository;

    private final AIMessageRepository messageRepository;

    private final AIContextSnapshotRepository contextRepository;

    private final AIContextSnapshotService contextService;

    private final AIRecommendationExplanationService explanationService;

    private final LanguageResponseService languageResponseService;


    /*
     * =========================================================
     * Ask AI Farm Copilot
     * =========================================================
     */
    public AICopilotResponse ask(
            AICopilotRequest request
    ) {

        validate(request);


        UUID farmId =
                request.getFarmId();


        SupportedLanguage language =
                languageResponseService.language(
                        request.getLanguage()
                );


        String channel =
                normalizeChannel(
                        request.getChannel()
                );


        Farm farm =
                farmRepository.findById(farmId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + farmId
                                )
                        );


        /*
         * ---------------------------------------------------------
         * Detect planning information directly from the farmer
         * question.
         *
         * This is particularly important for Member 6 because
         * Lowest-Cost Safe Plan requires targetSeason.
         * ---------------------------------------------------------
         */
        String question =
                request.getQuestion().trim();


        String detectedTargetSeason =
                extractTargetSeason(
                        question
                );


        String detectedFarmerConstraint =
                extractFarmerConstraint(
                        question
                );


        /*
         * ---------------------------------------------------------
         * Conversation
         * ---------------------------------------------------------
         */
        AIConversation conversation;


        if (request.getConversationId() != null) {

            conversation =
                    conversationRepository.findById(
                            request.getConversationId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "AI conversation not found with ID: "
                                            + request.getConversationId()
                            )
                    );


            if (
                    conversation.getFarm() == null
                            || !farmId.equals(
                            conversation.getFarm().getId()
                    )
            ) {

                throw new RuntimeException(
                        "Conversation does not belong to the specified farm"
                );
            }


        } else {

            conversation =
                    AIConversation.builder()
                            .farm(farm)
                            .userId(null)
                            .language(
                                    language.getCode()
                            )
                            .channel(channel)
                            .status("ACTIVE")
                            .title(
                                    buildConversationTitle(
                                            question
                                    )
                            )
                            .startedAt(
                                    LocalDateTime.now()
                            )
                            .lastMessageAt(
                                    LocalDateTime.now()
                            )
                            .notes(
                                    "Conversation created by AgriNexus AI Farm Copilot."
                            )
                            .build();


            conversation =
                    conversationRepository.save(
                            conversation
                    );
        }


        /*
         * ---------------------------------------------------------
         * Message sequence
         * ---------------------------------------------------------
         */
        AIMessage previousMessage =
                messageRepository
                        .findFirstByConversationIdOrderBySequenceNumberDesc(
                                conversation.getId()
                        );


        int nextSequence =
                previousMessage == null
                        ? 1
                        : previousMessage.getSequenceNumber() + 1;


        /*
         * ---------------------------------------------------------
         * Save farmer question
         * ---------------------------------------------------------
         */
        AIMessage userMessage =
                AIMessage.builder()
                        .conversation(conversation)
                        .role("USER")
                        .content(question)
                        .language(
                                language.getCode()
                        )
                        .channel(channel)
                        .sequenceNumber(nextSequence)
                        .decisionContextUsed(false)
                        .confidence(null)
                        .source("FARMER")
                        .requiresReview(false)
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();


        userMessage =
                messageRepository.save(
                        userMessage
                );


        /*
         * ---------------------------------------------------------
         * Build context snapshot
         * ---------------------------------------------------------
         *
         * Member 6 planning inputs are now forwarded.
         * ---------------------------------------------------------
         */
        AIContextRequest contextRequest =
                AIContextRequest.builder()

                        .farmId(
                                farmId
                        )

                        .conversationId(
                                conversation.getId()
                        )

                        .messageId(
                                userMessage.getId()
                        )

                        .includeLatestData(
                                true
                        )

                        .purpose(
                                "COPILOT"
                        )

                        .targetSeason(
                                detectedTargetSeason
                        )

                        .farmerConstraint(
                                detectedFarmerConstraint
                        )

                        .requireSoilCompatibility(
                                Boolean.TRUE
                        )

                        .build();


        AIContextResponse contextResponse =
                contextService.create(
                        contextRequest
                );


        AIContextSnapshot context =
                contextRepository.findById(
                        contextResponse.getId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Generated AI context could not be loaded"
                        )
                );


        /*
         * ---------------------------------------------------------
         * Generate grounded answer
         * ---------------------------------------------------------
         */
        CopilotDecision decision =
                generateAnswer(
                        question,
                        language,
                        context
                );


        /*
         * ---------------------------------------------------------
         * Save AI response
         * ---------------------------------------------------------
         */
        AIMessage aiMessage =
                AIMessage.builder()
                        .conversation(conversation)
                        .role("AI")
                        .content(
                                decision.answer()
                        )
                        .language(
                                language.getCode()
                        )
                        .channel(channel)
                        .sequenceNumber(
                                nextSequence + 1
                        )
                        .decisionContextUsed(true)
                        .confidence(
                                decision.confidence()
                        )
                        .source(
                                decision.source()
                        )
                        .requiresReview(
                                decision.requiresReview()
                        )
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();


        aiMessage =
                messageRepository.save(
                        aiMessage
                );


        /*
         * ---------------------------------------------------------
         * Create explanation
         * ---------------------------------------------------------
         */
        AIRecommendationExplanationRequest explanationRequest =
                AIRecommendationExplanationRequest.builder()

                        .farmId(
                                farmId
                        )

                        .conversationId(
                                conversation.getId()
                        )

                        .contextSnapshotId(
                                context.getId()
                        )

                        .decisionType(
                                decision.decisionType()
                        )

                        .decision(
                                decision.decision()
                        )

                        .why(
                                decision.why()
                        )

                        .contributingFactors(
                                decision.contributingFactors()
                        )

                        .dataUsed(
                                decision.dataUsed()
                        )

                        .confidence(
                                decision.confidence()
                        )

                        .uncertainty(
                                context.getUncertaintyContext()
                        )

                        .dataStatus(
                                context.getDataStatus()
                        )

                        .source(
                                "CENTRAL_DECISION_ENGINE"
                        )

                        .build();


        AIRecommendationExplanationResponse explanation =
                explanationService.create(
                        explanationRequest
                );


        conversation.setLastMessageAt(
                LocalDateTime.now()
        );


        conversationRepository.save(
                conversation
        );


        return AICopilotResponse.builder()

                .conversationId(
                        conversation.getId()
                )

                .messageId(
                        aiMessage.getId()
                )

                .contextSnapshotId(
                        context.getId()
                )

                .explanationId(
                        explanation.getId()
                )

                .farmId(
                        farmId
                )

                .question(
                        question
                )

                .answer(
                        decision.answer()
                )

                .why(
                        decision.why()
                )

                .confidenceLevel(
                        confidenceLabel(
                                decision.confidence()
                        )
                )

                .confidence(
                        decision.confidence()
                )

                .uncertainty(
                        context.getUncertaintyContext()
                )

                .dataStatus(
                        context.getDataStatus()
                )

                .language(
                        language.getCode()
                )

                .channel(
                        channel
                )

                .decisionContextUsed(
                        true
                )

                .requiresReview(
                        decision.requiresReview()
                )

                .answeredAt(
                        LocalDateTime.now()
                )

                .build();
    }


    /*
     * =========================================================
     * Main Copilot routing
     * =========================================================
     */
    private CopilotDecision generateAnswer(
            String question,
            SupportedLanguage language,
            AIContextSnapshot context
    ) {

        String normalized =
                question
                        .trim()
                        .toLowerCase();


        /*
         * ---------------------------------------------------------
         * LOWEST-COST / AFFORDABLE CROP
         * ---------------------------------------------------------
         *
         * IMPORTANT:
         *
         * This branch MUST come BEFORE the generic CROP branch.
         *
         * Otherwise:
         *
         * "Which crop should I choose for the lowest cost?"
         *
         * would match "crop" first and return only the current
         * crop history.
         * ---------------------------------------------------------
         */
        if (
                containsAny(
                        normalized,
                        "lowest cost",
                        "low cost",
                        "cheapest",
                        "cheap crop",
                        "least cost",
                        "minimum cost",
                        "affordable crop",
                        "lowest-cost",
                        "कम लागत",
                        "सबसे कम लागत",
                        "सबसे सस्ती फसल",
                        "सस्ती फसल",
                        "कम खर्च",
                        "कम कीमत वाली फसल"
                )
        ) {

            return buildLowestCostDecision(
                    language,
                    context
            );
        }


        /*
         * ---------------------------------------------------------
         * IRRIGATION
         * ---------------------------------------------------------
         */
        if (
                containsAny(
                        normalized,
                        "irrigate",
                        "irrigation",
                        "water",
                        "सिंचाई",
                        "पानी"
                )
        ) {

            String decision =
                    extractDecision(
                            context.getIrrigationDecision()
                    );


            String answer =
                    languageResponseService.irrigationAnswer(
                            language.getCode(),
                            decision
                    );


            String why =
                    languageResponseService.irrigationWhy(
                            language.getCode(),
                            context.getIrrigationDecision()
                    );


            return new CopilotDecision(
                    "IRRIGATION",
                    decision,
                    answer,
                    why,
                    context.getIrrigationDecision(),
                    "IrrigationTelemetry, IrrigationConfig, WeatherForecastData, CropWaterProfile",
                    0.85,
                    context.getDataStatus().equals("TEST")
                            ? "TEST_DATA_RULE_ENGINE"
                            : "RULE_ENGINE",
                    false
            );
        }


        /*
         * ---------------------------------------------------------
         * WEATHER
         * ---------------------------------------------------------
         */
        if (
                containsAny(
                        normalized,
                        "weather",
                        "temperature",
                        "rain",
                        "wind",
                        "heat",
                        "मौसम",
                        "बारिश",
                        "गर्मी",
                        "हवा"
                )
        ) {

            String answer =
                    languageResponseService.weatherAnswer(
                            language.getCode(),
                            context.getFieldOperationDecision(),
                            context.getWeatherRiskDecision()
                    );


            return new CopilotDecision(
                    "WEATHER_RISK",
                    safe(
                            context.getWeatherRiskDecision()
                    ),
                    answer,
                    language == SupportedLanguage.HI
                            ? "यह जवाब मौसम और मौसम-जोखिम की वर्तमान जानकारी पर आधारित है।"
                            : "The answer is based on the latest stored weather and weather-risk context.",
                    context.getWeatherRiskDecision(),
                    "WeatherRecord, WeatherForecastData, WeatherRisk",
                    0.85,
                    context.getDataStatus().equals("TEST")
                            ? "TEST_DATA_RULE_ENGINE"
                            : "RULE_ENGINE",
                    false
            );
        }


        /*
         * ---------------------------------------------------------
         * DISEASE / PEST / CROP HEALTH RISK
         * ---------------------------------------------------------
         */
        if (
                containsAny(
                        normalized,
                        "disease",
                        "diseases",
                        "pest",
                        "pests",
                        "infection",
                        "crop risk",
                        "crop health",
                        "plant disease",
                        "plant health",
                        "कीट",
                        "कीड़ा",
                        "रोग",
                        "बीमारी",
                        "संक्रमण",
                        "खतरा",
                        "जोखिम",
                        "फसल में खतरा",
                        "फसल का खतरा",
                        "फसल का जोखिम",
                        "फसल में जोखिम"
                )
        ) {

            String riskContext =
                    context.getDiseasePestRiskDecision();


            /*
             * No disease/pest assessment available.
             */
            if (
                    riskContext == null
                            || riskContext.isBlank()
                            || riskContext.contains(
                            "No recent disease/pest risk record is available."
                    )
            ) {

                String answer =
                        language == SupportedLanguage.HI
                                ? "अभी फसल के रोग या कीट जोखिम की जानकारी उपलब्ध नहीं है।"
                                : "A current disease or pest risk assessment is not available.";


                return new CopilotDecision(
                        "DISEASE_PEST_RISK",
                        "NO_DATA",
                        answer,
                        language == SupportedLanguage.HI
                                ? "फसल जोखिम का जवाब देने के लिए रोग/कीट जोखिम रिकॉर्ड चाहिए।"
                                : "A disease/pest risk record is required for this assessment.",
                        riskContext,
                        "DiseasePestRisk, Crop, WeatherRecord",
                        0.60,
                        "RULE_ENGINE",
                        true
                );
            }


            String riskLevel =
                    extractRiskLevel(
                            riskContext
                    );


            Double riskScore =
                    extractRiskScore(
                            riskContext
                    );


            String riskName =
                    extractRiskName(
                            riskContext
                    );


            String recommendation =
                    extractRecommendation(
                            riskContext
                    );


            String answer =
                    languageResponseService.diseasePestAnswer(
                            language.getCode(),
                            riskName,
                            riskLevel,
                            riskScore,
                            recommendation
                    );


            return new CopilotDecision(
                    "DISEASE_PEST_RISK",
                    riskLevel,
                    answer,
                    language == SupportedLanguage.HI
                            ? "यह सलाह फसल, मौसम और उपलब्ध रोग/कीट जोखिम जानकारी पर आधारित है।"
                            : "This answer uses the latest crop, weather and disease/pest risk context.",
                    riskContext,
                    "DiseasePestRisk, Crop, WeatherRecord, WeatherRisk",
                    riskLevelConfidence(
                            riskLevel
                    ),
                    context.getDataStatus().equals("TEST")
                            ? "TEST_DATA_RULE_ENGINE"
                            : "RULE_ENGINE",
                    false
            );
        }


        /*
         * ---------------------------------------------------------
         * SOIL TEST / SOIL TREND
         * ---------------------------------------------------------
         */
        if (
                containsAny(
                        normalized,
                        "soil test",
                        "soil trend",
                        "soil health trend",
                        "soil testing",
                        "soil report",
                        "मिट्टी की जांच",
                        "मिट्टी जांच",
                        "मिट्टी की रिपोर्ट",
                        "मिट्टी का ट्रेंड"
                )
        ) {

            String soilContext =
                    context.getSoilTestTrendDecision();


            String answer;


            if (
                    language == SupportedLanguage.HI
            ) {

                answer =
                        "मिट्टी जांच की जानकारी: "
                                + safe(
                                soilContext
                        );

            } else {

                answer =
                        soilContext;
            }


            return new CopilotDecision(
                    "SOIL_TEST_TREND",
                    extractOverallTrend(
                            soilContext
                    ),
                    answer,
                    language == SupportedLanguage.HI
                            ? "यह जवाब उपलब्ध मिट्टी जांच और उसके ऐतिहासिक ट्रेंड संदर्भ पर आधारित है।"
                            : "This answer uses the stored soil-test trend and reminder context.",
                    soilContext,
                    "SoilSample, SoilTestTrend",
                    context.getDataStatus().equals("TEST")
                            ? 0.70
                            : 0.80,
                    context.getDataStatus().equals("TEST")
                            ? "TEST_DATA_RULE_ENGINE"
                            : "RULE_ENGINE",
                    false
            );
        }


        /*
         * ---------------------------------------------------------
         * FIELD HEALTH / PASSPORT
         * ---------------------------------------------------------
         */
        if (
                containsAny(
                        normalized,
                        "field health",
                        "health passport",
                        "field passport",
                        "field health score",
                        "स्वास्थ्य स्कोर",
                        "खेत का स्वास्थ्य",
                        "खेत की सेहत",
                        "हेल्थ पासपोर्ट"
                )
        ) {

            String healthContext =
                    context.getFieldHealthPassportDecision();


            String answer;


            if (
                    language == SupportedLanguage.HI
            ) {

                answer =
                        "खेत स्वास्थ्य पासपोर्ट: "
                                + safe(
                                healthContext
                        );

            } else {

                answer =
                        healthContext;
            }


            return new CopilotDecision(
                    "FIELD_HEALTH_PASSPORT",
                    extractHealthStatus(
                            healthContext
                    ),
                    answer,
                    language == SupportedLanguage.HI
                            ? "यह जवाब खेत के स्वास्थ्य पासपोर्ट और उपलब्ध दीर्घकालिक खेत डेटा पर आधारित है।"
                            : "This answer uses the stored Field Health Passport context.",
                    healthContext,
                    "FieldHealthPassport, SoilSample, CropHistory, IrrigationTelemetry, WeatherRisk, SoilRecoveryPlan",
                    0.80,
                    context.getDataStatus().equals("TEST")
                            ? "TEST_DATA_RULE_ENGINE"
                            : "RULE_ENGINE",
                    false
            );
        }


        /*
         * ---------------------------------------------------------
         * VILLAGE COOPERATIVE
         * ---------------------------------------------------------
         */
        if (
                containsAny(
                        normalized,
                        "cooperative",
                        "co-operative",
                        "village group",
                        "shared resource",
                        "shared pump",
                        "shared machinery",
                        "farmer group",
                        "सहकारी",
                        "सहकारी समिति",
                        "साझा संसाधन",
                        "किसान समूह",
                        "साझा पंप",
                        "मशीन साझा"
                )
        ) {

            String cooperativeContext =
                    context.getCooperativeDecision();


            if (
                    cooperativeContext == null
                            || cooperativeContext.isBlank()
                            || cooperativeContext.contains(
                            "No active village cooperative is linked"
                    )
            ) {

                String answer =
                        language == SupportedLanguage.HI
                                ? "इस किसान के लिए कोई सक्रिय ग्राम सहकारी समूह जुड़ा नहीं है।"
                                : "No active village cooperative is currently linked to this farmer.";


                return new CopilotDecision(
                        "VILLAGE_COOPERATIVE",
                        "NO_DATA",
                        answer,
                        language == SupportedLanguage.HI
                                ? "किसान के सक्रिय सहकारी सदस्य रिकॉर्ड की आवश्यकता है।"
                                : "An active cooperative membership is required for this information.",
                        cooperativeContext,
                        "VillageCooperative, VillageCooperativeMember, SharedFarmResource, SharedResourceUsage",
                        0.70,
                        "RULE_ENGINE",
                        false
                );
            }


            String answer =
                    language == SupportedLanguage.HI
                            ? "ग्राम सहकारी जानकारी: "
                            + cooperativeContext
                            : cooperativeContext;


            return new CopilotDecision(
                    "VILLAGE_COOPERATIVE",
                    "COOPERATIVE_CONTEXT",
                    answer,
                    language == SupportedLanguage.HI
                            ? "यह जवाब किसान की सक्रिय सहकारी सदस्यता और साझा संसाधन रिकॉर्ड पर आधारित है।"
                            : "This answer uses the farmer's active cooperative membership and shared-resource records.",
                    cooperativeContext,
                    "VillageCooperative, VillageCooperativeMember, SharedFarmResource, SharedResourceUsage",
                    0.80,
                    context.getDataStatus().equals("TEST")
                            ? "TEST_DATA_RULE_ENGINE"
                            : "RULE_ENGINE",
                    false
            );
        }


        /*
         * ---------------------------------------------------------
         * CROP
         * ---------------------------------------------------------
         */
        if (
                containsAny(
                        normalized,
                        "crop",
                        "plant",
                        "sow",
                        "cultivate",
                        "फसल",
                        "बोना",
                        "बुवाई"
                )
        ) {

            String answer =
                    languageResponseService.cropAnswer(
                            language.getCode(),
                            context.getCropDecision()
                    );


            return new CopilotDecision(
                    "CROP",
                    "CROP_CONTEXT",
                    answer,
                    language == SupportedLanguage.HI
                            ? "यह जवाब उपलब्ध फसल इतिहास और खेत की जानकारी पर आधारित है।"
                            : "The answer uses the latest crop-history and farm context currently available.",
                    context.getCropDecision(),
                    "CropHistory, Crop, Farm",
                    0.75,
                    context.getDataStatus().equals("TEST")
                            ? "TEST_DATA_RULE_ENGINE"
                            : "RULE_ENGINE",
                    true
            );
        }


        /*
         * ---------------------------------------------------------
         * FERTILIZER / NUTRIENTS
         * ---------------------------------------------------------
         */
        if (
                containsAny(
                        normalized,
                        "fertilizer",
                        "fertiliser",
                        "urea",
                        "nutrient",
                        "nitrogen",
                        "phosphorus",
                        "potassium",
                        "खाद",
                        "उर्वरक",
                        "नाइट्रोजन",
                        "फास्फोरस",
                        "पोटाश"
                )
        ) {

            String answer =
                    languageResponseService.nutrientAnswer(
                            language.getCode(),
                            context.getNutrientDecision()
                    );


            return new CopilotDecision(
                    "FERTILIZER",
                    "NUTRIENT_CONTEXT",
                    answer,
                    language == SupportedLanguage.HI
                            ? "यह जवाब मिट्टी, पोषक तत्व और खाद की उपलब्ध जानकारी पर आधारित है।"
                            : "The answer uses the stored fertilizer recommendation and nutrient-balance context.",
                    context.getNutrientDecision(),
                    "FertilizerRecommendation, NutrientBalance, SoilSample",
                    0.80,
                    context.getDataStatus().equals("TEST")
                            ? "TEST_DATA_RULE_ENGINE"
                            : "RULE_ENGINE",
                    true
            );
        }


        /*
         * ---------------------------------------------------------
         * GENERAL
         * ---------------------------------------------------------
         */
        return new CopilotDecision(
                "GENERAL",
                "CONTEXT_AVAILABLE",
                languageResponseService.generalAnswer(
                        language.getCode()
                ),
                language == SupportedLanguage.HI
                        ? "यह जवाब आपके खेत की उपलब्ध जानकारी का सारांश है।"
                        : "The Copilot summarized the latest stored farm context.",
                context.getDecisionSummary(),
                "Farm, SoilSample, CropHistory, WeatherRecord, WeatherForecastData, IrrigationTelemetry, FertilizerRecommendation, NutrientBalance, WeatherRisk, DiseasePestRisk, FieldHealthPassport, SoilTestTrend, LowestCostSafePlan, VillageCooperative",
                0.70,
                context.getDataStatus().equals("TEST")
                        ? "TEST_DATA_RULE_ENGINE"
                        : "RULE_ENGINE",
                true
        );
    }


    /*
     * =========================================================
     * Member 6 — Lowest-Cost decision
     * =========================================================
     */
    private CopilotDecision buildLowestCostDecision(
            SupportedLanguage language,
            AIContextSnapshot context
    ) {

        String planContext =
                context.getLowestCostPlanDecision();


        /*
         * If no plan was evaluated, the AI Context was created
         * without a recognizable target season.
         */
        if (
                planContext == null
                        || planContext.isBlank()
                        || planContext.contains(
                        "was not evaluated"
                )
        ) {

            String answer;


            if (
                    language == SupportedLanguage.HI
            ) {

                answer =
                        "कम लागत वाली फसल की तुलना करने के लिए "
                                + "टारगेट सीजन, जैसे RABI या KHARIF, "
                                + "देना जरूरी है।";

            } else {

                answer =
                        "To compare the lowest-cost safe crop, "
                                + "please specify the target season, "
                                + "such as RABI or KHARIF.";
            }


            return new CopilotDecision(
                    "LOWEST_COST_SAFE_PLAN",
                    "INSUFFICIENT_PLANNING_INPUT",
                    answer,
                    language == SupportedLanguage.HI
                            ? "कम लागत वाली योजना के लिए लक्ष्य सीजन आवश्यक है।"
                            : "A target season is required for the Lowest-Cost Safe Plan.",
                    planContext,
                    "LowestCostSafePlan, Crop, SoilSample, Farm",
                    0.60,
                    "RULE_ENGINE",
                    true
            );
        }


        String crop =
                extractPlanCrop(
                        planContext
                );


        Double score =
                extractPlanScore(
                        planContext
                );


        Double estimatedCost =
                extractEstimatedCost(
                        planContext
                );


        String waterDemand =
                extractWaterDemand(
                        planContext
                );


        String season =
                extractPlanSeasonFromContext(
                        context.getFarmContext()
                );


        String answer;


        if (
                language == SupportedLanguage.HI
        ) {

            answer =
                    "आपके दिए गए कम-लागत मानदंड के अनुसार "
                            + "सबसे कम लागत वाली स्वीकार्य फसल "
                            + crop
                            + " है। अनुमानित लागत "
                            + estimatedCost
                            + " रुपये/हेक्टेयर है। "
                            + "जल आवश्यकता श्रेणी "
                            + waterDemand
                            + " है। "
                            + "यह निर्णय वर्तमान खेत, मिट्टी, "
                            + "सीजन और तुलना डेटा पर आधारित है।";

        } else {

            answer =
                    "For the current low-cost planning request, "
                            + "the highest-ranked acceptable crop is "
                            + crop
                            + ". The estimated combined planning cost "
                            + "is ₹"
                            + estimatedCost
                            + "/ha, with a "
                            + waterDemand
                            + " water-demand class. "
                            + "The result is based on the current farm, "
                            + "soil, season and crop-comparison data.";
        }


        return new CopilotDecision(
                "LOWEST_COST_SAFE_PLAN",
                crop,
                answer,
                language == SupportedLanguage.HI
                        ? "यह जवाब AgriNexus के Lowest-Cost Safe Plan और वर्तमान खेत की मिट्टी/सीजन जानकारी पर आधारित है।"
                        : "This answer uses the AgriNexus Lowest-Cost Safe Plan together with the current farm, soil and season context.",
                planContext,
                "LowestCostSafePlan, AffordableCropOption, Crop, SoilSample, Farm",
                0.80,
                context.getDataStatus().equals("TEST")
                        ? "TEST_DATA_RULE_ENGINE"
                        : "RULE_ENGINE",
                true
        );
    }


    /*
     * =========================================================
     * Extract plan crop
     * =========================================================
     */
    private String extractPlanCrop(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return "UNKNOWN";
        }


        String marker =
                "crop=";


        int start =
                value.indexOf(
                        marker
                );


        if (start < 0) {

            return "UNKNOWN";
        }


        start += marker.length();


        int end =
                value.indexOf(
                        ", score=",
                        start
                );


        if (end < 0) {

            end = value.length();
        }


        return value
                .substring(
                        start,
                        end
                )
                .trim();
    }


    /*
     * =========================================================
     * Extract plan score
     * =========================================================
     */
    private Double extractPlanScore(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return null;
        }


        String marker =
                "score=";


        int start =
                value.indexOf(
                        marker
                );


        if (start < 0) {

            return null;
        }


        start += marker.length();


        int end =
                value.indexOf(
                        ", estimatedCost=",
                        start
                );


        if (end < 0) {

            end =
                    value.indexOf(
                            ",",
                            start
                    );
        }


        if (end < 0) {

            end = value.length();
        }


        try {

            return Double.parseDouble(
                    value
                            .substring(
                                    start,
                                    end
                            )
                            .trim()
            );

        } catch (NumberFormatException ex) {

            return null;
        }
    }


    /*
     * =========================================================
     * Extract estimated cost
     * =========================================================
     */
    private Double extractEstimatedCost(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return null;
        }


        String marker =
                "estimatedCost=";


        int start =
                value.indexOf(
                        marker
                );


        if (start < 0) {

            return null;
        }


        start += marker.length();


        int end =
                value.indexOf(
                        " INR/ha",
                        start
                );


        if (end < 0) {

            end =
                    value.indexOf(
                            ", waterDemand=",
                            start
                    );
        }


        if (end < 0) {

            end = value.length();
        }


        try {

            return Double.parseDouble(
                    value
                            .substring(
                                    start,
                                    end
                            )
                            .trim()
            );

        } catch (NumberFormatException ex) {

            return null;
        }
    }


    /*
     * =========================================================
     * Extract water demand
     * =========================================================
     */
    private String extractWaterDemand(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return "UNKNOWN";
        }


        String marker =
                "waterDemand=";


        int start =
                value.indexOf(
                        marker
                );


        if (start < 0) {

            return "UNKNOWN";
        }


        start += marker.length();


        int end =
                value.indexOf(
                        ", reason=",
                        start
                );


        if (end < 0) {

            end = value.length();
        }


        return value
                .substring(
                        start,
                        end
                )
                .trim();
    }


    /*
     * =========================================================
     * Extract target season from farmer question
     * =========================================================
     */
    private String extractTargetSeason(
            String question
    ) {

        if (
                question == null
                        || question.isBlank()
        ) {

            return null;
        }


        String normalized =
                question
                        .trim()
                        .toUpperCase();


        /*
         * RABI
         */
        if (
                normalized.contains("RABI")
                        || normalized.contains("रबी")
        ) {

            return "RABI";
        }


        /*
         * KHARIF
         */
        if (
                normalized.contains("KHARIF")
                        || normalized.contains("खरीफ")
        ) {

            return "KHARIF";
        }


        /*
         * SUMMER
         */
        if (
                normalized.contains("SUMMER")
                        || normalized.contains("गर्मी")
                        || normalized.contains("जायद")
        ) {

            return "SUMMER";
        }


        return null;
    }


    /*
     * =========================================================
     * Extract farmer constraint from question
     * =========================================================
     */
    private String extractFarmerConstraint(
            String question
    ) {

        if (
                question == null
                        || question.isBlank()
        ) {

            return null;
        }


        String normalized =
                question
                        .trim()
                        .toLowerCase();


        /*
         * LOW COST
         */
        if (
                containsAny(
                        normalized,
                        "lowest cost",
                        "low cost",
                        "cheapest",
                        "cheap",
                        "least cost",
                        "minimum cost",
                        "affordable",
                        "कम लागत",
                        "सबसे सस्ती",
                        "सस्ती",
                        "कम खर्च"
                )
        ) {

            return "LOW_COST";
        }


        /*
         * LOW WATER
         */
        if (
                containsAny(
                        normalized,
                        "low water",
                        "less water",
                        "less-water",
                        "water saving",
                        "save water",
                        "कम पानी",
                        "पानी की बचत",
                        "कम सिंचाई"
                )
        ) {

            return "LOW_WATER";
        }


        /*
         * LOW ENERGY
         */
        if (
                containsAny(
                        normalized,
                        "low energy",
                        "less energy",
                        "energy saving",
                        "save energy",
                        "कम ऊर्जा",
                        "ऊर्जा की बचत"
                )
        ) {

            return "LOW_ENERGY";
        }


        /*
         * SHORT DURATION
         */
        if (
                containsAny(
                        normalized,
                        "short duration",
                        "short crop",
                        "quick crop",
                        "early crop",
                        "less duration",
                        "कम अवधि",
                        "जल्दी तैयार",
                        "कम समय"
                )
        ) {

            return "SHORT_DURATION";
        }


        return null;
    }


    /*
     * =========================================================
     * Extract trend
     * =========================================================
     */
    private String extractOverallTrend(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return "UNKNOWN";
        }


        String marker =
                "overallTrend=";


        int start =
                value.indexOf(
                        marker
                );


        if (start < 0) {

            return "UNKNOWN";
        }


        start += marker.length();


        int end =
                value.indexOf(
                        ", latestTestDate=",
                        start
                );


        if (end < 0) {

            end = value.length();
        }


        return value
                .substring(
                        start,
                        end
                )
                .trim();
    }


    /*
     * =========================================================
     * Extract health status
     * =========================================================
     */
    private String extractHealthStatus(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return "UNKNOWN";
        }


        String marker =
                "status=";


        int start =
                value.indexOf(
                        marker
                );


        if (start < 0) {

            return "UNKNOWN";
        }


        start += marker.length();


        int end =
                value.indexOf(
                        ", healthScore=",
                        start
                );


        if (end < 0) {

            end = value.length();
        }


        return value
                .substring(
                        start,
                        end
                )
                .trim();
    }


    /*
     * =========================================================
     * Extract plan season from context
     * =========================================================
     *
     * This is only used for explanatory context.
     * The actual planning request already reached AI Context.
     * =========================================================
     */
    private String extractPlanSeasonFromContext(
            String farmContext
    ) {

        if (
                farmContext == null
                        || farmContext.isBlank()
        ) {

            return "UNKNOWN";
        }


        if (
                farmContext.contains(
                        "targetSeason=RABI"
                )
        ) {

            return "RABI";
        }


        if (
                farmContext.contains(
                        "targetSeason=KHARIF"
                )
        ) {

            return "KHARIF";
        }


        if (
                farmContext.contains(
                        "targetSeason=SUMMER"
                )
        ) {

            return "SUMMER";
        }


        return "UNKNOWN";
    }


    /*
     * =========================================================
     * Existing irrigation decision extraction
     * =========================================================
     */
    private String extractDecision(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return null;
        }


        String prefix =
                "Decision=";


        int start =
                value.indexOf(
                        prefix
                );


        if (start >= 0) {

            start += prefix.length();


            int end =
                    value.indexOf(
                            ";",
                            start
                    );


            if (end < 0) {

                end = value.length();
            }


            return value
                    .substring(
                            start,
                            end
                    )
                    .trim();
        }


        if (value.length() <= 200) {

            return value.trim();
        }


        return "UNKNOWN";
    }


    /*
     * =========================================================
     * Disease/pest risk level
     * =========================================================
     */
    private String extractRiskLevel(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return "UNKNOWN";
        }


        String marker =
                "level=";


        int start =
                value.indexOf(
                        marker
                );


        if (start < 0) {

            return "UNKNOWN";
        }


        start += marker.length();


        int end =
                value.indexOf(
                        ", score=",
                        start
                );


        if (end < 0) {

            end =
                    value.indexOf(
                            ".",
                            start
                    );
        }


        if (end < 0) {

            end =
                    value.length();
        }


        return value
                .substring(
                        start,
                        end
                )
                .trim()
                .toUpperCase();
    }


    /*
     * =========================================================
     * Disease/pest risk score
     * =========================================================
     */
    private Double extractRiskScore(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return null;
        }


        String marker =
                "score=";


        int start =
                value.indexOf(
                        marker
                );


        if (start < 0) {

            return null;
        }


        start += marker.length();


        int end =
                value.indexOf(
                        ", recommendation=",
                        start
                );


        if (end < 0) {

            end =
                    value.indexOf(
                            ".",
                            start
                    );
        }


        if (end < 0) {

            end =
                    value.length();
        }


        String numeric =
                value
                        .substring(
                                start,
                                end
                        )
                        .trim();


        try {

            return Double.parseDouble(
                    numeric
            );

        } catch (NumberFormatException ex) {

            return null;
        }
    }


    /*
     * =========================================================
     * Disease/pest risk name
     * =========================================================
     */
    private String extractRiskName(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return "Crop health risk";
        }


        String marker =
                "Latest disease/pest risk: ";


        int start =
                value.indexOf(
                        marker
                );


        if (start < 0) {

            return "Crop health risk";
        }


        start += marker.length();


        int end =
                value.indexOf(
                        ", category=",
                        start
                );


        if (end < 0) {

            return value
                    .substring(
                            start
                    )
                    .trim();
        }


        return value
                .substring(
                        start,
                        end
                )
                .trim();
    }


    /*
     * =========================================================
     * Recommendation extraction
     * =========================================================
     */
    private String extractRecommendation(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {

            return
                    "Inspect the crop and follow local agricultural guidance.";
        }


        String marker =
                "recommendation=";


        int start =
                value.indexOf(
                        marker
                );


        if (start < 0) {

            return
                    "Inspect the crop and follow local agricultural guidance.";
        }


        start += marker.length();


        int end =
                value.indexOf(
                        ".",
                        start
                );


        if (end < 0) {

            end =
                    value.length();
        }


        String recommendation =
                value
                        .substring(
                                start,
                                end
                        )
                        .trim();


        if (recommendation.isBlank()) {

            return
                    "Inspect the crop and follow local agricultural guidance.";
        }


        return recommendation;
    }


    /*
     * =========================================================
     * Risk confidence
     * =========================================================
     */
    private Double riskLevelConfidence(
            String level
    ) {

        if (
                "HIGH".equalsIgnoreCase(
                        level
                )
        ) {

            return 0.85;
        }


        if (
                "MEDIUM".equalsIgnoreCase(
                        level
                )
        ) {

            return 0.75;
        }


        if (
                "LOW".equalsIgnoreCase(
                        level
                )
        ) {

            return 0.70;
        }


        return 0.60;
    }


    /*
     * =========================================================
     * Confidence label
     * =========================================================
     */
    private String confidenceLabel(
            Double confidence
    ) {

        if (confidence == null) {

            return "UNKNOWN";
        }


        if (confidence >= 0.85) {

            return "HIGH";
        }


        if (confidence >= 0.70) {

            return "MEDIUM";
        }


        return "LOW";
    }


    /*
     * =========================================================
     * Safe string
     * =========================================================
     */
    private String safe(
            String value
    ) {

        return value == null
                ? "No information is currently available."
                : value;
    }


    /*
     * =========================================================
     * Keyword helper
     * =========================================================
     */
    private boolean containsAny(
            String text,
            String... keywords
    ) {

        if (text == null) {

            return false;
        }


        for (
                String keyword
                : keywords
        ) {

            if (
                    keyword != null
                            && text.contains(
                            keyword
                    )
            ) {

                return true;
            }
        }


        return false;
    }


    /*
     * =========================================================
     * Conversation title
     * =========================================================
     */
    private String buildConversationTitle(
            String question
    ) {

        if (
                question == null
                        || question.isBlank()
        ) {

            return "AgriNexus Farm Copilot";
        }


        String title =
                question.trim();


        if (title.length() > 280) {

            title =
                    title.substring(
                            0,
                            280
                    );
        }


        return title;
    }


    /*
     * =========================================================
     * Channel
     * =========================================================
     */
    private String normalizeChannel(
            String channel
    ) {

        if (
                channel == null
                        || channel.isBlank()
        ) {

            return "WEB";
        }


        return channel
                .trim()
                .toUpperCase();
    }


    /*
     * =========================================================
     * Validation
     * =========================================================
     */
    private void validate(
            AICopilotRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Request cannot be null"
            );
        }


        if (request.getFarmId() == null) {

            throw new RuntimeException(
                    "Farm ID is required"
            );
        }


        if (
                request.getQuestion() == null
                        || request.getQuestion().isBlank()
        ) {

            throw new RuntimeException(
                    "Question is required"
            );
        }


        if (
                request.getQuestion().length() > 5000
        ) {

            throw new RuntimeException(
                    "Question cannot exceed 5000 characters"
            );
        }
    }


    /*
     * =========================================================
     * Internal Copilot result
     * =========================================================
     */
    private record CopilotDecision(
            String decisionType,
            String decision,
            String answer,
            String why,
            String contributingFactors,
            String dataUsed,
            Double confidence,
            String source,
            Boolean requiresReview
    ) {
    }
}