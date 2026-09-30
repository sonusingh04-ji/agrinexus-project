package com.agrinexus.service;

import com.agrinexus.enums.SupportedLanguage;
import org.springframework.stereotype.Service;

@Service
public class LanguageResponseService {

    /*
     * -------------------------------------------------------------
     * Normalize language.
     * -------------------------------------------------------------
     */
    public SupportedLanguage language(
            String language
    ) {

        return SupportedLanguage.fromCode(
                language
        );
    }

    /*
     * -------------------------------------------------------------
     * Irrigation response.
     * -------------------------------------------------------------
     */
    public String irrigationAnswer(
            String language,
            String decision
    ) {

        SupportedLanguage lang =
                language(language);

        if (lang == SupportedLanguage.HI) {

            return hindiIrrigation(
                    decision
            );
        }

        if (lang == SupportedLanguage.EN) {

            return englishIrrigation(
                    decision
            );
        }

        /*
         * Other registered languages currently use the
         * verified English fallback until their translations
         * are explicitly added and tested.
         */
        return englishIrrigation(
                decision
        );
    }

    /*
     * -------------------------------------------------------------
     * Irrigation explanation.
     * -------------------------------------------------------------
     */
    public String irrigationWhy(
            String language,
            String context
    ) {

        SupportedLanguage lang =
                language(language);

        if (lang == SupportedLanguage.HI) {

            return
                    "यह सिंचाई सलाह मिट्टी की नमी, "
                            + "फसल की जानकारी और मौसम/बारिश की उपलब्ध जानकारी "
                            + "को देखकर बनाई गई है। "
                            + safe(context);
        }

        return
                "This irrigation advice uses soil moisture, "
                        + "crop information and available weather/rainfall context. "
                        + safe(context);
    }

    /*
     * -------------------------------------------------------------
     * Weather response.
     * -------------------------------------------------------------
     */
    public String weatherAnswer(
            String language,
            String fieldOperation,
            String weatherRisk
    ) {

        SupportedLanguage lang =
                language(language);

        if (lang == SupportedLanguage.HI) {

            return
                    "मौसम की वर्तमान जानकारी: "
                            + safe(fieldOperation)
                            + " मौसम जोखिम: "
                            + safe(weatherRisk);
        }

        return
                "Current weather information: "
                        + safe(fieldOperation)
                        + " Weather risk: "
                        + safe(weatherRisk);
    }

    /*
     * -------------------------------------------------------------
     * Crop response.
     * -------------------------------------------------------------
     */
    public String cropAnswer(
            String language,
            String cropDecision
    ) {

        SupportedLanguage lang =
                language(language);

        if (lang == SupportedLanguage.HI) {

            return
                    "आपके खेत की फसल की जानकारी: "
                            + safe(cropDecision)
                            + " अगली फसल या बोआई के लिए पूरी कृषि सलाह देखें।";
        }

        return
                "Your farm crop information is: "
                        + safe(cropDecision)
                        + " For the next crop or sowing, check the full agricultural recommendation.";
    }

    /*
     * -------------------------------------------------------------
     * Nutrient response.
     * -------------------------------------------------------------
     */
    public String nutrientAnswer(
            String language,
            String nutrientDecision
    ) {

        SupportedLanguage lang =
                language(language);

        if (lang == SupportedLanguage.HI) {

            return
                    "मिट्टी और पोषक तत्वों की वर्तमान जानकारी: "
                            + safe(nutrientDecision)
                            + " खाद डालने से पहले नवीनतम मिट्टी जांच और स्थानीय कृषि सलाह देखें।";
        }

        return
                "Current soil and nutrient information: "
                        + safe(nutrientDecision)
                        + " Check the latest soil test and local agricultural guidance before applying fertilizer.";
    }

    /*
     * -------------------------------------------------------------
     * General response.
     * -------------------------------------------------------------
     */
    public String generalAnswer(
            String language
    ) {

        SupportedLanguage lang =
                language(language);

        if (lang == SupportedLanguage.HI) {

            return
                    "आपके खेत की वर्तमान जानकारी उपलब्ध है। "
                            + "सिंचाई, मौसम, फसल या खाद के बारे में पूछें।";
        }

        return
                "Your latest farm information is available. "
                        + "Ask about irrigation, weather, crops or fertilizer.";
    }

