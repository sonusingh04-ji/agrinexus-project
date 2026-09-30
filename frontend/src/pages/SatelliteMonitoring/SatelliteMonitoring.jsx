import {
    useEffect,
    useMemo,
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
    getWeatherRadar,
} from "../../api/weatherRiskApi";

import BottomNavBar
    from "../../components/BottomNavBar";

import "./SatelliteMonitoring.css";


/* =========================================================
   HELPERS
========================================================= */

const isObject = (value) =>
    value !== null &&
    typeof value === "object" &&
    !Array.isArray(value);


const hasValue = (value) =>
    value !== null &&
    value !== undefined &&
    value !== "";


const toNumber = (value) => {
    if (
        value === null ||
        value === undefined ||
        value === ""
    ) {
        return null;
    }

    const parsed =
        Number(value);

    return Number.isFinite(parsed)
        ? parsed
        : null;
};


const formatNumber = (
    value,
    digits = 3
) => {
    const number =
        toNumber(value);

    if (number === null) {
        return "—";
    }

    return number.toFixed(
        digits
    );
};


const formatDateTime = (
    value,
    language
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
        return String(value);
    }

    return date.toLocaleString(
        language === "hi"
            ? "hi-IN"
            : "en-IN",
        {
            day: "2-digit",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        }
    );
};


const normalizeStatus =
    (value) => {
        if (!value) {
            return "—";
        }

        return String(value)
            .replaceAll(
                "_",
                " "
            )
            .trim();
    };


const normalizeList = (
    response
) => {
    if (Array.isArray(response)) {
        return response;
    }

    if (
        Array.isArray(
            response?.content
        )
    ) {
        return response.content;
    }

    if (
        Array.isArray(
            response?.data
        )
    ) {
        return response.data;
    }

    if (response?.id) {
        return [response];
    }

    return [];
};


/*
 * Return the latest record based on
 * whichever date field is actually available.
 *
 * No date or measurement is fabricated.
 */
const getLatestByDate = (
    records,
    ...dateFields
) => {
    if (
        !Array.isArray(records) ||
        records.length === 0
    ) {
        return null;
    }

    return (
        [...records]
            .filter(
                (item) =>
                    item &&
                    dateFields.some(
                        (field) =>
                            item?.[field]
                    )
            )
            .sort(
                (a, b) => {
                    const dateA =
                        dateFields
                            .map(
                                (field) =>
                                    a?.[field]
                            )
                            .find(Boolean);

                    const dateB =
                        dateFields
                            .map(
                                (field) =>
                                    b?.[field]
                            )
                            .find(Boolean);

                    return (
                        new Date(
                            dateB ?? 0
                        ).getTime() -
                        new Date(
                            dateA ?? 0
                        ).getTime()
                    );
                }
            )[0] ??
        null
    );
};


/* =========================================================
   COMPONENT
========================================================= */

