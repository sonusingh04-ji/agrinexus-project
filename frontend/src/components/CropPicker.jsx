import { useEffect, useMemo, useState } from "react";
import { getAllCrops } from "../api/cropSoilApi";
import "./CropPicker.css";

/*
 * =========================================================
 * UI metadata only
 * =========================================================
 *
 * Backend remains the source of truth for:
 * - crop UUID
 * - crop name
 * - season
 * - agronomic information
 *
 * This object only supplies:
 * - icon
 * - Hindi display name
 */

const CROP_UI = {
  wheat: {
    icon: "🌾",
    nameHi: "गेहूं",
  },

  rice: {
    icon: "🌱",
    nameHi: "धान",
  },

  maize: {
    icon: "🌽",
    nameHi: "मक्का",
  },

  mustard: {
    icon: "🌻",
    nameHi: "सरसों",
  },

  "mustard-greens": {
    icon: "🥬",
    nameHi: "सरसों का साग",
  },

  chickpea: {
    icon: "🫘",
    nameHi: "चना",
  },

  sugarcane: {
    icon: "🎋",
    nameHi: "गन्ना",
  },

  potato: {
    icon: "🥔",
    nameHi: "आलू",
  },

  barley: {
    icon: "🌾",
    nameHi: "जौ",
  },

  lentil: {
    icon: "🫘",
    nameHi: "मसूर",
  },

  masur: {
    icon: "🫘",
    nameHi: "मसूर",
  },

  peas: {
    icon: "🫛",
    nameHi: "मटर",
  },

  "field-pea": {
    icon: "🫛",
    nameHi: "मटर",
  },

  onion: {
    icon: "🧅",
    nameHi: "प्याज",
  },

  garlic: {
    icon: "🧄",
    nameHi: "लहसुन",
  },

  tomato: {
    icon: "🍅",
    nameHi: "टमाटर",
  },

  brinjal: {
    icon: "🍆",
    nameHi: "बैंगन",
  },

  cabbage: {
    icon: "🥬",
    nameHi: "पत्तागोभी",
  },

  cauliflower: {
    icon: "🥦",
    nameHi: "फूलगोभी",
  },

  carrot: {
    icon: "🥕",
    nameHi: "गाजर",
  },

  radish: {
    icon: "🌱",
    nameHi: "मूली",
  },

  spinach: {
    icon: "🥬",
    nameHi: "पालक",
  },

  okra: {
    icon: "🥬",
    nameHi: "भिंडी",
  },

  chilli: {
    icon: "🌶️",
    nameHi: "मिर्च",
  },

  groundnut: {
    icon: "🥜",
    nameHi: "मूंगफली",
  },

  soybean: {
    icon: "🌱",
    nameHi: "सोयाबीन",
  },

  cotton: {
    icon: "🌿",
    nameHi: "कपास",
  },

  sunflower: {
    icon: "🌻",
    nameHi: "सूरजमुखी",
  },

  sesame: {
    icon: "🌱",
    nameHi: "तिल",
  },
  banana: {
    icon: "🍌",
    nameHi: "केला",
  },

  mango: {
    icon: "🥭",
    nameHi: "आम",
  },

  guava: {
    icon: "🍈",
    nameHi: "अमरूद",
  },

  papaya: {
    icon: "🥭",
    nameHi: "पपीता",
  },

  watermelon: {
    icon: "🍉",
    nameHi: "तरबूज",
  },

  muskmelon: {
    icon: "🍈",
    nameHi: "खरबूजा",
  },

  turmeric: {
    icon: "🌿",
    nameHi: "हल्दी",
  },

  ginger: {
    icon: "🌿",
    nameHi: "अदरक",
  },

  coriander: {
    icon: "🌿",
    nameHi: "धनिया",
  },

  cumin: {
    icon: "🌿",
    nameHi: "जीरा",
  },

  tea: {
    icon: "🍃",
    nameHi: "चाय",
  },

  coffee: {
    icon: "☕",
    nameHi: "कॉफी",
  },

  tobacco: {
    icon: "🌿",
    nameHi: "तंबाकू",
  },

  rubber: {
    icon: "🌳",
    nameHi: "रबर",
  },

  ragi: {
    icon: "🌾",
    nameHi: "रागी",
  },

  "finger-millet": {
    icon: "🌾",
    nameHi: "रागी",
  },

  jowar: {
    icon: "🌾",
    nameHi: "ज्वार",
  },

  sorghum: {
    icon: "🌾",
    nameHi: "ज्वार",
  },

  bajra: {
    icon: "🌾",
    nameHi: "बाजरा",
  },

  "pearl-millet": {
    icon: "🌾",
    nameHi: "बाजरा",
  },

  "black-gram": {
    icon: "🫘",
    nameHi: "उड़द",
  },

  urad: {
    icon: "🫘",
    nameHi: "उड़द",
  },

  "green-gram": {
    icon: "🫘",
    nameHi: "मूंग",
  },

  moong: {
    icon: "🫘",
    nameHi: "मूंग",
  },

  "pigeon-pea": {
    icon: "🫘",
    nameHi: "अरहर",
  },

  "tur-arhar": {
    icon: "🫘",
    nameHi: "अरहर",
  },

  safflower: {
    icon: "🌼",
    nameHi: "कुसुम",
  },

  linseed: {
    icon: "🌱",
    nameHi: "अलसी",
  },

  castor: {
    icon: "🌿",
    nameHi: "अरंडी",
  },

  sesbania: {
    icon: "🌿",
    nameHi: "सनई",
  },

  oat: {
    icon: "🌾",
    nameHi: "जई",
  },

  "sweet-potato": {
    icon: "🍠",
    nameHi: "शकरकंद",
  },

  jute: {
    icon: "🌿",
    nameHi: "जूट",
  },

  cardamom: {
    icon: "🌿",
    nameHi: "इलायची",
  },
};


