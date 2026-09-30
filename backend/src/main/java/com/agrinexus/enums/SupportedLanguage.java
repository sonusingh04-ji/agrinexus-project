package com.agrinexus.enums;

public enum SupportedLanguage {

    EN("English", "en"),
    HI("Hindi", "hi"),
    BN("Bengali", "bn"),
    GU("Gujarati", "gu"),
    MR("Marathi", "mr"),
    TA("Tamil", "ta"),
    TE("Telugu", "te"),
    KN("Kannada", "kn"),
    ML("Malayalam", "ml"),
    PA("Punjabi", "pa"),
    OR("Odia", "or"),
    AS("Assamese", "as"),
    UR("Urdu", "ur"),
    NE("Nepali", "ne"),
    KOK("Konkani", "kok"),
    MAI("Maithili", "mai"),
    DOG("Dogri", "doi"),
    KS("Kashmiri", "ks"),
    MNI("Manipuri", "mni"),
    SAT("Santali", "sat"),
    BODO("Bodo", "brx"),
    SA("Sanskrit", "sa");

    private final String displayName;
    private final String code;

    SupportedLanguage(
            String displayName,
            String code
    ) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public static SupportedLanguage fromCode(
            String value
    ) {

        if (value == null || value.isBlank()) {
            return EN;
        }

        String normalized =
                value.trim().toUpperCase();

        for (SupportedLanguage language :
                values()) {

            if (language.name().equals(normalized)
                    || language.code.equalsIgnoreCase(value.trim())) {

                return language;
            }
        }

        return EN;
    }
}