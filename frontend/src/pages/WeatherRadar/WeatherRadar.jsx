import {
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";

import { useNavigate } from "react-router-dom";

import { useLanguage } from "../../context/LanguageContext";

import {
    apiRequest,
    getCurrentFarmId,
} from "../../api/apiClient";

import {
    getWeatherRadar,
    fetchLiveWeather,
} from "../../api/weatherRiskApi";

import {
    getLatestLulc,
    syncLulc,
} from "../../api/lulcApi";

import BottomNavBar from "../../components/BottomNavBar";

import "./WeatherRadar.css";


function WeatherRadar() {

    const { language } =
        useLanguage();

    const navigate =
        useNavigate();

    const isHindi =
        language === "hi";


    /* =====================================================
       WEATHER RADAR STATE
    ===================================================== */

    const [radar, setRadar] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [refreshing, setRefreshing] =
        useState(false);

    const [error, setError] =
        useState("");


    /* =====================================================
       LULC STATE
    ===================================================== */

    const [lulc, setLulc] =
        useState(null);

    const [lulcLoading, setLulcLoading] =
        useState(true);

    const [lulcSyncing, setLulcSyncing] =
        useState(false);

    const [lulcError, setLulcError] =
        useState("");


    /* =====================================================
       REAL SENTINEL-2 SATELLITE OBSERVATION STATE
    ===================================================== */

    const [satelliteObservation, setSatelliteObservation] =
        useState(null);

    const [satelliteLoading, setSatelliteLoading] =
        useState(true);

    const [satelliteSyncing, setSatelliteSyncing] =
        useState(false);

    const [satelliteError, setSatelliteError] =
        useState("");


    /*
     * Prevent duplicate live-weather requests
     * during React StrictMode development rendering.
     */

    const liveWeatherLoadedRef =
        useRef(false);


    /* =====================================================
       WEATHER RADAR LOADER
    ===================================================== */

    const loadRadar = useCallback(
        async ({
                   includeLiveWeather = false,
                   silent = false,
               } = {}) => {

            const farmId =
                getCurrentFarmId();


            if (!farmId) {

                throw new Error(
                    isHindi
                        ? "कृपया पहले एक खेत चुनें।"
                        : "Please select a farm first."
                );
            }


            if (silent) {

                setRefreshing(true);

            } else {

                setLoading(true);
            }


            setError("");


            try {

                /*
                 * Live weather is fetched only once
                 * during the initial radar load.
                 *
                 * Normal refresh only GETs the
                 * existing radar data.
                 */

                if (
                    includeLiveWeather &&
                    !liveWeatherLoadedRef.current
                ) {

                    liveWeatherLoadedRef.current =
                        true;


                    await fetchLiveWeather(
                        farmId
                    );
                }


                const response =
                    await getWeatherRadar(
                        farmId
                    );


                setRadar(
                    response
                );

            } catch (requestError) {

                setError(
                    requestError?.message ||
                    (
                        isHindi
                            ? "मौसम जोखिम डेटा लोड नहीं हो सका।"
                            : "Unable to load weather risk data."
                    )
                );


                if (!silent) {

                    setRadar(null);
                }

            } finally {

                setLoading(false);

                setRefreshing(false);
            }
        },

        [isHindi]
    );


    /* =====================================================
       INITIAL WEATHER RADAR LOAD
    ===================================================== */

    useEffect(() => {

        const startLoad =
            async () => {

                try {

                    await loadRadar({
                        includeLiveWeather: true,
                    });

                } catch (requestError) {

                    setError(
                        requestError?.message ||
                        (
                            isHindi
                                ? "मौसम जोखिम डेटा लोड नहीं हो सका।"
                                : "Unable to load weather risk data."
                        )
                    );


                    setLoading(false);
                }
            };


        startLoad();

    }, [
        loadRadar,
        isHindi,
    ]);


    /* =====================================================
       LOAD LATEST LULC
    ===================================================== */

    useEffect(() => {

        let cancelled =
            false;


        const loadLatestLulc =
            async () => {

                const farmId =
                    getCurrentFarmId();


                /*
                 * If no farm is selected,
                 * do not make a backend request.
                 */

                if (!farmId) {

                    if (!cancelled) {

                        setLulc(null);

                        setLulcLoading(false);

                        setLulcError(
                            isHindi
                                ? "कृपया पहले एक खेत चुनें।"
                                : "Please select a farm first."
                        );
                    }

                    return;
                }


                setLulcLoading(true);

                setLulcError("");


                try {

                    /*
                     * GET the latest stored LULC observation.
                     *
                     * This does NOT call Copernicus.
                     *
                     * Copernicus synchronization happens
                     * only when the user presses Refresh.
                     */

                    const response =
                        await getLatestLulc(
                            farmId
                        );


                    if (!cancelled) {

                        setLulc(
                            response
                        );
                    }

                } catch (requestError) {

                    console.error(
                        "LULC latest observation could not be loaded:",
                        requestError
                    );


                    if (!cancelled) {

                        setLulc(null);

                        setLulcError(
                            requestError?.message ||
                            (
                                isHindi
                                    ? "भूमि आवरण डेटा अभी उपलब्ध नहीं है।"
                                    : "Land cover data is not available yet."
                            )
                        );
                    }

                } finally {

                    if (!cancelled) {

                        setLulcLoading(false);
                    }
                }
            };


        loadLatestLulc();


        return () => {

            cancelled = true;
        };

    }, [
        isHindi,
    ]);


    /* =====================================================
       LOAD LATEST REAL SENTINEL-2 OBSERVATION
    ===================================================== */

    useEffect(() => {

        let cancelled =
            false;

        const loadLatestSatellite =
            async () => {

                const farmId =
                    getCurrentFarmId();

                if (!farmId) {

                    if (!cancelled) {

                        setSatelliteObservation(null);
                        setSatelliteLoading(false);

                        setSatelliteError(
                            isHindi
                                ? "कृपया पहले एक खेत चुनें।"
                                : "Please select a farm first."
                        );
                    }

                    return;
                }

                setSatelliteLoading(true);
                setSatelliteError("");

                try {

                    const response =
                        await apiRequest(
                            `/api/satellite-observations/farm/${farmId}/latest`
                        );

                    if (!cancelled) {

                        setSatelliteObservation(
                            response || null
                        );
                    }

                } catch (requestError) {

                    console.error(
                        "Latest Sentinel-2 observation could not be loaded:",
                        requestError
                    );

                    if (!cancelled) {

                        setSatelliteObservation(null);

                        setSatelliteError(
                            requestError?.message ||
                            (
                                isHindi
                                    ? "वास्तविक सैटेलाइट डेटा अभी उपलब्ध नहीं है।"
                                    : "Real satellite data is not currently available."
                            )
                        );
                    }

                } finally {

                    if (!cancelled) {
                        setSatelliteLoading(false);
                    }
                }
            };

        loadLatestSatellite();

        return () => {
            cancelled = true;
        };

    }, [
        isHindi,
    ]);


    /* =====================================================
       ACTIVE RISKS
    ===================================================== */

    const activeRisks =
        useMemo(() => {

            return [
                ...(radar?.activeRisks ?? [])
            ].sort(
                (a, b) =>
                    Number(
                        b?.riskScore ?? 0
                    ) -
                    Number(
                        a?.riskScore ?? 0
                    )
            );

        }, [
            radar,
        ]);


    /* =====================================================
       ALERTS
    ===================================================== */

    const unreadAlerts =
        radar?.unreadAlerts ??
        [];


    /* =====================================================
       COMBINED RISK
    ===================================================== */

    const combinedRisk =
        radar?.combinedRisk ??
        null;


    /* =====================================================
       WEATHER
    ===================================================== */

    const latestWeather =
        radar?.latestWeather ??
        null;


    /* =====================================================
       SATELLITE
    ===================================================== */

    /*
     * Use only the real SatelliteObservation produced by
     * the Copernicus Sentinel-2 backend integration.
     *
     * There is intentionally no fallback to the older
     * SatelliteFieldData response, so unavailable data
     * cannot be presented as current satellite telemetry.
     */
    const satellite =
        satelliteObservation;


    /* =====================================================
       SUMMARY
    ===================================================== */

    const summary =
        radar?.summary ??
        null;


    /*
     * Prefer the backend summary score.
     *
     * Fall back to combined risk score.
     *
     * No artificial score is generated.
     */

    const overallScore =
        Number(
            summary?.overallRiskScore ??
            combinedRisk?.combinedRiskScore ??
            0
        );


    const hasOverallScore =
        summary?.overallRiskScore != null ||
        combinedRisk?.combinedRiskScore != null;


    /* =====================================================
       RISK LEVEL
    ===================================================== */

    const riskLevel =
        combinedRisk?.riskLevel ||
        summary?.overallRiskLevel ||
        "UNKNOWN";


    /* =====================================================
       RISK CLASS
    ===================================================== */

    const getRiskClass =
        (level) => {

            switch (
                String(
                    level || ""
                ).toUpperCase()
                ) {

                case "CRITICAL":
                    return "critical";


                case "HIGH":
                    return "high";


                case "MEDIUM":
                    return "medium";


                case "LOW":
                    return "low";


                default:
                    return "unknown";
            }
        };


    /* =====================================================
       RISK ICON
    ===================================================== */

    const getRiskIcon =
        (riskType) => {

            switch (riskType) {

                case "HEAT_STRESS":
                    return "🔥";


                case "DROUGHT":
                    return "☀️";


                case "HIGH_WIND":
                    return "💨";


                case "FROST":
                    return "❄️";


                case "WATERLOGGING":
                    return "🌧️";


                case "PEST_RISK":
                    return "🐛";


                case "DISEASE_RISK":
                    return "🦠";


                default:
                    return "⚠️";
            }
        };


    /* =====================================================
       LEVEL ICON
    ===================================================== */

    const getLevelIcon =
        (level) => {

            switch (
                String(
                    level || ""
                ).toUpperCase()
                ) {

                case "CRITICAL":
                    return "🚨";


                case "HIGH":
                    return "⚠️";


                case "MEDIUM":
                    return "🟠";


                case "LOW":
                    return "🟢";


                default:
                    return "ℹ️";
            }
        };


    /* =====================================================
       RISK LABEL
    ===================================================== */

    const riskLabel =
        (level) => {

            const labels = {

                CRITICAL:
                    isHindi
                        ? "अत्यंत गंभीर"
                        : "Critical",


                HIGH:
                    isHindi
                        ? "उच्च जोखिम"
                        : "High Risk",


                MEDIUM:
                    isHindi
                        ? "मध्यम जोखिम"
                        : "Medium Risk",


                LOW:
                    isHindi
                        ? "कम जोखिम"
                        : "Low Risk",


                UNKNOWN:
                    isHindi
                        ? "डेटा उपलब्ध नहीं"
                        : "Data unavailable",
            };


            return (
                labels[
                    String(
                        level ||
                        "UNKNOWN"
                    ).toUpperCase()
                    ] ||
                level ||
                "—"
            );
        };


    /* =====================================================
       DATE FORMATTER
    ===================================================== */

    const formatDateTime =
        (value) => {

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

                return value;
            }


            return date.toLocaleString(
                isHindi
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


    /* =====================================================
       RISK TYPE FORMATTER
    ===================================================== */

    const formatRiskType =
        (value) => {

            if (!value) {

                return isHindi
                    ? "मौसम जोखिम"
                    : "WEATHER RISK";
            }


            return String(value)
                .replaceAll(
                    "_",
                    " "
                )
                .toUpperCase();
        };


    /* =====================================================
       LULC PERCENTAGE FORMATTER
    ===================================================== */

    /* =====================================================
       SATELLITE OBSERVATION FRESHNESS
    ===================================================== */

    const getSatelliteFreshness =
        (observedAt) => {

            if (!observedAt) {
                return {
                    key: "UNAVAILABLE",
                    label: isHindi
                        ? "डेटा उपलब्ध नहीं"
                        : "Data unavailable",
                    detail: "—",
                };
            }

            const observedDate =
                new Date(observedAt);

            const observedTime =
                observedDate.getTime();

            if (Number.isNaN(observedTime)) {
                return {
                    key: "UNKNOWN",
                    label: isHindi
                        ? "समय उपलब्ध नहीं"
                        : "Observation time unavailable",
                    detail: String(observedAt),
                };
            }

            const ageMilliseconds =
                Math.max(
                    0,
                    Date.now() - observedTime
                );

            const ageHours =
                ageMilliseconds /
                (1000 * 60 * 60);

            const ageDays =
                ageHours / 24;

            let key;
            let label;

            /*
             * Sentinel-2 is real Earth-observation data and is not
             * continuous live telemetry.
             *
             * Recent: <= 7 days
             * Older:  > 7 and <= 30 days
             * Stale:  > 30 days
             */
            if (ageDays <= 7) {
                key = "RECENT";
                label = isHindi
                    ? "हालिया अवलोकन"
                    : "Recent observation";
            } else if (ageDays <= 30) {
                key = "OLDER";
                label = isHindi
                    ? "पुराना अवलोकन"
                    : "Older observation";
            } else {
                key = "STALE";
                label = isHindi
                    ? "बहुत पुराना अवलोकन"
                    : "Stale observation";
            }

            let detail;

            if (ageHours < 1) {
                detail = isHindi
                    ? "1 घंटे से कम पुराना"
                    : "Less than 1 hour old";
            } else if (ageHours < 24) {
                detail = isHindi
                    ? `${Math.floor(ageHours)} घंटे पुराना`
                    : `${Math.floor(ageHours)} hour${Math.floor(ageHours) === 1 ? "" : "s"} old`;
            } else {
                const roundedDays =
                    Math.floor(ageDays);

                detail = isHindi
                    ? `${roundedDays} दिन पुराना`
                    : `${roundedDays} day${roundedDays === 1 ? "" : "s"} old`;
            }

            return {
                key,
                label,
                detail,
            };
        };


    const satelliteFreshness =
        getSatelliteFreshness(
            satellite?.observedAt
        );


    /* =====================================================
       LULC PERCENTAGE FORMATTER
    ===================================================== */

    const formatPercent =
        (value) => {

            if (
                value === null ||
                value === undefined ||
                Number.isNaN(
                    Number(value)
                )
            ) {

                return "—";
            }


            return `${Number(
                value
            ).toFixed(1)}%`;
        };


    /* =====================================================
       DATA STATE
    ===================================================== */

    const dataState = {

        weather:
            Boolean(
                latestWeather
            ),

        satellite:
            Boolean(
                satellite
            ),

        combined:
            Boolean(
                combinedRisk
            ),

        risks:
            activeRisks.length >
            0,

        alerts:
            unreadAlerts.length >
            0,
    };


    const availableCount =
        Object.values(
            dataState
        ).filter(Boolean)
            .length;


    /* =====================================================
       RISK GAUGE
    ===================================================== */

    const circumference =
        2 *
        Math.PI *
        48;


    const normalizedScore =
        Math.min(
            Math.max(
                overallScore,
                0
            ),
            100
        );


    const scoreOffset =
        circumference -
        (
            normalizedScore /
            100
        ) *
        circumference;


    /* =====================================================
       LULC SYNCHRONIZATION
    ===================================================== */

    const handleLulcSync =
        async () => {

            const farmId =
                getCurrentFarmId();


            if (!farmId) {

                setLulcError(
                    isHindi
                        ? "कृपया पहले एक खेत चुनें।"
                        : "Please select a farm first."
                );

                return;
            }


            setLulcSyncing(
                true
            );

            setLulcError("");


            try {

                /*
                 * This POST triggers the actual backend
                 * Copernicus LULC synchronization.
                 */

                const response =
                    await syncLulc(
                        farmId
                    );


                if (response) {

                    setLulc(
                        response
                    );

                } else {

                    /*
                     * If the backend does not return a body,
                     * retrieve the latest stored observation.
                     */

                    const latest =
                        await getLatestLulc(
                            farmId
                        );


                    setLulc(
                        latest
                    );
                }

            } catch (requestError) {

                console.error(
                    "LULC synchronization failed:",
                    requestError
                );


                setLulcError(
                    requestError?.message ||
                    (
                        isHindi
                            ? "भूमि आवरण डेटा सिंक नहीं हो सका।"
                            : "Land cover data synchronization failed."
                    )
                );

            } finally {

                setLulcSyncing(
                    false
                );
            }
        };


    /* =====================================================
       REAL SENTINEL-2 SYNCHRONIZATION
    ===================================================== */

    const handleSatelliteSync =
        async () => {

            const farmId =
                getCurrentFarmId();

            if (!farmId) {

                setSatelliteError(
                    isHindi
                        ? "कृपया पहले एक खेत चुनें।"
                        : "Please select a farm first."
                );

                return;
            }

            setSatelliteSyncing(true);
            setSatelliteError("");

            try {

                /*
                 * Trigger the real backend Copernicus
                 * Sentinel-2 synchronization.
                 *
                 * No satellite values are generated
                 * by the frontend.
                 */

                const response =
                    await apiRequest(
                        `/api/satellite-observations/farm/${farmId}/sync`,
                        {
                            method: "POST",
                        }
                    );

                if (response) {

                    setSatelliteObservation(
                        response
                    );

                } else {

                    const latest =
                        await apiRequest(
                            `/api/satellite-observations/farm/${farmId}/latest`
                        );

                    setSatelliteObservation(
                        latest || null
                    );
                }

            } catch (requestError) {

                console.error(
                    "Sentinel-2 synchronization failed:",
                    requestError
                );

                setSatelliteObservation(null);

                setSatelliteError(
                    requestError?.message ||
                    (
                        isHindi
                            ? "वास्तविक Sentinel-2 डेटा सिंक नहीं हो सका।"
                            : "Real Sentinel-2 data could not be synchronized."
                    )
                );

            } finally {

                setSatelliteSyncing(false);
            }
        };


    /* =====================================================
       LOADING SCREEN
    ===================================================== */

    if (loading) {

        return (

            <div className="weather-radar-page">

                <div className="weather-radar-loading-card">

                    <div className="radar-loader">

                        <span>
                            🌦️
                        </span>

                    </div>


                    <p className="eyebrow">

                        AGRINEXUS • FIELD INTELLIGENCE

                    </p>


                    <h2>

                        {isHindi
                            ? "मौसम जोखिम रडार तैयार हो रहा है…"
                            : "Preparing Weather Risk Radar…"}

                    </h2>


                    <p>

                        {isHindi
                            ? "आपके खेत का नवीनतम मौसम और जोखिम डेटा प्राप्त किया जा रहा है।"
                            : "Fetching the latest weather and risk data for your selected farm."}

                    </p>


                    <div className="loading-line" />

                </div>


                <BottomNavBar />

            </div>
        );
    }


    /* =====================================================
       ERROR SCREEN
    ===================================================== */

    if (
        error ||
        !radar
    ) {

        return (

            <div className="weather-radar-page">

                <header className="weather-radar-topbar">

                    <button
                        className="icon-button"
                        type="button"
                        onClick={() =>
                            navigate(
                                "/home"
                            )
                        }
                        aria-label="Back"
                    >
                        ←
                    </button>


                    <div>

                        <p className="eyebrow">

                            AGRINEXUS

                        </p>


                        <h1>

                            {isHindi
                                ? "मौसम जोखिम रडार"
                                : "Weather Risk Radar"}

                        </h1>

                    </div>

                </header>


                <section className="radar-state-card error-state">

                    <div className="state-icon">

                        ⚠️

                    </div>


                    <h2>

                        {isHindi
                            ? "डेटा उपलब्ध नहीं"
                            : "Data unavailable"}

                    </h2>


                    <p>

                        {error ||
                            (
                                isHindi
                                    ? "मौसम जोखिम डेटा प्राप्त नहीं हुआ।"
                                    : "Weather risk data was not received."
                            )}

                    </p>


                    <button
                        className="primary-button"
                        type="button"
                        onClick={() =>
                            loadRadar({
                                includeLiveWeather: true,
                            })
                        }
                    >

                        {isHindi
                            ? "पुनः प्रयास करें"
                            : "Try again"}

                    </button>

                </section>


                <BottomNavBar />

            </div>
        );
    }


    /* =====================================================
       MAIN WEATHER RADAR PAGE
    ===================================================== */

    return (

        <div className="weather-radar-page">

            {/* =================================================
                TOP BAR
            ================================================= */}

            <header className="weather-radar-topbar">

                <button
                    className="icon-button"
                    type="button"
                    onClick={() =>
                        navigate(
                            "/home"
                        )
                    }
                    aria-label="Back"
                >
                    ←
                </button>


                <div className="topbar-copy">

                    <p className="eyebrow">

                        AGRINEXUS • WEATHER INTELLIGENCE

                    </p>


                    <h1>

                        {isHindi
                            ? "मौसम जोखिम रडार"
                            : "Weather Risk Radar"}

                    </h1>


                    <p className="farm-location">

                        {radar.farmName ||
                            (
                                isHindi
                                    ? "चयनित खेत"
                                    : "Selected Farm"
                            )}


                        {radar.location
                            ? ` • ${radar.location}`
                            : ""}

                    </p>

                </div>


                <button
                    className={`icon-button refresh-button ${
                        refreshing
                            ? "is-refreshing"
                            : ""
                    }`}
                    type="button"
                    onClick={() =>
                        loadRadar({
                            silent: true,
                        })
                    }
                    disabled={
                        refreshing
                    }
                    aria-label="Refresh radar"
                >

                    ↻

                </button>

            </header>


            {/* =================================================
                HERO
            ================================================= */}

            <section
                className={`radar-hero ${getRiskClass(
                    riskLevel
                )}`}
            >

                <div className="hero-copy">

                    <div className="live-pill">

                        <span />

                        {isHindi
                            ? "लाइव खेत स्थिति"
                            : "LIVE FIELD STATUS"}

                    </div>


                    <p className="hero-kicker">

                        {isHindi
                            ? "कुल मौसम जोखिम"
                            : "OVERALL WEATHER RISK"}

                    </p>


                    <h2>

                        {combinedRisk?.title ||
                            riskLabel(
                                riskLevel
                            )}

                    </h2>


                    <p className="hero-description">

                        {combinedRisk?.recommendation ||
                            activeRisks[0]
                                ?.recommendation ||
                            (
                                isHindi
                                    ? "उपलब्ध खेत डेटा के आधार पर स्थिति की निगरानी जारी रखें।"
                                    : "Continue monitoring the field using the available farm data."
                            )}

                    </p>


                    <div className="hero-meta">

                        <span>

                            {getLevelIcon(
                                riskLevel
                            )}{" "}

                            {riskLabel(
                                riskLevel
                            )}

                        </span>


                        <span>

                            {activeRisks.length}{" "}

                            {isHindi
                                ? "सक्रिय जोखिम"
                                : "active risks"}

                        </span>

                    </div>

                </div>


                {/* =================================================
                    SCORE GAUGE
                ================================================= */}

                <div
                    className="risk-gauge"
                    aria-label={`Risk score ${
                        hasOverallScore
                            ? overallScore
                            : "unavailable"
                    }`}
                >

                    <svg
                        viewBox="0 0 120 120"
                        role="img"
                        aria-hidden="true"
                    >

                        <circle
                            className="gauge-track"
                            cx="60"
                            cy="60"
                            r="48"
                        />


                        {hasOverallScore && (

                            <circle
                                className="gauge-value"
                                cx="60"
                                cy="60"
                                r="48"
                                strokeDasharray={
                                    circumference
                                }
                                strokeDashoffset={
                                    scoreOffset
                                }
                            />

                        )}

                    </svg>


                    <div className="gauge-content">

                        <strong>

                            {hasOverallScore
                                ? Math.round(
                                    overallScore
                                )
                                : "—"}

                        </strong>


                        <span>

                            {isHindi
                                ? "स्कोर"
                                : "SCORE"}

                        </span>

                    </div>

                </div>

            </section>


            {/* =================================================
                SUMMARY
            ================================================= */}

            <section className="data-strip">

                <div>

                    <strong>

                        {availableCount}/5

                    </strong>


                    <span>

                        {isHindi
                            ? "डेटा स्रोत उपलब्ध"
                            : "data sources available"}

                    </span>

                </div>


                <div>

                    <strong>

                        {summary
                                ?.activeWeatherRiskCount ??
                            activeRisks.length}

                    </strong>


                    <span>

                        {isHindi
                            ? "सक्रिय जोखिम"
                            : "active risks"}

                    </span>

                </div>


                <div>

                    <strong>

                        {summary
                                ?.unreadAlertCount ??
                            unreadAlerts.length}

                    </strong>


                    <span>

                        {isHindi
                            ? "नए अलर्ट"
                            : "new alerts"}

                    </span>

                </div>

            </section>


            {/* =================================================
                CURRENT WEATHER
            ================================================= */}

            <section className="weather-radar-section">

                <div className="section-heading">

                    <div>

                        <p className="eyebrow">

                            LIVE CONDITIONS

                        </p>


                        <h2>

                            {isHindi
                                ? "वर्तमान मौसम"
                                : "Current Weather"}

                        </h2>

                    </div>


                    {latestWeather?.recordedAt && (

                        <span className="updated-label">

                            {formatDateTime(
                                latestWeather.recordedAt
                            )}

                        </span>

                    )}

                </div>


                <div className="weather-metrics-grid">

                    {/* Temperature */}

                    <div className="metric-card featured">

                        <span>
                            🌡️
                        </span>


                        <div>

                            <small>

                                {isHindi
                                    ? "तापमान"
                                    : "Temperature"}

                            </small>


                            <strong>

                                {latestWeather
                                        ?.temperature ??
                                    "—"}


                                {latestWeather
                                    ?.temperature !=
                                null
                                    ? "°C"
                                    : ""}

                            </strong>

                        </div>

                    </div>


                    {/* Humidity */}

                    <div className="metric-card">

                        <span>
                            💧
                        </span>


                        <div>

                            <small>

                                {isHindi
                                    ? "आर्द्रता"
                                    : "Humidity"}

                            </small>


                            <strong>

                                {latestWeather
                                        ?.humidity ??
                                    "—"}


                                {latestWeather
                                    ?.humidity !=
                                null
                                    ? "%"
                                    : ""}

                            </strong>

                        </div>

                    </div>


                    {/* Rainfall */}

                    <div className="metric-card">

                        <span>
                            🌧️
                        </span>


                        <div>

                            <small>

                                {isHindi
                                    ? "वर्षा"
                                    : "Rainfall"}

                            </small>


                            <strong>

                                {latestWeather
                                        ?.rainfall ??
                                    "—"}


                                {latestWeather
                                    ?.rainfall !=
                                null
                                    ? " mm"
                                    : ""}

                            </strong>

                        </div>

                    </div>


                    {/* Wind */}

                    <div className="metric-card">

                        <span>
                            💨
                        </span>


                        <div>

                            <small>

                                {isHindi
                                    ? "हवा"
                                    : "Wind"}

                            </small>


                            <strong>

                                {latestWeather
                                        ?.windSpeed ??
                                    "—"}


                                {latestWeather
                                    ?.windSpeed !=
                                null
                                    ? " km/h"
                                    : ""}

                            </strong>

                        </div>

                    </div>


                    {/* Feels Like */}

                    <div className="metric-card">

                        <span>
                            🌡️
                        </span>


                        <div>

                            <small>

                                {isHindi
                                    ? "महसूस तापमान"
                                    : "Feels like"}

                            </small>


                            <strong>

                                {latestWeather
                                        ?.feelsLike ??
                                    "—"}


                                {latestWeather
                                    ?.feelsLike !=
                                null
                                    ? "°C"
                                    : ""}

                            </strong>

                        </div>

                    </div>


                    {/* Pressure */}

                    <div className="metric-card">

                        <span>
                            ⏱️
                        </span>


                        <div>

                            <small>

                                {isHindi
                                    ? "दबाव"
                                    : "Pressure"}

                            </small>


                            <strong>

                                {latestWeather
                                        ?.pressure ??
                                    "—"}


                                {latestWeather
                                    ?.pressure !=
                                null
                                    ? " hPa"
                                    : ""}

                            </strong>

                        </div>

                    </div>

                </div>


                {/* Weather condition */}

                <div className="condition-banner">

                    <div className="condition-icon">

                        {latestWeather
                            ? "⛅"
                            : "ℹ️"}

                    </div>


                    <div>

                        <small>

                            {isHindi
                                ? "मौसम स्थिति"
                                : "WEATHER CONDITION"}

                        </small>


                        <strong>

                            {latestWeather
                                    ?.weatherCondition ||
                                "—"}

                        </strong>


                        <p>

                            {latestWeather
                                    ?.weatherDescription ||
                                (
                                    isHindi
                                        ? "मौसम विवरण उपलब्ध नहीं है।"
                                        : "No weather description is available."
                                )}

                        </p>

                    </div>

                </div>

            </section>


            {/* =================================================
                ACTIVE RISKS
            ================================================= */}

            <section className="weather-radar-section">

                <div className="section-heading">

                    <div>

                        <p className="eyebrow">

                            EARLY WARNING

                        </p>


                        <h2>

                            {isHindi
                                ? "सक्रिय जोखिम"
                                : "Active Risks"}

                        </h2>

                    </div>


                    <span className="count-badge">

                        {activeRisks.length}

                    </span>

                </div>


                {activeRisks.length === 0 ? (

                    <div className="empty-card">

                        <span>
                            ✅
                        </span>


                        <div>

                            <strong>

                                {isHindi
                                    ? "कोई सक्रिय मौसम जोखिम नहीं"
                                    : "No active weather risks"}

                            </strong>


                            <p>

                                {isHindi
                                    ? "वर्तमान उपलब्ध डेटा में कोई सक्रिय जोखिम नहीं मिला।"
                                    : "No active weather risk was detected in the currently available data."}

                            </p>

                        </div>

                    </div>

                ) : (

                    <div className="risk-list">

                        {activeRisks.map(
                            (
                                risk,
                                index
                            ) => {

                                const levelClass =
                                    getRiskClass(
                                        risk.riskLevel
                                    );


                                return (

                                    <article
                                        className={`risk-card ${levelClass}`}
                                        key={
                                            risk.id ||
                                            `${risk.riskType}-${index}`
                                        }
                                    >

                                        <div className="risk-icon">

                                            {getRiskIcon(
                                                risk.riskType
                                            )}

                                        </div>


                                        <div className="risk-main">

                                            <div className="risk-topline">

                                                <span>

                                                    {formatRiskType(
                                                        risk.riskType
                                                    )}

                                                </span>


                                                <b>

                                                    {risk.riskLevel ||
                                                        "—"}

                                                </b>

                                            </div>


                                            <h3>

                                                {risk.title ||
                                                    (
                                                        isHindi
                                                            ? "मौसम जोखिम"
                                                            : "Weather Risk"
                                                    )}

                                            </h3>


                                            <p>

                                                {risk.recommendation ||
                                                    risk.description ||
                                                    (
                                                        isHindi
                                                            ? "कोई अतिरिक्त सिफारिश उपलब्ध नहीं है।"
                                                            : "No additional recommendation is available."
                                                    )}

                                            </p>


                                            {risk.validUntil && (

                                                <small>

                                                    {isHindi
                                                        ? "मान्य जब तक"
                                                        : "Valid until"}

                                                    :{" "}

                                                    {formatDateTime(
                                                        risk.validUntil
                                                    )}

                                                </small>

                                            )}

                                        </div>


                                        <div className="risk-score">

                                            <strong>

                                                {risk.riskScore ??
                                                    "—"}

                                            </strong>


                                            <span>
                                                /100
                                            </span>

                                        </div>

                                    </article>
                                );
                            }
                        )}

                    </div>
                )}

            </section>


            {/* =================================================
                SATELLITE FIELD SIGNALS
            ================================================= */}

            <section className="weather-radar-section">

                <div className="section-heading">

                    <div>

                        <p className="eyebrow">
                            REMOTE SENSING
                        </p>

                        <h2>
                            {isHindi
                                ? "सैटेलाइट फील्ड सिग्नल"
                                : "Satellite Field Signals"}
                        </h2>

                    </div>

                    <div
                        style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "10px",
                            flexWrap: "wrap",
                            justifyContent: "flex-end",
                        }}
                    >

                        <span
                            className={`source-status ${
                                satellite
                                    ? "available"
                                    : "missing"
                            }`}
                        >
                            {satellite
                                ? `● ${satelliteFreshness.key}`
                                : "— UNAVAILABLE"}
                        </span>

                        <button
                            type="button"
                            className="lulc-sync-button"
                            onClick={handleSatelliteSync}
                            disabled={
                                satelliteSyncing ||
                                satelliteLoading
                            }
                        >
                            {satelliteSyncing
                                ? (
                                    isHindi
                                        ? "सिंक हो रहा है..."
                                        : "Syncing Sentinel-2..."
                                )
                                : (
                                    isHindi
                                        ? "↻ Sentinel-2 Sync"
                                        : "↻ Sync Sentinel-2"
                                )}
                        </button>

                    </div>

                </div>


                {satelliteLoading ? (

                    <div className="empty-card satellite-empty">

                        <span>
                            🛰️
                        </span>

                        <div>

                            <strong>
                                {isHindi
                                    ? "सैटेलाइट डेटा जाँचा जा रहा है..."
                                    : "Checking Sentinel-2 data..."}
                            </strong>

                            <p>
                                {isHindi
                                    ? "खेत के लिए उपलब्ध वास्तविक सैटेलाइट रिकॉर्ड प्राप्त किया जा रहा है।"
                                    : "Fetching the latest real satellite observation available for this farm."}
                            </p>

                        </div>

                    </div>

                ) : satellite ? (

                    <>

                        <div className="weather-metrics-grid satellite-grid">

                            <div className="metric-card">

                                <span>
                                    🛰️
                                </span>

                                <div>

                                    <small>
                                        NDVI
                                    </small>

                                    <strong>
                                        {satellite.ndvi != null
                                            ? Number(
                                                satellite.ndvi
                                            ).toFixed(3)
                                            : "—"}
                                    </strong>

                                </div>

                            </div>


                            <div className="metric-card">

                                <span>
                                    💧
                                </span>

                                <div>

                                    <small>
                                        NDWI
                                    </small>

                                    <strong>
                                        {satellite.ndwi != null
                                            ? Number(
                                                satellite.ndwi
                                            ).toFixed(3)
                                            : "—"}
                                    </strong>

                                </div>

                            </div>


                            <div className="metric-card">

                                <span>
                                    ☁️
                                </span>

                                <div>

                                    <small>
                                        {isHindi
                                            ? "बादल"
                                            : "Cloud cover"}
                                    </small>

                                    <strong>
                                        {satellite.cloudCoverPercent != null
                                            ? `${Number(
                                                satellite.cloudCoverPercent
                                            ).toFixed(1)}%`
                                            : "—"}
                                    </strong>

                                </div>

                            </div>


                            <div className="metric-card">

                                <span>
                                    🎯
                                </span>

                                <div>

                                    <small>
                                        {isHindi
                                            ? "विश्वसनीयता"
                                            : "Confidence"}
                                    </small>

                                    <strong>
                                        {satellite.confidence != null
                                            ? `${Number(
                                                satellite.confidence
                                            ).toFixed(1)}%`
                                            : "—"}
                                    </strong>

                                </div>

                            </div>

                        </div>


                        <div className="insight-card">
                            <span>
                                🕒
                            </span>

                            <div>
                                <small>
                                    {isHindi
                                        ? "अवलोकन ताज़गी"
                                        : "OBSERVATION FRESHNESS"}
                                </small>

                                <strong>
                                    {satelliteFreshness.label}
                                </strong>

                                <p>
                                    {satelliteFreshness.detail}
                                    {satellite.observedAt
                                        ? ` • ${formatDateTime(
                                            satellite.observedAt
                                        )}`
                                        : ""}
                                </p>

                                <small>
                                    {isHindi
                                        ? "Sentinel-2 एक पुनरावर्ती पृथ्वी-अवलोकन उपग्रह है; यह लगातार लाइव सेंसर स्ट्रीम नहीं है।"
                                        : "Sentinel-2 is periodic Earth-observation data, not a continuous live sensor stream."}
                                </small>
                            </div>
                        </div>


                        <div className="insight-card">

                            <span>
                                🌿
                            </span>

                            <div>

                                <small>
                                    {isHindi
                                        ? "फसल स्थिति"
                                        : "CROP CONDITION"}
                                </small>

                                <strong>
                                    {satellite.cropCondition ||
                                        "—"}
                                </strong>

                                <p>
                                    {satellite.interpretation ||
                                        (
                                            isHindi
                                                ? "सैटेलाइट व्याख्या उपलब्ध नहीं है।"
                                                : "No satellite interpretation is available."
                                        )}
                                </p>

                            </div>

                        </div>


                        <div className="data-strip">

                            <div>

                                <strong>
                                    {satellite.changeStatus ||
                                        "—"}
                                </strong>

                                <span>
                                    {isHindi
                                        ? "परिवर्तन स्थिति"
                                        : "change status"}
                                </span>

                            </div>


                            <div>

                                <strong>
                                    {satellite.changePercent != null
                                        ? `${Number(
                                            satellite.changePercent
                                        ).toFixed(2)}%`
                                        : "—"}
                                </strong>

                                <span>
                                    {isHindi
                                        ? "NDVI परिवर्तन"
                                        : "NDVI change"}
                                </span>

                            </div>


                            <div>

                                <strong>
                                    {satellite.dataStatus ||
                                        "—"}
                                </strong>

                                <span>
                                    {isHindi
                                        ? "डेटा स्थिति"
                                        : "data status"}
                                </span>

                            </div>

                        </div>


                        <div className="insight-card">

                            <span>
                                🛰️
                            </span>

                            <div>

                                <small>
                                    {isHindi
                                        ? "स्रोत"
                                        : "DATA SOURCE"}
                                </small>

                                <strong>
                                    {satellite.dataSource ||
                                        "—"}
                                </strong>

                                <p>
                                    {satellite.satelliteSource ||
                                        "—"}

                                    {satellite.sceneId
                                        ? ` • ${satellite.sceneId}`
                                        : ""}

                                    {satellite.observedAt
                                        ? ` • ${formatDateTime(
                                            satellite.observedAt
                                        )}`
                                        : ""}
                                </p>

                            </div>

                        </div>


                        {satellite.recommendation && (

                            <div className="insight-card">

                                <span>
                                    💡
                                </span>

                                <div>

                                    <small>
                                        {isHindi
                                            ? "सैटेलाइट संकेत"
                                            : "SATELLITE GUIDANCE"}
                                    </small>

                                    <p>
                                        {satellite.recommendation}
                                    </p>

                                </div>

                            </div>

                        )}


                        {satellite.uncertaintyNote && (

                            <div className="lulc-note">

                                <strong>
                                    ℹ️{" "}
                                    {isHindi
                                        ? "डेटा अनिश्चितता"
                                        : "Data uncertainty"}
                                </strong>

                                <p>
                                    {satellite.uncertaintyNote}
                                </p>

                            </div>

                        )}

                    </>

                ) : (

                    <div className="empty-card satellite-empty">

                        <span>
                            🛰️
                        </span>

                        <div>

                            <strong>
                                {isHindi
                                    ? "वास्तविक सैटेलाइट रिकॉर्ड उपलब्ध नहीं"
                                    : "Real satellite record unavailable"}
                            </strong>

                            <p>
                                {satelliteError ||
                                    (
                                        isHindi
                                            ? "इस खेत के लिए वर्तमान Sentinel-2 रिकॉर्ड उपलब्ध नहीं है। Sync Sentinel-2 दबाकर वास्तविक Copernicus डेटा प्राप्त करें।"
                                            : "No current Sentinel-2 record is available for this farm. Use Sync Sentinel-2 to request real Copernicus data."
                                    )}
                            </p>

                        </div>

                    </div>

                )}

            </section>


            {/* =================================================
                LAND COVER MAPPING / LULC
            ================================================= */}

            <section className="weather-radar-section">

                <div className="section-heading">

                    <div>

                        <p className="eyebrow">

                            {isHindi
                                ? "उपग्रह मानचित्रण"
                                : "SPACE INTELLIGENCE"}

                        </p>


                        <h2>

                            {isHindi
                                ? "भूमि आवरण मानचित्रण"
                                : "Land Cover Mapping"}

                        </h2>

                    </div>


                    <button
                        type="button"
                        className="lulc-sync-button"
                        onClick={
                            handleLulcSync
                        }
                        disabled={
                            lulcSyncing ||
                            lulcLoading
                        }
                    >

                        {lulcSyncing

                            ? (
                                isHindi
                                    ? "सिंक हो रहा है..."
                                    : "Syncing..."
                            )

                            : (
                                isHindi
                                    ? "↻ ताज़ा करें"
                                    : "↻ Refresh"
                            )}

                    </button>

                </div>


                {/* =================================================
                    LULC LOADING
                ================================================= */}

                {lulcLoading ? (

                    <div className="lulc-status-card">

                        <div className="lulc-status-icon">

                            🛰️

                        </div>


                        <div>

                            <strong>

                                {isHindi
                                    ? "भूमि आवरण डेटा जाँचा जा रहा है..."
                                    : "Checking land cover data..."}

                            </strong>


                            <p>

                                {isHindi
                                    ? "खेत के लिए उपलब्ध नवीनतम LULC रिकॉर्ड प्राप्त किए जा रहे हैं।"
                                    : "Fetching the latest available LULC record for this farm."}

                            </p>

                        </div>

                    </div>

                ) : lulc ? (

                    <>

                        {/* =================================================
                            LULC SOURCE
                        ================================================= */}

                        <div className="lulc-source-card">

                            <div className="lulc-source-icon">

                                🌍

                            </div>


                            <div>

                                <strong>

                                    {lulc.productName ||
                                        "Land Cover Map at 10m - Annual V1"}

                                </strong>


                                <p>

                                    {isHindi

                                        ? `आधार वर्ष: ${
                                            lulc.baseYear ??
                                            "—"
                                        } • रिज़ॉल्यूशन: ${
                                            lulc.spatialResolutionMeters ??
                                            "—"
                                        } m`

                                        : `Base year: ${
                                            lulc.baseYear ??
                                            "—"
                                        } • Resolution: ${
                                            lulc.spatialResolutionMeters ??
                                            "—"
                                        } m`}

                                </p>

                            </div>


                            <span className="lulc-source-badge">

                                {lulc.dataSource ||
                                    "COPERNICUS_CLMS"}

                            </span>

                        </div>


                        {/* =================================================
                            LULC METRICS
                        ================================================= */}

                        <div className="lulc-grid">

                            {/* Cropland */}

                            <div className="lulc-metric-card">

                                <span>

                                    🌾{" "}

                                    {isHindi
                                        ? "फसल भूमि"
                                        : "Cropland"}

                                </span>


                                <strong>

                                    {formatPercent(
                                        lulc.croplandPercent
                                    )}

                                </strong>

                            </div>


                            {/* Tree Cover */}

                            <div className="lulc-metric-card">

                                <span>

                                    🌳{" "}

                                    {isHindi
                                        ? "वृक्ष आवरण"
                                        : "Tree Cover"}

                                </span>


                                <strong>

                                    {formatPercent(
                                        lulc.treeCoverPercent
                                    )}

                                </strong>

                            </div>


                            {/* Grassland */}

                            <div className="lulc-metric-card">

                                <span>

                                    🌿{" "}

                                    {isHindi
                                        ? "घासभूमि"
                                        : "Grassland"}

                                </span>


                                <strong>

                                    {formatPercent(
                                        lulc.grasslandPercent
                                    )}

                                </strong>

                            </div>


                            {/* Water */}

                            <div className="lulc-metric-card">

                                <span>

                                    💧{" "}

                                    {isHindi
                                        ? "जल क्षेत्र"
                                        : "Water"}

                                </span>


                                <strong>

                                    {formatPercent(
                                        lulc.waterPercent
                                    )}

                                </strong>

                            </div>


                            {/* Built Up */}

                            <div className="lulc-metric-card">

                                <span>

                                    🏠{" "}

                                    {isHindi
                                        ? "निर्मित क्षेत्र"
                                        : "Built-up"}

                                </span>


                                <strong>

                                    {formatPercent(
                                        lulc.builtUpPercent
                                    )}

                                </strong>

                            </div>


                            {/* Natural Vegetation */}

                            <div className="lulc-metric-card">

                                <span>

                                    🌱{" "}

                                    {isHindi
                                        ? "प्राकृतिक वनस्पति"
                                        : "Natural Vegetation"}

                                </span>


                                <strong>

                                    {formatPercent(
                                        lulc.naturalVegetationPercent
                                    )}

                                </strong>

                            </div>

                        </div>


                        {/* =================================================
                            LULC SUMMARY
                        ================================================= */}

                        <div className="lulc-summary-card">

                            <div>

                                <span>

                                    {isHindi
                                        ? "प्रमुख भूमि आवरण"
                                        : "Dominant Land Cover"}

                                </span>


                                <strong>

                                    {lulc.dominantClass ||
                                        "—"}

                                </strong>

                            </div>


                            <div>

                                <span>

                                    {isHindi
                                        ? "वर्गीकृत कवरेज"
                                        : "Classified Coverage"}

                                </span>


                                <strong>

                                    {formatPercent(
                                        lulc.classifiedCoveragePercent
                                    )}

                                </strong>

                            </div>

                        </div>


                        {/* =================================================
                            LULC DATA CONTEXT
                        ================================================= */}

                        <div className="lulc-note">

                            <strong>

                                ℹ️{" "}

                                {isHindi
                                    ? "डेटा संदर्भ"
                                    : "Data context"}

                            </strong>


                            <p>

                                {lulc.analysisNote ||

                                    (

                                        isHindi

                                            ? "यह LULC परत आधार-वर्ष डेटा है और इसे वर्तमान 2026 भूमि आवरण के रूप में नहीं समझना चाहिए।"

                                            : "This LULC layer is base-year data and should not be interpreted as live 2026 land cover."

                                    )}

                            </p>

                        </div>

                    </>

                ) : (

                    /* =================================================
                       NO LULC DATA
                    ================================================= */

                    <div className="lulc-status-card">

                        <div className="lulc-status-icon">

                            🛰️

                        </div>


                        <div>

                            <strong>

                                {isHindi
                                    ? "अभी LULC रिकॉर्ड उपलब्ध नहीं है"
                                    : "No LULC record available yet"}

                            </strong>


                            <p>

                                {lulcError ||

                                    (

                                        isHindi

                                            ? "Refresh दबाकर Copernicus LULC डेटा सिंक करें।"

                                            : "Click Refresh to synchronize Copernicus LULC data."

                                    )}

                            </p>

                        </div>

                    </div>

                )}

            </section>


            {/* =================================================
                IMPORTANT ALERTS
            ================================================= */}

            <section className="weather-radar-section">

                <div className="section-heading">

                    <div>

                        <p className="eyebrow">

                            FARMER NOTIFICATIONS

                        </p>


                        <h2>

                            {isHindi
                                ? "महत्वपूर्ण अलर्ट"
                                : "Important Alerts"}

                        </h2>

                    </div>


                    <span className="count-badge">

                        {unreadAlerts.length}

                    </span>

                </div>


                {unreadAlerts.length === 0 ? (

                    <div className="empty-card">

                        <span>
                            🔔
                        </span>


                        <div>

                            <strong>

                                {isHindi
                                    ? "कोई नया अलर्ट नहीं"
                                    : "No new alerts"}

                            </strong>


                            <p>

                                {isHindi
                                    ? "आपके खेत के लिए कोई अपठित अलर्ट उपलब्ध नहीं है।"
                                    : "There are no unread alerts available for your farm."}

                            </p>

                        </div>

                    </div>

                ) : (

                    <div className="alert-list">

                        {unreadAlerts.map(
                            (
                                alert,
                                index
                            ) => {

                                const levelClass =
                                    getRiskClass(
                                        alert.riskLevel
                                    );


                                return (

                                    <article
                                        className={`alert-card ${levelClass}`}
                                        key={
                                            alert.id ||
                                            `alert-${index}`
                                        }
                                    >

                                        <span className="alert-icon">

                                            {getLevelIcon(
                                                alert.riskLevel
                                            )}

                                        </span>


                                        <div>

                                            <span className="alert-level">

                                                {alert.riskLevel ||
                                                    "—"}

                                            </span>


                                            <h3>

                                                {alert.title ||
                                                    (
                                                        isHindi
                                                            ? "मौसम अलर्ट"
                                                            : "Weather Alert"
                                                    )}

                                            </h3>


                                            <p>

                                                {alert.message ||
                                                    alert.description ||
                                                    "—"}

                                            </p>

                                        </div>

                                    </article>
                                );
                            }
                        )}

                    </div>
                )}

            </section>


            {/* =================================================
                TRUST / VERIFIED DATA
            ================================================= */}

            <section className="radar-trust-card">

                <div className="trust-icon">

                    ✓

                </div>


                <div>

                    <strong>

                        {isHindi
                            ? "सत्यापित खेत डेटा"
                            : "Verified farm data"}

                    </strong>


                    <p>

                        {isHindi
                            ? "यह रडार केवल चयनित खेत से प्राप्त वास्तविक backend records दिखाता है। उपलब्ध न होने वाले डेटा को अनुमान से नहीं भरा गया है।"
                            : "This radar displays real backend records for the selected farm. Missing data is never replaced with invented values."}

                    </p>

                </div>


                <span className="trust-badge">

                    REAL DATA

                </span>

            </section>


            {/* =================================================
                DISCLAIMER
            ================================================= */}

            <section className="weather-disclaimer">

                <strong>

                    {isHindi
                        ? "महत्वपूर्ण"
                        : "Decision support"}

                </strong>


                <p>

                    {isHindi
                        ? "मौसम और जोखिम जानकारी निर्णय-सहायता के लिए है। महत्वपूर्ण कृषि निर्णय लेने से पहले खेत की वर्तमान स्थिति और विश्वसनीय स्थानीय मौसम जानकारी की पुष्टि करें।"
                        : "Weather and risk information is decision-support data. Validate important agricultural decisions using current field observations and reliable local weather information."}

                </p>

            </section>


            {/* =================================================
                BOTTOM NAVIGATION
            ================================================= */}

            <BottomNavBar />

        </div>
    );
}


export default WeatherRadar;
