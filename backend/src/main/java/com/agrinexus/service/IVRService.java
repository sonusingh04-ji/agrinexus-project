package com.agrinexus.service;

import com.agrinexus.dto.AICopilotRequest;
import com.agrinexus.dto.AICopilotResponse;
import com.agrinexus.dto.IVRRequest;
import com.agrinexus.dto.IVRResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.IVRSession;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.IVRSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IVRService {

    private final IVRSessionRepository sessionRepository;
    private final FarmRepository farmRepository;
    private final AICopilotService copilotService;

    /*
     * Start a new IVR session.
     */
    @Transactional
    public IVRResponse start(
            IVRRequest request
    ) {

        if (request == null
                || request.getFarmId() == null) {

            throw new RuntimeException(
                    "Farm ID is required"
            );
        }

        Farm farm =
                farmRepository.findById(
                        request.getFarmId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Farm not found"
                        )
                );

        LocalDateTime now =
                LocalDateTime.now();

        IVRSession session =
                IVRSession.builder()
                        .farm(farm)
                        .callerReference(
                                request.getCallerReference()
                        )
                        .language(
                                normalizeLanguage(
                                        request.getLanguage()
                                )
                        )
                        .status("ACTIVE")
                        .currentMenu("MAIN")
                        .startedAt(now)
                        .lastInteractionAt(now)
                        .expiresAt(
                                now.plusMinutes(10)
                        )
                        .notes(
                                "Provider-neutral AgriNexus IVR session."
                        )
                        .build();

        session =
                sessionRepository.save(
                        session
                );

        return buildMainMenu(
                session
        );
    }

    /*
     * Process an IVR interaction.
     */
    @Transactional
    public IVRResponse interact(
            IVRRequest request
    ) {

        if (request == null
                || request.getSessionId() == null) {

            throw new RuntimeException(
                    "Session ID is required"
            );
        }

        IVRSession session =
                sessionRepository.findById(
                        request.getSessionId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "IVR session not found"
                        )
                );

        ensureActive(session);

        session.setLastInteractionAt(
                LocalDateTime.now()
        );

        String digit =
                request.getDigit() == null
                        ? ""
                        : request.getDigit().trim();

        /*
         * Main menu.
         */
        if ("MAIN".equals(
                session.getCurrentMenu()
        )) {

            return handleMainMenu(
                    session,
                    digit,
                    request
            );
        }

        /*
         * Question mode.
         */
        if ("ASK".equals(
                session.getCurrentMenu()
        )) {

            String question =
                    request.getQuestion();

            if (question == null
                    || question.isBlank()) {

                return response(
                        session,
                        "COPILOT",
                        "VOICE_INPUT_REQUIRED",
                        "Please provide your question.",
                        true
                );
            }

            return handleQuestion(
                    session,
                    question
            );
        }

        /*
         * Unknown state.
         */
        session.setCurrentMenu(
                "MAIN"
        );

        sessionRepository.save(
                session
        );

        return buildMainMenu(
                session
        );
    }

    /*
     * Get session by ID.
     */
    public IVRSession getSession(
            UUID sessionId
    ) {

        return sessionRepository.findById(
                        sessionId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "IVR session not found"
                        )
                );
    }

    /*
     * End session.
     */
    @Transactional
    public IVRResponse end(
            UUID sessionId
    ) {

        IVRSession session =
                getSession(
                        sessionId
                );

        session.setStatus(
                "COMPLETED"
        );

        session.setCurrentMenu(
                "END"
        );

        session.setLastInteractionAt(
                LocalDateTime.now()
        );

        sessionRepository.save(
                session
        );

        return response(
                session,
                "EXIT",
                "END_SESSION",
                "Thank you for using AgriNexus. Goodbye.",
                false
        );
    }

    /*
     * Main IVR menu.
     */
    private IVRResponse buildMainMenu(
            IVRSession session
    ) {

        String language =
                session.getLanguage();

        String text;

        if ("HI".equals(language)) {

            text =
                    "AgriNexus mein aapka swagat hai. "
                            + "Aaj ki salah ke liye 1 dabayein. "
                            + "Mausam ke liye 2. "
                            + "Paani ke liye 3. "
                            + "Fasal ke liye 4. "
                            + "AgriNexus se sawaal poochhne ke liye 5. "
                            + "Bahaar nikalne ke liye 0 dabayein.";

        } else {

            text =
                    "Welcome to AgriNexus. "
                            + "Press 1 for today's advice. "
                            + "Press 2 for weather. "
                            + "Press 3 for water. "
                            + "Press 4 for crop advice. "
                            + "Press 5 to ask AgriNexus a question. "
                            + "Press 0 to exit.";
        }

        return response(
                session,
                "MENU",
                "MAIN_MENU",
                text,
                true
        );
    }

    /*
     * Main menu routing.
     */
    private IVRResponse handleMainMenu(
            IVRSession session,
            String digit,
            IVRRequest request
    ) {

        switch (digit) {

            case "1":
                return todayAdvice(
                        session
                );

            case "2":
                return weatherAdvice(
                        session
                );

            case "3":
                return waterAdvice(
                        session
                );

            case "4":
                return cropAdvice(
                        session
                );

            case "5":

                session.setCurrentMenu(
                        "ASK"
                );

                sessionRepository.save(
                        session
                );

                return response(
                        session,
                        "MENU",
                        "ASK_QUESTION",
                        askQuestionText(
                                session.getLanguage()
                        ),
                        true
                );

            case "0":

                session.setStatus(
                        "COMPLETED"
                );

                session.setCurrentMenu(
                        "END"
                );

                sessionRepository.save(
                        session
                );

                return response(
                        session,
                        "EXIT",
                        "END_SESSION",
                        goodbyeText(
                                session.getLanguage()
                        ),
                        false
                );

            default:

                return response(
                        session,
                        "MENU",
                        "INVALID_OPTION",
                        invalidOptionText(
                                session.getLanguage()
                        ),
                        true
                );
        }
    }

    /*
     * Today's advice.
     *
     * This first version is intentionally lightweight.
     * Later it can call the central Today orchestration API.
     */
    private IVRResponse todayAdvice(
            IVRSession session
    ) {

        String text;

        if ("HI".equals(
                session.getLanguage()
        )) {

            text =
                    "Aaj ki salah jaanne ke liye "
                            + "AgriNexus app ya SMS service ka upyog karein. "
                            + "Krishi faisla sthiti aur mausam ke anusaar badal sakta hai.";

        } else {

            text =
                    "Today's advice is based on your latest farm "
                            + "decision information. Please check the "
                            + "current farm recommendation before taking action.";
        }

        return response(
                session,
                "INFORMATION",
                "TODAY",
                text,
                true
        );
    }

    /*
     * Weather.
     *
     * Provider-neutral version.
     */
    private IVRResponse weatherAdvice(
            IVRSession session
    ) {

        String text;

        if ("HI".equals(
                session.getLanguage()
        )) {

            text =
                    "Mausam ki vartamaan sthiti aur kheti ke jokhim ki "
                            + "jaankari AgriNexus mein uplabdh hai. "
                            + "Kripya vartamaan weather alert dekhein.";

        } else {

            text =
                    "Current weather and farm-risk information "
                            + "is available in AgriNexus. Please check "
                            + "the latest weather alert before field operations.";
        }

        return response(
                session,
                "ALERT",
                "WEATHER",
                text,
                true
        );
    }

    /*
     * Water.
     */
    private IVRResponse waterAdvice(
            IVRSession session
    ) {

        String text;

        if ("HI".equals(
                session.getLanguage()
        )) {

            text =
                    "Paani ki salah aapke khet ki mitti ki nami, "
                            + "baarish aur sinchai faisle par aadharit hai. "
                            + "AgriNexus ki latest sinchai salah dekhein.";

        } else {

            text =
                    "Water advice uses your field moisture, "
                            + "rain forecast and irrigation decision. "
                            + "Please check the latest AgriNexus irrigation recommendation.";
        }

        return response(
                session,
                "INFORMATION",
                "WATER",
                text,
                true
        );
    }

    /*
     * Crop.
     */
    private IVRResponse cropAdvice(
            IVRSession session
    ) {

        String text;

        if ("HI".equals(
                session.getLanguage()
        )) {

            text =
                    "Fasal ki salah ke liye mausam, mitti, pichhli fasal "
                            + "aur kheti ki sthiti ko dekha jata hai. "
                            + "AgriNexus ki latest crop recommendation dekhein.";

        } else {

            text =
                    "Crop advice considers weather, soil, previous crop "
                            + "and current field conditions. Check the latest AgriNexus crop recommendation.";
        }

        return response(
                session,
                "INFORMATION",
                "CROP",
                text,
                true
        );
    }

    /*
     * Ask the Copilot.
     */
    private IVRResponse handleQuestion(
            IVRSession session,
            String question
    ) {

        AICopilotResponse copilotResponse =
                copilotService.ask(
                        AICopilotRequest.builder()
                                .farmId(
                                        session.getFarm().getId()
                                )
                                .question(question)
                                .language(
                                        session.getLanguage()
                                )
                                .channel("IVR")
                                .build()
                );

        /*
         * Return a short response suitable for IVR.
         */
        String shortAnswer =
                shortenForIVR(
                        copilotResponse.getAnswer()
                );

        session.setCurrentMenu(
                "MAIN"
        );

        sessionRepository.save(
                session
        );

        return response(
                session,
                "COPILOT",
                "COPILOT_ANSWER",
                shortAnswer,
                true
        );
    }

    /*
     * Keep IVR answers short.
     */
    private String shortenForIVR(
            String answer
    ) {

        if (answer == null
                || answer.isBlank()) {

            return
                    "No clear answer is available right now.";
        }

        String clean =
                answer
                        .replace("\n", " ")
                        .replace("  ", " ")
                        .trim();

        /*
         * Approximate maximum safe spoken length
         * for a simple IVR interaction.
         */
        if (clean.length() > 450) {

            clean =
                    clean.substring(
                            0,
                            447
                    )
                            + "...";
        }

        return clean;
    }

    private IVRResponse response(
            IVRSession session,
            String responseType,
            String action,
            String spokenText,
            boolean active
    ) {

        return IVRResponse.builder()
                .sessionId(
                        session.getId()
                )
                .farmId(
                        session.getFarm().getId()
                )
                .language(
                        session.getLanguage()
                )
                .menu(
                        session.getCurrentMenu()
                )
                .spokenText(
                        spokenText
                )
                .responseType(
                        responseType
                )
                .action(
                        action
                )
                .sessionActive(
                        active
                )
                .dataStatus(
                        "LIVE_BACKEND"
                )
                .respondedAt(
                        LocalDateTime.now()
                )
                .build();
    }

    private void ensureActive(
            IVRSession session
    ) {

        if (!"ACTIVE".equalsIgnoreCase(
                session.getStatus()
        )) {

            throw new RuntimeException(
                    "IVR session is no longer active"
            );
        }

        if (session.getExpiresAt() != null
                && LocalDateTime.now()
                .isAfter(
                        session.getExpiresAt()
                )) {

            session.setStatus(
                    "EXPIRED"
            );

            sessionRepository.save(
                    session
            );

            throw new RuntimeException(
                    "IVR session has expired"
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

    private String askQuestionText(
            String language
    ) {

        return "HI".equals(language)
                ? "Apna sawaal bolkar batayein."
                : "Please tell us your question.";
    }

    private String invalidOptionText(
            String language
    ) {

        return "HI".equals(language)
                ? "Galat vikalp. Kripya dobara chuniye."
                : "Invalid option. Please try again.";
    }

    private String goodbyeText(
            String language
    ) {

        return "HI".equals(language)
                ? "Dhanyavaad. AgriNexus ka upyog karne ke liye dhanyavaad."
                : "Thank you for using AgriNexus. Goodbye.";
    }
}