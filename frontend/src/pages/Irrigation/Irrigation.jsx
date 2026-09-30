import {
    useCallback,
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    useNavigate,
} from "react-router-dom";

import {
    useFields,
} from "../../context/FieldsContext";

import {
    apiRequest,
    getCurrentFarmId,
} from "../../api/apiClient";

import {
    getCurrentCropForFarm,
} from "../../api/cropHistoryApi";

import {
    getIrrigationConfig,
    getFarmIrrigationTelemetry,
    getLatestIrrigationTelemetry,
    getFarmIrrigationEvents,
    getPumpCompletion,
    evaluateSmartIrrigation,
    syncLiveWeatherForecast,
} from "../../api/irrigationApi";

import "./Irrigation.css";


/* =========================================================
   HELPERS
   ========================================================= */

function normalizeList(response) {

    if (Array.isArray(response)) {
        return response;
    }

    if (Array.isArray(response?.content)) {
        return response.content;
    }

    if (Array.isArray(response?.data)) {
        return response.data;
    }

    if (response?.id) {
        return [response];
    }

    return [];
}


function numberExists(value) {

    return (
        value !== null &&
        value !== undefined &&
        value !== "" &&
        Number.isFinite(Number(value))
    );
}


function displayNumber(
    value,
    decimals = 2
) {

    if (!numberExists(value)) {
        return "—";
    }

    return Number(value).toFixed(
        decimals
    );
}


function formatDateTime(value) {

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
        "en-IN",
        {
            day: "numeric",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        }
    );
}


function formatStatus(value) {

    if (
        value === null ||
        value === undefined ||
        value === ""
    ) {
        return "—";
    }

    return String(value)
        .replace(
            /_/g,
            " "
        );
}


function statusClass(value) {

    const status =
        String(value || "")
            .toUpperCase();

    if (
        status.includes("SUFFICIENT") ||
        status.includes("COMPLETED") ||
        status.includes("REACHED") ||
        status.includes("ONLINE")
    ) {
        return "success";
    }

    if (
        status.includes("MONITOR") ||
        status.includes("KEEP_RUNNING") ||
        status.includes("WARNING") ||
        status.includes("DUE")
    ) {
        return "warning";
    }

    if (
        status.includes("RECOMMENDED") ||
        status.includes("TRIGGER")
    ) {
        return "danger";
    }

    if (
        status.includes("INSUFFICIENT") ||
        status.includes("UNKNOWN") ||
        status.includes("OFFLINE")
    ) {
        return "info";
    }

    return "neutral";
}


function getPumpStateLabel(
    pumpState
) {

    if (
        pumpState === 1 ||
        pumpState === "1"
    ) {
        return "ON";
    }

    if (
        pumpState === 0 ||
        pumpState === "0"
    ) {
        return "OFF";
    }

    return "—";
}


function getCropName(crop) {

    return (
        crop?.cropName ??
        crop?.crop?.name ??
        crop?.name ??
        "—"
    );
}


function getCropId(crop) {

    return (
        crop?.cropId ??
        crop?.crop?.id ??
        null
    );
}


function getCropStage(crop) {

    return (
        crop?.cropStage ??
        crop?.stage ??
        ""
    );
}


/* =========================================================
   COMPONENT
   ========================================================= */

