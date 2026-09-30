import {
    useEffect,
    useState,
} from "react";

import {
    useNavigate,
} from "react-router-dom";

import {
    useLanguage,
} from "../../context/LanguageContext";

import {
    apiRequest,
    getCurrentFarmId,
} from "../../api/apiClient";

import {
    useFields,
} from "../../context/FieldsContext";

import BottomNavBar from "../../components/BottomNavBar";

import "./FieldHealthPassport.css";


function FieldHealthPassport() {

    const {
        language,
    } = useLanguage();

    const navigate =
        useNavigate();

    const {
        currentField,
        fields = [],
        loading: fieldsLoading,
    } = useFields();

    const isHindi =
        language === "hi";


    const [
        passport,
        setPassport,
    ] = useState(null);


    const [
        loading,
        setLoading,
    ] = useState(true);


    const [
        error,
        setError,
    ] = useState("");


    /*
     * =========================================================
     * LOAD REAL FIELD HEALTH PASSPORT
     * =========================================================
     *
     * Priority:
     *
     * 1. Current field from FieldsContext
     * 2. Current farm ID stored by apiClient
     *
     * Never creates a demo/default farm.
     */

    useEffect(() => {

        let cancelled = false;


        const loadPassport = async () => {

            try {

                setLoading(true);
                setError("");


                const farmId =
                    currentField?.id ??
                    getCurrentFarmId();


                if (!farmId) {

                    throw new Error(
                        isHindi
                            ? "कृपया पहले एक वास्तविक खेत चुनें।"
                            : "Please select a real farm first."
                    );
                }


                /*
                 * Real backend request only.
                 */

                const response =
                    await apiRequest(
                        `/api/field-health-passport/farm/${farmId}`
                    );


                if (cancelled) {
                    return;
                }


                if (
                    response &&
                    typeof response === "object"
                ) {

                    setPassport(
                        response
                    );

                } else {

                    setPassport(
                        null
                    );

                    setError(
                        isHindi
                            ? "पासपोर्ट डेटा backend से उपलब्ध नहीं है।"
                            : "Passport data is not available from the backend."
                    );
                }

            } catch (requestError) {

                if (cancelled) {
                    return;
                }


                console.error(
                    "Failed to load Field Health Passport:",
                    requestError
                );


                setPassport(null);


                setError(
                    requestError?.message ||
                    (
                        isHindi
                            ? "फील्ड हेल्थ पासपोर्ट लोड नहीं हो सका।"
                            : "Unable to load Field Health Passport."
                    )
                );

            } finally {

                if (!cancelled) {

                    setLoading(false);
                }
            }
        };


        /*
         * Wait until farm context has finished loading.
         */

        if (!fieldsLoading) {

            loadPassport();
        }


        return () => {

            cancelled = true;
        };

    }, [
        currentField?.id,
        fieldsLoading,
        isHindi,
    ]);


    /*
     * =========================================================
     * LOADING
     * =========================================================
     */

    if (
        fieldsLoading ||
        loading
    ) {

        return (

            <div className="passport-page">

                <div className="passport-loading">

                    <div className="passport-loading-icon">
                        🌱
                    </div>


                    <h2>

                        {
                            isHindi
                                ? "फील्ड हेल्थ पासपोर्ट लोड हो रहा है..."
                                : "Loading Field Health Passport..."
                        }

                    </h2>


                    <p>

                        {
                            isHindi
                                ? "वास्तविक backend रिकॉर्ड से जानकारी ली जा रही है।"
                                : "Loading information from the real backend record."
                        }

                    </p>

                </div>


                <BottomNavBar />

            </div>
        );
    }


    /*
     * =========================================================
     * ERROR / NO DATA
     * =========================================================
     */

    if (
        error ||
        !passport
    ) {

        return (

            <div className="passport-page">

                <div className="passport-header">

                    <button
                        type="button"
                        className="passport-back-button"
                        onClick={() =>
                            navigate("/home")
                        }
                    >
                        ←
                    </button>


                    <div>

                        <h1>

                            {
                                isHindi
                                    ? "फील्ड हेल्थ पासपोर्ट"
                                    : "Field Health Passport"
                            }

                        </h1>


                        <p>

                            {
                                isHindi
                                    ? "खेत का दीर्घकालिक स्वास्थ्य रिकॉर्ड"
                                    : "Long-term field health record"
                            }

                        </p>

                    </div>

                </div>


                <div className="passport-error-card">

                    <div className="passport-error-icon">
                        ⚠️
                    </div>


                    <h2>

                        {
                            isHindi
                                ? "डेटा उपलब्ध नहीं"
                                : "Data unavailable"
                        }

                    </h2>


                    <p>

                        {
                            error ||
                            (
                                isHindi
                                    ? "वास्तविक पासपोर्ट डेटा backend से प्राप्त नहीं हुआ।"
                                    : "Real passport data was not received from the backend."
                            )
                        }

                    </p>


                    <button
                        type="button"
                        className="passport-primary-button"
                        onClick={() =>
                            navigate("/home")
                        }
                    >

                        {
                            isHindi
                                ? "होम पर जाएं"
                                : "Go to Home"
                        }

                    </button>

                </div>


                <BottomNavBar />

            </div>
        );
    }


    /*
     * =========================================================
     * PASSPORT DATA
     * =========================================================
     */

    const healthScore =
        passport.healthScore ?? null;


    const healthStatus =
        passport.overallHealthStatus ??
        "UNKNOWN";


    const cropName =
        passport.currentCropName ??
        "—";


    const cropSeason =
        passport.currentSeason ??
        "—";


    const cropStage =
        passport.currentCropStage ??
        "—";


    const soilPh =
        passport.soilPh ?? null;


    const organicCarbon =
        passport.organicCarbon ?? null;


    const nitrogen =
        passport.soilNitrogen ?? null;


    const phosphorus =
        passport.soilPhosphorus ?? null;


    const potassium =
        passport.soilPotassium ?? null;


    const electricalConductivity =
        passport.electricalConductivity ??
        null;


    const currentVwc15 =
        passport.currentVwc15cm ??
        null;


    const currentVwc30 =
        passport.currentVwc30cm ??
        null;


    const currentVwc60 =
        passport.currentVwc60cm ??
        null;


    const temperature =
        passport.latestTemperature ??
        null;


    const humidity =
        passport.latestHumidity ??
        null;


    const rainfall =
        passport.latestRainfall ??
        null;


    const windSpeed =
        passport.latestWindSpeed ??
        null;


    const highestWeatherRisk =
        passport.highestWeatherRisk ??
        "NONE";


    const highestWeatherRiskScore =
        passport.highestWeatherRiskScore ??
        null;


    const activeWeatherRiskCount =
        passport.activeWeatherRiskCount ??
        0;


    const recoveryPlans =
        Array.isArray(
            passport.recoveryPlans
        )
            ? passport.recoveryPlans
            : [];


    const cropHistory =
        Array.isArray(
            passport.cropHistory
        )
            ? passport.cropHistory
            : [];


    /*
     * =========================================================
     * SATELLITE DATA
     * =========================================================
     *
     * Display only what backend actually supplies.
     */

    const ndvi =
        passport.ndvi ??
        passport.latestNdvi ??
        passport.satelliteNdvi ??
        null;


    const vegetationHealth =
        passport.vegetationHealth ??
        passport.latestVegetationHealth ??
        passport.satelliteVegetationHealth ??
        null;


    const satelliteSoilMoisture =
        passport.satelliteSoilMoisture ??
        passport.latestSatelliteSoilMoisture ??
        null;


    const cloudCoverage =
        passport.cloudCoverage ??
        passport.latestCloudCoverage ??
        null;


    const satelliteHealthStatus =
        passport.satelliteHealthStatus ??
        passport.fieldHealthStatus ??
        null;


    const satelliteRecommendation =
        passport.satelliteRecommendation ??
        null;


    const satelliteRecordedAt =
        passport.satelliteRecordedAt ??
        passport.latestSatelliteRecordedAt ??
        null;


    /*
     * =========================================================
     * CSS STATUS CLASSES
     * =========================================================
     */

    const statusClass =
        healthStatus === "HEALTHY"
            ? "healthy"
            : healthStatus === "CRITICAL"
                ? "critical"
                : "warning";


    const riskClass =
        highestWeatherRisk === "HEAT_STRESS" ||
        highestWeatherRisk === "DROUGHT" ||
        highestWeatherRisk === "HIGH_WIND"
            ? "critical"
            : "warning";


    /*
     * =========================================================
     * FORMAT HELPERS
     * =========================================================
     */

    const formatNumber = (
        value,
        digits = 2
    ) => {

        if (
            value === null ||
            value === undefined ||
            value === "" ||
            Number.isNaN(
                Number(value)
            )
        ) {

            return "—";
        }


        return Number(
            value
        ).toFixed(
            digits
        );
    };


    const formatDateTime = (
        value
    ) => {

        if (!value) {

            return "—";
        }


        const date =
            new Date(value);


        if (
            Number.isNaN(
                date.getTime()
            )
        ) {

            return String(
                value
            );
        }


        return date.toLocaleString(
            isHindi
                ? "hi-IN"
                : "en-IN"
        );
    };


    const formatDate = (
        value
    ) => {

        if (!value) {

            return "—";
        }


        const date =
            new Date(value);


        if (
            Number.isNaN(
                date.getTime()
            )
        ) {

            return String(
                value
            );
        }


        return date.toLocaleDateString(
            isHindi
                ? "hi-IN"
                : "en-IN",
            {
                day:
                    "numeric",
                month:
                    "short",
                year:
                    "numeric",
            }
        );
    };


    const displayValue = (
        value
    ) => {

        return (
            value === null ||
            value === undefined ||
            value === ""
        )
            ? "—"
            : value;
    };


    /*
     * =========================================================
     * MAIN UI
     * =========================================================
     */

    return (

        <div className="passport-page">


            {/* =================================================
                HEADER
            ================================================= */}

            <div className="passport-header">

                <button
                    type="button"
                    className="passport-back-button"
                    onClick={() =>
                        navigate("/home")
                    }
                >
                    ←
                </button>


                <div className="passport-header-content">

                    <h1>

                        {
                            isHindi
                                ? "फील्ड हेल्थ पासपोर्ट"
                                : "Field Health Passport"
                        }

                    </h1>


                    <p>

                        {
                            passport.farmName ??
                            "—"
                        }

                    </p>


                    <span>

                        {
                            passport.location ??
                            "—"
                        }

                    </span>

                </div>

            </div>


            {/* =================================================
                HEALTH HERO
            ================================================= */}

            <section
                className={`passport-health-card ${statusClass}`}
            >

                <div className="passport-health-left">

                    <div className="passport-health-icon">

                        {
                            healthStatus === "HEALTHY"
                                ? "✅"
                                : healthStatus === "CRITICAL"
                                    ? "🚨"
                                    : "⚠️"
                        }

                    </div>


                    <div>

                        <div className="passport-small-label">

                            {
                                isHindi
                                    ? "फील्ड स्थिति"
                                    : "Field Status"
                            }

                        </div>


                        <h2>

                            {
                                displayValue(
                                    healthStatus
                                )
                            }

                        </h2>

                    </div>

                </div>


                <div className="passport-score">

                    <div className="passport-score-number">

                        {
                            healthScore !== null
                                ? Number(
                                    healthScore
                                ).toFixed(2)
                                : "—"
                        }

                    </div>


                    <div className="passport-score-label">

                        {
                            isHindi
                                ? "हेल्थ स्कोर"
                                : "Health Score"
                        }

                    </div>

                </div>

            </section>


            {/* =================================================
                HEALTH SUMMARY
            ================================================= */}

            {
                passport.healthSummary && (

                    <section className="passport-section">

                        <h2 className="passport-section-title">

                            {
                                isHindi
                                    ? "स्वास्थ्य सारांश"
                                    : "Health Summary"
                            }

                        </h2>


                        <div className="passport-summary-card">

                            {
                                passport.healthSummary
                            }

                        </div>

                    </section>

                )
            }


            {/* =================================================
                SATELLITE FIELD MONITORING
            ================================================= */}

            <section className="passport-section">

                <h2 className="passport-section-title">

                    {
                        isHindi
                            ? "🛰️ सैटेलाइट फील्ड मॉनिटरिंग"
                            : "🛰️ Satellite Field Monitoring"
                    }

                </h2>


                <div className="passport-grid">


                    <div className="passport-metric-card">

                        <span>
                            NDVI
                        </span>


                        <strong>

                            {
                                ndvi !== null
                                    ? formatNumber(
                                        ndvi,
                                        3
                                    )
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>

                            {
                                isHindi
                                    ? "वनस्पति स्वास्थ्य"
                                    : "Vegetation Health"
                            }

                        </span>


                        <strong>

                            {
                                vegetationHealth !== null
                                    ? `${formatNumber(
                                        vegetationHealth,
                                        1
                                    )}%`
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>

                            {
                                isHindi
                                    ? "सैटेलाइट मिट्टी नमी"
                                    : "Satellite Soil Moisture"
                            }

                        </span>


                        <strong>

                            {
                                satelliteSoilMoisture !== null
                                    ? `${formatNumber(
                                        satelliteSoilMoisture,
                                        1
                                    )}%`
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>

                            {
                                isHindi
                                    ? "बादल कवरेज"
                                    : "Cloud Coverage"
                            }

                        </span>


                        <strong>

                            {
                                cloudCoverage !== null
                                    ? `${formatNumber(
                                        cloudCoverage,
                                        1
                                    )}%`
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>

                            {
                                isHindi
                                    ? "सैटेलाइट फील्ड स्थिति"
                                    : "Satellite Field Status"
                            }

                        </span>


                        <strong>

                            {
                                displayValue(
                                    satelliteHealthStatus
                                )
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>

                            {
                                isHindi
                                    ? "रिकॉर्ड समय"
                                    : "Recorded At"
                            }

                        </span>


                        <strong>

                            {
                                formatDateTime(
                                    satelliteRecordedAt
                                )
                            }

                        </strong>

                    </div>

                </div>


                {
                    satelliteRecommendation && (

                        <div className="passport-summary-card">

                            <strong>

                                {
                                    isHindi
                                        ? "सैटेलाइट सलाह"
                                        : "Satellite Recommendation"
                                }

                            </strong>


                            <p>

                                {
                                    satelliteRecommendation
                                }

                            </p>

                        </div>

                    )
                }

            </section>


            {/* =================================================
                CURRENT CROP
            ================================================= */}

            <section className="passport-section">

                <h2 className="passport-section-title">

                    {
                        isHindi
                            ? "वर्तमान फसल"
                            : "Current Crop"
                    }

                </h2>


                <div className="passport-grid">


                    <div className="passport-info-card">

                        <span className="passport-info-icon">
                            🌾
                        </span>


                        <div>

                            <span className="passport-info-label">

                                {
                                    isHindi
                                        ? "फसल"
                                        : "Crop"
                                }

                            </span>


                            <strong>

                                {
                                    cropName
                                }

                            </strong>

                        </div>

                    </div>


                    <div className="passport-info-card">

                        <span className="passport-info-icon">
                            🗓️
                        </span>


                        <div>

                            <span className="passport-info-label">

                                {
                                    isHindi
                                        ? "सीजन"
                                        : "Season"
                                }

                            </span>


                            <strong>

                                {
                                    cropSeason
                                }

                            </strong>

                        </div>

                    </div>


                    <div className="passport-info-card">

                        <span className="passport-info-icon">
                            🌱
                        </span>


                        <div>

                            <span className="passport-info-label">

                                {
                                    isHindi
                                        ? "फसल चरण"
                                        : "Crop Stage"
                                }

                            </span>


                            <strong>

                                {
                                    cropStage
                                }

                            </strong>

                        </div>

                    </div>


                    <div className="passport-info-card">

                        <span className="passport-info-icon">
                            🧪
                        </span>


                        <div>

                            <span className="passport-info-label">

                                {
                                    isHindi
                                        ? "मिट्टी प्रकार"
                                        : "Soil Type"
                                }

                            </span>


                            <strong>

                                {
                                    displayValue(
                                        passport.soilType
                                    )
                                }

                            </strong>

                        </div>

                    </div>

                </div>

            </section>


            {/* =================================================
                SOIL HEALTH
            ================================================= */}

            <section className="passport-section">

                <h2 className="passport-section-title">

                    {
                        isHindi
                            ? "मिट्टी स्वास्थ्य"
                            : "Soil Health"
                    }

                </h2>


                <div className="passport-grid">


                    <div className="passport-metric-card">

                        <span>
                            pH
                        </span>


                        <strong>

                            {
                                soilPh !== null
                                    ? soilPh
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>

                            {
                                isHindi
                                    ? "ऑर्गेनिक कार्बन"
                                    : "Organic Carbon"
                            }

                        </span>


                        <strong>

                            {
                                organicCarbon !== null
                                    ? `${organicCarbon}%`
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>

                            {
                                isHindi
                                    ? "नाइट्रोजन"
                                    : "Nitrogen"
                            }

                        </span>


                        <strong>

                            {
                                nitrogen !== null
                                    ? nitrogen
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>

                            {
                                isHindi
                                    ? "फॉस्फोरस"
                                    : "Phosphorus"
                            }

                        </span>


                        <strong>

                            {
                                phosphorus !== null
                                    ? phosphorus
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>

                            {
                                isHindi
                                    ? "पोटैशियम"
                                    : "Potassium"
                            }

                        </span>


                        <strong>

                            {
                                potassium !== null
                                    ? potassium
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>
                            EC
                        </span>


                        <strong>

                            {
                                electricalConductivity !== null
                                    ? electricalConductivity
                                    : "—"
                            }

                        </strong>

                    </div>

                </div>

            </section>


            {/* =================================================
                SOIL MOISTURE
            ================================================= */}

            <section className="passport-section">

                <h2 className="passport-section-title">

                    {
                        isHindi
                            ? "मिट्टी की नमी"
                            : "Soil Moisture"
                    }

                </h2>


                <div className="passport-grid">


                    <div className="passport-moisture-card">

                        <div>
                            15 cm
                        </div>


                        <strong>

                            {
                                currentVwc15 !== null
                                    ? `${currentVwc15}%`
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-moisture-card highlight">

                        <div>
                            30 cm
                        </div>


                        <strong>

                            {
                                currentVwc30 !== null
                                    ? `${currentVwc30}%`
                                    : "—"
                            }

                        </strong>


                        <small>

                            {
                                isHindi
                                    ? "मुख्य रीडिंग"
                                    : "Primary reading"
                            }

                        </small>

                    </div>


                    <div className="passport-moisture-card">

                        <div>
                            60 cm
                        </div>


                        <strong>

                            {
                                currentVwc60 !== null
                                    ? `${currentVwc60}%`
                                    : "—"
                            }

                        </strong>

                    </div>

                </div>


                <div className="passport-extra-row">

                    <span>

                        💧{" "}

                        {
                            isHindi
                                ? "सिंचाई स्रोत"
                                : "Irrigation Source"
                        }

                    </span>


                    <strong>

                        {
                            displayValue(
                                passport.irrigationSource
                            )
                        }

                    </strong>

                </div>


                <div className="passport-extra-row">

                    <span>

                        ⚙️{" "}

                        {
                            isHindi
                                ? "पंप स्थिति"
                                : "Pump State"
                        }

                    </span>


                    <strong>

                        {
                            displayValue(
                                passport.pumpState
                            )
                        }

                    </strong>

                </div>

            </section>


            {/* =================================================
                WEATHER
            ================================================= */}

            <section className="passport-section">

                <h2 className="passport-section-title">

                    {
                        isHindi
                            ? "मौसम"
                            : "Weather"
                    }

                </h2>


                <div className="passport-grid">


                    <div className="passport-metric-card">

                        <span>
                            🌡️ Temperature
                        </span>


                        <strong>

                            {
                                temperature !== null
                                    ? `${temperature}°C`
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>
                            💧 Humidity
                        </span>


                        <strong>

                            {
                                humidity !== null
                                    ? `${humidity}%`
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>
                            🌧️ Rainfall
                        </span>


                        <strong>

                            {
                                rainfall !== null
                                    ? `${rainfall} mm`
                                    : "—"
                            }

                        </strong>

                    </div>


                    <div className="passport-metric-card">

                        <span>
                            💨 Wind
                        </span>


                        <strong>

                            {
                                windSpeed !== null
                                    ? `${windSpeed}`
                                    : "—"
                            }

                        </strong>

                    </div>

                </div>

            </section>


            {/* =================================================
                WEATHER RISK
            ================================================= */}

            <section className="passport-section">

                <h2 className="passport-section-title">

                    {
                        isHindi
                            ? "मौसम जोखिम"
                            : "Weather Risk"
                    }

                </h2>


                <div
                    className={`passport-risk-card ${riskClass}`}
                >

                    <div className="passport-risk-icon">

                        {
                            highestWeatherRisk ===
                            "HEAT_STRESS"
                                ? "🔥"
                                : highestWeatherRisk ===
                                "DROUGHT"
                                    ? "☀️"
                                    : highestWeatherRisk ===
                                    "HIGH_WIND"
                                        ? "💨"
                                        : highestWeatherRisk ===
                                        "NONE"
                                            ? "✅"
                                            : "⚠️"
                        }

                    </div>


                    <div className="passport-risk-content">

                        <span>

                            {
                                isHindi
                                    ? "मुख्य जोखिम"
                                    : "Highest Risk"
                            }

                        </span>


                        <h3>

                            {
                                displayValue(
                                    highestWeatherRisk
                                )
                            }

                        </h3>


                        {
                            highestWeatherRiskScore !==
                            null && (

                                <strong>

                                    {
                                        highestWeatherRiskScore
                                    }

                                </strong>

                            )
                        }

                    </div>

                </div>


                <div className="passport-extra-row">

                    <span>

                        ⚠️{" "}

                        {
                            isHindi
                                ? "सक्रिय मौसम जोखिम"
                                : "Active Weather Risks"
                        }

                    </span>


                    <strong>

                        {
                            activeWeatherRiskCount
                        }

                    </strong>

                </div>

            </section>


            {/* =================================================
                NUTRIENT BALANCE
            ================================================= */}

            <section className="passport-section">

                <h2 className="passport-section-title">

                    {
                        isHindi
                            ? "पोषक तत्व संतुलन"
                            : "Nutrient Balance"
                    }

                </h2>


                <div className="passport-grid">


                    <div className="passport-metric-card">

                        <span>
                            N
                        </span>


                        <strong>

                            {
                                displayValue(
                                    passport.nitrogenBalance
                                )
                            }

                        </strong>


                        <small>

                            {
                                passport.nitrogenStatus ||
                                ""
                            }

                        </small>

                    </div>


                    <div className="passport-metric-card">

                        <span>
                            P
                        </span>


                        <strong>

                            {
                                displayValue(
                                    passport.phosphorusBalance
                                )
                            }

                        </strong>


                        <small>

                            {
                                passport.phosphorusStatus ||
                                ""
                            }

                        </small>

                    </div>


                    <div className="passport-metric-card">

                        <span>
                            K
                        </span>


                        <strong>

                            {
                                displayValue(
                                    passport.potassiumBalance
                                )
                            }

                        </strong>


                        <small>

                            {
                                passport.potassiumStatus ||
                                ""
                            }

                        </small>

                    </div>


                    <div className="passport-metric-card">

                        <span>
                            S
                        </span>


                        <strong>

                            {
                                displayValue(
                                    passport.sulphurBalance
                                )
                            }

                        </strong>


                        <small>

                            {
                                passport.sulphurStatus ||
                                ""
                            }

                        </small>

                    </div>


                    <div className="passport-metric-card">

                        <span>
                            Zn
                        </span>


                        <strong>

                            {
                                displayValue(
                                    passport.zincBalance
                                )
                            }

                        </strong>


                        <small>

                            {
                                passport.zincStatus ||
                                ""
                            }

                        </small>

                    </div>

                </div>

            </section>


            {/* =================================================
                CROP HISTORY SUMMARY
            ================================================= */}

            <section className="passport-section">

                <h2 className="passport-section-title">

                    {
                        isHindi
                            ? "फसल इतिहास"
                            : "Crop History"
                    }

                </h2>


                {
                    cropHistory.length === 0 ? (

                        <div className="passport-empty-card">

                            {
                                isHindi
                                    ? "कोई फसल इतिहास उपलब्ध नहीं है।"
                                    : "No crop history available."
                            }

                        </div>

                    ) : (

                        <div className="passport-timeline">

                            {
                                cropHistory.map(
                                    (
                                        crop,
                                        index
                                    ) => (

                                        <button
                                            key={
                                                crop?.id ||
                                                `crop-history-${index}`
                                            }
                                            type="button"
                                            className="passport-timeline-item"
                                            onClick={() =>
                                                crop?.id &&
                                                navigate(
                                                    `/crop-history/${crop.id}`,
                                                    {
                                                        state: {
                                                            cropHistory:
                                                            crop,
                                                        },
                                                    }
                                                )
                                            }
                                        >

                                            <div className="passport-timeline-dot">

                                                🌾

                                            </div>


                                            <div className="passport-timeline-content">

                                                <h3>

                                                    {
                                                        displayValue(
                                                            crop?.cropName
                                                        )
                                                    }

                                                </h3>


                                                <p>

                                                    {
                                                        displayValue(
                                                            crop?.season
                                                        )
                                                    }

                                                </p>


                                                <span>

                                                    {
                                                        formatDate(
                                                            crop?.sowingDate
                                                        )
                                                    }

                                                    {" → "}

                                                    {
                                                        formatDate(
                                                            crop?.harvestDate
                                                        )
                                                    }

                                                </span>


                                                {
                                                    crop?.cropStage && (

                                                        <small>

                                                            {
                                                                crop.cropStage
                                                            }

                                                        </small>

                                                    )
                                                }

                                            </div>


                                            <div
                                                style={{
                                                    marginLeft: "auto",
                                                    fontSize: "24px",
                                                }}
                                            >
                                                ›
                                            </div>

                                        </button>

                                    )
                                )
                            }

                        </div>

                    )
                }

            </section>


            {/* =================================================
                SOIL RECOVERY ROADMAP
            ================================================= */}

            <section className="passport-section">

                <h2 className="passport-section-title">

                    {
                        isHindi
                            ? "मिट्टी रिकवरी रोडमैप"
                            : "Soil Recovery Roadmap"
                    }

                </h2>


                {
                    recoveryPlans.length === 0 ? (

                        <div className="passport-empty-card">

                            {
                                isHindi
                                    ? "कोई रिकवरी प्लान उपलब्ध नहीं है।"
                                    : "No recovery plan available."
                            }

                        </div>

                    ) : (

                        <div className="passport-recovery-list">

                            {
                                recoveryPlans.map(
                                    (
                                        plan,
                                        index
                                    ) => (

                                        <div
                                            className="passport-recovery-card"
                                            key={
                                                plan?.id ||
                                                `recovery-${index}`
                                            }
                                        >

                                            <div className="passport-recovery-number">

                                                {
                                                    displayValue(
                                                        plan?.seasonNumber
                                                    )
                                                }

                                            </div>


                                            <div className="passport-recovery-content">

                                                <h3>

                                                    {
                                                        displayValue(
                                                            plan?.season
                                                        )
                                                    }

                                                </h3>


                                                <div className="passport-recovery-meta">

                                                    <span>

                                                        {
                                                            displayValue(
                                                                plan?.focusArea
                                                            )
                                                        }

                                                    </span>


                                                    {
                                                        plan?.targetValue !==
                                                        null &&
                                                        plan?.targetValue !==
                                                        undefined && (

                                                            <span>

                                                                Target:{" "}

                                                                {
                                                                    plan.targetValue
                                                                }{" "}

                                                                {
                                                                    displayValue(
                                                                        plan.unit
                                                                    )
                                                                }

                                                            </span>

                                                        )
                                                    }

                                                </div>


                                                <p>

                                                    {
                                                        displayValue(
                                                            plan?.recommendation
                                                        )
                                                    }

                                                </p>


                                                {
                                                    plan?.actionType && (

                                                        <small>

                                                            {
                                                                plan.actionType
                                                            }

                                                        </small>

                                                    )
                                                }

                                            </div>

                                        </div>

                                    )
                                )
                            }

                        </div>

                    )
                }

            </section>


            {/* =================================================
                DATA STATUS
            ================================================= */}

            <section className="passport-section">

                <div className="passport-disclaimer">

                    <strong>

                        {
                            isHindi
                                ? "डेटा स्थिति"
                                : "Data Status"
                        }

                    </strong>


                    <span>

                        {
                            displayValue(
                                passport.dataStatus
                            )
                        }

                    </span>


                    <p>

                        {
                            passport.uncertaintyNote ||
                            (
                                isHindi
                                    ? "यह पासपोर्ट निर्णय-सहायता के लिए है। महत्वपूर्ण कृषि निर्णय वर्तमान मिट्टी परीक्षण, खेत निरीक्षण और स्थानीय कृषि सलाह से सत्यापित करें।"
                                    : "This passport is decision-support information. Validate important agricultural decisions using current soil tests, field observations and appropriate local agronomic guidance."
                            )
                        }

                    </p>

                </div>

            </section>


            <BottomNavBar />

        </div>
    );
}


export default FieldHealthPassport;