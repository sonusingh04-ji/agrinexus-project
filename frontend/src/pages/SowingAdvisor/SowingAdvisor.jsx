import {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    getCurrentFarmId,
} from "../../api/apiClient";

import {
    getAllCrops,
} from "../../api/cropSoilApi";

import {
    getLatestSowingRecommendation,
    getSowingRecommendation,
} from "../../api/sowingAdvisorApi";

import "./SowingAdvisor.css";


/* =========================================================
   Helpers
   ========================================================= */

function safeNumber(value) {
    return (
        typeof value === "number" &&
        Number.isFinite(value)
    );
}

function formatValue(
    value,
    suffix = "",
    decimals = 1
) {
    if (!safeNumber(value)) {
        return "—";
    }

    return `${value.toFixed(decimals)}${suffix}`;
}

function formatInteger(
    value,
    suffix = ""
) {
    if (
        typeof value !== "number" ||
        !Number.isFinite(value)
    ) {
        return "—";
    }

    return `${Math.round(value)}${suffix}`;
}

function formatPercent(value) {
    if (
        typeof value !== "number" ||
        !Number.isFinite(value)
    ) {
        return "—";
    }

    const normalized =
        value <= 1
            ? value * 100
            : value;

    return `${Math.round(normalized)}%`;
}

function formatDateTime(value) {
    if (!value) {
        return "—";
    }

    const parsed =
        new Date(value);

    if (
        Number.isNaN(
            parsed.getTime()
        )
    ) {
        return String(value);
    }

    return parsed.toLocaleString(
        undefined,
        {
            dateStyle: "medium",
            timeStyle: "short",
        }
    );
}

function splitFactors(value) {
    if (!value) {
        return [];
    }

    return String(value)
        .split(";")
        .map((item) =>
            item.trim()
        )
        .filter(Boolean);
}

function normalizeCropRecords(response) {
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

    return [];
}

function normalizeLatestDecision(
    response
) {
    if (!response) {
        return null;
    }

    if (
        response?.data &&
        !response.decision &&
        typeof response.data === "object"
    ) {
        return response.data;
    }

    return response;
}

function normalizeDecisionStatus(
    decision
) {
    return String(
        decision || ""
    )
        .trim()
        .toUpperCase();
}

function getDecisionMeta(decision) {
    switch (
        normalizeDecisionStatus(
            decision
        )
        ) {
        case "SOW_NOW":
            return {
                label: "Sow Now",
                icon: "🌱",
                tone: "success",
                description:
                    "Current available conditions are compatible with the crop's configured sowing requirements.",
            };

        case "WAIT":
            return {
                label: "Wait",
                icon: "⏳",
                tone: "warning",
                description:
                    "One or more current conditions suggest waiting before sowing.",
            };

        case "UNSUITABLE":
            return {
                label: "Unsuitable",
                icon: "⚠️",
                tone: "danger",
                description:
                    "Available conditions are not suitable for the selected crop.",
            };

        case "INSUFFICIENT_DATA":
            return {
                label: "Insufficient Data",
                icon: "📊",
                tone: "info",
                description:
                    "There is not enough current information to make a reliable sowing decision.",
            };

        default:
            return {
                label: decision || "Unknown",
                icon: "🌾",
                tone: "neutral",
                description:
                    "The backend returned a sowing decision.",
            };
    }
}

function getWindowLabel(windowValue) {
    const value =
        String(
            windowValue || ""
        )
            .trim()
            .toUpperCase();

    const labels = {
        NOW: "Now",
        WAIT_FOR_MOISTURE:
            "Wait for Suitable Moisture",
        WAIT_FOR_WEATHER:
            "Wait for Weather",
        WAIT_FOR_FIELD_CONDITION:
            "Wait for Field Condition",
        WAIT_FOR_TEMPERATURE:
            "Wait for Temperature",
        OUTSIDE_CROP_WINDOW:
            "Outside Crop Window",
        CHECK_SOIL:
            "Check Soil",
        INSUFFICIENT_DATA:
            "Insufficient Data",
        REASSESS:
            "Reassess",
    };

    return (
        labels[value] ||
        windowValue ||
        "—"
    );
}


/* =========================================================
   Component
   ========================================================= */