export default function Irrigation() {

    const navigate =
        useNavigate();

    const {
        currentField,
    } = useFields();


    /* =====================================================
       FARM
       ===================================================== */

    const farmId =
        currentField?.id ??
        getCurrentFarmId() ??
        "";


    /* =====================================================
       STATE
       ===================================================== */

    const [
        farm,
        setFarm,
    ] = useState(null);

    const [
        currentCrop,
        setCurrentCrop,
    ] = useState(null);

    const [
        config,
        setConfig,
    ] = useState(null);

    const [
        telemetry,
        setTelemetry,
    ] = useState(null);

    const [
        telemetryHistory,
        setTelemetryHistory,
    ] = useState([]);

    const [
        events,
        setEvents,
    ] = useState([]);

    const [
        pumpCompletion,
        setPumpCompletion,
    ] = useState(null);

    const [
        smartDecision,
        setSmartDecision,
    ] = useState(null);


    /* =====================================================
       UI
       ===================================================== */

    const [
        loading,
        setLoading,
    ] = useState(true);

    const [
        refreshing,
        setRefreshing,
    ] = useState(false);

    const [
        error,
        setError,
    ] = useState("");

    const [
        pumpError,
        setPumpError,
    ] = useState("");

    const [
        lastCheckedAt,
        setLastCheckedAt,
    ] = useState(null);


    const [
        forecastSyncing,
        setForecastSyncing,
    ] = useState(false);

    const [
        forecastSyncError,
        setForecastSyncError,
    ] = useState("");

    const [
        forecastSyncInfo,
        setForecastSyncInfo,
    ] = useState(null);


    /* =====================================================
       LOAD FARM
       ===================================================== */

    const loadFarm =
        useCallback(
            async () => {

                if (!farmId) {
                    return null;
                }

                try {

                    return await apiRequest(
                        `/api/farms/${farmId}`
                    );

                } catch (farmError) {

                    console.warn(
                        "Farm load failed:",
                        farmError
                    );

                    return null;
                }
            },
            [farmId]
        );


    /* =====================================================
       LOAD CURRENT CROP
       ===================================================== */

    const loadCurrentCrop =
        useCallback(
            async () => {

                if (!farmId) {
                    return null;
                }

                try {

                    return (
                        await getCurrentCropForFarm(
                            farmId
                        )
                    ) || null;

                } catch (cropError) {

                    console.warn(
                        "Current crop load failed:",
                        cropError
                    );

                    return null;
                }
            },
            [farmId]
        );


    /* =====================================================
       LOAD IRRIGATION DATA
       ===================================================== */

    const loadIrrigationData =
        useCallback(
            async () => {

                if (!farmId) {
                    return {
                        config: null,
                        telemetry: null,
                        telemetryHistory: [],
                        events: [],
                        pumpCompletion: null,
                        smartDecision: null,
                    };
                }


                const results =
                    await Promise.allSettled([

                        getIrrigationConfig(
                            farmId
                        ),

                        getLatestIrrigationTelemetry(
                            farmId
                        ),

                        getFarmIrrigationTelemetry(
                            farmId
                        ),

                        getFarmIrrigationEvents(
                            farmId
                        ),

                        getPumpCompletion(
                            farmId
                        ),

                    ]);


                const [
                    configResult,
                    latestTelemetryResult,
                    telemetryHistoryResult,
                    eventsResult,
                    pumpResult,
                ] = results;


                let irrigationConfig =
                    null;

                if (
                    configResult.status ===
                    "fulfilled"
                ) {

                    irrigationConfig =
                        configResult.value ||
                        null;
                }


                let latestTelemetry =
                    null;

                if (
                    latestTelemetryResult.status ===
                    "fulfilled"
                ) {

                    latestTelemetry =
                        latestTelemetryResult.value ||
                        null;
                }


                let telemetryRecords =
                    [];

                if (
                    telemetryHistoryResult.status ===
                    "fulfilled"
                ) {

                    telemetryRecords =
                        normalizeList(
                            telemetryHistoryResult.value
                        );
                }


                let eventRecords =
                    [];

                if (
                    eventsResult.status ===
                    "fulfilled"
                ) {

                    eventRecords =
                        normalizeList(
                            eventsResult.value
                        );
                }


                let pumpResultData =
                    null;

                if (
                    pumpResult.status ===
                    "fulfilled"
                ) {

                    pumpResultData =
                        pumpResult.value ||
                        null;
                }


                return {
                    config:
                    irrigationConfig,

                    telemetry:
                    latestTelemetry,

                    telemetryHistory:
                    telemetryRecords,

                    events:
                    eventRecords,

                    pumpCompletion:
                    pumpResultData,

                    smartDecision:
                        null,
                };

            },
            [farmId]
        );


    /* =====================================================
       LOAD ALL
       ===================================================== */

    const loadAll =
        useCallback(
            async (
                showSpinner = true
            ) => {

                if (!farmId) {

                    setFarm(null);
                    setCurrentCrop(null);
                    setConfig(null);
                    setTelemetry(null);
                    setTelemetryHistory([]);
                    setEvents([]);
                    setPumpCompletion(null);
                    setSmartDecision(null);
                    setLoading(false);

                    return;
                }


                if (showSpinner) {
                    setLoading(true);
                } else {
                    setRefreshing(true);
                }


                setError("");
                setPumpError("");


                try {

                    const [
                        farmResult,
                        cropResult,
                        irrigationResult,
                    ] = await Promise.all([
                        loadFarm(),
                        loadCurrentCrop(),
                        loadIrrigationData(),
                    ]);


                    setFarm(
                        farmResult
                    );

                    setCurrentCrop(
                        cropResult
                    );

                    setConfig(
                        irrigationResult.config
                    );

                    setTelemetry(
                        irrigationResult.telemetry
                    );

                    setTelemetryHistory(
                        irrigationResult.telemetryHistory
                    );

                    setEvents(
                        irrigationResult.events
                    );

                    setPumpCompletion(
                        irrigationResult.pumpCompletion
                    );


                    if (
                        !irrigationResult.pumpCompletion
                    ) {

                        const failedPumpRequest =
                            irrigationResult
                                .pumpCompletion;

                        void failedPumpRequest;

                    }

                } catch (loadError) {

                    console.error(
                        "Irrigation load failed:",
                        loadError
                    );

                    setError(
                        loadError?.message ||
                        "Unable to load irrigation data."
                    );

                } finally {

                    setLoading(false);
                    setRefreshing(false);
                    setLastCheckedAt(new Date().toISOString());
                }

            },
            [
                farmId,
                loadFarm,
                loadCurrentCrop,
                loadIrrigationData,
            ]
        );



    /* =====================================================
       INITIAL LOAD
       ===================================================== */

    useEffect(() => {

        void loadAll(
            true
        );

    }, [
        loadAll,
    ]);


    /* =====================================================
       SMART IRRIGATION EVALUATION
       ===================================================== */

    const evaluateDecision =
        useCallback(
            async () => {

                if (!farmId) {
                    return;
                }


                try {

                    const cropId =
                        getCropId(
                            currentCrop
                        );

                    const cropStage =
                        getCropStage(
                            currentCrop
                        );


                    /*
                     * Only real known values are sent.
                     * Stored backend data fills the rest.
                     */
                    const payload = {
                        farmId,

                        ...(cropId
                            ? {
                                cropId,
                            }
                            : {}),

                        ...(cropStage
                            ? {
                                cropStage,
                            }
                            : {}),
                    };


                    const response =
                        await evaluateSmartIrrigation(
                            payload
                        );


                    setSmartDecision(
                        response ||
                        null
                    );

                } catch (
                    decisionError
                    ) {

                    console.warn(
                        "Smart irrigation evaluation failed:",
                        decisionError
                    );

                    setSmartDecision(
                        null
                    );
                }

            },
            [
                farmId,
                currentCrop,
            ]
        );


    useEffect(() => {

        if (!loading) {
            void evaluateDecision();
        }

    }, [
        loading,
        evaluateDecision,
    ]);



    /* =====================================================
       LIVE FORECAST SYNC
       ===================================================== */

    const handleSyncLiveForecast =
        useCallback(
            async () => {

                if (!farmId) {
                    return;
                }

                setForecastSyncing(true);
                setForecastSyncError("");
                setForecastSyncInfo(null);

                try {

                    const response =
                        await syncLiveWeatherForecast(
                            farmId
                        );

                    const records =
                        normalizeList(response);

                    const dates =
                        records
                            .map((record) => record?.forecastDate)
                            .filter(Boolean)
                            .sort();

                    setForecastSyncInfo({
                        count: records.length,
                        startDate: dates[0] || null,
                        endDate: dates[dates.length - 1] || null,
                    });

                    await evaluateDecision();

                } catch (syncError) {

                    console.warn(
                        "Live weather forecast sync failed:",
                        syncError
                    );

                    setForecastSyncError(
                        syncError?.message ||
                        "Unable to synchronize the live weather forecast."
                    );

                } finally {
                    setForecastSyncing(false);
                }
            },
            [
                farmId,
                evaluateDecision,
            ]
        );
    /* =====================================================
       FRONTEND VERIFICATION
       ===================================================== */

    const handleCheckNow =
        useCallback(
            async () => {

                if (!farmId) {
                    return;
                }

                setRefreshing(true);

                try {
                    await loadAll(false);
                    await evaluateDecision();
                } catch (checkError) {
                    console.warn(
                        "Irrigation verification failed:",
                        checkError
                    );
                } finally {
                    setRefreshing(false);
                    setLastCheckedAt(
                        new Date().toISOString()
                    );
                }
            },
            [
                farmId,
                loadAll,
                evaluateDecision,
            ]
        );


    /* =====================================================
       TELEMETRY SUMMARY
       ===================================================== */

    const telemetrySource =
        telemetry?.source ??
        "—";

    const sensorStatus =
        telemetry?.sensorStatus ??
        "—";

    const pumpState =
        telemetry?.pumpState;

    const pumpLabel =
        getPumpStateLabel(
            pumpState
        );


    /* =====================================================
       EVENT SORT
       ===================================================== */

    const sortedEvents =
        useMemo(
            () =>
                [...events].sort(
                    (a, b) =>
                        new Date(
                            b.startTime ||
                            0
                        ).getTime() -
                        new Date(
                            a.startTime ||
                            0
                        ).getTime()
                ),
            [
                events,
            ]
        );


    /* =====================================================
       TELEMETRY HISTORY SORT
       ===================================================== */

    const sortedTelemetry =
        useMemo(
            () =>
                [...telemetryHistory]
                    .sort(
                        (a, b) =>
                            new Date(
                                b.recordedAt ||
                                0
                            ).getTime() -
                            new Date(
                                a.recordedAt ||
                                0
                            ).getTime()
                    )
                    .slice(
                        0,
                        8
                    ),
            [
                telemetryHistory,
            ]
        );


    /* =====================================================
       FARM ABSENT
       ===================================================== */

    if (!farmId) {

        return (
            <div className="irrigation-page">

                <div className="irrigation-empty-page">

                    <div className="irrigation-empty-icon">
                        💧
                    </div>

                    <h1>
                        Smart Irrigation
                    </h1>

                    <p>
                        Select a farm before using
                        irrigation intelligence.
                    </p>

                </div>

            </div>
        );
    }


    /* =====================================================
       RENDER
       ===================================================== */

    return (
        <div className="irrigation-page">

            {/* =================================================
                HERO
            ================================================= */}

            <section className="irrigation-hero">

                <div>

                    <span>
                        AGRINEXUS WATER INTELLIGENCE
                    </span>

                    <h1>
                        Smart
                        <strong>
                            Irrigation
                        </strong>
                    </h1>

                    <p>
                        Connect real soil-water telemetry,
                        hydraulic measurements, weather,
                        pump state and irrigation decisions
                        in one farm view.
                    </p>

                </div>


                <div className="irrigation-hero-icon">
                    💧
                </div>

            </section>


            {/* =================================================
                FARM CONTEXT
            ================================================= */}

            <section className="irrigation-farm-bar">

                <div className="irrigation-farm-icon">
                    🚜
                </div>

                <div className="irrigation-farm-main">

                    <span>
                        CURRENT FARM
                    </span>

                    <strong>
                        {
                            farm?.name ??
                            currentField?.name ??
                            "—"
                        }
                    </strong>

                    <small>
                        Farm ID:
                        {" "}
                        {farmId}
                    </small>

                </div>


                <div className="irrigation-farm-crop">

                    <span>
                        ACTIVE CROP
                    </span>

                    <strong>
                        {
                            getCropName(
                                currentCrop
                            )
                        }
                    </strong>

                    <small>
                        {
                            getCropStage(
                                currentCrop
                            ) ||
                            "—"
                        }
                    </small>

                </div>


                <button
                    type="button"
                    className="irrigation-home-button"
                    onClick={() =>
                        navigate(
                            "/home"
                        )
                    }
                >
                    ← Home
                </button>

            </section>


            {/* =================================================
                ERROR
            ================================================= */}

            {error && (

                <div className="irrigation-alert irrigation-alert-error">

                    <span>
                        ⚠️
                    </span>

                    <div>

                        <strong>
                            Irrigation data unavailable
                        </strong>

                        <p>
                            {error}
                        </p>

                    </div>

                </div>
            )}


            {/* =================================================
                TOP STATUS
            ================================================= */}

            <section className="irrigation-status-grid">

                <StatusCard
                    label="PUMP"
                    value={
                        pumpLabel
                    }
                    tone={
                        pumpLabel === "ON"
                            ? "warning"
                            : "success"
                    }
                    icon="⚙️"
                />


                <StatusCard
                    label="SENSOR STATUS"
                    value={
                        sensorStatus
                    }
                    tone={
                        statusClass(
                            sensorStatus
                        )
                    }
                    icon="📡"
                />


                <StatusCard
                    label="DATA SOURCE"
                    value={
                        telemetrySource
                    }
                    tone={
                        statusClass(
                            telemetrySource
                        )
                    }
                    icon="🔌"
                />


                <StatusCard
                    label="LAST READING"
                    value={
                        telemetry?.recordedAt
                            ? formatDateTime(
                                telemetry.recordedAt
                            )
                            : "—"
                    }
                    tone="neutral"
                    icon="🕐"
                />

            </section>


            {/* =================================================
                LIVE TELEMETRY
            ================================================= */}

            <section className="irrigation-section">

                <SectionHeading
                    eyebrow="01 · LIVE TELEMETRY"
                    title="Field & Hydraulic Monitoring"
                    description="Latest irrigation telemetry returned by the backend."
                />


                {loading ? (

                    <LoadingCard />

                ) : (

                    <div className="irrigation-metric-grid">

                        <MetricCard
                            label="VWC · 15 cm"
                            value={
                                telemetry?.vwc15cm
                                !== null &&
                                telemetry?.vwc15cm
                                !== undefined
                                    ? `${displayNumber(
    telemetry.vwc15cm,
    2
)}%`
                                    : "—"
                            }
                            icon="🌱"
                        />

                        <MetricCard
                            label="VWC · 30 cm"
                            value={
                                telemetry?.vwc30cm
                                !== null &&
                                telemetry?.vwc30cm
                                !== undefined
                                    ? `${displayNumber(
    telemetry.vwc30cm,
    2
)}%`
                                    : "—"
                            }
                            icon="🌿"
                            emphasis
                        />

                        <MetricCard
                            label="VWC · 60 cm"
                            value={
                                telemetry?.vwc60cm
                                !== null &&
                                telemetry?.vwc60cm
                                !== undefined
                                    ? `${displayNumber(
    telemetry.vwc60cm,
    2
)}%`
                                    : "—"
                            }
                            icon="🌳"
                        />

                        <MetricCard
                            label="Flow"
                            value={
                                numberExists(
                                    telemetry?.flowRateLpm
                                )
                                    ? `${displayNumber(
    telemetry.flowRateLpm,
    2
)} L/min`
                                    : "—"
                            }
                            icon="💦"
                        />

                        <MetricCard
                            label="Line Pressure"
                            value={
                                numberExists(
                                    telemetry?.linePressureBar
                                )
                                    ? `${displayNumber(
    telemetry.linePressureBar,
    2
)} bar`
                                    : "—"
                            }
                            icon="📈"
                        />

                        <MetricCard
                            label="Pump Runtime"
                            value={
                                numberExists(
                                    telemetry?.pumpRuntimeMinutes
                                )
                                    ? `${displayNumber(
    telemetry.pumpRuntimeMinutes,
    2
)} min`
                                    : "—"
                            }
                            icon="⏱️"
                        />

                        <MetricCard
                            label="Rainfall"
                            value={
                                numberExists(
                                    telemetry?.rainfallMm
                                )
                                    ? `${displayNumber(
    telemetry.rainfallMm,
    2
)} mm`
                                    : "—"
                            }
                            icon="🌧️"
                        />

                        <MetricCard
                            label="Water Table"
                            value={
                                numberExists(
                                    telemetry?.waterTableDepthM
                                )
                                    ? `${displayNumber(
    telemetry.waterTableDepthM,
    2
)} m`
                                    : "—"
                            }
                            icon="🪣"
                        />

                    </div>
                )}

            </section>


            {/* =================================================
                SMART DECISION
            ================================================= */}

            <section className="irrigation-section">

                <SectionHeading
                    eyebrow="02 · SMART DECISION"
                    title="Irrigation Decision"
                    description="Rule-based decision support generated by the AgriNexus backend."
                />

                <div className="irrigation-forecast-sync-panel">

                    <div>
                        <strong>Live 7-day weather forecast</strong>

                        <p>
                            Synchronize the real forecast for this farm from the configured weather provider.
                            No forecast values are entered manually by the dashboard.
                        </p>

                        {forecastSyncInfo && (
                            <small>
                                Synced {forecastSyncInfo.count} forecast record{forecastSyncInfo.count === 1 ? "" : "s"}
                                {forecastSyncInfo.startDate
                                    ? ` · ${forecastSyncInfo.startDate}`
                                    : ""}
                                {forecastSyncInfo.endDate && forecastSyncInfo.endDate !== forecastSyncInfo.startDate
                                    ? ` → ${forecastSyncInfo.endDate}`
                                    : ""}
                            </small>
                        )}

                        {forecastSyncError && (
                            <small className="irrigation-forecast-sync-error">
                                {forecastSyncError}
                            </small>
                        )}
                    </div>

                    <button
                        type="button"
                        disabled={forecastSyncing || loading || !farmId}
                        onClick={handleSyncLiveForecast}
                    >
                        {forecastSyncing
                            ? "Syncing forecast..."
                            : "Sync Live Forecast"}
                    </button>

                </div>


                {!smartDecision ? (

                    <div className="irrigation-empty-card">

                        <div className="irrigation-empty-icon">
                            💧
                        </div>

                        <h3>
                            No irrigation decision available
                        </h3>

                        <p>
                            The backend has not returned a
                            smart irrigation decision for
                            the current farm context.
                        </p>

                    </div>

                ) : (

                    <div className="irrigation-decision-layout">

                        <article className="irrigation-decision-main">

                            <div className="irrigation-decision-top">

                                <div>

                                    <span>
                                        FIELD STATUS
                                    </span>

                                    <h3>
                                        {
                                            formatStatus(
                                                smartDecision.fieldStatus
                                            )
                                        }
                                    </h3>

                                </div>


                                <div
                                    className={`irrigation-decision-pill ${statusClass(
    smartDecision.fieldStatus
)}`}
                                >
                                    {
                                        formatStatus(
                                            smartDecision.dataStatus
                                        )
                                    }
                                </div>

                            </div>


                            <div className="irrigation-decision-action">

                                <span>
                                    FARMER ACTION
                                </span>

                                <p>
                                    {
                                        smartDecision.actionItem ||
                                        "—"
                                    }
                                </p>

                            </div>


                            <div className="irrigation-decision-metrics">

                                <MetricMini
                                    label="Current VWC"
                                    value={
                                        numberExists(
                                            smartDecision.currentVwc
                                        )
                                            ? `${displayNumber(
    smartDecision.currentVwc,
    2
)}%`
                                            : "—"
                                    }
                                />

                                <MetricMini
                                    label="Field Capacity"
                                    value={
                                        numberExists(
                                            smartDecision.fieldCapacity
                                        )
                                            ? `${displayNumber(
    smartDecision.fieldCapacity,
    2
)}%`
                                            : "—"
                                    }
                                />

                                <MetricMini
                                    label="Wilting Point"
                                    value={
                                        numberExists(
                                            smartDecision.wiltingPoint
                                        )
                                            ? `${displayNumber(
    smartDecision.wiltingPoint,
    2
)}%`
                                            : "—"
                                    }
                                />

                                <MetricMini
                                    label="MAD"
                                    value={
                                        numberExists(
                                            smartDecision.mad
                                        )
                                            ? displayNumber(
                                                smartDecision.mad,
                                                3
                                            )
                                            : "—"
                                    }
                                />

                            </div>

                        </article>


                        <article className="irrigation-decision-side">

                            <h3>
                                Water Balance Context
                            </h3>


                            <DecisionRow
                                label="Depletion"
                                value={
                                    numberExists(
                                        smartDecision.depletionLevel
                                    )
                                        ? displayNumber(
                                        smartDecision.depletionLevel *
                                        100,
                                        1
                                    ) + "%"
                                        : "—"
                                }
                            />


                            <DecisionRow
                                label="ET0"
                                value={
                                    numberExists(
                                        smartDecision.et0
                                    )
                                        ? `${displayNumber(
    smartDecision.et0,
    2
)} mm`
                                        : "—"
                                }
                            />


                            <DecisionRow
                                label="Kc"
                                value={
                                    numberExists(
                                        smartDecision.kc
                                    )
                                        ? displayNumber(
                                            smartDecision.kc,
                                            3
                                        )
                                        : "—"
                                }
                            />


                            <DecisionRow
                                label="ETc"
                                value={
                                    numberExists(
                                        smartDecision.etc
                                    )
                                        ? `${displayNumber(
    smartDecision.etc,
    2
)} mm`
                                        : "—"
                                }
                            />


                            <DecisionRow
                                label="7-day forecast rain"
                                value={
                                    numberExists(
                                        smartDecision.forecastRain7d
                                    )
                                        ? `${displayNumber(
    smartDecision.forecastRain7d,
    2
)} mm`
                                        : "—"
                                }
                            />


                            <DecisionRow
                                label="Forecast confidence"
                                value={
                                    numberExists(
                                        smartDecision.forecastConfidence
                                    )
                                        ? `${displayNumber(
    smartDecision.forecastConfidence *
    100,
    1
)}%`
                                        : "—"
                                }
                            />


                            <div className="irrigation-uncertainty-box">

                                <span>
                                    UNCERTAINTY
                                </span>

                                <p>
                                    {
                                        smartDecision.uncertaintyNote ||
                                        "—"
                                    }
                                </p>

                            </div>

                        </article>

                    </div>
                )}

            </section>


            {/* =================================================
                PUMP COMPLETION
            ================================================= */}

            <section className="irrigation-section">

                <SectionHeading
                    eyebrow="03 · PUMP VERIFICATION"
                    title="Pump Completion Monitoring"
                    description="Checks the latest telemetry against the configured irrigation target."
                />


                {!pumpCompletion ? (

                    <div className="irrigation-empty-card compact">

                        <span className="irrigation-empty-icon">
                            ⚙️
                        </span>

                        <div>

                            <h3>
                                Pump completion unavailable
                            </h3>

                            <p>
                                No valid latest irrigation
                                telemetry is currently available
                                for completion analysis.
                            </p>

                        </div>

                    </div>

                ) : (

                    <div className="pump-completion-card">

                        <div className="pump-completion-header">

                            <div className="pump-state-large">

                                <span>
                                    PUMP
                                </span>

                                <strong>
                                    {
                                        getPumpStateLabel(
                                            pumpCompletion.pumpState
                                        )
                                    }
                                </strong>

                            </div>


                            <div
                                className={`irrigation-status-pill ${statusClass(
    pumpCompletion.pumpAction
)}`}
                            >
                                {
                                    formatStatus(
                                        pumpCompletion.pumpAction
                                    )
                                }
                            </div>

                        </div>


                        <div className="pump-completion-grid">

                            <MetricMini
                                label="Current Moisture"
                                value={
                                    numberExists(
                                        pumpCompletion.currentMoisture
                                    )
                                        ? `${displayNumber(
    pumpCompletion.currentMoisture,
    2
)}%`
                                        : "—"
                                }
                            />


                            <MetricMini
                                label="Target Moisture"
                                value={
                                    numberExists(
                                        pumpCompletion.targetMoisture
                                    )
                                        ? `${displayNumber(
    pumpCompletion.targetMoisture,
    2
)}%`
                                        : "—"
                                }
                            />


                            <MetricMini
                                label="Target Reached"
                                value={
                                    pumpCompletion.targetReached ===
                                    true
                                        ? "YES"
                                        : pumpCompletion.targetReached ===
                                        false
                                            ? "NO"
                                            : "—"
                                }
                            />


                            <MetricMini
                                label="Device"
                                value={
                                    pumpCompletion.deviceId ||
                                    "—"
                                }
                            />

                        </div>


                        <div className="pump-message">

                            <span>
                                SYSTEM MESSAGE
                            </span>

                            <p>
                                {
                                    pumpCompletion.message ||
                                    "—"
                                }
                            </p>

                        </div>


                        <div className="pump-provenance">

                            Data status:
                            {" "}
                            {
                                pumpCompletion.dataStatus ||
                                "—"
                            }

                        </div>

                    </div>
                )}

            </section>


            {/* =================================================
                CROP WATER + CONFIG
            ================================================= */}

            <section className="irrigation-section">

                <SectionHeading
                    eyebrow="04 · CONFIGURATION"
                    title="Irrigation Water Model"
                    description="Stored farm configuration used by the irrigation decision engine."
                />


                {!config ? (

                    <div className="irrigation-empty-card compact">

                        <span className="irrigation-empty-icon">
                            🧪
                        </span>

                        <div>

                            <h3>
                                No irrigation configuration
                            </h3>

                            <p>
                                No configuration record was
                                returned for this farm.
                            </p>

                        </div>

                    </div>

                ) : (

                    <div className="irrigation-config-grid">

                        <ConfigCard
                            label="Field Capacity"
                            value={
                                numberExists(
                                    config.fieldCapacity
                                )
                                    ? `${displayNumber(
    config.fieldCapacity,
    2
)}%`
                                    : "—"
                            }
                        />

                        <ConfigCard
                            label="Wilting Point"
                            value={
                                numberExists(
                                    config.wiltingPoint
                                )
                                    ? `${displayNumber(
    config.wiltingPoint,
    2
)}%`
                                    : "—"
                            }
                        />

                        <ConfigCard
                            label="MAD"
                            value={
                                numberExists(
                                    config.maxAllowableDepletion
                                )
                                    ? displayNumber(
                                        config.maxAllowableDepletion,
                                        3
                                    )
                                    : "—"
                            }
                        />

                        <ConfigCard
                            label="Root Zone"
                            value={
                                numberExists(
                                    config.rootZoneDepthCm
                                )
                                    ? `${displayNumber(
    config.rootZoneDepthCm,
    1
)} cm`
                                    : "—"
                            }
                        />

                        <ConfigCard
                            label="Target FC Fraction"
                            value={
                                numberExists(
                                    config.targetFieldCapacityFraction
                                )
                                    ? displayNumber(
                                        config.targetFieldCapacityFraction,
                                        3
                                    )
                                    : "—"
                            }
                        />

                        <ConfigCard
                            label="Forecast Horizon"
                            value={
                                numberExists(
                                    config.forecastHorizonDays
                                )
                                    ? `${config.forecastHorizonDays} days`
                                    : "—"
                            }
                        />

                        <ConfigCard
                            label="Forecast Confidence"
                            value={
                                numberExists(
                                    config.minimumForecastConfidence
                                )
                                    ? `${displayNumber(
    config.minimumForecastConfidence *
    100,
    1
)}%`
                                    : "—"
                            }
                        />

                        <ConfigCard
                            label="ET0 Method"
                            value={
                                config.et0Method ||
                                "—"
                            }
                        />

                        <ConfigCard
                            label="Sensor Based"
                            value={
                                config.sensorBased ===
                                true
                                    ? "YES"
                                    : config.sensorBased ===
                                    false
                                        ? "NO"
                                        : "—"
                            }
                        />

                        <ConfigCard
                            label="Source"
                            value={
                                config.source ||
                                "—"
                            }
                        />

                        <ConfigCard
                            label="Data Status"
                            value={
                                config.dataStatus ||
                                "—"
                            }
                        />

                        <ConfigCard
                            label="Readily Available Water"
                            value={
                                numberExists(
                                    config.readilyAvailableWater
                                )
                                    ? displayNumber(
                                        config.readilyAvailableWater,
                                        3
                                    )
                                    : "—"
                            }
                        />

                    </div>
                )}

            </section>


            {/* =================================================
                SOLAR / WEATHER WATER DATA
            ================================================= */}

            <section className="irrigation-section">

                <SectionHeading
                    eyebrow="05 · WATER & ENERGY"
                    title="Supporting Field Signals"
                    description="Additional real measurements returned by the irrigation telemetry stream."
                />


                <div className="irrigation-metric-grid">

                    <MetricCard
                        label="ET0"
                        value={
                            numberExists(
                                telemetry?.et0Mm
                            )
                                ? `${displayNumber(
    telemetry.et0Mm,
    2
)} mm`
                                : "—"
                        }
                        icon="☀️"
                    />

                    <MetricCard
                        label="ETc"
                        value={
                            numberExists(
                                telemetry?.etcMm
                            )
                                ? `${displayNumber(
    telemetry.etcMm,
    2
)} mm`
                                : "—"
                        }
                        icon="🌿"
                    />

                    <MetricCard
                        label="Kc"
                        value={
                            numberExists(
                                telemetry?.kc
                            )
                                ? displayNumber(
                                    telemetry.kc,
                                    3
                                )
                                : "—"
                        }
                        icon="📐"
                    />

                    <MetricCard
                        label="Solar Irradiance"
                        value={
                            numberExists(
                                telemetry?.solarIrradianceWm2
                            )
                                ? `${displayNumber(
    telemetry.solarIrradianceWm2,
    1
)} W/m²`
                                : "—"
                        }
                        icon="☀️"
                    />

                    <MetricCard
                        label="Solar Power"
                        value={
                            numberExists(
                                telemetry?.solarPowerKw
                            )
                                ? `${displayNumber(
    telemetry.solarPowerKw,
    2
)} kW`
                                : "—"
                        }
                        icon="🔋"
                    />

                    <MetricCard
                        label="Cloud Cover"
                        value={
                            numberExists(
                                telemetry?.cloudCoverPercent
                            )
                                ? `${displayNumber(
    telemetry.cloudCoverPercent,
    1
)}%`
                                : "—"
                        }
                        icon="☁️"
                    />

                    <MetricCard
                        label="Storage Level"
                        value={
                            numberExists(
                                telemetry?.storageLevelPercent
                            )
                                ? `${displayNumber(
    telemetry.storageLevelPercent,
    1
)}%`
                                : "—"
                        }
                        icon="🛢️"
                    />

                    <MetricCard
                        label="Battery SoC"
                        value={
                            numberExists(
                                telemetry?.batterySocPercent
                            )
                                ? `${displayNumber(
    telemetry.batterySocPercent,
    1
)}%`
                                : "—"
                        }
                        icon="🔋"
                    />

                </div>

            </section>


            {/* =================================================
                TELEMETRY HISTORY
            ================================================= */}

            <section className="irrigation-section">

                <SectionHeading
                    eyebrow="06 · SENSOR HISTORY"
                    title="Recent Telemetry"
                    description="Recent real telemetry records stored for the farm."
                />


                <div className="irrigation-history-card">

                    {sortedTelemetry.length ===
                    0 ? (

                        <div className="irrigation-empty-card compact">

                            <span>
                                📡
                            </span>

                            <div>

                                <h3>
                                    No telemetry records
                                </h3>

                                <p>
                                    The backend returned no
                                    irrigation telemetry for
                                    this farm.
                                </p>

                            </div>

                        </div>

                    ) : (

                        <div className="irrigation-table-wrapper">

                            <table className="irrigation-table">

                                <thead>

                                <tr>

                                    <th>
                                        Time
                                    </th>

                                    <th>
                                        Device
                                    </th>

                                    <th>
                                        VWC 15
                                    </th>

                                    <th>
                                        VWC 30
                                    </th>

                                    <th>
                                        VWC 60
                                    </th>

                                    <th>
                                        Pump
                                    </th>

                                    <th>
                                        Flow
                                    </th>

                                    <th>
                                        Pressure
                                    </th>

                                    <th>
                                        Status
                                    </th>

                                </tr>

                                </thead>


                                <tbody>

                                {sortedTelemetry.map(
                                    (record) => (

                                        <tr
                                            key={
                                                record.id
                                            }
                                        >

                                            <td>
                                                {
                                                    formatDateTime(
                                                        record.recordedAt
                                                    )
                                                }
                                            </td>

                                            <td>
                                                {
                                                    record.deviceId ||
                                                    "—"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    numberExists(
                                                        record.vwc15cm
                                                    )
                                                        ? `${displayNumber(
    record.vwc15cm,
    1
)}%`
                                                        : "—"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    numberExists(
                                                        record.vwc30cm
                                                    )
                                                        ? `${displayNumber(
    record.vwc30cm,
    1
)}%`
                                                        : "—"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    numberExists(
                                                        record.vwc60cm
                                                    )
                                                        ? `${displayNumber(
    record.vwc60cm,
    1
)}%`
                                                        : "—"
                                                }
                                            </td>

                                            <td>

                                                    <span
                                                        className={`irrigation-table-status ${record.pumpState === 1
    ? "warning"
    : record.pumpState ===
    0
        ? "success"
        : "neutral"
}`}
                                                    >
                                                        {
                                                            getPumpStateLabel(
                                                                record.pumpState
                                                            )
                                                        }
                                                    </span>

                                            </td>

                                            <td>
                                                {
                                                    numberExists(
                                                        record.flowRateLpm
                                                    )
                                                        ? `${displayNumber(
    record.flowRateLpm,
    1
)}`
                                                        : "—"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    numberExists(
                                                        record.linePressureBar
                                                    )
                                                        ? `${displayNumber(
    record.linePressureBar,
    2
)}`
                                                        : "—"
                                                }
                                            </td>

                                            <td>
                                                {
                                                    record.sensorStatus ||
                                                    "—"
                                                }
                                            </td>

                                        </tr>

                                    )
                                )}

                                </tbody>

                            </table>

                        </div>
                    )}

                </div>

            </section>


            {/* =================================================
                IRRIGATION EVENTS
            ================================================= */}

            <section className="irrigation-section">

                <SectionHeading
                    eyebrow="07 · IRRIGATION HISTORY"
                    title="Irrigation Events"
                    description="Actual irrigation events recorded by the backend."
                />


                {sortedEvents.length ===
                0 ? (

                    <div className="irrigation-empty-card">

                        <div className="irrigation-empty-icon">
                            💧
                        </div>

                        <h3>
                            No irrigation events returned
                        </h3>

                        <p>
                            No completed or recorded
                            irrigation event exists for
                            this farm in the backend.
                        </p>

                    </div>

                ) : (

                    <div className="irrigation-event-list">

                        {sortedEvents.map(
                            (event) => (

                                <article
                                    key={event.id}
                                    className="irrigation-event-card"
                                >

                                    <div className="irrigation-event-date">

                                        <strong>
                                            {
                                                formatDateTime(
                                                    event.startTime
                                                )
                                            }
                                        </strong>

                                        <span>
                                            {
                                                event.deviceId ||
                                                "Device unavailable"
                                            }
                                        </span>

                                    </div>


                                    <div className="irrigation-event-main">

                                        <h3>
                                            {
                                                event.triggerReason ||
                                                "Irrigation event"
                                            }
                                        </h3>

                                        <p>
                                            Completion:
                                            {" "}
                                            {
                                                formatStatus(
                                                    event.completionStatus
                                                )
                                            }
                                        </p>

                                        <p>
                                            Pump:
                                            {" "}
                                            {
                                                event.pumpWasOn ===
                                                true
                                                    ? "ON"
                                                    : event.pumpWasOn ===
                                                    false
                                                        ? "OFF"
                                                        : "—"
                                            }
                                        </p>

                                    </div>


                                    <div className="irrigation-event-metrics">

                                        <span>
                                            Duration
                                            {" "}
                                            {
                                                numberExists(
                                                    event.durationMinutes
                                                )
                                                    ? `${displayNumber(
    event.durationMinutes,
    1
)} min`
                                                    : "—"
                                            }
                                        </span>

                                        <span>
                                            Volume
                                            {" "}
                                            {
                                                numberExists(
                                                    event.volumeLiters
                                                )
                                                    ? `${displayNumber(
    event.volumeLiters,
    1
)} L`
                                                    : "—"
                                            }
                                        </span>

                                        <span>
                                            Before
                                            {" "}
                                            {
                                                numberExists(
                                                    event.moistureBefore
                                                )
                                                    ? `${displayNumber(
    event.moistureBefore,
    1
)}%`
                                                    : "—"
                                            }
                                        </span>

                                        <span>
                                            After
                                            {" "}
                                            {
                                                numberExists(
                                                    event.moistureAfter
                                                )
                                                    ? `${displayNumber(
    event.moistureAfter,
    1
)}%`
                                                    : "—"
                                            }
                                        </span>

                                    </div>


                                    <div className="irrigation-event-status">

                                        <span
                                            className={`irrigation-status-pill ${statusClass(
    event.completionStatus
)}`}
                                        >
                                            {
                                                formatStatus(
                                                    event.completionStatus
                                                )
                                            }
                                        </span>

                                    </div>

                                </article>

                            )
                        )}

                    </div>
                )}

            </section>


            {/* =================================================
                FRONTEND / BACKEND VERIFICATION
            ================================================= */}

            <section className="irrigation-section">

                <SectionHeading
                    eyebrow="08 · SYSTEM VERIFICATION"
                    title="IoT & Backend Readiness"
                    description="Live verification of the selected farm's irrigation services and hardware connection state."
                />


                <div className="irrigation-status-grid">

                    <StatusCard
                        label="BACKEND API"
                        value={
                            loading
                                ? "CHECKING"
                                : farm
                                    ? "CONNECTED"
                                    : "UNAVAILABLE"
                        }
                        tone={
                            loading
                                ? "neutral"
                                : farm
                                    ? "success"
                                    : "danger"
                        }
                        icon="🌐"
                    />


                    <StatusCard
                        label="TELEMETRY"
                        value={
                            loading
                                ? "CHECKING"
                                : telemetry
                                    ? "AVAILABLE"
                                    : telemetryHistory.length > 0
                                        ? "HISTORY ONLY"
                                        : "NO READINGS"
                        }
                        tone={
                            loading
                                ? "neutral"
                                : telemetry
                                    ? "success"
                                    : "info"
                        }
                        icon="📡"
                    />


                    <StatusCard
                        label="IRRIGATION CONFIG"
                        value={
                            loading
                                ? "CHECKING"
                                : config
                                    ? "CONFIGURED"
                                    : "NOT CONFIGURED"
                        }
                        tone={
                            loading
                                ? "neutral"
                                : config
                                    ? "success"
                                    : "info"
                        }
                        icon="🧪"
                    />


                    <StatusCard
                        label="SMART IRRIGATION"
                        value={
                            loading
                                ? "CHECKING"
                                : smartDecision
                                    ? formatStatus(
                                        smartDecision.fieldStatus
                                    )
                                    : "NO DECISION"
                        }
                        tone={
                            loading
                                ? "neutral"
                                : smartDecision
                                    ? statusClass(
                                        smartDecision.fieldStatus
                                    )
                                    : "info"
                        }
                        icon="🧠"
                    />


                    <StatusCard
                        label="PUMP COMPLETION"
                        value={
                            loading
                                ? "CHECKING"
                                : pumpCompletion
                                    ? formatStatus(
                                        pumpCompletion.pumpAction
                                    )
                                    : "WAITING FOR TELEMETRY"
                        }
                        tone={
                            loading
                                ? "neutral"
                                : pumpCompletion
                                    ? statusClass(
                                        pumpCompletion.pumpAction
                                    )
                                    : "info"
                        }
                        icon="⚙️"
                    />


                    <StatusCard
                        label="IRRIGATION EVENTS"
                        value={
                            loading
                                ? "CHECKING"
                                : events.length > 0
                                    ? `${events.length} RECORDS`
                                    : "NO EVENTS"
                        }
                        tone={
                            loading
                                ? "neutral"
                                : events.length > 0
                                    ? "success"
                                    : "info"
                        }
                        icon="💧"
                    />

                </div>


                <div className="irrigation-provenance">

                    <div className="irrigation-provenance-icon">
                        ✓
                    </div>

                    <div>

                        <strong>
                            Physical IoT hardware status
                        </strong>

                        <p>
                            Sensor hardware is not connected yet.
                            The software pipeline is ready for real
                            ESP32 / field-sensor telemetry through
                            the existing irrigation telemetry API.
                            No sensor measurements are generated by
                            this verification panel.
                        </p>

                        <small>
                            Last checked: {" "}
                            {lastCheckedAt
                                ? formatDateTime(
                                    lastCheckedAt
                                )
                                : "—"}
                        </small>

                    </div>

                    <button
                        type="button"
                        disabled={
                            refreshing || loading
                        }
                        onClick={
                            handleCheckNow
                        }
                    >
                        {refreshing
                            ? "Checking..."
                            : "Check Now"}
                    </button>

                </div>

            </section>


            {/* =================================================
                FOOTER PROVENANCE
            ================================================= */}

            <section className="irrigation-provenance">

                <div className="irrigation-provenance-icon">
                    ✓
                </div>

                <div>

                    <strong>
                        IoT-ready irrigation architecture
                    </strong>

                    <p>
                        This dashboard displays irrigation
                        values returned by the AgriNexus
                        backend. When ESP32/field sensors are
                        connected later, their telemetry can be
                        stored through the existing telemetry
                        endpoint without redesigning this
                        dashboard.
                    </p>

                </div>


                <button
                    type="button"
                    disabled={refreshing}
                    onClick={() =>
                        loadAll(false)
                    }
                >
                    {
                        refreshing
                            ? "Refreshing..."
                            : "Refresh Data"
                    }
                </button>

            </section>

        </div>
    );
}