function SatelliteMonitoring() {
    const {
        language,
    } = useLanguage();

    const navigate =
        useNavigate();

    const isHindi =
        language === "hi";


    /* =====================================================
       STATE
    ===================================================== */

    const [
        radar,
        setRadar,
    ] = useState(null);


    const [
        latestObservation,
        setLatestObservation,
    ] = useState(null);


    const [
        observationHistory,
        setObservationHistory,
    ] = useState([]);


    const [
        satelliteFieldRecords,
        setSatelliteFieldRecords,
    ] = useState([]);


    const [
        loading,
        setLoading,
    ] = useState(true);


    const [
        error,
        setError,
    ] = useState("");


    /* =====================================================
       CURRENT FARM
    ===================================================== */

    const currentFarmId =
        getCurrentFarmId();


    /* =====================================================
       LOAD REAL SATELLITE DATA
    ===================================================== */

    useEffect(() => {
        let cancelled = false;


        const loadSatellite =
            async () => {
                try {
                    setLoading(
                        true
                    );

                    setError("");

                    /*
                     * Clear the previous farm's
                     * values before loading the
                     * newly selected farm.
                     */
                    setRadar(null);

                    setLatestObservation(
                        null
                    );

                    setObservationHistory(
                        []
                    );

                    setSatelliteFieldRecords(
                        []
                    );


                    const farmId =
                        getCurrentFarmId();


                    if (!farmId) {
                        throw new Error(
                            isHindi
                                ? "कृपया पहले एक खेत चुनें।"
                                : "Please select a farm first."
                        );
                    }


                    /*
                     * =================================================
                     * REAL SATELLITE DATA SOURCES
                     * =================================================
                     *
                     * We read only records belonging
                     * to the selected farm.
                     *
                     * Source priority:
                     *
                     * 1. Latest Satellite Observation
                     * 2. Satellite Observation history
                     * 3. Satellite Field Data
                     * 4. Weather Radar latestSatelliteData
                     *
                     * No values are invented.
                     */

                    const [
                        radarResult,
                        latestObservationResult,
                        observationHistoryResult,
                        satelliteFieldResult,
                    ] = await Promise.allSettled([
                        /*
                         * Weather Radar
                         */
                        getWeatherRadar(
                            farmId
                        ),


                        /*
                         * Latest satellite observation
                         */
                        apiRequest(
                            `/api/satellite-observations/farm/${farmId}/latest`
                        ),


                        /*
                         * Satellite observation history
                         */
                        apiRequest(
                            `/api/satellite-observations/farm/${farmId}`
                        ),


                        /*
                         * Satellite field data
                         */
                        apiRequest(
                            `/api/satellite/farm/${farmId}`
                        ),
                    ]);


                    if (
                        cancelled
                    ) {
                        return;
                    }


                    /*
                     * =================================================
                     * WEATHER RADAR RESULT
                     * =================================================
                     */

                    setRadar(
                        radarResult.status ===
                        "fulfilled" &&
                        isObject(
                            radarResult.value
                        )
                            ? radarResult.value
                            : null
                    );


                    /*
                     * =================================================
                     * LATEST SATELLITE OBSERVATION
                     * =================================================
                     */

                    setLatestObservation(
                        latestObservationResult.status ===
                        "fulfilled"
                            ? (
                                isObject(
                                    latestObservationResult.value
                                )
                                    ? latestObservationResult.value
                                    : null
                            )
                            : null
                    );


                    /*
                     * =================================================
                     * SATELLITE OBSERVATION HISTORY
                     * =================================================
                     */

                    setObservationHistory(
                        observationHistoryResult.status ===
                        "fulfilled"
                            ? normalizeList(
                                observationHistoryResult.value
                            )
                            : []
                    );


                    /*
                     * =================================================
                     * SATELLITE FIELD DATA
                     * =================================================
                     */

                    setSatelliteFieldRecords(
                        satelliteFieldResult.status ===
                        "fulfilled"
                            ? normalizeList(
                                satelliteFieldResult.value
                            )
                            : []
                    );

                } catch (
                    requestError
                    ) {

                    if (
                        cancelled
                    ) {
                        return;
                    }


                    console.error(
                        "Satellite monitoring request failed:",
                        requestError
                    );


                    setRadar(
                        null
                    );

                    setLatestObservation(
                        null
                    );

                    setObservationHistory(
                        []
                    );

                    setSatelliteFieldRecords(
                        []
                    );


                    setError(
                        requestError?.message ||
                        (
                            isHindi
                                ? "सैटेलाइट मॉनिटरिंग डेटा लोड नहीं हो सका।"
                                : "Unable to load satellite monitoring data."
                        )
                    );

                } finally {

                    if (
                        !cancelled
                    ) {
                        setLoading(
                            false
                        );
                    }
                }
            };


        loadSatellite();


        return () => {
            cancelled = true;
        };

    }, [
        isHindi,
    ]);


    /* =====================================================
       LATEST HISTORY RECORD
    ===================================================== */

    const historyLatest =
        useMemo(
            () =>
                getLatestByDate(
                    observationHistory,
                    "observedAt",
                    "recordedAt",
                    "timestamp"
                ),
            [
                observationHistory,
            ]
        );


    /* =====================================================
       LATEST SATELLITE FIELD DATA
    ===================================================== */

    const fieldDataLatest =
        useMemo(
            () =>
                getLatestByDate(
                    satelliteFieldRecords,
                    "recordedAt",
                    "observedAt",
                    "timestamp"
                ),
            [
                satelliteFieldRecords,
            ]
        );


    /* =====================================================
       MERGED REAL SATELLITE RECORD
    ===================================================== */

    const satellite =
        useMemo(
            () => {
                /*
                 * Primary real observation.
                 */
                const observation =
                    latestObservation ??
                    historyLatest ??
                    null;


                /*
                 * Weather Radar satellite
                 * fallback.
                 */
                const radarSatellite =
                    radar?.latestSatelliteData ??
                    null;


                /*
                 * Satellite Field Data fallback.
                 */
                const fieldData =
                    fieldDataLatest ??
                    null;


                /*
                 * Nothing available anywhere.
                 */
                if (
                    !observation &&
                    !fieldData &&
                    !radarSatellite
                ) {
                    return null;
                }


                /*
                 * Merge only backend-supplied
                 * values.
                 *
                 * A later source is used only
                 * when the earlier source does
                 * not provide that field.
                 */
                return {

                    /*
                     * Preserve original observation
                     * fields.
                     */
                    ...(
                        isObject(
                            observation
                        )
                            ? observation
                            : {}
                    ),


                    /*
                     * NDVI
                     */
                    ndvi:
                        observation?.ndvi ??
                        fieldData?.ndvi ??
                        radarSatellite?.ndvi ??
                        null,


                    /*
                     * NDWI
                     */
                    ndwi:
                        observation?.ndwi ??
                        fieldData?.ndwi ??
                        radarSatellite?.ndwi ??
                        null,


                    /*
                     * Vegetation Health
                     */
                    vegetationHealth:
                        fieldData?.vegetationHealth ??
                        observation?.vegetationHealth ??
                        radarSatellite?.vegetationHealth ??
                        null,


                    /*
                     * Satellite Soil Moisture
                     */
                    soilMoisture:
                        fieldData?.soilMoisture ??
                        observation?.soilMoisture ??
                        radarSatellite?.soilMoisture ??
                        null,


                    /*
                     * Cloud Coverage
                     *
                     * Observation DTO uses
                     * cloudCoverPercent.
                     */
                    cloudCoverage:
                        observation?.cloudCoverPercent ??
                        observation?.cloudCoverage ??
                        fieldData?.cloudCoverage ??
                        radarSatellite?.cloudCoverage ??
                        null,


                    /*
                     * Health Status
                     */
                    healthStatus:
                        fieldData?.healthStatus ??
                        observation?.healthStatus ??
                        radarSatellite?.healthStatus ??
                        null,


                    /*
                     * Recommendation
                     */
                    recommendation:
                        fieldData?.recommendation ??
                        observation?.recommendation ??
                        radarSatellite?.recommendation ??
                        null,


                    /*
                     * Data Source
                     */
                    dataSource:
                        observation?.dataSource ??
                        observation?.satelliteSource ??
                        fieldData?.dataSource ??
                        radarSatellite?.dataSource ??
                        null,


                    /*
                     * Data Status
                     */
                    dataStatus:
                        observation?.dataStatus ??
                        fieldData?.dataStatus ??
                        radarSatellite?.dataStatus ??
                        null,


                    /*
                     * Observation Time
                     */
                    observedAt:
                        observation?.observedAt ??
                        observation?.recordedAt ??
                        observation?.timestamp ??
                        fieldData?.recordedAt ??
                        radarSatellite?.recordedAt ??
                        null,
                };
            },
            [
                latestObservation,
                historyLatest,
                fieldDataLatest,
                radar,
            ]
        );


    /* =====================================================
       FARM INFO
    ===================================================== */

    const farmName =
        radar?.farmName ??
        satellite?.farmName ??
        null;


    const location =
        radar?.location ??
        satellite?.location ??
        null;


    /* =====================================================
       SATELLITE VALUES
    ===================================================== */

    const ndvi =
        satellite?.ndvi ??
        null;


    const vegetationHealth =
        satellite?.vegetationHealth ??
        null;


    const satelliteSoilMoisture =
        satellite?.soilMoisture ??
        null;


    const cloudCoverage =
        satellite?.cloudCoverage ??
        null;


    const healthStatus =
        satellite?.healthStatus ??
        null;


    const recommendation =
        satellite?.recommendation ??
        null;


    /*
     * NDWI is shown only when the backend
     * actually provides it.
     *
     * We DO NOT calculate NDWI from NDVI
     * or any other value.
     */
    const ndwi =
        satellite?.ndwi ??
        null;


    /*
     * Observation time is shown only if a
     * real backend timestamp exists.
     */
    const observedAt =
        satellite?.observedAt ??
        null;


    /* =====================================================
       HEALTH STATUS CLASS
    ===================================================== */

    const healthClass =
        useMemo(() => {

            const status =
                String(
                    healthStatus ??
                    ""
                ).toUpperCase();


            if (
                status.includes(
                    "HEALTHY"
                ) ||
                status === "GOOD"
            ) {
                return "healthy";
            }


            if (
                status.includes(
                    "ATTENTION"
                ) ||
                status.includes(
                    "WARNING"
                ) ||
                status === "MODERATE"
            ) {
                return "attention";
            }


            if (
                status.includes(
                    "CRITICAL"
                ) ||
                status.includes(
                    "POOR"
                )
            ) {
                return "critical";
            }


            return "neutral";

        }, [
            healthStatus,
        ]);


    /* =====================================================
       REAL DATA AVAILABLE
    ===================================================== */

    const hasSatelliteData =
        Boolean(
            satellite &&
            (
                hasValue(
                    satellite.ndvi
                ) ||
                hasValue(
                    satellite.ndwi
                ) ||
                hasValue(
                    satellite.vegetationHealth
                ) ||
                hasValue(
                    satellite.soilMoisture
                ) ||
                hasValue(
                    satellite.cloudCoverage
                ) ||
                hasValue(
                    satellite.healthStatus
                ) ||
                hasValue(
                    satellite.recommendation
                )
            )
        );


    /* =====================================================
       BACK
    ===================================================== */

    const handleBack =
        () => {
            navigate(
                "/home"
            );
        };


    /* =====================================================
       OPEN WEATHER RADAR
    ===================================================== */

    const openWeatherRadar =
        () => {
            navigate(
                "/weather-radar"
            );
        };


    /* =====================================================
       LOADING
    ===================================================== */

    if (loading) {
        return (
            <div className="satellite-page">

                <div className="satellite-loading">

                    <div className="satellite-loading-icon">
                        🛰️
                    </div>


                    <h2>
                        {isHindi
                            ? "सैटेलाइट डेटा लोड हो रहा है..."
                            : "Loading Satellite Monitoring..."}
                    </h2>


                    <p>
                        {isHindi
                            ? "चयनित खेत का वास्तविक रिमोट-सेंसिंग डेटा लिया जा रहा है।"
                            : "Loading real remote-sensing data for the selected farm."}
                    </p>

                </div>


                <BottomNavBar />

            </div>
        );
    }


    /* =====================================================
       ERROR / NO FARM
    ===================================================== */

    if (
        error ||
        !currentFarmId
    ) {
        return (
            <div className="satellite-page">

                <header className="satellite-header">

                    <button
                        type="button"
                        className="satellite-back-button"
                        onClick={
                            handleBack
                        }
                        aria-label={
                            isHindi
                                ? "वापस"
                                : "Back"
                        }
                    >
                        ←
                    </button>


                    <div>

                        <span className="satellite-kicker">
                            REMOTE SENSING
                        </span>


                        <h1>
                            {isHindi
                                ? "सैटेलाइट मॉनिटरिंग"
                                : "Satellite Monitoring"}
                        </h1>


                        <p>
                            {isHindi
                                ? "खेत की वनस्पति और सतह संकेत"
                                : "Field vegetation and surface indicators"}
                        </p>

                    </div>

                </header>


                <main className="satellite-content">

                    <section className="satellite-empty-card satellite-error-card">

                        <div className="satellite-empty-icon">
                            🛰️
                        </div>


                        <h2>
                            {isHindi
                                ? "डेटा उपलब्ध नहीं"
                                : "Satellite data unavailable"}
                        </h2>


                        <p>
                            {
                                error ||
                                (
                                    isHindi
                                        ? "कृपया पहले एक खेत चुनें।"
                                        : "Please select a farm first."
                                )
                            }
                        </p>


                        <div className="satellite-empty-actions">

                            <button
                                type="button"
                                className="satellite-primary-button"
                                onClick={
                                    handleBack
                                }
                            >
                                {isHindi
                                    ? "होम पर जाएं"
                                    : "Go to Home"}
                            </button>


                            <button
                                type="button"
                                className="satellite-secondary-button"
                                onClick={
                                    openWeatherRadar
                                }
                            >
                                {isHindi
                                    ? "मौसम रडार खोलें"
                                    : "Open Weather Radar"}
                            </button>

                        </div>

                    </section>

                </main>


                <BottomNavBar />

            </div>
        );
    }


    /* =====================================================
       MAIN UI
    ===================================================== */

    return (
        <div className="satellite-page">

            {/* =================================================
                HEADER
            ================================================= */}

            <header className="satellite-header">

                <button
                    type="button"
                    className="satellite-back-button"
                    onClick={
                        handleBack
                    }
                    aria-label={
                        isHindi
                            ? "वापस"
                            : "Back"
                    }
                >
                    ←
                </button>


                <div className="satellite-header-main">

                    <span className="satellite-kicker">
                        REMOTE SENSING
                    </span>


                    <h1>
                        {isHindi
                            ? "सैटेलाइट मॉनिटरिंग"
                            : "Satellite Monitoring"}
                    </h1>


                    <p>
                        {
                            farmName ??
                            "—"
                        }

                        {" "}

                        ·{" "}

                        {
                            location ??
                            "—"
                        }
                    </p>

                </div>


                <div className="satellite-header-icon">
                    🛰️
                </div>

            </header>


            {/* =================================================
                DATA NOTICE
            ================================================= */}

            <div className="satellite-data-notice">

                <span className="satellite-data-notice-icon">
                    ✓
                </span>


                <div>

                    <strong>
                        {
                            isHindi
                                ? "वास्तविक बैकएंड डेटा"
                                : "Real backend data"
                        }
                    </strong>


                    <span>
                        {
                            isHindi
                                ? "सैटेलाइट संकेत स्क्रीनिंग के लिए हैं, अंतिम निदान नहीं।"
                                : "Satellite indicators are screening signals, not definitive diagnoses."
                        }
                    </span>

                </div>

            </div>


            <main className="satellite-content">

                {/* =================================================
                    HERO
                ================================================= */}

                <section className="satellite-hero-card">

                    <div className="satellite-hero-left">

                        <div className="satellite-orbit-icon">
                            🛰️
                        </div>


                        <div>

                            <span className="satellite-small-label">
                                {
                                    isHindi
                                        ? "चयनित खेत"
                                        : "Selected Farm"
                                }
                            </span>


                            <h2>
                                {
                                    farmName ??
                                    "—"
                                }
                            </h2>


                            <p>
                                {
                                    location ??
                                    "—"
                                }
                            </p>

                        </div>

                    </div>


                    <div className="satellite-data-state">

                        <span
                            className={
                                hasSatelliteData
                                    ? "available"
                                    : "unavailable"
                            }
                        >
                            {
                                hasSatelliteData
                                    ? (
                                        isHindi
                                            ? "डेटा उपलब्ध"
                                            : "Data available"
                                    )
                                    : (
                                        isHindi
                                            ? "डेटा उपलब्ध नहीं"
                                            : "Data unavailable"
                                    )
                            }
                        </span>

                    </div>

                </section>


                {/* =================================================
                    PRIMARY METRICS
                ================================================= */}

                <section className="satellite-section">

                    <div className="satellite-section-heading">

                        <div>

                            <span className="satellite-kicker">
                                VEGETATION SIGNALS
                            </span>


                            <h2>
                                {
                                    isHindi
                                        ? "फील्ड संकेतक"
                                        : "Field Indicators"
                                }
                            </h2>

                        </div>


                        <span className="satellite-section-icon">
                            🌱
                        </span>

                    </div>


                    <div className="satellite-metrics-grid">

                        {/* NDVI */}

                        <article className="satellite-metric-card satellite-metric-primary">

                            <div className="satellite-metric-top">

                                <span className="satellite-metric-icon">
                                    🌿
                                </span>


                                <span className="satellite-metric-label">
                                    NDVI
                                </span>

                            </div>


                            <strong>
                                {
                                    formatNumber(
                                        ndvi,
                                        3
                                    )
                                }
                            </strong>


                            <small>
                                {
                                    isHindi
                                        ? "वनस्पति संकेत"
                                        : "Vegetation signal"
                                }
                            </small>

                        </article>


                        {/* NDWI */}

                        <article className="satellite-metric-card">

                            <div className="satellite-metric-top">

                                <span className="satellite-metric-icon">
                                    💧
                                </span>


                                <span className="satellite-metric-label">
                                    NDWI
                                </span>

                            </div>


                            <strong>
                                {
                                    formatNumber(
                                        ndwi,
                                        3
                                    )
                                }
                            </strong>


                            <small>
                                {
                                    isHindi
                                        ? "उपलब्ध होने पर जल संकेत"
                                        : "Water signal when supplied by backend"
                                }
                            </small>

                        </article>


                        {/* VEGETATION HEALTH */}

                        <article className="satellite-metric-card">

                            <div className="satellite-metric-top">

                                <span className="satellite-metric-icon">
                                    🌾
                                </span>


                                <span className="satellite-metric-label">
                                    {
                                        isHindi
                                            ? "वनस्पति स्वास्थ्य"
                                            : "Vegetation Health"
                                    }
                                </span>

                            </div>


                            <strong className="satellite-text-value">
                                {
                                    normalizeStatus(
                                        vegetationHealth
                                    )
                                }
                            </strong>


                            <small>
                                {
                                    isHindi
                                        ? "बैकएंड संकेत"
                                        : "Backend signal"
                                }
                            </small>

                        </article>


                        {/* SOIL MOISTURE */}

                        <article className="satellite-metric-card">

                            <div className="satellite-metric-top">

                                <span className="satellite-metric-icon">
                                    💦
                                </span>


                                <span className="satellite-metric-label">
                                    {
                                        isHindi
                                            ? "सैटेलाइट मिट्टी नमी"
                                            : "Satellite Soil Moisture"
                                    }
                                </span>

                            </div>


                            <strong>
                                {
                                    hasValue(
                                        satelliteSoilMoisture
                                    )
                                        ? formatNumber(
                                            satelliteSoilMoisture,
                                            2
                                        )
                                        : "—"
                                }
                            </strong>


                            <small>
                                {
                                    isHindi
                                        ? "बैकएंड रिमोट-सेंसिंग मान"
                                        : "Backend remote-sensing value"
                                }
                            </small>

                        </article>


                        {/* CLOUD COVERAGE */}

                        <article className="satellite-metric-card">

                            <div className="satellite-metric-top">

                                <span className="satellite-metric-icon">
                                    ☁️
                                </span>


                                <span className="satellite-metric-label">
                                    {
                                        isHindi
                                            ? "क्लाउड कवरेज"
                                            : "Cloud Coverage"
                                    }
                                </span>

                            </div>


                            <strong>
                                {
                                    hasValue(
                                        cloudCoverage
                                    )
                                        ? `${formatNumber(
                                            cloudCoverage,
                                            1
                                        )}%`
                                        : "—"
                                }
                            </strong>


                            <small>
                                {
                                    isHindi
                                        ? "बैकएंड मान"
                                        : "Backend value"
                                }
                            </small>

                        </article>


                        {/* HEALTH STATUS */}

                        <article className="satellite-metric-card">

                            <div className="satellite-metric-top">

                                <span className="satellite-metric-icon">
                                    ❤️
                                </span>


                                <span className="satellite-metric-label">
                                    {
                                        isHindi
                                            ? "स्वास्थ्य स्थिति"
                                            : "Health Status"
                                    }
                                </span>

                            </div>


                            <strong
                                className={
                                    `satellite-health-value ${healthClass}`
                                }
                            >
                                {
                                    normalizeStatus(
                                        healthStatus
                                    )
                                }
                            </strong>


                            <small>
                                {
                                    isHindi
                                        ? "सैटेलाइट स्क्रीनिंग संकेत"
                                        : "Satellite screening signal"
                                }
                            </small>

                        </article>

                    </div>

                </section>


                {/* =================================================
                    INTERPRETATION
                ================================================= */}

                <section className="satellite-section">

                    <div className="satellite-section-heading">

                        <div>

                            <span className="satellite-kicker">
                                SCREENING RESULT
                            </span>


                            <h2>
                                {
                                    isHindi
                                        ? "फील्ड स्थिति"
                                        : "Field Condition"
                                }
                            </h2>

                        </div>


                        <span className="satellite-section-icon">
                            📡
                        </span>

                    </div>


                    <div
                        className={
                            `satellite-condition-card ${healthClass}`
                        }
                    >

                        <div className="satellite-condition-icon">

                            {
                                healthClass ===
                                "healthy"
                                    ? "✅"
                                    : healthClass ===
                                    "critical"
                                        ? "⚠️"
                                        : "📡"
                            }

                        </div>


                        <div>

                            <span className="satellite-condition-label">
                                {
                                    isHindi
                                        ? "स्वास्थ्य स्क्रीनिंग"
                                        : "Health Screening"
                                }
                            </span>


                            <h3>
                                {
                                    normalizeStatus(
                                        healthStatus
                                    )
                                }
                            </h3>


                            <p>
                                {
                                    hasValue(
                                        vegetationHealth
                                    )
                                        ? String(
                                            vegetationHealth
                                        )
                                        : (
                                            isHindi
                                                ? "कोई वनस्पति स्वास्थ्य विवरण उपलब्ध नहीं है।"
                                                : "No vegetation-health description is available."
                                        )
                                }
                            </p>

                        </div>

                    </div>

                </section>


                {/* =================================================
                    RECOMMENDATION
                ================================================= */}

                <section className="satellite-section">

                    <div className="satellite-section-heading">

                        <div>

                            <span className="satellite-kicker">
                                BACKEND GUIDANCE
                            </span>


                            <h2>
                                {
                                    isHindi
                                        ? "सिफारिश"
                                        : "Recommendation"
                                }
                            </h2>

                        </div>


                        <span className="satellite-section-icon">
                            💡
                        </span>

                    </div>


                    <div className="satellite-recommendation-card">

                        <div className="satellite-recommendation-icon">
                            💡
                        </div>


                        <div>

                            <h3>
                                {
                                    hasValue(
                                        recommendation
                                    )
                                        ? (
                                            isHindi
                                                ? "बैकएंड से प्राप्त सलाह"
                                                : "Backend recommendation"
                                        )
                                        : (
                                            isHindi
                                                ? "सिफारिश उपलब्ध नहीं"
                                                : "No recommendation available"
                                        )
                                }
                            </h3>


                            <p>
                                {
                                    recommendation ??
                                    "—"
                                }
                            </p>

                        </div>

                    </div>

                </section>


                {/* =================================================
                    DATA PROVENANCE
                ================================================= */}

                <section className="satellite-section">

                    <div className="satellite-section-heading">

                        <div>

                            <span className="satellite-kicker">
                                DATA INFORMATION
                            </span>


                            <h2>
                                {
                                    isHindi
                                        ? "डेटा विवरण"
                                        : "Data Information"
                                }
                            </h2>

                        </div>


                        <span className="satellite-section-icon">
                            ℹ️
                        </span>

                    </div>


                    <div className="satellite-info-grid">

                        <div className="satellite-info-card">

                            <span>
                                {
                                    isHindi
                                        ? "डेटा स्रोत"
                                        : "Data Source"
                                }
                            </span>


                            <strong>
                                {
                                    satellite?.dataSource ??
                                    satellite?.satelliteSource ??
                                    satellite?.source ??
                                    "—"
                                }
                            </strong>

                        </div>


                        <div className="satellite-info-card">

                            <span>
                                {
                                    isHindi
                                        ? "डेटा स्थिति"
                                        : "Data Status"
                                }
                            </span>


                            <strong>
                                {
                                    satellite?.dataStatus ??
                                    "—"
                                }
                            </strong>

                        </div>


                        <div className="satellite-info-card">

                            <span>
                                {
                                    isHindi
                                        ? "अवलोकन समय"
                                        : "Observation Time"
                                }
                            </span>


                            <strong>
                                {
                                    formatDateTime(
                                        observedAt,
                                        language
                                    )
                                }
                            </strong>

                        </div>


                        <div className="satellite-info-card">

                            <span>
                                Farm ID
                            </span>


                            <strong>
                                {
                                    radar?.farmId ??
                                    currentFarmId ??
                                    "—"
                                }
                            </strong>

                        </div>

                    </div>

                </section>


                {/* =================================================
                    SCIENTIFIC NOTICE
                ================================================= */}

                <section className="satellite-scientific-notice">

                    <div className="satellite-scientific-icon">
                        🔬
                    </div>


                    <div>

                        <strong>
                            {
                                isHindi
                                    ? "महत्वपूर्ण वैज्ञानिक नोट"
                                    : "Important scientific note"
                            }
                        </strong>


                        <p>
                            {
                                isHindi
                                    ? "NDVI, NDWI और अन्य रिमोट-सेंसिंग संकेत फसल/खेत में बदलाव की स्क्रीनिंग के लिए हैं। इन्हें अकेले रोग, कीट या फसल समस्या का अंतिम निदान नहीं माना जाना चाहिए।"
                                    : "NDVI, NDWI and other remote-sensing indicators are screening signals for crop or field change. They should not be treated alone as a definitive diagnosis of disease, pests, or crop problems."
                            }
                        </p>

                    </div>

                </section>


                {/* =================================================
                    ACTIONS
                ================================================= */}

                <section className="satellite-actions">

                    <button
                        type="button"
                        className="satellite-primary-button"
                        onClick={
                            openWeatherRadar
                        }
                    >
                        🌦️{" "}

                        {
                            isHindi
                                ? "मौसम जोखिम रडार"
                                : "Weather Risk Radar"
                        }

                    </button>


                    <button
                        type="button"
                        className="satellite-secondary-button"
                        onClick={
                            handleBack
                        }
                    >
                        ←{" "}

                        {
                            isHindi
                                ? "होम पर वापस जाएं"
                                : "Back to Home"
                        }

                    </button>

                </section>

            </main>


            <BottomNavBar />

        </div>
    );
}


export default SatelliteMonitoring;