import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

import en from "../locales/en.json";
import hi from "../locales/hi.json";

/*
 * =========================================================
 * AgriNexus Language Configuration
 * =========================================================
 *
 * The application supports the 22 Scheduled Languages
 * as selectable preferences.
 *
 * Translation files can be added progressively.
 */

const LanguageContext =
    createContext(null);

const STORAGE_KEY =
    "agrinexus_language";


/*
 * =========================================================
 * Supported Languages
 * =========================================================
 */

export const LANGUAGES = [

  {
    code: "en",
    name: "English",
    nativeName: "English",
  },

  {
    code: "as",
    name: "Assamese",
    nativeName: "অসমীয়া",
  },

  {
    code: "bn",
    name: "Bengali",
    nativeName: "বাংলা",
  },

  {
    code: "brx",
    name: "Bodo",
    nativeName: "बड़ो",
  },

  {
    code: "doi",
    name: "Dogri",
    nativeName: "डोगरी",
  },

  {
    code: "gu",
    name: "Gujarati",
    nativeName: "ગુજરાતી",
  },

  {
    code: "hi",
    name: "Hindi",
    nativeName: "हिन्दी",
  },

  {
    code: "kn",
    name: "Kannada",
    nativeName: "ಕನ್ನಡ",
  },

  {
    code: "ks",
    name: "Kashmiri",
    nativeName: "कॉशुर",
  },

  {
    code: "kok",
    name: "Konkani",
    nativeName: "कोंकणी",
  },

  {
    code: "mai",
    name: "Maithili",
    nativeName: "मैथिली",
  },

  {
    code: "ml",
    name: "Malayalam",
    nativeName: "മലയാളം",
  },

  {
    code: "mni",
    name: "Manipuri",
    nativeName: "মৈতৈলোন্",
  },

  {
    code: "mr",
    name: "Marathi",
    nativeName: "मराठी",
  },

  {
    code: "ne",
    name: "Nepali",
    nativeName: "नेपाली",
  },

  {
    code: "or",
    name: "Odia",
    nativeName: "ଓଡ଼ିଆ",
  },

  {
    code: "pa",
    name: "Punjabi",
    nativeName: "ਪੰਜਾਬੀ",
  },

  {
    code: "sa",
    name: "Sanskrit",
    nativeName: "संस्कृत",
  },

  {
    code: "sat",
    name: "Santali",
    nativeName: "ᱥᱟᱱᱛᱟᱲᱤ",
  },

  {
    code: "sd",
    name: "Sindhi",
    nativeName: "सिन्धी",
  },

  {
    code: "ta",
    name: "Tamil",
    nativeName: "தமிழ்",
  },

  {
    code: "te",
    name: "Telugu",
    nativeName: "తెలుగు",
  },

  {
    code: "ur",
    name: "Urdu",
    nativeName: "اردو",
  },
];


/*
 * =========================================================
 * Translation Registry
 * =========================================================
 *
 * At the moment:
 * English + Hindi are real translation files.
 *
 * The remaining languages can be added as:
 *
 * ../locales/mr.json
 * ../locales/ta.json
 * etc.
 *
 * Until then, the UI safely falls back to English.
 */

const translations = {
  en,
  hi,
};


/*
 * =========================================================
 * Validate language
 * =========================================================
 */

function isSupportedLanguage(
    languageCode
) {

  return LANGUAGES.some(
      (language) =>
          language.code === languageCode
  );
}


/*
 * =========================================================
 * Normalize language
 * =========================================================
 */

function normalizeLanguage(
    languageCode
) {

  if (
      languageCode &&
      isSupportedLanguage(
          languageCode
      )
  ) {
    return languageCode;
  }

  return "en";
}


/*
 * =========================================================
 * Translation lookup
 * =========================================================
 *
 * Priority:
 *
 * 1. Selected language
 * 2. English fallback
 * 3. Translation key itself
 */

function getTranslation(
    languageCode,
    key
) {

  if (!key) {
    return "";
  }


  const selectedTranslations =
      translations[
          languageCode
          ];


  if (
      selectedTranslations &&
      Object.prototype.hasOwnProperty.call(
          selectedTranslations,
          key
      )
  ) {

    return selectedTranslations[
        key
        ];
  }


  /*
   * English fallback.
   */

  if (
      languageCode !== "en" &&
      Object.prototype.hasOwnProperty.call(
          en,
          key
      )
  ) {

    return en[key];
  }


  return key;
}


/*
 * =========================================================
 * Provider
 * =========================================================
 */

export function LanguageProvider({
                                   children,
                                 }) {

  const [
    language,
    setLanguageState,
  ] = useState(() => {

    const saved =
        localStorage.getItem(
            STORAGE_KEY
        );

    return normalizeLanguage(
        saved
    );
  });


  /*
   * =======================================================
   * Change language
   * =======================================================
   */

  const setLanguage =
      (newLanguage) => {

        const normalized =
            normalizeLanguage(
                String(
                    newLanguage || ""
                ).toLowerCase()
            );


        setLanguageState(
            normalized
        );
      };


  /*
   * =======================================================
   * Persist language
   * =======================================================
   */

  useEffect(() => {

    localStorage.setItem(
        STORAGE_KEY,
        language
    );


    /*
     * Useful for the browser and future
     * accessibility / localization work.
     */

    document.documentElement
        .setAttribute(
            "lang",
            language
        );

  }, [
    language,
  ]);


  /*
   * =======================================================
   * Translation function
   * =======================================================
   *
   * Supports:
   *
   * t("home.title")
   *
   * and interpolation:
   *
   * t("welcome", { name: "Sonu" })
   *
   */

  const t = (
      key,
      variables = {}
  ) => {

    let value =
        getTranslation(
            language,
            key
        );


    /*
     * Replace:
     *
     * {{name}}
     * {{count}}
     * etc.
     */

    Object.entries(
        variables
    ).forEach(
        ([variable, variableValue]) => {

          value =
              String(value)
                  .replaceAll(
                      `{{${variable}}}`,
                      String(
                          variableValue
                      )
                  );
        }
    );


    return value;
  };


  /*
   * =======================================================
   * Current language information
   * =======================================================
   */

  const currentLanguage =
      useMemo(
          () =>
              LANGUAGES.find(
                  (item) =>
                      item.code ===
                      language
              ) ||
              LANGUAGES[0],
          [
            language,
          ]
      );


  /*
   * =======================================================
   * Whether complete translation file exists
   * =======================================================
   */

  const hasFullTranslation =
      Boolean(
          translations[
              language
              ]
      );


  const value =
      useMemo(
          () => ({
            language,

            setLanguage,

            t,

            languages:
            LANGUAGES,

            currentLanguage,

            hasFullTranslation,
          }),
          [
            language,
            currentLanguage,
            hasFullTranslation,
          ]
      );


  return (
      <LanguageContext.Provider
          value={value}
      >
        {children}
      </LanguageContext.Provider>
  );
}


/*
 * =========================================================
 * Hook
 * =========================================================
 */

export function useLanguage() {

  const context =
      useContext(
          LanguageContext
      );


  if (!context) {

    throw new Error(
        "useLanguage must be used inside LanguageProvider"
    );
  }


  return context;
}