/*
 * Convert backend crop name to a predictable key.
 */
function normalizeCropKey(name) {
  return String(name || "")
      .trim()
      .toLowerCase()
      .replace(/[\/()]+/g, " ")
      .replace(/\s+/g, "-");
}

function getCropUi(crop) {

  const key =
      normalizeCropKey(crop?.name);

  return (
      CROP_UI[key] || {
        icon: "🌱",
        nameHi: crop?.name || "फसल",
      }
  );
}


function CropPicker({
                      selectedCrop,
                      onSelect,
                      language,
                    }) {

  const [crops, setCrops] =
      useState([]);

  const [loading, setLoading] =
      useState(true);

  const [error, setError] =
      useState("");

  const [search, setSearch] =
      useState("");


  /*
   * =========================================================
   * Load real crops from backend
   * =========================================================
   */
  useEffect(() => {

    let cancelled = false;


    const loadCrops = async () => {

      setLoading(true);
      setError("");


      try {

        const response =
            await getAllCrops();


        let records = [];


        if (Array.isArray(response)) {

          records = response;

        } else if (
            Array.isArray(
                response?.content
            )
        ) {

          records =
              response.content;
        }


        /*
         * Remove invalid records.
         */
        records =
            records.filter(
                (crop) =>
                    crop?.id &&
                    crop?.name
            );


        /*
         * Sort alphabetically.
         */
        records.sort(
            (a, b) =>
                String(a.name)
                    .localeCompare(
                        String(b.name)
                    )
        );


        if (!cancelled) {
          setCrops(records);
        }

      } catch (requestError) {

        console.error(
            "Failed to load crops:",
            requestError
        );


        if (!cancelled) {

          setError(
              requestError?.message ||
              (
                  language === "hi"
                      ? "फसल डेटा लोड नहीं हो सका।"
                      : "Unable to load crop data."
              )
          );

          setCrops([]);
        }

      } finally {

        if (!cancelled) {
          setLoading(false);
        }
      }
    };


    loadCrops();


    return () => {
      cancelled = true;
    };

  }, [language]);


  /*
   * =========================================================
   * Search
   * =========================================================
   */
  const filteredCrops = useMemo(() => {

    const term =
        search.trim().toLowerCase();


    if (!term) {
      return crops;
    }


    return crops.filter(
        (crop) => {

          const name =
              String(
                  crop.name || ""
              ).toLowerCase();


          const season =
              String(
                  crop.season || ""
              ).toLowerCase();


          const soil =
              String(
                  crop.soilType || ""
              ).toLowerCase();


          return (
              name.includes(term) ||
              season.includes(term) ||
              soil.includes(term)
          );
        }
    );

  }, [crops, search]);


  /*
   * =========================================================
   * Loading
   * =========================================================
   */
  if (loading) {

    return (
        <div className="crop-picker-loading">
          <div className="crop-picker-loading-icon">
            🌱
          </div>

          <div>
            {language === "hi"
                ? "वास्तविक crop data लोड हो रहा है..."
                : "Loading real crop data..."}
          </div>
        </div>
    );
  }


  /*
   * =========================================================
   * Error
   * =========================================================
   */
  if (error) {

    return (
        <div className="crop-picker-error">
          <span>⚠️</span>

          <span>
                    {error}
                </span>
        </div>
    );
  }


  /*
   * =========================================================
   * No data
   * =========================================================
   */
  if (crops.length === 0) {

    return (
        <div className="crop-picker-error">
          {language === "hi"
              ? "Backend में कोई crop उपलब्ध नहीं है।"
              : "No crops are available from the backend."}
        </div>
    );
  }


  return (
      <div className="crop-picker-wrapper">

        {/* =================================================
                Search
            ================================================= */}

        <div className="crop-picker-search-wrapper">

                <span className="crop-picker-search-icon">
                    🔍
                </span>

          <input
              type="search"
              className="crop-picker-search"
              value={search}
              onChange={(event) =>
                  setSearch(
                      event.target.value
                  )
              }
              placeholder={
                language === "hi"
                    ? "फसल खोजें..."
                    : "Search crop..."
              }
          />

        </div>


        {/* =================================================
                Crop count
            ================================================= */}

        <div className="crop-picker-count">

          {language === "hi"
              ? `${filteredCrops.length} फसलें उपलब्ध`
              : `${filteredCrops.length} crops available`}

        </div>


        {/* =================================================
                Crop Grid
            ================================================= */}

        {filteredCrops.length === 0 ? (

            <div className="crop-picker-no-results">

              🔎

              <span>
                        {language === "hi"
                            ? "कोई फसल नहीं मिली"
                            : "No crops found"}
                    </span>

            </div>

        ) : (

            <div className="crop-picker-grid">

              {filteredCrops.map((crop) => {

                const ui =
                    getCropUi(crop);


                const isSelected =
                    String(
                        selectedCrop
                    ) === String(
                        crop.id
                    );


                return (
                    <button
                        key={crop.id}
                        type="button"
                        className={`crop-picker-item ${
                            isSelected
                                ? "crop-picker-item-selected"
                                : ""
                        }`}
                        onClick={() =>
                            onSelect(
                                crop.id
                            )
                        }
                    >

                                <span className="crop-picker-icon">
                                    {ui.icon}
                                </span>


                      <span className="crop-picker-name">

                                    {language === "hi"
                                        ? ui.nameHi
                                        : crop.name}

                                </span>


                      {crop.season && (

                          <span className="crop-picker-season">
                                        {crop.season}
                                    </span>

                      )}


                      {isSelected && (

                          <span className="crop-picker-check">
                                        ✓
                                    </span>

                      )}

                    </button>
                );

              })}

            </div>

        )}

      </div>
  );
}


export default CropPicker;