export default function SowingAdvisor() {

    const [farmId, setFarmId] =
        useState("");

    const [farm, setFarm] =
        useState(null);

    const [crops, setCrops] =
        useState([]);

    const [selectedCropId, setSelectedCropId] =
        useState("");

    const [latestDecision, setLatestDecision] =
        useState(null);

    const [loadingCrops, setLoadingCrops] =
        useState(true);

    const [loadingDecision, setLoadingDecision] =
        useState(true);

    const [generating, setGenerating] =
        useState(false);

    const [loadingFarm, setLoadingFarm] =
        useState(true);

    const [error, setError] =
        useState("");

    const [successMessage, setSuccessMessage] =
        useState("");

    const [search, setSearch] =
        useState("");


    /* =====================================================
       Load current farm
       ===================================================== */

    const loadFarm = useCallback(
        async () => {

            const currentFarmId =
                getCurrentFarmId();

            if (!currentFarmId) {
                setFarmId("");
                setFarm(null);
                setLoadingFarm(false);
                return;
            }

            setFarmId(
                currentFarmId
            );

            setLoadingFarm(true);

            try {

                const response =
                    await fetch(
                        `${
                            import.meta.env
                                .VITE_API_BASE_URL ||
                            "http://localhost:8080"
                        }/api/farms/${currentFarmId}`,
                        {
                            headers: {
                                Accept:
                                    "application/json",
                                ...(localStorage.getItem(
                                    "agrinexus_token"
                                )
                                    ? {
                                        Authorization:
                                            `Bearer ${localStorage.getItem(
                                                "agrinexus_token"
                                            )}`,
                                    }
                                    : {}),
                            },
                        }
                    );

                if (
                    response.status === 204
                ) {
                    setFarm(null);
                    return;
                }

                if (!response.ok) {
                    throw new Error(
                        `Unable to load farm (${response.status})`
                    );
                }

                const data =
                    await response.json();

                setFarm(
                    data || null
                );

            } catch (farmError) {

                console.error(
                    "Failed to load farm:",
                    farmError
                );

                setFarm(null);

            } finally {

                setLoadingFarm(false);
            }
        },
        []
    );


    /* =====================================================
       Load real crops
       ===================================================== */

    const loadCrops =
        useCallback(
            async () => {

                setLoadingCrops(
                    true
                );

                try {

                    const response =
                        await getAllCrops();

                    const records =
                        normalizeCropRecords(
                            response
                        )
                            .filter(
                                (crop) =>
                                    crop?.id &&
                                    crop?.name
                            )
                            .sort(
                                (a, b) =>
                                    String(
                                        a.name
                                    ).localeCompare(
                                        String(
                                            b.name
                                        )
                                    )
                            );

                    setCrops(records);

                } catch (cropError) {

                    console.error(
                        "Failed to load crops:",
                        cropError
                    );

                    setCrops([]);

                    setError(
                        cropError?.message ||
                        "Unable to load crop data."
                    );

                } finally {

                    setLoadingCrops(
                        false
                    );
                }
            },
            []
        );


    /* =====================================================
       Load latest sowing decision
       ===================================================== */

    const loadLatestDecision =
        useCallback(
            async (
                selectedFarmId
            ) => {

                if (!selectedFarmId) {
                    setLatestDecision(
                        null
                    );
                    setLoadingDecision(
                        false
                    );
                    return;
                }

                setLoadingDecision(
                    true
                );

                try {

                    const response =
                        await getLatestSowingRecommendation(
                            selectedFarmId
                        );

                    const normalized =
                        normalizeLatestDecision(
                            response
                        );

                    setLatestDecision(
                        normalized
                    );

                    /*
                     * Keep crop selector aligned
                     * with the latest backend decision
                     * when that crop actually exists
                     * in the real crop list.
                     */
                    if (
                        normalized?.cropId
                    ) {

                        setSelectedCropId(
                            String(
                                normalized.cropId
                            )
                        );
                    }

                } catch (decisionError) {

                    /*
                     * A missing decision is a valid
                     * empty state. We only show an
                     * error when the request itself fails.
                     */
                    if (
                        decisionError?.status ===
                        204
                    ) {

                        setLatestDecision(
                            null
                        );

                    } else {

                        console.error(
                            "Failed to load latest sowing decision:",
                            decisionError
                        );

                        setLatestDecision(
                            null
                        );

                        setError(
                            decisionError?.message ||
                            "Unable to load sowing advice."
                        );
                    }

                } finally {

                    setLoadingDecision(
                        false
                    );
                }
            },
            []
        );


    /* =====================================================
       Initial load
       ===================================================== */

    useEffect(() => {

        let cancelled = false;

        async function initialize() {

            setError("");

            await Promise.all([
                loadFarm(),
                loadCrops(),
            ]);

            if (cancelled) {
                return;
            }

            const currentFarmId =
                getCurrentFarmId();

            if (currentFarmId) {
                await loadLatestDecision(
                    currentFarmId
                );
            } else {
                setLoadingDecision(
                    false
                );
            }
        }

        initialize();

        return () => {
            cancelled = true;
        };

    }, [
        loadFarm,
        loadCrops,
        loadLatestDecision,
    ]);


    /* =====================================================
       Search crops
       ===================================================== */

    const filteredCrops =
        useMemo(() => {

            const term =
                search
                    .trim()
                    .toLowerCase();

            if (!term) {
                return crops;
            }

            return crops.filter(
                (crop) => {

                    const name =
                        String(
                            crop?.name ||
                            ""
                        )
                            .toLowerCase();

                    const season =
                        String(
                            crop?.season ||
                            ""
                        )
                            .toLowerCase();

                    const soilType =
                        String(
                            crop?.soilType ||
                            ""
                        )
                            .toLowerCase();

                    return (
                        name.includes(
                            term
                        ) ||
                        season.includes(
                            term
                        ) ||
                        soilType.includes(
                            term
                        )
                    );
                }
            );
        }, [
            crops,
            search,
        ]);


    /* =====================================================
       Selected crop
       ===================================================== */

    const selectedCrop =
        useMemo(
            () =>
                crops.find(
                    (crop) =>
                        String(
                            crop.id
                        ) ===
                        String(
                            selectedCropId
                        )
                ) || null,
            [
                crops,
                selectedCropId,
            ]
        );


    /* =====================================================
       Decision metadata
       ===================================================== */

    const decisionMeta =
        useMemo(
            () =>
                getDecisionMeta(
                    latestDecision?.decision
                ),
            [
                latestDecision?.decision,
            ]
        );


    const factors =
        useMemo(
            () =>
                splitFactors(
                    latestDecision
                        ?.contributingFactors
                ),
            [
                latestDecision
                    ?.contributingFactors,
            ]
        );


    const isTestDecision =
        String(
            latestDecision?.dataStatus ||
            ""
        )
            .trim()
            .toUpperCase() ===
        "TEST";


    /* =====================================================
       Generate recommendation
       ===================================================== */

    const handleGenerate =
        async () => {

            setError("");
            setSuccessMessage("");

            if (!farmId) {
                setError(
                    "No current farm is selected."
                );
                return;
            }

            if (!selectedCropId) {
                setError(
                    "Please select a real crop first."
                );
                return;
            }

            setGenerating(
                true
            );

            try {

                const response =
                    await getSowingRecommendation(
                        farmId,
                        selectedCropId
                    );

                const normalized =
                    normalizeLatestDecision(
                        response
                    );

                setLatestDecision(
                    normalized
                );

                setSuccessMessage(
                    "Sowing advice generated from the current backend data."
                );

            } catch (generateError) {

                console.error(
                    "Failed to generate sowing advice:",
                    generateError
                );

                setError(
                    generateError?.message ||
                    "Unable to generate sowing advice."
                );

            } finally {

                setGenerating(
                    false
                );
            }
        };


    /* =====================================================
       Empty farm state
       ===================================================== */

    if (
        !loadingFarm &&
        !farmId
    ) {

        return (
            <div className="sowing-page">

                <div className="sowing-empty-state">

                    <div className="sowing-empty-icon">
                        🌾
                    </div>

                    <h1>
                        Sowing Advisor
                    </h1>

                    <p>
                        Select a farm before generating a sowing decision.
                    </p>

                </div>

            </div>
        );
    }


    /* =====================================================
       Render
       ===================================================== */

    return (
        <div className="sowing-page">

            {/* =================================================
                Hero
            ================================================= */}

            <section className="sowing-hero">

                <div className="sowing-hero-glow" />

                <div className="sowing-hero-content">

                    <div className="sowing-eyebrow">
                        AGRINEXUS DECISION INTELLIGENCE
                    </div>

                    <h1>
                        Weather-Aware
                        <span>
                            Sowing Advisor
                        </span>
                    </h1>

                    <p>
                        Select a real crop and let AgriNexus evaluate
                        the current farm conditions before sowing.
                    </p>

                </div>

                <div className="sowing-hero-icon">
                    🌱
                </div>

            </section>


            {/* =================================================
                Alerts
            ================================================= */}

            {error && (

                <div className="sowing-alert sowing-alert-error">

                    <span>
                        ⚠️
                    </span>

                    <div>
                        <strong>
                            Unable to complete request
                        </strong>

                        <p>
                            {error}
                        </p>
                    </div>

                </div>
            )}


            {successMessage && (

                <div className="sowing-alert sowing-alert-success">

                    <span>
                        ✓
                    </span>

                    <div>
                        <strong>
                            Decision updated
                        </strong>

                        <p>
                            {successMessage}
                        </p>
                    </div>

                </div>
            )}


            {/* =================================================
                Farm context
            ================================================= */}

            <section className="sowing-card sowing-farm-card">

                <div className="sowing-farm-icon">
                    🚜
                </div>

                <div className="sowing-farm-main">

                    <span className="sowing-card-kicker">
                        CURRENT FARM
                    </span>

                    <h2>
                        {loadingFarm
                            ? "Loading..."
                            : farm?.name ||
                            "—"}
                    </h2>

                    <p>
                        {farm?.location ||
                            "Farm location unavailable"}
                    </p>

                </div>

                <div className="sowing-farm-meta">

                    <span>
                        Farm ID
                    </span>

                    <strong>
                        {farmId || "—"}
                    </strong>

                </div>

            </section>


            {/* =================================================
                Crop selection
            ================================================= */}

            <section className="sowing-card sowing-selector-card">

                <div className="sowing-section-heading">

                    <div>

                        <span className="sowing-card-kicker">
                            CROP SELECTION
                        </span>

                        <h2>
                            What do you want to sow?
                        </h2>

                        <p>
                            Select a crop from the real crop knowledge
                            records stored in the backend.
                        </p>

                    </div>

                    <div className="sowing-crop-count">
                        {crops.length}
                        <span>
                            real crops
                        </span>
                    </div>

                </div>


                <div className="sowing-search-wrap">

                    <span>
                        🔍
                    </span>

                    <input
                        type="search"
                        value={search}
                        onChange={(event) =>
                            setSearch(
                                event.target.value
                            )
                        }
                        placeholder="Search crop, season or soil type..."
                    />

                </div>


                {loadingCrops ? (

                    <div className="sowing-loading">

                        <div className="sowing-spinner" />

                        <span>
                            Loading real crop data...
                        </span>

                    </div>

                ) : filteredCrops.length === 0 ? (

                    <div className="sowing-no-data">

                        <span>
                            🌱
                        </span>

                        <strong>
                            No crop records found
                        </strong>

                        <p>
                            The backend did not return a matching crop.
                        </p>

                    </div>

                ) : (

                    <div className="sowing-crop-grid">

                        {filteredCrops.map(
                            (crop) => {

                                const selected =
                                    String(
                                        selectedCropId
                                    ) ===
                                    String(
                                        crop.id
                                    );

                                return (
                                    <button
                                        key={crop.id}
                                        type="button"
                                        className={`sowing-crop-option ${
                                            selected
                                                ? "selected"
                                                : ""
                                        }`}
                                        onClick={() =>
                                            setSelectedCropId(
                                                crop.id
                                            )
                                        }
                                    >

                                        <div className="sowing-crop-option-icon">
                                            🌱
                                        </div>

                                        <div className="sowing-crop-option-content">

                                            <strong>
                                                {crop.name}
                                            </strong>

                                            <span>
                                                {crop.season ||
                                                    "Season unavailable"}
                                            </span>

                                            {crop.soilType && (
                                                <small>
                                                    {crop.soilType}
                                                </small>
                                            )}

                                        </div>

                                        {selected && (
                                            <div className="sowing-crop-selected">
                                                ✓
                                            </div>
                                        )}

                                    </button>
                                );
                            }
                        )}

                    </div>
                )}


                <div className="sowing-selected-bar">

                    <div>

                        <span>
                            Selected crop
                        </span>

                        <strong>
                            {selectedCrop?.name ||
                                "No crop selected"}
                        </strong>

                    </div>

                    <button
                        type="button"
                        className="sowing-generate-btn"
                        disabled={
                            generating ||
                            loadingCrops ||
                            !farmId ||
                            !selectedCropId
                        }
                        onClick={
                            handleGenerate
                        }
                    >

                        {generating ? (
                            <>
                                <span className="sowing-btn-spinner" />
                                Evaluating...
                            </>
                        ) : (
                            <>
                                <span>
                                    ✨
                                </span>
                                Generate Sowing Advice
                            </>
                        )}

                    </button>

                </div>

            </section>


            {/* =================================================
                Latest decision
            ================================================= */}

            <section className="sowing-section">

                <div className="sowing-section-title-row">

                    <div>

                        <span className="sowing-card-kicker">
                            LATEST BACKEND DECISION
                        </span>

                        <h2>
                            Sowing Decision
                        </h2>

                    </div>

                    {latestDecision?.evaluatedAt && (
                        <div className="sowing-evaluated">
                            Evaluated{" "}
                            <strong>
                                {formatDateTime(
                                    latestDecision.evaluatedAt
                                )}
                            </strong>
                        </div>
                    )}

                </div>


                {loadingDecision ? (

                    <div className="sowing-card sowing-decision-loading">

                        <div className="sowing-spinner" />

                        <span>
                            Loading latest sowing decision...
                        </span>

                    </div>

                ) : !latestDecision ? (

                    <div className="sowing-card sowing-no-decision">

                        <div className="sowing-no-decision-icon">
                            🌾
                        </div>

                        <h3>
                            No sowing decision yet
                        </h3>

                        <p>
                            Select a crop above and generate advice.
                            The result will be stored by the backend
                            as a sowing decision.
                        </p>

                    </div>

                ) : (

                    <>

                        {/* Test-data warning */}

                        {isTestDecision && (

                            <div className="sowing-test-warning">

                                <span>
                                    🧪
                                </span>

                                <div>

                                    <strong>
                                        Backend marked this decision as TEST data
                                    </strong>

                                    <p>
                                        This result is shown for transparency,
                                        but it must not be presented as a
                                        real farm measurement or field validation.
                                    </p>

                                </div>

                            </div>
                        )}


                        {/* Main decision */}

                        <div className="sowing-decision-layout">

                            <article className="sowing-card sowing-decision-card">

                                <div className="sowing-decision-top">

                                    <div className={`sowing-decision-badge ${decisionMeta.tone}`}>

                                        <span className="sowing-decision-icon">
                                            {decisionMeta.icon}
                                        </span>

                                        <div>

                                            <span>
                                                RECOMMENDATION
                                            </span>

                                            <strong>
                                                {decisionMeta.label}
                                            </strong>

                                        </div>

                                    </div>

                                    <div className="sowing-decision-crop">

                                        <span>
                                            Crop
                                        </span>

                                        <strong>
                                            {latestDecision.cropName ||
                                                selectedCrop?.name ||
                                                "—"}
                                        </strong>

                                    </div>

                                </div>


                                <div className="sowing-window">

                                    <span className="sowing-window-icon">
                                        🗓️
                                    </span>

                                    <div>

                                        <span>
                                            RECOMMENDED SOWING WINDOW
                                        </span>

                                        <strong>
                                            {getWindowLabel(
                                                latestDecision.recommendedWindow
                                            )}
                                        </strong>

                                    </div>

                                </div>


                                <div className="sowing-decision-description">

                                    <strong>
                                        What this means
                                    </strong>

                                    <p>
                                        {decisionMeta.description}
                                    </p>

                                </div>

                            </article>


                            {/* Confidence */}

                            <article className="sowing-card sowing-confidence-card">

                                <span className="sowing-card-kicker">
                                    DECISION CONFIDENCE
                                </span>

                                <div className="sowing-confidence-value">
                                    {formatPercent(
                                        latestDecision.confidence
                                    )}
                                </div>

                                <div className="sowing-confidence-track">

                                    <div
                                        className="sowing-confidence-fill"
                                        style={{
                                            width:
                                                safeNumber(
                                                    latestDecision.confidence
                                                )
                                                    ? `${Math.min(
                                                        100,
                                                        Math.max(
                                                            0,
                                                            latestDecision.confidence *
                                                            100
                                                        )
                                                    )}%`
                                                    : "0%",
                                        }}
                                    />

                                </div>

                                <p>
                                    Confidence is generated by the
                                    backend from the availability of
                                    soil, weather, forecast and telemetry
                                    observations.
                                </p>

                            </article>

                        </div>


                        {/* =================================================
                            Measurements
                        ================================================= */}

                        <div className="sowing-card sowing-metrics-card">

                            <div className="sowing-section-heading compact">

                                <div>

                                    <span className="sowing-card-kicker">
                                        OBSERVED CONDITIONS
                                    </span>

                                    <h3>
                                        Farm & Crop Conditions
                                    </h3>

                                </div>

                            </div>


                            <div className="sowing-metrics-grid">

                                <Metric
                                    icon="💧"
                                    label="Root-zone moisture"
                                    value={formatValue(
                                        latestDecision.currentMoisture,
                                        " %",
                                        1
                                    )}
                                />

                                <Metric
                                    icon="🌡️"
                                    label="Current temperature"
                                    value={formatValue(
                                        latestDecision.currentTemperature,
                                        " °C",
                                        1
                                    )}
                                />

                                <Metric
                                    icon="💦"
                                    label="Humidity"
                                    value={formatInteger(
                                        latestDecision.currentHumidity,
                                        " %"
                                    )}
                                />

                                <Metric
                                    icon="🌧️"
                                    label="Recent rainfall"
                                    value={formatValue(
                                        latestDecision.recentRainfall,
                                        " mm",
                                        1
                                    )}
                                />

                                <Metric
                                    icon="☁️"
                                    label="Forecast rainfall"
                                    value={formatValue(
                                        latestDecision.forecastRainfall,
                                        " mm",
                                        1
                                    )}
                                />

                                <Metric
                                    icon="🎯"
                                    label="Forecast confidence"
                                    value={formatPercent(
                                        latestDecision.forecastConfidence
                                    )}
                                />

                                <Metric
                                    icon="🧪"
                                    label="Observed soil pH"
                                    value={formatValue(
                                        latestDecision.soilPh,
                                        "",
                                        2
                                    )}
                                />

                                <Metric
                                    icon="📅"
                                    label="Crop season"
                                    value={
                                        latestDecision.season ||
                                        "—"
                                    }
                                />

                            </div>

                        </div>


                        {/* =================================================
                            Crop suitability
                        ================================================= */}

                        <div className="sowing-card sowing-suitability-card">

                            <div className="sowing-section-heading compact">

                                <div>

                                    <span className="sowing-card-kicker">
                                        CROP REQUIREMENTS
                                    </span>

                                    <h3>
                                        Crop Suitability Reference
                                    </h3>

                                </div>

                            </div>


                            <div className="sowing-suitability-grid">

                                <SuitabilityItem
                                    icon="🌡️"
                                    label="Ideal temperature"
                                    value={
                                        safeNumber(
                                            latestDecision.idealTemperatureMin
                                        ) &&
                                        safeNumber(
                                            latestDecision.idealTemperatureMax
                                        )
                                            ? `${latestDecision.idealTemperatureMin} – ${latestDecision.idealTemperatureMax} °C`
                                            : "—"
                                    }
                                />

                                <SuitabilityItem
                                    icon="🧪"
                                    label="Suitable soil pH"
                                    value={
                                        safeNumber(
                                            latestDecision.cropSoilPhMin
                                        ) &&
                                        safeNumber(
                                            latestDecision.cropSoilPhMax
                                        )
                                            ? `${latestDecision.cropSoilPhMin} – ${latestDecision.cropSoilPhMax}`
                                            : "—"
                                    }
                                />

                                <SuitabilityItem
                                    icon="📆"
                                    label="Configured season"
                                    value={
                                        latestDecision.season ||
                                        "—"
                                    }
                                />

                                <SuitabilityItem
                                    icon="🌱"
                                    label="Crop"
                                    value={
                                        latestDecision.cropName ||
                                        selectedCrop?.name ||
                                        "—"
                                    }
                                />

                            </div>

                        </div>


                        {/* =================================================
                            Explanation
                        ================================================= */}

                        <div className="sowing-explanation-grid">

                            <article className="sowing-card sowing-reason-card">

                                <div className="sowing-section-heading compact">

                                    <div>

                                        <span className="sowing-card-kicker">
                                            EXPLAINABILITY
                                        </span>

                                        <h3>
                                            Why did AgriNexus decide this?
                                        </h3>

                                    </div>

                                    <span className="sowing-ai-mark">
                                        AI
                                    </span>

                                </div>


                                <div className="sowing-reason-box">

                                    <span className="sowing-reason-icon">
                                        💡
                                    </span>

                                    <p>
                                        {latestDecision.reason ||
                                            "—"}
                                    </p>

                                </div>

                            </article>


                            <article className="sowing-card sowing-action-card">

                                <div className="sowing-section-heading compact">

                                    <div>

                                        <span className="sowing-card-kicker">
                                            FARMER ACTION
                                        </span>

                                        <h3>
                                            What should the farmer do?
                                        </h3>

                                    </div>

                                </div>


                                <div className="sowing-action-box">

                                    <span>
                                        🚜
                                    </span>

                                    <p>
                                        {latestDecision.recommendation ||
                                            "—"}
                                    </p>

                                </div>

                            </article>

                        </div>


                        {/* =================================================
                            Factors
                        ================================================= */}

                        <div className="sowing-card sowing-factors-card">

                            <div className="sowing-section-heading compact">

                                <div>

                                    <span className="sowing-card-kicker">
                                        DECISION FACTORS
                                    </span>

                                    <h3>
                                        Contributing Conditions
                                    </h3>

                                </div>

                            </div>


                            {factors.length === 0 ? (

                                <div className="sowing-inline-empty">
                                    —
                                </div>

                            ) : (

                                <div className="sowing-factor-list">

                                    {factors.map(
                                        (factor, index) => (
                                            <div
                                                key={`${factor}-${index}`}
                                                className="sowing-factor"
                                            >

                                                <span>
                                                    {index + 1}
                                                </span>

                                                <p>
                                                    {factor}
                                                </p>

                                            </div>
                                        )
                                    )}

                                </div>
                            )}

                        </div>


                        {/* =================================================
                            Audit / provenance
                        ================================================= */}

                        <div className="sowing-audit-grid">

                            <article className="sowing-card sowing-audit-card">

                                <span className="sowing-card-kicker">
                                    DATA PROVENANCE
                                </span>

                                <h3>
                                    Data used
                                </h3>

                                <p>
                                    {latestDecision.dataUsed ||
                                        "—"}
                                </p>

                                <div className="sowing-audit-status">

                                    <span>
                                        Data status
                                    </span>

                                    <strong
                                        className={
                                            isTestDecision
                                                ? "test"
                                                : "live"
                                        }
                                    >
                                        {latestDecision.dataStatus ||
                                            "—"}
                                    </strong>

                                </div>

                            </article>


                            <article className="sowing-card sowing-audit-card">

                                <span className="sowing-card-kicker">
                                    METHODOLOGY
                                </span>

                                <h3>
                                    Decision version
                                </h3>

                                <p>
                                    {latestDecision.methodologyVersion ||
                                        "—"}
                                </p>

                                <div className="sowing-audit-status">

                                    <span>
                                        Valid until
                                    </span>

                                    <strong>
                                        {formatDateTime(
                                            latestDecision.validUntil
                                        )}
                                    </strong>

                                </div>

                            </article>

                        </div>


                        {/* =================================================
                            Uncertainty
                        ================================================= */}

                        <div className="sowing-card sowing-uncertainty-card">

                            <div className="sowing-uncertainty-icon">
                                ℹ️
                            </div>

                            <div>

                                <span className="sowing-card-kicker">
                                    LIMITATIONS & UNCERTAINTY
                                </span>

                                <p>
                                    {latestDecision.uncertaintyNote ||
                                        "—"}
                                </p>

                            </div>

                        </div>

                    </>
                )}

            </section>


        </div>
    );
}


/* =========================================================
   Small metric components
   ========================================================= */

function Metric({
                    icon,
                    label,
                    value,
                }) {

    return (
        <div className="sowing-metric">

            <div className="sowing-metric-icon">
                {icon}
            </div>

            <div>

                <span>
                    {label}
                </span>

                <strong>
                    {value}
                </strong>

            </div>

        </div>
    );
}


function SuitabilityItem({
                             icon,
                             label,
                             value,
                         }) {

    return (
        <div className="sowing-suitability-item">

            <div className="sowing-suitability-icon">
                {icon}
            </div>

            <div>

                <span>
                    {label}
                </span>

                <strong>
                    {value}
                </strong>

            </div>

        </div>
    );
}