/* =========================================================
   SMALL COMPONENTS
   ========================================================= */

function SectionHeading({
                            eyebrow,
                            title,
                            description,
                        }) {

    return (
        <div className="irrigation-section-heading">

            <div>

                <span>
                    {eyebrow}
                </span>

                <h2>
                    {title}
                </h2>

                <p>
                    {description}
                </p>

            </div>

        </div>
    );
}


function StatusCard({
                        label,
                        value,
                        tone,
                        icon,
                    }) {

    return (
        <article className="irrigation-status-card">

            <div className="irrigation-status-icon">
                {icon}
            </div>

            <div>

                <span>
                    {label}
                </span>

                <strong
                    className={
                        `irrigation-value-${tone}`
                    }
                >
                    {value}
                </strong>

            </div>

        </article>
    );
}


function MetricCard({
                        label,
                        value,
                        icon,
                        emphasis = false,
                    }) {

    return (
        <article
            className={`irrigation-metric-card ${
    emphasis
        ? "emphasis"
        : ""
}`}
        >

            <div className="irrigation-metric-icon">
                {icon}
            </div>

            <span>
                {label}
            </span>

            <strong>
                {value}
            </strong>

        </article>
    );
}


function MetricMini({
                        label,
                        value,
                    }) {

    return (
        <div className="irrigation-mini-metric">

            <span>
                {label}
            </span>

            <strong>
                {value}
            </strong>

        </div>
    );
}


function DecisionRow({
                         label,
                         value,
                     }) {

    return (
        <div className="irrigation-decision-row">

            <span>
                {label}
            </span>

            <strong>
                {value}
            </strong>

        </div>
    );
}


function ConfigCard({
                        label,
                        value,
                    }) {

    return (
        <article className="irrigation-config-card">

            <span>
                {label}
            </span>

            <strong>
                {value}
            </strong>

        </article>
    );
}


function LoadingCard() {

    return (
        <div className="irrigation-loading-card">

            <div className="irrigation-spinner" />

            <span>
                Loading real irrigation telemetry...
            </span>

        </div>
    );
}


/*
 * apiClient import is intentionally kept here because
 * this component needs the farm endpoint.
 */