    /*
     * -------------------------------------------------------------
     * Voice / IVR short response.
     * -------------------------------------------------------------
     */
    public String shortForVoice(
            String language,
            String text
    ) {

        SupportedLanguage lang =
                language(language);

        if (text == null
                || text.isBlank()) {

            if (lang == SupportedLanguage.HI) {

                return
                        "अभी कोई स्पष्ट जवाब उपलब्ध नहीं है।";
            }

            return
                    "No clear answer is available right now.";
        }

        String clean =
                text
                        .replace("\n", " ")
                        .replace("  ", " ")
                        .trim();

        /*
         * Keep voice answers short enough for IVR/voice use.
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

    /*
     * -------------------------------------------------------------
     * English irrigation messages.
     * -------------------------------------------------------------
     */
    private String englishIrrigation(
            String decision
    ) {

        if ("IRRIGATION_RECOMMENDED".equals(
                decision
        )) {

            return
                    "The current irrigation context indicates that irrigation may need to be evaluated. "
                            + "Check the latest field moisture and weather conditions before taking action.";
        }

        if ("SUFFICIENT".equals(
                decision
        )) {

            return
                    "The latest stored irrigation context indicates that soil water is currently within the configured range, "
                            + "so additional irrigation is not indicated by this context alone.";
        }

        if ("MONITOR".equals(
                decision
        )) {

            return
                    "The latest irrigation context suggests monitoring soil moisture before deciding on irrigation.";
        }

        if ("INSUFFICIENT_DATA".equals(
                decision
        )) {

            return
                    "More information is required before making a reliable irrigation decision.";
        }

        return
                "The latest irrigation information does not provide enough evidence for a confident irrigation action.";
    }

    /*
     * -------------------------------------------------------------
     * Hindi irrigation messages.
     * -------------------------------------------------------------
     */
    private String hindiIrrigation(
            String decision
    ) {

        if ("IRRIGATION_RECOMMENDED".equals(
                decision
        )) {

            return
                    "मिट्टी की नमी कम हो रही है। "
                            + "सिंचाई की जरूरत हो सकती है। "
                            + "पानी देने से पहले खेत की वर्तमान स्थिति जरूर देखें।";
        }

        if ("SUFFICIENT".equals(
                decision
        )) {

            return
                    "अभी अतिरिक्त सिंचाई की जरूरत नहीं है। "
                            + "मिट्टी में पानी पर्याप्त है।";
        }

        if ("MONITOR".equals(
                decision
        )) {

            return
                    "मिट्टी की नमी पर नजर रखें। "
                            + "अभी सिंचाई का फैसला करने से पहले थोड़ा इंतजार करें।";
        }

        if ("INSUFFICIENT_DATA".equals(
                decision
        )) {

            return
                    "सिंचाई का सही फैसला देने के लिए और जानकारी चाहिए।";
        }

        return
                "अभी सिंचाई के बारे में स्पष्ट सलाह उपलब्ध नहीं है।";
    }

    /*
     * -------------------------------------------------------------
     * Disease / Pest / Crop Health response.
     * -------------------------------------------------------------
     */
    public String diseasePestAnswer(
            String language,
            String riskName,
            String riskLevel,
            Double riskScore,
            String recommendation
    ) {

        SupportedLanguage lang =
                language(language);

        /*
         * ---------------------------------------------------------
         * HINDI
         * ---------------------------------------------------------
         */
        if (lang == SupportedLanguage.HI) {

            if ("HIGH".equalsIgnoreCase(
                    riskLevel
            )) {

                return
                        "आपकी फसल में अभी पर्यावरण से जुड़ा खतरा अधिक है। "
                                + safeRiskNameHindi(riskName)
                                + " खेत की फसल को ध्यान से देखें। "
                                + "फसल की स्थिति पर नजर रखें और मान्य गर्मी तथा सिंचाई सलाह का पालन करें।";
            }

            if ("MEDIUM".equalsIgnoreCase(
                    riskLevel
            )) {

                return
                        "आपकी फसल में कुछ पर्यावरणीय खतरे के संकेत हैं। "
                                + safeRiskNameHindi(riskName)
                                + " खेत की नियमित निगरानी करें। "
                                + "फसल में असामान्य लक्षण दिखाई दें तो स्थानीय कृषि सलाह लें।";
            }

            if ("LOW".equalsIgnoreCase(
                    riskLevel
            )) {

                return
                        "अभी आपकी फसल में खतरे का स्तर कम है। "
                                + safeRiskNameHindi(riskName)
                                + " खेत पर नजर बनाए रखें।";
            }

            return
                    "अभी आपकी फसल के रोग या कीट जोखिम के बारे में स्पष्ट जानकारी उपलब्ध नहीं है।";
        }

        /*
         * ---------------------------------------------------------
         * ENGLISH
         * ---------------------------------------------------------
         */
        if ("HIGH".equalsIgnoreCase(
                riskLevel
        )) {

            return
                    "Your crop currently has a high environmental risk signal. "
                            + safe(riskName)
                            + " Inspect the field carefully. "
                            + "Monitor crop condition and follow validated heat and irrigation guidance.";
        }

        if ("MEDIUM".equalsIgnoreCase(
                riskLevel
        )) {

            return
                    "Your crop has a moderate environmental risk signal. "
                            + safe(riskName)
                            + " Monitor the field carefully and seek local agricultural guidance if unusual symptoms appear.";
        }

        if ("LOW".equalsIgnoreCase(
                riskLevel
        )) {

            return
                    "Your crop currently has a low risk signal. "
                            + safe(riskName)
                            + " Continue monitoring the field.";
        }

        return
                "A clear disease or pest risk assessment is not currently available.";
    }

    /*
     * -------------------------------------------------------------
     * Hindi names for known AgriNexus environmental risks.
     * -------------------------------------------------------------
     */
    private String safeRiskNameHindi(
            String riskName
    ) {

        if (riskName == null
                || riskName.isBlank()) {

            return "फसल स्वास्थ्य जोखिम";
        }

        if (riskName.equalsIgnoreCase(
                "Extreme Heat Crop Stress Risk"
        )) {

            return
                    "अत्यधिक गर्मी से फसल पर तनाव का खतरा";
        }

        if (riskName.equalsIgnoreCase(
                "Fungal Disease Environmental Risk"
        )) {

            return
                    "फफूंद रोग का पर्यावरणीय खतरा";
        }

        if (riskName.equalsIgnoreCase(
                "General Insect Pest Environmental Risk"
        )) {

            return
                    "कीट प्रकोप का सामान्य पर्यावरणीय खतरा";
        }

        /*
         * Unknown names are preserved rather than
         * being incorrectly translated.
         */
        return riskName;
    }

    /*
     * -------------------------------------------------------------
     * Null safety.
     * -------------------------------------------------------------
     */
    private String safe(
            String value
    ) {

        return value == null
                ? "जानकारी उपलब्ध नहीं है।"
                : value;
    }
}