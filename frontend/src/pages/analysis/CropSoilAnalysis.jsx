import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import "./CropSoilAnalysis.css";

import { useLanguage } from "../../context/LanguageContext";
import { useFields } from "../../context/FieldsContext";

import {
    getSoilIntelligence,
    getLatestSoilSample,
    getLatestCropRotation,
    getSoilTestTrend,
    getActiveRecoveryPlans,
    getAllCrops,
} from "../../api/cropSoilApi";

import BottomNavBar from "../../components/BottomNavBar";


function CropSoilAnalysis() {
    const navigate = useNavigate();
    const location = useLocation();

    const { language } = useLanguage();
    const { fields } = useFields();

    const isHindi = language === "hi";


    /*
     * =========================================================
     * Farm ID
     * =========================================================
     *
     * Priority:
     *
     * 1. fieldId passed through navigation state
     * 2. farmId passed through navigation state
     * 3. current farm stored in localStorage
     * 4. first available field
     *
     * No hardcoded demo farm is used.
     * =========================================================
     */

    const stateFarmId =
        location.state?.fieldId ||
        location.state?.farmId ||
        null;

    const storedFarmId =
        localStorage.getItem(
            "agrinexus_current_farm_id"
        );

    const firstFarmId =
        Array.isArray(fields) && fields.length > 0
            ? fields[0]?.id
            : null;

    const farmId =
        stateFarmId ||
        storedFarmId ||
        firstFarmId;


    /*
     * =========================================================
     * State
     * =========================================================
     */

    const [soil, setSoil] = useState(null);

    const [soilSample, setSoilSample] =
        useState(null);

    const [cropRotation, setCropRotation] =
        useState(null);

    const [soilTrend, setSoilTrend] =
        useState([]);

    const [recoveryPlans, setRecoveryPlans] =
        useState([]);

    const [crops, setCrops] =
        useState([]);

    const [error, setError] =
        useState("");

    const [errors, setErrors] =
        useState([]);

    const [selectedCropId, setSelectedCropId] =
        useState("");

    /*
     * Instead of calling setLoading(true) synchronously
     * inside useEffect, we track which farm has finished loading.
     */
    const [loadedFarmId, setLoadedFarmId] =
        useState(null);


    /*
     * =========================================================
     * Loading state
     * =========================================================
     */

    const loading =
        Boolean(farmId) &&
        String(loadedFarmId) !== String(farmId);


    /*
     * =========================================================
     * Find current field
     * =========================================================
     */

    const currentField =
        farmId && Array.isArray(fields)
            ? fields.find(
                (field) =>
                    String(field.id) ===
                    String(farmId)
            )
            : null;


    /*
     * =========================================================
     * Load data
     * =========================================================
     */

    useEffect(() => {
        if (!farmId) {
            return undefined;
        }

        let cancelled = false;

        async function loadData() {
            const requests = [
                {
                    name: "Soil intelligence",
                    request: () =>
                        getSoilIntelligence(farmId),
                },

                {
                    name: "Latest soil sample",
                    request: () =>
                        getLatestSoilSample(farmId),
                },

                {
                    name: "Crop rotation",
                    request: () =>
                        getLatestCropRotation(farmId),
                },

                {
                    name: "Soil test trend",
                    request: () =>
                        getSoilTestTrend(farmId),
                },

                {
                    name: "Soil recovery plans",
                    request: () =>
                        getActiveRecoveryPlans(farmId),
                },

                {
                    name: "Crops",
                    request: () =>
                        getAllCrops(),
                },
            ];


            const results =
                await Promise.all(
                    requests.map(
                        async (item) => {
                            try {
                                const value =
                                    await item.request();

                                return {
                                    name: item.name,
                                    value,
                                    error: null,
                                };
                            } catch (requestError) {
                                console.warn(
                                    `${item.name} failed:`,
                                    requestError
                                );

                                return {
                                    name: item.name,
                                    value: null,
                                    error:
                                        requestError?.message ||
                                        `Unable to load ${item.name}`,
                                };
                            }
                        }
                    )
                );


            if (cancelled) {
                return;
            }


            /*
             * -------------------------------------------------
             * Failed requests
             * -------------------------------------------------
             */

            const failed =
                results
                    .filter(
                        (item) =>
                            Boolean(item.error)
                    )
                    .map(
                        (item) =>
                            item.name
                    );


            /*
             * -------------------------------------------------
             * Store API data
             * -------------------------------------------------
             */

            setErrors(failed);

            setError("");

            setSoil(
                results[0]?.value || null
            );

            setSoilSample(
                results[1]?.value || null
            );

            setCropRotation(
                results[2]?.value || null
            );

            setSoilTrend(
                Array.isArray(
                    results[3]?.value
                )
                    ? results[3].value
                    : []
            );

            setRecoveryPlans(
                Array.isArray(
                    results[4]?.value
                )
                    ? results[4].value
                    : []
            );

            setCrops(
                Array.isArray(
                    results[5]?.value
                )
                    ? results[5].value
                    : []
            );

            /*
             * Mark this farm as loaded.
             */
            setLoadedFarmId(farmId);
        }


        loadData();


        return () => {
            cancelled = true;
        };
    }, [farmId]);


    /*
     * =========================================================
     * No farm message
     * =========================================================
     */

    const noFarmMessage = isHindi
        ? "कोई खेत उपलब्ध नहीं है। पहले खेत जोड़ें।"
        : "No farm is available. Please add a farm first.";


    /*
     * =========================================================
     * Helpers
     * =========================================================
     */

    const display = (value) => {
        if (
            value === null ||
            value === undefined ||
            value === ""
        ) {
            return "—";
        }

        return String(value);
    };


    const number = (
        value,
        digits = 1
    ) => {
        const parsed = Number(value);

        if (!Number.isFinite(parsed)) {
            return "—";
        }

        return parsed.toFixed(digits);
    };


    const readableLabel = (key) => {
        if (!key) {
            return "";
        }

        return String(key)
            .replace(
                /([a-z])([A-Z])/g,
                "$1 $2"
            )
            .replace(
                /_/g,
                " "
            )
            .replace(
                /\b\w/g,
                (letter) =>
                    letter.toUpperCase()
            );
    };


    /*
     * =========================================================
     * Soil values
     * =========================================================
     */

    const soilData =
        soil ||
        soilSample ||
        null;


    const soilValues = useMemo(() => {
        if (!soilData) {
            return [];
        }

        const hiddenKeys =
            new Set([
                "id",
                "farmId",
                "cropId",
                "createdAt",
                "updatedAt",
            ]);

        return Object.entries(
            soilData
        ).filter(
            ([key, value]) =>
                !hiddenKeys.has(key) &&
                value !== null &&
                value !== undefined &&
                value !== ""
        );
    }, [soilData]);


    /*
     * =========================================================
     * Main nutrient values
     * =========================================================
     */

    const ph =
        soil?.pH ??
        soil?.ph ??
        soilSample?.pH ??
        soilSample?.ph ??
        null;


    const nitrogen =
        soil?.nitrogen ??
        soil?.n ??
        soilSample?.nitrogen ??
        soilSample?.n ??
        null;


    const phosphorus =
        soil?.phosphorus ??
        soil?.p ??
        soilSample?.phosphorus ??
        soilSample?.p ??
        null;


    const potassium =
        soil?.potassium ??
        soil?.k ??
        soilSample?.potassium ??
        soilSample?.k ??
        null;


    const organicCarbon =
        soil?.organicCarbon ??
        soil?.organic_carbon ??
        soilSample?.organicCarbon ??
        soilSample?.organic_carbon ??
        null;


    const soilType =
        soil?.soilType ??
        soil?.soil_type ??
        soilSample?.soilType ??
        soilSample?.soil_type ??
        null;


    /*
     * =========================================================
     * Soil condition
     * =========================================================
     */

    const soilCondition = useMemo(() => {
        const value = Number(ph);

        if (!Number.isFinite(value)) {
            return {
                label:
                    isHindi
                        ? "उपलब्ध नहीं"
                        : "Not available",

                className:
                    "unknown",
            };
        }

        if (
            value < 5.5 ||
            value > 8.5
        ) {
            return {
                label:
                    isHindi
                        ? "ध्यान आवश्यक"
                        : "Needs attention",

                className:
                    "warning",
            };
        }

        return {
            label:
                isHindi
                    ? "सामान्य"
                    : "Within common range",

            className:
                "good",
        };
    }, [ph, isHindi]);


    /*
     * =========================================================
     * Latest recovery plan
     * =========================================================
     */

    const latestRecoveryPlan =
        recoveryPlans.length > 0
            ? recoveryPlans[0]
            : null;


    /*
     * =========================================================
     * Latest soil trend records
     * =========================================================
     */

    const latestTrend =
        soilTrend.length > 0
            ? [...soilTrend].slice(-6)
            : [];


    /*
     * =========================================================
     * Selected crop
     * =========================================================
     */

    const selectedCrop = useMemo(() => {
        if (!selectedCropId) {
            return null;
        }

        return crops.find(
            (crop) =>
                String(crop.id) ===
                String(selectedCropId)
        ) || null;
    }, [crops, selectedCropId]);


    /*
     * =========================================================
     * Error screen
     * =========================================================
     */

    if (!farmId) {
        return (
            <div className="crop-soil-analysis-page">

                <button
                    className="crop-soil-analysis-back"
                    onClick={() =>
                        navigate(-1)
                    }
                >
                    ←{" "}
                    {isHindi
                        ? "वापस"
                        : "Back"}
                </button>


                <div className="crop-soil-analysis-error">

                    <div className="crop-soil-analysis-error-icon">
                        ⚠️
                    </div>

                    <h2>
                        {isHindi
                            ? "खेत उपलब्ध नहीं"
                            : "Farm not available"}
                    </h2>

                    <p>
                        {noFarmMessage}
                    </p>

                    <button
                        className="crop-soil-analysis-primary-button"
                        onClick={() =>
                            navigate("/fields")
                        }
                    >
                        {isHindi
                            ? "खेत जोड़ें"
                            : "Add Farm"}
                    </button>

                </div>

                <BottomNavBar />

            </div>
        );
    }


    /*
     * =========================================================
     * Loading screen
     * =========================================================
     */

    if (loading) {
        return (
            <div className="crop-soil-analysis-page">

                <div className="crop-soil-analysis-loading">

                    <div className="crop-soil-analysis-loading-icon">
                        🧪
                    </div>

                    <h2>
                        {isHindi
                            ? "मिट्टी का विश्लेषण हो रहा है..."
                            : "Analyzing soil data..."}
                    </h2>

                    <p>
                        {isHindi
                            ? "AgriNexus आपके खेत का वास्तविक डेटा प्राप्त कर रहा है।"
                            : "AgriNexus is loading the real data for your farm."}
                    </p>

                </div>

                <BottomNavBar />

            </div>
        );
    }


    /*
     * =========================================================
     * Main page
     * =========================================================
     */

    return (
        <div className="crop-soil-analysis-page">

            {/* =================================================
                Header
            ================================================= */}

            <button
                className="crop-soil-analysis-back"
                onClick={() =>
                    navigate(-1)
                }
            >
                ←{" "}
                {isHindi
                    ? "वापस"
                    : "Back"}
            </button>


            <header className="crop-soil-analysis-header">

                <div className="crop-soil-analysis-header-icon">
                    🧪
                </div>

                <div>

                    <span className="crop-soil-analysis-eyebrow">
                        {isHindi
                            ? "स्मार्ट कृषि विश्लेषण"
                            : "SMART AGRICULTURE ANALYSIS"}
                    </span>

                    <h1>
                        {isHindi
                            ? "मिट्टी और फसल विश्लेषण"
                            : "Crop & Soil Analysis"}
                    </h1>

                    <p>
                        {isHindi
                            ? "आपके खेत के वास्तविक मिट्टी डेटा के आधार पर जानकारी।"
                            : "Insights based on the real soil data available for your farm."}
                    </p>

                </div>

            </header>


            {/* =================================================
                Farm information
            ================================================= */}

            {currentField && (
                <section className="crop-soil-analysis-section">

                    <div className="crop-soil-analysis-section-header">

                        <h2>
                            📍{" "}
                            {isHindi
                                ? "चयनित खेत"
                                : "Selected Farm"}
                        </h2>

                    </div>


                    <div className="crop-soil-analysis-farm-card">

                        <div className="crop-soil-analysis-farm-icon">
                            🌾
                        </div>

                        <div>

                            <strong>
                                {display(
                                    currentField.name
                                )}
                            </strong>

                            <span>
                                {display(
                                    currentField.location
                                )}
                            </span>

                        </div>

                    </div>

                </section>
            )}


            {/* =================================================
                API warning
            ================================================= */}

            {errors.length > 0 && (
                <div className="crop-soil-analysis-warning">

                    ⚠️{" "}

                    {isHindi
                        ? `${errors.length} डेटा स्रोत उपलब्ध नहीं हैं।`
                        : `${errors.length} data source(s) are currently unavailable.`}

                </div>
            )}


            {/* =================================================
                Soil summary
            ================================================= */}

            <section className="crop-soil-analysis-section">

                <div className="crop-soil-analysis-section-header">

                    <h2>
                        🧪{" "}
                        {isHindi
                            ? "मिट्टी का सारांश"
                            : "Soil Summary"}
                    </h2>

                    <span
                        className={`crop-soil-analysis-condition ${soilCondition.className}`}
                    >
                        {soilCondition.label}
                    </span>

                </div>


                {soilData ? (

                    <div className="crop-soil-analysis-grid">

                        <div className="crop-soil-analysis-card">

                            <span>
                                ⚗️
                            </span>

                            <small>
                                pH
                            </small>

                            <strong>
                                {number(
                                    ph,
                                    2
                                )}
                            </strong>

                        </div>


                        <div className="crop-soil-analysis-card">

                            <span>
                                🌍
                            </span>

                            <small>
                                {isHindi
                                    ? "मिट्टी का प्रकार"
                                    : "Soil Type"}
                            </small>

                            <strong>
                                {display(
                                    soilType
                                )}
                            </strong>

                        </div>


                        <div className="crop-soil-analysis-card">

                            <span>
                                🌿
                            </span>

                            <small>
                                {isHindi
                                    ? "नाइट्रोजन"
                                    : "Nitrogen"}
                            </small>

                            <strong>
                                {display(
                                    nitrogen
                                )}
                            </strong>

                        </div>


                        <div className="crop-soil-analysis-card">

                            <span>
                                🧬
                            </span>

                            <small>
                                {isHindi
                                    ? "फॉस्फोरस"
                                    : "Phosphorus"}
                            </small>

                            <strong>
                                {display(
                                    phosphorus
                                )}
                            </strong>

                        </div>


                        <div className="crop-soil-analysis-card">

                            <span>
                                ⚡
                            </span>

                            <small>
                                {isHindi
                                    ? "पोटैशियम"
                                    : "Potassium"}
                            </small>

                            <strong>
                                {display(
                                    potassium
                                )}
                            </strong>

                        </div>


                        <div className="crop-soil-analysis-card">

                            <span>
                                🍂
                            </span>

                            <small>
                                {isHindi
                                    ? "जैविक कार्बन"
                                    : "Organic Carbon"}
                            </small>

                            <strong>
                                {display(
                                    organicCarbon
                                )}
                            </strong>

                        </div>

                    </div>

                ) : (

                    <div className="crop-soil-analysis-empty">

                        {isHindi
                            ? "मिट्टी का डेटा उपलब्ध नहीं है।"
                            : "Soil data is not available for this farm."}

                    </div>

                )}

            </section>


            {/* =================================================
                Full soil intelligence
            ================================================= */}

            {soilData &&
                soilValues.length > 0 && (

                    <section className="crop-soil-analysis-section">

                        <h2>
                            📊{" "}
                            {isHindi
                                ? "मिट्टी का विस्तृत डेटा"
                                : "Detailed Soil Data"}
                        </h2>


                        <div className="crop-soil-analysis-detail-card">

                            {soilValues.map(
                                ([key, value]) => (

                                    <div
                                        className="crop-soil-analysis-detail-row"
                                        key={key}
                                    >

                                        <span>
                                            {readableLabel(
                                                key
                                            )}
                                        </span>

                                        <strong>
                                            {typeof value ===
                                            "object"
                                                ? JSON.stringify(
                                                    value
                                                )
                                                : String(
                                                    value
                                                )}
                                        </strong>

                                    </div>

                                )
                            )}

                        </div>

                    </section>
                )}


            {/* =================================================
                Latest soil sample
            ================================================= */}

            {soilSample && (

                <section className="crop-soil-analysis-section">

                    <h2>
                        🔬{" "}
                        {isHindi
                            ? "नवीनतम मिट्टी नमूना"
                            : "Latest Soil Sample"}
                    </h2>


                    <div className="crop-soil-analysis-detail-card">

                        {Object.entries(
                            soilSample
                        )
                            .filter(
                                ([key, value]) =>
                                    value !== null &&
                                    value !== undefined &&
                                    value !== "" &&
                                    key !== "id"
                            )
                            .map(
                                ([key, value]) => (

                                    <div
                                        className="crop-soil-analysis-detail-row"
                                        key={key}
                                    >

                                        <span>
                                            {readableLabel(
                                                key
                                            )}
                                        </span>

                                        <strong>
                                            {typeof value ===
                                            "object"
                                                ? JSON.stringify(
                                                    value
                                                )
                                                : String(
                                                    value
                                                )}
                                        </strong>

                                    </div>

                                )
                            )}

                    </div>

                </section>

            )}


            {/* =================================================
                Crop rotation
            ================================================= */}

            {cropRotation && (

                <section className="crop-soil-analysis-section">

                    <h2>
                        🌱{" "}
                        {isHindi
                            ? "अगली फसल की सलाह"
                            : "Next Crop Recommendation"}
                    </h2>


                    <div className="crop-soil-analysis-recommendation-card">

                        <div className="crop-soil-analysis-recommendation-icon">
                            🌾
                        </div>

                        <div>

                            <small>
                                {isHindi
                                    ? "सुझाई गई अगली फसल"
                                    : "Recommended Next Crop"}
                            </small>

                            <strong>
                                {display(
                                    cropRotation.recommendedCropName
                                )}
                            </strong>

                            {cropRotation.recommendationScore !==
                                null &&
                                cropRotation.recommendationScore !==
                                undefined && (

                                    <span>
                                        {isHindi
                                            ? "स्कोर"
                                            : "Score"}{" "}
                                        {display(
                                            cropRotation.recommendationScore
                                        )}
                                    </span>

                                )}

                        </div>

                    </div>


                    {cropRotation.reason && (

                        <div className="crop-soil-analysis-advice">

                            <strong>
                                💡{" "}
                                {isHindi
                                    ? "कारण"
                                    : "Reason"}
                            </strong>

                            <p>
                                {cropRotation.reason}
                            </p>

                        </div>

                    )}

                </section>

            )}


            {/* =================================================
                Available crops
            ================================================= */}

            {crops.length > 0 && (

                <section className="crop-soil-analysis-section">

                    <div className="crop-soil-analysis-section-header">

                        <h2>
                            🌾{" "}
                            {isHindi
                                ? "फसल विकल्प"
                                : "Crop Options"}
                        </h2>

                    </div>


                    <div className="crop-soil-analysis-crop-selector">

                        <label>
                            {isHindi
                                ? "फसल चुनें"
                                : "Select a crop"}
                        </label>

                        <select
                            value={
                                selectedCropId
                            }
                            onChange={(event) =>
                                setSelectedCropId(
                                    event.target.value
                                )
                            }
                        >

                            <option value="">
                                {isHindi
                                    ? "फसल चुनें..."
                                    : "Choose a crop..."}
                            </option>

                            {crops.map(
                                (crop, index) => {

                                    const cropId =
                                        crop?.id ??
                                        `crop-${index}`;

                                    const cropName =
                                        crop?.name ||
                                        crop?.cropName ||
                                        `Crop ${cropId}`;

                                    return (
                                        <option
                                            key={cropId}
                                            value={
                                                crop?.id || ""
                                            }
                                        >
                                            {cropName}
                                        </option>
                                    );
                                }
                            )}

                        </select>

                    </div>


                    {selectedCropId && (

                        <div className="crop-soil-analysis-selected-crop">

                            <strong>
                                {isHindi
                                    ? "चयनित फसल"
                                    : "Selected Crop"}
                            </strong>

                            <span>
                                {selectedCrop?.name ||
                                    selectedCrop?.cropName ||
                                    "—"}
                            </span>

                        </div>

                    )}

                </section>

            )}


            {/* =================================================
                Soil trend
            ================================================= */}

            {latestTrend.length > 0 && (

                <section className="crop-soil-analysis-section">

                    <h2>
                        📈{" "}
                        {isHindi
                            ? "मिट्टी परीक्षण ट्रेंड"
                            : "Soil Test Trend"}
                    </h2>


                    <div className="crop-soil-analysis-trend-list">

                        {latestTrend.map(
                            (item, index) => (

                                <div
                                    className="crop-soil-analysis-trend-card"
                                    key={
                                        item?.id ||
                                        `trend-${index}`
                                    }
                                >

                                    {Object.entries(
                                        item || {}
                                    )
                                        .filter(
                                            ([key, value]) =>
                                                key !== "id" &&
                                                value !== null &&
                                                value !== undefined &&
                                                value !== ""
                                        )
                                        .map(
                                            ([key, value]) => (

                                                <div
                                                    key={key}
                                                    className="crop-soil-analysis-trend-row"
                                                >

                                                    <span>
                                                        {readableLabel(
                                                            key
                                                        )}
                                                    </span>

                                                    <strong>
                                                        {typeof value ===
                                                        "object"
                                                            ? JSON.stringify(
                                                                value
                                                            )
                                                            : String(
                                                                value
                                                            )}
                                                    </strong>

                                                </div>

                                            )
                                        )}

                                </div>

                            )
                        )}

                    </div>

                </section>

            )}


            {/* =================================================
                Soil recovery
            ================================================= */}

            {latestRecoveryPlan && (

                <section className="crop-soil-analysis-section">

                    <h2>
                        ♻️{" "}
                        {isHindi
                            ? "मिट्टी सुधार योजना"
                            : "Soil Recovery Plan"}
                    </h2>


                    <div className="crop-soil-analysis-recovery-card">

                        {Object.entries(
                            latestRecoveryPlan
                        )
                            .filter(
                                ([, value]) =>
                                    value !== null &&
                                    value !== undefined &&
                                    value !== ""
                            )
                            .map(
                                ([key, value]) => (

                                    <div
                                        className="crop-soil-analysis-detail-row"
                                        key={key}
                                    >

                                        <span>
                                            {readableLabel(
                                                key
                                            )}
                                        </span>

                                        <strong>
                                            {typeof value ===
                                            "object"
                                                ? JSON.stringify(
                                                    value
                                                )
                                                : String(
                                                    value
                                                )}
                                        </strong>

                                    </div>

                                )
                            )}

                    </div>

                </section>

            )}


            {/* =================================================
                Navigation actions
            ================================================= */}

            <section className="crop-soil-analysis-actions">

                <button
                    className="crop-soil-analysis-primary-button"
                    onClick={() =>
                        navigate("/fertilizer")
                    }
                >
                    🌿{" "}
                    {isHindi
                        ? "उर्वरक जांचें"
                        : "Check Fertilizer"}
                </button>


                <button
                    className="crop-soil-analysis-secondary-button"
                    onClick={() =>
                        navigate("/irrigation")
                    }
                >
                    💧{" "}
                    {isHindi
                        ? "सिंचाई जांचें"
                        : "Check Irrigation"}
                </button>


                <button
                    className="crop-soil-analysis-secondary-button"
                    onClick={() =>
                        navigate("/history")
                    }
                >
                    📜{" "}
                    {isHindi
                        ? "फसल इतिहास"
                        : "Crop History"}
                </button>

            </section>


            <BottomNavBar />

        </div>
    );
}


export default CropSoilAnalysis;