import {
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    useParams,
    useNavigate,
} from "react-router-dom";

import {
    useLanguage,
} from "../../context/LanguageContext";

import {
    useFields,
} from "../../context/FieldsContext";

import {
    useReports,
} from "../../context/ReportsContext";

import FieldCard
    from "../../components/FieldCard";

import BottomNavBar
    from "../../components/BottomNavBar";

import {
    apiRequest,
} from "../../api/apiClient";

import {
    getCurrentCropForFarm,
} from "../../api/cropHistoryApi";

import "./FieldDetail.css";


/* =========================================================
   HELPERS
========================================================= */

const isObject = (value) =>
    value !== null &&
    typeof value === "object" &&
    !Array.isArray(value);

const isNumber = (value) =>
    typeof value === "number" &&
    Number.isFinite(value);

const toNumberOrNull = (value) => {
    if (value === null || value === undefined || value === "") {
        return null;
    }

    const number = Number(value);

    return Number.isFinite(number)
        ? number
        : null;
};

const formatNumber = (
    value,
    maximumFractionDigits = 2
) => {
    const number = toNumberOrNull(value);

    if (number === null) {
        return "—";
    }

    return new Intl.NumberFormat(
        undefined,
        {
            maximumFractionDigits,
        }
    ).format(number);
};

const formatDate = (value) => {
    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "—";
    }

    return date.toLocaleDateString(
        undefined,
        {
            day: "2-digit",
            month: "short",
            year: "numeric",
        }
    );
};

const formatDateTime = (value) => {
    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "—";
    }

    return date.toLocaleString(
        undefined,
        {
            day: "2-digit",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        }
    );
};

const normalizeArrayResponse = (response) => {
    if (Array.isArray(response)) {
        return response;
    }

    if (
        isObject(response) &&
        Array.isArray(response.content)
    ) {
        return response.content;
    }

    if (
        isObject(response) &&
        Array.isArray(response.data)
    ) {
        return response.data;
    }

    if (
        isObject(response) &&
        Array.isArray(response.items)
    ) {
        return response.items;
    }

    if (
        isObject(response) &&
        Array.isArray(response.results)
    ) {
        return response.results;
    }

    if (response && isObject(response)) {
        return [response];
    }

    return [];
};

const unwrapResponse = (response) => {
    if (!response) {
        return null;
    }

    if (
        isObject(response) &&
        response.data &&
        isObject(response.data) &&
        !Array.isArray(response.data)
    ) {
        return response.data;
    }

    return response;
};

const firstValue = (...values) => {
    for (const value of values) {
        if (
            value !== null &&
            value !== undefined &&
            value !== ""
        ) {
            return value;
        }
    }

    return null;
};

const hasValue = (value) =>
    value !== null &&
    value !== undefined &&
    value !== "";

const getRiskLevelClass = (riskLevel) => {
    const level = String(
        riskLevel || ""
    ).toLowerCase();

    if (level === "critical") {
        return "critical";
    }

    if (level === "high") {
        return "high";
    }

    if (level === "medium") {
        return "medium";
    }

    if (level === "low") {
        return "low";
    }

    return "neutral";
};

const getHealthClass = (status) => {
    const normalized = String(
        status || ""
    ).toLowerCase();

    if (
        normalized.includes("healthy") ||
        normalized === "good" ||
        normalized === "normal"
    ) {
        return "healthy";
    }

    if (
        normalized.includes("attention") ||
        normalized.includes("warning") ||
        normalized === "fair"
    ) {
        return "attention";
    }

    if (
        normalized.includes("critical") ||
        normalized.includes("poor")
    ) {
        return "critical";
    }

    return "neutral";
};


/* =========================================================
   COMPONENT
========================================================= */

export default function FieldDetail() {
    const {
        id,
    } = useParams();

    const navigate = useNavigate();

    const {
        language,
    } = useLanguage();

    const {
        fields,
        currentField,
        selectField,
        reloadFields,
    } = useFields();

    const {
        reports,
    } = useReports();

    /* =====================================================
       STATE
    ===================================================== */

    const [
        farm,
        setFarm,
    ] = useState(null);

    const [
        passport,
        setPassport,
    ] = useState(null);

    const [
        weather,
        setWeather,
    ] = useState(null);

    const [
        weatherRadar,
        setWeatherRadar,
    ] = useState(null);

    const [
        soil,
        setSoil,
    ] = useState(null);

    const [
        soilSample,
        setSoilSample,
    ] = useState(null);

    const [
        irrigationConfig,
        setIrrigationConfig,
    ] = useState(null);

    const [
        latestTelemetry,
        setLatestTelemetry,
    ] = useState(null);

    const [
        irrigationEvents,
        setIrrigationEvents,
    ] = useState([]);

    const [
        fertilizerRecommendation,
        setFertilizerRecommendation,
    ] = useState(null);

    const [
        fertilizerSchedule,
        setFertilizerSchedule,
    ] = useState(null);

    const [
        cropHistory,
        setCropHistory,
    ] = useState([]);

    /*
     * =====================================================
     * CURRENT CROP
     * =====================================================
     *
     * Single source of truth:
     * getCurrentCropForFarm(id)
     *
     * Only the real active/unharvested crop-history record
     * is treated as the current crop.
     *
     * No passport fallback.
     * No farm fallback.
     * No previous crop fallback.
     * No hardcoded crop.
     */

    const [
        currentCropRecord,
        setCurrentCropRecord,
    ] = useState(null);

    const [
        loading,
        setLoading,
    ] = useState(true);

    const [
        error,
        setError,
    ] = useState("");

    const [
        activeSection,
        setActiveSection,
    ] = useState("overview");

    const [
        refreshing,
        setRefreshing,
    ] = useState(false);

    /* =====================================================
       LANGUAGE
    ===================================================== */

    const isHindi =
        language === "hi" ||
        language === "hindi";

    const text = useMemo(
        () => ({
            back: isHindi
                ? "वापस"
                : "Back",

            overview: isHindi
                ? "अवलोकन"
                : "Overview",

            health: isHindi
                ? "खेत स्वास्थ्य"
                : "Field Health",

            soil: isHindi
                ? "मिट्टी"
                : "Soil",

            weather: isHindi
                ? "मौसम"
                : "Weather",

            irrigation: isHindi
                ? "सिंचाई"
                : "Irrigation",

            fertilizer: isHindi
                ? "उर्वरक"
                : "Fertilizer",

            history: isHindi
                ? "इतिहास"
                : "History",

            refresh: isHindi
                ? "रिफ्रेश"
                : "Refresh",

            loading: isHindi
                ? "खेत का डेटा लोड हो रहा है..."
                : "Loading field data...",

            unavailable: isHindi
                ? "डेटा उपलब्ध नहीं है"
                : "Data unavailable",

            noData: isHindi
                ? "कोई वास्तविक डेटा उपलब्ध नहीं है"
                : "No real data available",

            currentCrop: isHindi
                ? "वर्तमान फसल"
                : "Current Crop",

            crop: isHindi
                ? "फसल"
                : "Crop",

            season: isHindi
                ? "सीज़न"
                : "Season",

            stage: isHindi
                ? "अवस्था"
                : "Stage",

            area: isHindi
                ? "क्षेत्रफल"
                : "Area",

            soilHealth: isHindi
                ? "मिट्टी स्वास्थ्य"
                : "Soil Health",

            soilPh: isHindi
                ? "मिट्टी pH"
                : "Soil pH",

            organicCarbon: isHindi
                ? "ऑर्गेनिक कार्बन"
                : "Organic Carbon",

            nitrogen: isHindi
                ? "नाइट्रोजन"
                : "Nitrogen",

            phosphorus: isHindi
                ? "फॉस्फोरस"
                : "Phosphorus",

            potassium: isHindi
                ? "पोटैशियम"
                : "Potassium",

            electricalConductivity: isHindi
                ? "EC"
                : "Electrical Conductivity",

            moisture: isHindi
                ? "नमी"
                : "Moisture",

            temperature: isHindi
                ? "तापमान"
                : "Temperature",

            humidity: isHindi
                ? "आर्द्रता"
                : "Humidity",

            rainfall: isHindi
                ? "वर्षा"
                : "Rainfall",

            windSpeed: isHindi
                ? "हवा की गति"
                : "Wind Speed",

            risk: isHindi
                ? "जोखिम"
                : "Risk",

            recommendation: isHindi
                ? "सिफारिश"
                : "Recommendation",

            irrigationSettings: isHindi
                ? "सिंचाई सेटिंग"
                : "Irrigation Settings",

            irrigationEvents: isHindi
                ? "सिंचाई घटनाएँ"
                : "Irrigation Events",

            fertilizerRecommendation: isHindi
                ? "उर्वरक सिफारिश"
                : "Fertilizer Recommendation",

            fertilizerSchedule: isHindi
                ? "उर्वरक शेड्यूल"
                : "Fertilizer Schedule",

            cropHistory: isHindi
                ? "फसल इतिहास"
                : "Crop History",

            viewHistory: isHindi
                ? "पूरा इतिहास देखें"
                : "View Full History",

            openWeatherRadar: isHindi
                ? "मौसम रडार खोलें"
                : "Open Weather Radar",

            openIrrigation: isHindi
                ? "स्मार्ट सिंचाई खोलें"
                : "Open Smart Irrigation",

            openFertilizer: isHindi
                ? "उर्वरक खोलें"
                : "Open Fertilizer",

            openPassport: isHindi
                ? "हेल्थ पासपोर्ट खोलें"
                : "Open Health Passport",

            noCurrentCrop: isHindi
                ? "वर्तमान फसल रिकॉर्ड उपलब्ध नहीं है"
                : "No current crop record available",

            realDataOnly: isHindi
                ? "यह स्क्रीन केवल बैकएंड के वास्तविक डेटा का उपयोग करती है।"
                : "This screen uses only real backend data.",
        }),
        [
            isHindi,
        ]
    );

    /* =====================================================
       LOCAL FIELD
    ===================================================== */

    const localField = useMemo(
        () => {
            if (!Array.isArray(fields)) {
                return null;
            }

            return (
                fields.find(
                    (item) =>
                        String(item?.id) ===
                        String(id)
                ) ||
                null
            );
        },
        [
            fields,
            id,
        ]
    );

    /* =====================================================
       LOAD FIELD DATA
    ===================================================== */

    useEffect(
        () => {
            let cancelled = false;

            const loadFieldData = async () => {
                if (!id) {
                    setLoading(false);
                    setError(
                        "Field ID is missing."
                    );
                    return;
                }

                setLoading(true);
                setError("");

                try {
                    try {
                        selectField(id);
                    } catch (selectError) {
                        console.warn(
                            "Unable to select field:",
                            selectError
                        );
                    }

                    const results =
                        await Promise.allSettled(
                            [
                                apiRequest(
                                    `/api/farms/${id}`
                                ),

                                apiRequest(
                                    `/api/field-health-passport/farm/${id}`
                                ),

                                apiRequest(
                                    `/api/weather/farm/${id}`
                                ),

                                apiRequest(
                                    `/api/weather-radar/farm/${id}`
                                ),

                                apiRequest(
                                    `/api/soil-intelligence/farm/${id}`
                                ),

                                apiRequest(
                                    `/api/soil-samples/farm/${id}/latest`
                                ),

                                apiRequest(
                                    `/api/irrigation-config/farm/${id}`
                                ),

                                apiRequest(
                                    `/api/irrigation-telemetry/farm/${id}/latest`
                                ),

                                apiRequest(
                                    `/api/irrigation-events/farm/${id}`
                                ),

                                apiRequest(
                                    `/api/fertilizer-recommendations/farm/${id}/latest`
                                ),

                                apiRequest(
                                    `/api/fertilizer-schedules/farm/${id}/next`
                                ),

                                apiRequest(
                                    `/api/crop-history/farm/${id}`
                                ),
                            ]
                        );

                    if (cancelled) {
                        return;
                    }

                    const [
                        farmResult,
                        passportResult,
                        weatherResult,
                        radarResult,
                        soilResult,
                        soilSampleResult,
                        irrigationConfigResult,
                        telemetryResult,
                        irrigationEventsResult,
                        fertilizerRecommendationResult,
                        fertilizerScheduleResult,
                        cropHistoryResult,
                    ] = results;

                    /* -----------------------------------------
                       FARM
                    ----------------------------------------- */

                    if (
                        farmResult.status ===
                        "fulfilled"
                    ) {
                        setFarm(
                            unwrapResponse(
                                farmResult.value
                            )
                        );
                    } else {
                        console.warn(
                            "Farm request failed:",
                            farmResult.reason
                        );

                        setFarm(null);
                    }

                    /* -----------------------------------------
                       PASSPORT
                    ----------------------------------------- */

                    if (
                        passportResult.status ===
                        "fulfilled"
                    ) {
                        setPassport(
                            unwrapResponse(
                                passportResult.value
                            )
                        );
                    } else {
                        setPassport(null);
                    }

                    /* -----------------------------------------
                       WEATHER
                    ----------------------------------------- */

                    if (
                        weatherResult.status ===
                        "fulfilled"
                    ) {
                        setWeather(
                            unwrapResponse(
                                weatherResult.value
                            )
                        );
                    } else {
                        setWeather(null);
                    }

                    /* -----------------------------------------
                       WEATHER RADAR
                    ----------------------------------------- */

                    if (
                        radarResult.status ===
                        "fulfilled"
                    ) {
                        setWeatherRadar(
                            unwrapResponse(
                                radarResult.value
                            )
                        );
                    } else {
                        setWeatherRadar(null);
                    }

                    /* -----------------------------------------
                       SOIL
                    ----------------------------------------- */

                    if (
                        soilResult.status ===
                        "fulfilled"
                    ) {
                        setSoil(
                            unwrapResponse(
                                soilResult.value
                            )
                        );
                    } else {
                        setSoil(null);
                    }

                    /* -----------------------------------------
                       SOIL SAMPLE
                    ----------------------------------------- */

                    if (
                        soilSampleResult.status ===
                        "fulfilled"
                    ) {
                        setSoilSample(
                            unwrapResponse(
                                soilSampleResult.value
                            )
                        );
                    } else {
                        setSoilSample(null);
                    }

                    /* -----------------------------------------
                       IRRIGATION CONFIG
                    ----------------------------------------- */

                    if (
                        irrigationConfigResult.status ===
                        "fulfilled"
                    ) {
                        setIrrigationConfig(
                            unwrapResponse(
                                irrigationConfigResult.value
                            )
                        );
                    } else {
                        setIrrigationConfig(null);
                    }

                    /* -----------------------------------------
                       IRRIGATION TELEMETRY
                    ----------------------------------------- */

                    if (
                        telemetryResult.status ===
                        "fulfilled"
                    ) {
                        setLatestTelemetry(
                            unwrapResponse(
                                telemetryResult.value
                            )
                        );
                    } else {
                        setLatestTelemetry(null);
                    }

                    /* -----------------------------------------
                       IRRIGATION EVENTS
                    ----------------------------------------- */

                    if (
                        irrigationEventsResult.status ===
                        "fulfilled"
                    ) {
                        setIrrigationEvents(
                            normalizeArrayResponse(
                                irrigationEventsResult.value
                            )
                        );
                    } else {
                        setIrrigationEvents([]);
                    }

                    /* -----------------------------------------
                       FERTILIZER RECOMMENDATION
                    ----------------------------------------- */

                    if (
                        fertilizerRecommendationResult.status ===
                        "fulfilled"
                    ) {
                        setFertilizerRecommendation(
                            unwrapResponse(
                                fertilizerRecommendationResult.value
                            )
                        );
                    } else {
                        setFertilizerRecommendation(
                            null
                        );
                    }

                    /* -----------------------------------------
                       FERTILIZER SCHEDULE
                    ----------------------------------------- */

                    if (
                        fertilizerScheduleResult.status ===
                        "fulfilled"
                    ) {
                        setFertilizerSchedule(
                            unwrapResponse(
                                fertilizerScheduleResult.value
                            )
                        );
                    } else {
                        setFertilizerSchedule(
                            null
                        );
                    }

                    /* -----------------------------------------
                       CROP HISTORY
                    ----------------------------------------- */

                    if (
                        cropHistoryResult.status ===
                        "fulfilled"
                    ) {
                        const normalizedHistory =
                            normalizeArrayResponse(
                                cropHistoryResult.value
                            );

                        setCropHistory(
                            normalizedHistory
                        );
                    } else {
                        setCropHistory([]);
                    }

                    /* -----------------------------------------
                       CURRENT CROP
                    ----------------------------------------- */

                    let realCurrentCrop = null;

                    try {
                        realCurrentCrop =
                            await getCurrentCropForFarm(
                                id
                            );
                    } catch (
                        currentCropError
                        ) {
                        console.warn(
                            "Current crop request failed:",
                            currentCropError
                        );
                    }

                    if (!cancelled) {
                        setCurrentCropRecord(
                            realCurrentCrop
                        );
                    }
                } catch (requestError) {
                    if (!cancelled) {
                        console.error(
                            "Failed to load field:",
                            requestError
                        );

                        setError(
                            requestError?.message ||
                            "Unable to load field data."
                        );
                    }
                } finally {
                    if (!cancelled) {
                        setLoading(false);
                    }
                }
            };

            loadFieldData();

            return () => {
                cancelled = true;
            };
        },
        [
            id,
            selectField,
        ]
    );

    /* =====================================================
       REFRESH
    ===================================================== */

    const handleRefresh = async () => {
        if (!id) {
            return;
        }

        setRefreshing(true);

        try {
            const results =
                await Promise.allSettled(
                    [
                        apiRequest(
                            `/api/farms/${id}`
                        ),

                        apiRequest(
                            `/api/field-health-passport/farm/${id}`
                        ),

                        apiRequest(
                            `/api/weather/farm/${id}`
                        ),

                        apiRequest(
                            `/api/weather-radar/farm/${id}`
                        ),

                        apiRequest(
                            `/api/soil-intelligence/farm/${id}`
                        ),

                        apiRequest(
                            `/api/soil-samples/farm/${id}/latest`
                        ),

                        apiRequest(
                            `/api/irrigation-config/farm/${id}`
                        ),

                        apiRequest(
                            `/api/irrigation-telemetry/farm/${id}/latest`
                        ),

                        apiRequest(
                            `/api/irrigation-events/farm/${id}`
                        ),

                        apiRequest(
                            `/api/fertilizer-recommendations/farm/${id}/latest`
                        ),

                        apiRequest(
                            `/api/fertilizer-schedules/farm/${id}/next`
                        ),

                        apiRequest(
                            `/api/crop-history/farm/${id}`
                        ),
                    ]
                );

            const [
                farmResult,
                passportResult,
                weatherResult,
                radarResult,
                soilResult,
                soilSampleResult,
                irrigationConfigResult,
                telemetryResult,
                irrigationEventsResult,
                fertilizerRecommendationResult,
                fertilizerScheduleResult,
                cropHistoryResult,
            ] = results;

            if (
                farmResult.status ===
                "fulfilled"
            ) {
                setFarm(
                    unwrapResponse(
                        farmResult.value
                    )
                );
            }

            if (
                passportResult.status ===
                "fulfilled"
            ) {
                setPassport(
                    unwrapResponse(
                        passportResult.value
                    )
                );
            }

            if (
                weatherResult.status ===
                "fulfilled"
            ) {
                setWeather(
                    unwrapResponse(
                        weatherResult.value
                    )
                );
            }

            if (
                radarResult.status ===
                "fulfilled"
            ) {
                setWeatherRadar(
                    unwrapResponse(
                        radarResult.value
                    )
                );
            }

            if (
                soilResult.status ===
                "fulfilled"
            ) {
                setSoil(
                    unwrapResponse(
                        soilResult.value
                    )
                );
            }

            if (
                soilSampleResult.status ===
                "fulfilled"
            ) {
                setSoilSample(
                    unwrapResponse(
                        soilSampleResult.value
                    )
                );
            }

            if (
                irrigationConfigResult.status ===
                "fulfilled"
            ) {
                setIrrigationConfig(
                    unwrapResponse(
                        irrigationConfigResult.value
                    )
                );
            }

            if (
                telemetryResult.status ===
                "fulfilled"
            ) {
                setLatestTelemetry(
                    unwrapResponse(
                        telemetryResult.value
                    )
                );
            }

            if (
                irrigationEventsResult.status ===
                "fulfilled"
            ) {
                setIrrigationEvents(
                    normalizeArrayResponse(
                        irrigationEventsResult.value
                    )
                );
            }

            if (
                fertilizerRecommendationResult.status ===
                "fulfilled"
            ) {
                setFertilizerRecommendation(
                    unwrapResponse(
                        fertilizerRecommendationResult.value
                    )
                );
            }

            if (
                fertilizerScheduleResult.status ===
                "fulfilled"
            ) {
                setFertilizerSchedule(
                    unwrapResponse(
                        fertilizerScheduleResult.value
                    )
                );
            }

            if (
                cropHistoryResult.status ===
                "fulfilled"
            ) {
                setCropHistory(
                    normalizeArrayResponse(
                        cropHistoryResult.value
                    )
                );
            }

            try {
                const latestCurrentCrop =
                    await getCurrentCropForFarm(
                        id
                    );

                setCurrentCropRecord(
                    latestCurrentCrop
                );
            } catch (
                currentCropError
                ) {
                console.warn(
                    "Unable to refresh current crop:",
                    currentCropError
                );

                setCurrentCropRecord(
                    null
                );
            }

            try {
                await reloadFields();
            } catch (reloadError) {
                console.warn(
                    "Unable to refresh field list:",
                    reloadError
                );
            }
        } catch (refreshError) {
            console.error(
                "Refresh failed:",
                refreshError
            );
        } finally {
            setRefreshing(false);
        }
    };

    /* =====================================================
       CURRENT CROP
       REAL BACKEND ONLY
    ===================================================== */

    const currentCrop =
        currentCropRecord?.cropName ??
        currentCropRecord?.crop?.name ??
        null;

    const currentCropStage =
        currentCropRecord?.cropStage ??
        currentCropRecord?.stage ??
        null;

    const currentCropSeason =
        currentCropRecord?.season ??
        null;

    const currentCropSowingDate =
        currentCropRecord?.sowingDate ??
        null;

    const currentCropHarvestDate =
        currentCropRecord?.harvestDate ??
        null;

    /* =====================================================
       FIELD DATA
       REAL BACKEND / SELECTED FIELD ONLY
    ===================================================== */

    const fieldData = useMemo(
        () => {
            const backendFarm =
                isObject(farm)
                    ? farm
                    : {};

            const contextField =
                isObject(localField)
                    ? localField
                    : {};

            return {
                id:
                    backendFarm?.id ??
                    contextField?.id ??
                    id ??
                    null,

                name:
                    backendFarm?.name ??
                    contextField?.name ??
                    null,

                location:
                    backendFarm?.location ??
                    contextField?.location ??
                    null,

                area:
                    firstValue(
                        backendFarm?.areaAcres,
                        backendFarm?.area,
                        contextField?.areaAcres,
                        contextField?.area
                    ),

                latitude:
                    firstValue(
                        backendFarm?.latitude,
                        contextField?.latitude
                    ),

                longitude:
                    firstValue(
                        backendFarm?.longitude,
                        contextField?.longitude
                    ),

                farmerId:
                    firstValue(
                        backendFarm?.farmerId,
                        contextField?.farmerId
                    ),

                createdAt:
                    firstValue(
                        backendFarm?.createdAt,
                        contextField?.createdAt
                    ),
            };
        },
        [
            farm,
            localField,
            id,
        ]
    );

    /* =====================================================
       PASSPORT VALUES
    ===================================================== */

    const healthScore = firstValue(
        passport?.healthScore,
        passport?.score,
        passport?.fieldHealthScore
    );

    const healthStatus = firstValue(
        passport?.status,
        passport?.healthStatus,
        passport?.fieldHealthStatus
    );

    const healthStatusClass =
        getHealthClass(
            healthStatus
        );

    /* =====================================================
       SOIL VALUES
    ===================================================== */

    const soilData = {
        pH: firstValue(
            soilSample?.ph,
            soilSample?.pH,
            soil?.ph,
            soil?.pH,
            passport?.soilPh
        ),

        organicCarbon: firstValue(
            soilSample?.organicCarbon,
            soilSample?.organicCarbonPercent,
            soil?.organicCarbon,
            soil?.organicCarbonPercent
        ),

        nitrogen: firstValue(
            soilSample?.nitrogen,
            soil?.nitrogen,
            passport?.nitrogen
        ),

        phosphorus: firstValue(
            soilSample?.phosphorus,
            soil?.phosphorus,
            passport?.phosphorus
        ),

        potassium: firstValue(
            soilSample?.potassium,
            soil?.potassium,
            passport?.potassium
        ),

        electricalConductivity: firstValue(
            soilSample?.electricalConductivity,
            soilSample?.ec,
            soil?.electricalConductivity,
            soil?.ec,
            passport?.electricalConductivity
        ),

        moisture: firstValue(
            soilSample?.moisture,
            soilSample?.soilMoisture,
            soil?.moisture,
            soil?.soilMoisture
        ),

        recordedAt: firstValue(
            soilSample?.recordedAt,
            soilSample?.sampledAt,
            soilSample?.createdAt
        ),
    };

    /* =====================================================
       WEATHER VALUES
    ===================================================== */

    const weatherData = {
        temperature: firstValue(
            weather?.temperature,
            weather?.temp,
            weatherRadar?.temperature
        ),

        feelsLike: firstValue(
            weather?.feelsLike,
            weather?.feels_like,
            weatherRadar?.feelsLike
        ),

        humidity: firstValue(
            weather?.humidity,
            weatherRadar?.humidity
        ),

        rainfall: firstValue(
            weather?.rainfall,
            weather?.rain,
            weatherRadar?.rainfall
        ),

        windSpeed: firstValue(
            weather?.windSpeed,
            weather?.wind_speed,
            weatherRadar?.windSpeed
        ),

        windDirection: firstValue(
            weather?.windDirection,
            weather?.wind_direction,
            weatherRadar?.windDirection
        ),

        pressure: firstValue(
            weather?.pressure,
            weatherRadar?.pressure
        ),

        weatherCondition: firstValue(
            weather?.weatherCondition,
            weather?.condition,
            weather?.main,
            weatherRadar?.weatherCondition
        ),

        weatherDescription: firstValue(
            weather?.weatherDescription,
            weather?.description,
            weatherRadar?.weatherDescription
        ),

        recordedAt: firstValue(
            weather?.recordedAt,
            weather?.observedAt,
            weather?.createdAt
        ),
    };

    /* =====================================================
       WEATHER RISKS
    ===================================================== */

    const rawRisks =
        weatherRadar?.risks ??
        weatherRadar?.weatherRisks ??
        weatherRadar?.activeRisks ??
        weatherRadar?.risk ??
        [];

    const risks =
        normalizeArrayResponse(
            rawRisks
        );

    const normalizedRisks =
        risks
            .filter(Boolean)
            .map(
                (
                    risk,
                    index
                ) => ({
                    id:
                        risk?.id ??
                        `risk-${index}`,

                    title:
                        firstValue(
                            risk?.title,
                            risk?.name,
                            risk?.riskType
                        ),

                    description:
                        firstValue(
                            risk?.description,
                            risk?.details
                        ),

                    recommendation:
                        firstValue(
                            risk?.recommendation,
                            risk?.action
                        ),

                    riskType:
                        firstValue(
                            risk?.riskType,
                            risk?.type
                        ),

                    riskLevel:
                        firstValue(
                            risk?.riskLevel,
                            risk?.level
                        ),

                    riskScore:
                        firstValue(
                            risk?.riskScore,
                            risk?.score
                        ),

                    detectedAt:
                        firstValue(
                            risk?.detectedAt,
                            risk?.createdAt
                        ),

                    validUntil:
                        firstValue(
                            risk?.validUntil,
                            risk?.expiresAt
                        ),
                })
            );

    /* =====================================================
       IRRIGATION
    ===================================================== */

    const irrigationData = {
        mode: firstValue(
            irrigationConfig?.mode,
            irrigationConfig?.irrigationMode
        ),

        schedule:
            irrigationConfig?.schedule ??
            irrigationConfig?.irrigationSchedule ??
            null,

        duration: firstValue(
            irrigationConfig?.durationMinutes,
            irrigationConfig?.duration,
            irrigationConfig?.wateringDuration
        ),

        frequency: firstValue(
            irrigationConfig?.frequency,
            irrigationConfig?.frequencyDays
        ),

        soilMoisture: firstValue(
            latestTelemetry?.soilMoisture,
            latestTelemetry?.moisture,
            irrigationConfig?.currentMoisture
        ),

        flowRate: firstValue(
            latestTelemetry?.flowRate,
            latestTelemetry?.waterFlow
        ),

        pumpStatus: firstValue(
            latestTelemetry?.pumpStatus,
            latestTelemetry?.status
        ),

        lastWateredAt: firstValue(
            latestTelemetry?.recordedAt,
            latestTelemetry?.timestamp
        ),
    };

    /* =====================================================
       FERTILIZER
    ===================================================== */

    const normalizedFertilizerRecommendation =
        useMemo(
            () => {
                if (
                    !fertilizerRecommendation ||
                    !isObject(
                        fertilizerRecommendation
                    )
                ) {
                    return null;
                }

                const serialized =
                    JSON.stringify(
                        fertilizerRecommendation
                    ).toUpperCase();

                if (
                    serialized.includes("TEST") ||
                    serialized.includes("DEMO")
                ) {
                    return null;
                }

                return fertilizerRecommendation;
            },
            [
                fertilizerRecommendation,
            ]
        );

    const fertilizerData = {
        crop:
            normalizedFertilizerRecommendation?.cropName ??
            normalizedFertilizerRecommendation?.crop?.name ??
            null,

        recommendation:
            firstValue(
                normalizedFertilizerRecommendation?.recommendation,
                normalizedFertilizerRecommendation?.action,
                normalizedFertilizerRecommendation?.message
            ),

        fertilizerName:
            firstValue(
                normalizedFertilizerRecommendation?.fertilizerName,
                normalizedFertilizerRecommendation?.productName,
                normalizedFertilizerRecommendation?.fertilizer
            ),

        quantity:
            firstValue(
                normalizedFertilizerRecommendation?.quantity,
                normalizedFertilizerRecommendation?.recommendedQuantity,
                normalizedFertilizerRecommendation?.dose
            ),

        unit:
            firstValue(
                normalizedFertilizerRecommendation?.unit,
                normalizedFertilizerRecommendation?.quantityUnit
            ),

        timing:
            firstValue(
                normalizedFertilizerRecommendation?.timing,
                normalizedFertilizerRecommendation?.recommendedAt,
                normalizedFertilizerRecommendation?.applicationDate
            ),
    };

    const nextFertilizerSchedule =
        normalizedFertilizerRecommendation
            ? fertilizerSchedule
            : null;

    /* =====================================================
       CROP HISTORY
    ===================================================== */

    const sortedCropHistory =
        useMemo(
            () => {
                return [
                    ...cropHistory,
                ].sort(
                    (
                        first,
                        second
                    ) => {
                        const firstDate =
                            new Date(
                                first?.harvestDate ??
                                first?.sowingDate ??
                                first?.createdAt ??
                                0
                            ).getTime();

                        const secondDate =
                            new Date(
                                second?.harvestDate ??
                                second?.sowingDate ??
                                second?.createdAt ??
                                0
                            ).getTime();

                        return (
                            secondDate -
                            firstDate
                        );
                    }
                );
            },
            [
                cropHistory,
            ]
        );

    /* =====================================================
       CURRENT CROP MATCH
    ===================================================== */

    const isCurrentCropMatch =
        currentCrop &&
        fertilizerData.crop
            ? String(
                currentCrop
            ).toLowerCase() ===
            String(
                fertilizerData.crop
            ).toLowerCase()
            : null;

    /* =====================================================
       REPORTS
    ===================================================== */

    const latestReport =
        Array.isArray(reports)
            ? reports
                .filter(Boolean)
                .sort(
                    (
                        first,
                        second
                    ) =>
                        new Date(
                            second?.createdAt ??
                            0
                        ).getTime() -
                        new Date(
                            first?.createdAt ??
                            0
                        ).getTime()
                )[0] ??
            null
            : null;

    /* =====================================================
       NAVIGATION
    ===================================================== */

    const saveCurrentFarm = () => {
        if (!id) {
            return;
        }

        localStorage.setItem(
            "agrinexus_current_farm_id",
            String(id)
        );
    };

    const navigateTo = (
        path
    ) => {
        saveCurrentFarm();

        navigate(path);
    };

    const handleBack = () => {
        navigate(
            "/home"
        );
    };

    const handleHistory = () => {
        saveCurrentFarm();

        navigate(
            "/history"
        );
    };

    const handlePassport = () => {
        saveCurrentFarm();

        navigate(
            "/field-health-passport"
        );
    };

    const handleWeather = () => {
        saveCurrentFarm();

        navigate(
            "/weather-radar"
        );
    };

    const handleIrrigation = () => {
        saveCurrentFarm();

        navigate(
            "/irrigation"
        );
    };

    const handleFertilizer = () => {
        saveCurrentFarm();

        navigate(
            "/fertilizer"
        );
    };

    const handleCropHistoryRecord = (
        record
    ) => {
        if (!record?.id) {
            return;
        }

        saveCurrentFarm();

        navigate(
            `/crop-history/${record.id}`,
            {
                state: {
                    cropHistory: record,
                },
            }
        );
    };

    /* =====================================================
       LOADING
    ===================================================== */

    if (loading) {
        return (
            <div className="field-detail-page">
                <div className="field-detail-loading">
                    <div className="field-detail-spinner" />

                    <p>
                        {text.loading}
                    </p>
                </div>

                <BottomNavBar />
            </div>
        );
    }

    /* =====================================================
       ERROR / MISSING FIELD
    ===================================================== */

    if (
        error &&
        !farm &&
        !localField
    ) {
        return (
            <div className="field-detail-page">
                <div className="field-detail-error">
                    <div className="field-detail-error-icon">
                        !
                    </div>

                    <h2>
                        {text.unavailable}
                    </h2>

                    <p>
                        {error}
                    </p>

                    <button
                        type="button"
                        className="field-detail-primary-button"
                        onClick={
                            handleBack
                        }
                    >
                        {text.back}
                    </button>
                </div>

                <BottomNavBar />
            </div>
        );
    }

    /* =====================================================
       RENDER
    ===================================================== */

    return (
        <div className="field-detail-page">

            {/* =================================================
                HEADER
            ================================================= */}

            <header className="field-detail-header">

                <div className="field-detail-header-left">

                    <button
                        type="button"
                        className="field-detail-back-button"
                        onClick={
                            handleBack
                        }
                        aria-label={
                            text.back
                        }
                    >
                        ←
                    </button>

                    <div>
                        <div className="field-detail-header-label">
                            {isHindi
                                ? "खेत विवरण"
                                : "FIELD DETAILS"}
                        </div>

                        <h1 className="field-detail-title">
                            {
                                fieldData.name ??
                                "—"
                            }
                        </h1>

                        <p className="field-detail-location">
                            {
                                fieldData.location ??
                                "—"
                            }
                        </p>
                    </div>
                </div>

                <div className="field-detail-header-actions">

                    <button
                        type="button"
                        className="field-detail-refresh-button"
                        onClick={
                            handleRefresh
                        }
                        disabled={
                            refreshing
                        }
                    >
                        {refreshing
                            ? "…"
                            : "↻"}
                        <span>
                            {text.refresh}
                        </span>
                    </button>

                </div>
            </header>

            {/* =================================================
                DATA NOTICE
            ================================================= */}

            <div className="field-detail-data-notice">
                <span className="field-detail-data-notice-icon">
                    ✓
                </span>

                <span>
                    {text.realDataOnly}
                </span>
            </div>

            {/* =================================================
                FIELD SUMMARY
            ================================================= */}

            <section className="field-detail-summary-section">

                <div className="field-detail-summary-card">

                    <div className="field-detail-summary-top">

                        <div className="field-detail-summary-name-wrap">

                            <div className="field-detail-field-icon">
                                🌱
                            </div>

                            <div>
                                <h2>
                                    {
                                        fieldData.name ??
                                        "—"
                                    }
                                </h2>

                                <p>
                                    {
                                        fieldData.location ??
                                        "—"
                                    }
                                </p>
                            </div>

                        </div>

                        <div className="field-detail-field-id">
                            ID:{" "}
                            {
                                fieldData.id ??
                                "—"
                            }
                        </div>

                    </div>

                    <div className="field-detail-summary-grid">

                        <div className="field-detail-mini-stat">

                            <span>
                                {text.area}
                            </span>

                            <strong>
                                {
                                    hasValue(
                                        fieldData.area
                                    )
                                        ? `${formatNumber(
                                            fieldData.area
                                        )} ac`
                                        : "—"
                                }
                            </strong>

                        </div>

                        <div className="field-detail-mini-stat">

                            <span>
                                Latitude
                            </span>

                            <strong>
                                {
                                    hasValue(
                                        fieldData.latitude
                                    )
                                        ? formatNumber(
                                            fieldData.latitude,
                                            6
                                        )
                                        : "—"
                                }
                            </strong>

                        </div>

                        <div className="field-detail-mini-stat">

                            <span>
                                Longitude
                            </span>

                            <strong>
                                {
                                    hasValue(
                                        fieldData.longitude
                                    )
                                        ? formatNumber(
                                            fieldData.longitude,
                                            6
                                        )
                                        : "—"
                                }
                            </strong>

                        </div>

                        <div className="field-detail-mini-stat">

                            <span>
                                Created
                            </span>

                            <strong>
                                {
                                    formatDate(
                                        fieldData.createdAt
                                    )
                                }
                            </strong>

                        </div>

                    </div>

                </div>
            </section>

            {/* =================================================
                SECTION TABS
            ================================================= */}

            <nav className="field-detail-tabs">

                <button
                    type="button"
                    className={
                        activeSection ===
                        "overview"
                            ? "active"
                            : ""
                    }
                    onClick={() =>
                        setActiveSection(
                            "overview"
                        )
                    }
                >
                    {text.overview}
                </button>

                <button
                    type="button"
                    className={
                        activeSection ===
                        "health"
                            ? "active"
                            : ""
                    }
                    onClick={() =>
                        setActiveSection(
                            "health"
                        )
                    }
                >
                    {text.health}
                </button>

                <button
                    type="button"
                    className={
                        activeSection ===
                        "soil"
                            ? "active"
                            : ""
                    }
                    onClick={() =>
                        setActiveSection(
                            "soil"
                        )
                    }
                >
                    {text.soil}
                </button>

                <button
                    type="button"
                    className={
                        activeSection ===
                        "weather"
                            ? "active"
                            : ""
                    }
                    onClick={() =>
                        setActiveSection(
                            "weather"
                        )
                    }
                >
                    {text.weather}
                </button>

                <button
                    type="button"
                    className={
                        activeSection ===
                        "irrigation"
                            ? "active"
                            : ""
                    }
                    onClick={() =>
                        setActiveSection(
                            "irrigation"
                        )
                    }
                >
                    {text.irrigation}
                </button>

                <button
                    type="button"
                    className={
                        activeSection ===
                        "fertilizer"
                            ? "active"
                            : ""
                    }
                    onClick={() =>
                        setActiveSection(
                            "fertilizer"
                        )
                    }
                >
                    {text.fertilizer}
                </button>

                <button
                    type="button"
                    className={
                        activeSection ===
                        "history"
                            ? "active"
                            : ""
                    }
                    onClick={() =>
                        setActiveSection(
                            "history"
                        )
                    }
                >
                    {text.history}
                </button>

            </nav>

            <main className="field-detail-content">

                {/* =================================================
                    OVERVIEW
                ================================================= */}

                {activeSection ===
                    "overview" && (
                        <>
                            {/* Current Crop */}

                            <section className="field-detail-section">

                                <div className="field-detail-section-header">

                                    <div>
                                    <span className="field-detail-section-kicker">
                                        CROP CYCLE
                                    </span>

                                        <h2>
                                            {text.currentCrop}
                                        </h2>
                                    </div>

                                    <div className="field-detail-section-icon">
                                        🌾
                                    </div>

                                </div>

                                <div className="field-detail-current-crop-card">

                                    <div className="field-detail-current-crop-main">

                                        <div className="field-detail-current-crop-icon">
                                            🌱
                                        </div>

                                        <div>

                                            <h3>
                                                {
                                                    currentCrop ??
                                                    "—"
                                                }
                                            </h3>

                                            <p>
                                                {
                                                    currentCrop
                                                        ? (
                                                            currentCropRecord?.notes ??
                                                            text.realDataOnly
                                                        )
                                                        : text.noCurrentCrop
                                                }
                                            </p>

                                        </div>

                                    </div>

                                    <div className="field-detail-current-crop-grid">

                                        <div className="field-detail-current-crop-stat">

                                        <span>
                                            {text.season}
                                        </span>

                                            <strong>
                                                {
                                                    currentCropSeason ??
                                                    "—"
                                                }
                                            </strong>

                                        </div>

                                        <div className="field-detail-current-crop-stat">

                                        <span>
                                            {text.stage}
                                        </span>

                                            <strong>
                                                {
                                                    currentCropStage ??
                                                    "—"
                                                }
                                            </strong>

                                        </div>

                                        <div className="field-detail-current-crop-stat">

                                        <span>
                                            Sowing Date
                                        </span>

                                            <strong>
                                                {
                                                    formatDate(
                                                        currentCropSowingDate
                                                    )
                                                }
                                            </strong>

                                        </div>

                                        <div className="field-detail-current-crop-stat">

                                        <span>
                                            Harvest Date
                                        </span>

                                            <strong>
                                                {
                                                    formatDate(
                                                        currentCropHarvestDate
                                                    )
                                                }
                                            </strong>

                                        </div>

                                    </div>

                                </div>

                            </section>

                            {/* Health + Weather */}

                            <div className="field-detail-two-column">

                                <section className="field-detail-section">

                                    <div className="field-detail-section-header">

                                        <div>
                                        <span className="field-detail-section-kicker">
                                            HEALTH
                                        </span>

                                            <h2>
                                                {text.health}
                                            </h2>
                                        </div>

                                        <div className="field-detail-section-icon">
                                            ❤️
                                        </div>

                                    </div>

                                    <div className="field-detail-health-card">

                                        <div className="field-detail-health-score">

                                            <strong>
                                                {
                                                    hasValue(
                                                        healthScore
                                                    )
                                                        ? formatNumber(
                                                            healthScore,
                                                            2
                                                        )
                                                        : "—"
                                                }
                                            </strong>

                                            <span>
                                            / 100
                                        </span>

                                        </div>

                                        <div className="field-detail-health-info">

                                        <span
                                            className={
                                                `field-detail-status-pill ${healthStatusClass}`
                                            }
                                        >
                                            {
                                                healthStatus ??
                                                "—"
                                            }
                                        </span>

                                            <p>
                                                {
                                                    passport?.summary ??
                                                    passport?.description ??
                                                    text.unavailable
                                                }
                                            </p>

                                        </div>

                                    </div>

                                    <button
                                        type="button"
                                        className="field-detail-secondary-button"
                                        onClick={
                                            handlePassport
                                        }
                                    >
                                        {text.openPassport}
                                    </button>

                                </section>

                                <section className="field-detail-section">

                                    <div className="field-detail-section-header">

                                        <div>
                                        <span className="field-detail-section-kicker">
                                            WEATHER
                                        </span>

                                            <h2>
                                                {text.weather}
                                            </h2>
                                        </div>

                                        <div className="field-detail-section-icon">
                                            ☁️
                                        </div>

                                    </div>

                                    <div className="field-detail-weather-card">

                                        <div className="field-detail-weather-main">

                                            <div className="field-detail-weather-temp">

                                                <strong>
                                                    {
                                                        hasValue(
                                                            weatherData.temperature
                                                        )
                                                            ? `${formatNumber(
                                                                weatherData.temperature,
                                                                1
                                                            )}°`
                                                            : "—"
                                                    }
                                                </strong>

                                                <span>
                                                {
                                                    weatherData.weatherCondition ??
                                                    "—"
                                                }
                                            </span>

                                            </div>

                                            <div className="field-detail-weather-meta">

                                            <span>
                                                Feels like{" "}
                                                {
                                                    hasValue(
                                                        weatherData.feelsLike
                                                    )
                                                        ? `${formatNumber(
                                                            weatherData.feelsLike,
                                                            1
                                                        )}°`
                                                        : "—"
                                                }
                                            </span>

                                                <span>
                                                {
                                                    weatherData.weatherDescription ??
                                                    "—"
                                                }
                                            </span>

                                            </div>

                                        </div>

                                        <div className="field-detail-weather-grid">

                                            <div>
                                            <span>
                                                {text.humidity}
                                            </span>

                                                <strong>
                                                    {
                                                        hasValue(
                                                            weatherData.humidity
                                                        )
                                                            ? `${formatNumber(
                                                                weatherData.humidity,
                                                                1
                                                            )}%`
                                                            : "—"
                                                    }
                                                </strong>
                                            </div>

                                            <div>
                                            <span>
                                                {text.rainfall}
                                            </span>

                                                <strong>
                                                    {
                                                        hasValue(
                                                            weatherData.rainfall
                                                        )
                                                            ? `${formatNumber(
                                                                weatherData.rainfall,
                                                                1
                                                            )} mm`
                                                            : "—"
                                                    }
                                                </strong>
                                            </div>

                                            <div>
                                            <span>
                                                {text.windSpeed}
                                            </span>

                                                <strong>
                                                    {
                                                        hasValue(
                                                            weatherData.windSpeed
                                                        )
                                                            ? `${formatNumber(
                                                                weatherData.windSpeed,
                                                                1
                                                            )}`
                                                            : "—"
                                                    }
                                                </strong>
                                            </div>

                                            <div>
                                            <span>
                                                Pressure
                                            </span>

                                                <strong>
                                                    {
                                                        hasValue(
                                                            weatherData.pressure
                                                        )
                                                            ? formatNumber(
                                                                weatherData.pressure,
                                                                1
                                                            )
                                                            : "—"
                                                    }
                                                </strong>
                                            </div>

                                        </div>

                                    </div>

                                    <button
                                        type="button"
                                        className="field-detail-secondary-button"
                                        onClick={
                                            handleWeather
                                        }
                                    >
                                        {text.openWeatherRadar}
                                    </button>

                                </section>

                            </div>

                            {/* Soil */}

                            <section className="field-detail-section">

                                <div className="field-detail-section-header">

                                    <div>
                                    <span className="field-detail-section-kicker">
                                        SOIL DATA
                                    </span>

                                        <h2>
                                            {text.soil}
                                        </h2>
                                    </div>

                                    <div className="field-detail-section-icon">
                                        🪴
                                    </div>

                                </div>

                                <div className="field-detail-metric-grid">

                                    <div className="field-detail-metric-card">

                                    <span>
                                        {text.soilPh}
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    soilData.pH
                                                )
                                                    ? formatNumber(
                                                        soilData.pH,
                                                        2
                                                    )
                                                    : "—"
                                            }
                                        </strong>

                                    </div>

                                    <div className="field-detail-metric-card">

                                    <span>
                                        {text.organicCarbon}
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    soilData.organicCarbon
                                                )
                                                    ? `${formatNumber(
                                                        soilData.organicCarbon,
                                                        2
                                                    )}%`
                                                    : "—"
                                            }
                                        </strong>

                                    </div>

                                    <div className="field-detail-metric-card">

                                    <span>
                                        {text.nitrogen}
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    soilData.nitrogen
                                                )
                                                    ? formatNumber(
                                                        soilData.nitrogen
                                                    )
                                                    : "—"
                                            }
                                        </strong>

                                    </div>

                                    <div className="field-detail-metric-card">

                                    <span>
                                        {text.phosphorus}
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    soilData.phosphorus
                                                )
                                                    ? formatNumber(
                                                        soilData.phosphorus
                                                    )
                                                    : "—"
                                            }
                                        </strong>

                                    </div>

                                    <div className="field-detail-metric-card">

                                    <span>
                                        {text.potassium}
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    soilData.potassium
                                                )
                                                    ? formatNumber(
                                                        soilData.potassium
                                                    )
                                                    : "—"
                                            }
                                        </strong>

                                    </div>

                                    <div className="field-detail-metric-card">

                                    <span>
                                        {text.electricalConductivity}
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    soilData.electricalConductivity
                                                )
                                                    ? formatNumber(
                                                        soilData.electricalConductivity,
                                                        2
                                                    )
                                                    : "—"
                                            }
                                        </strong>

                                    </div>

                                </div>

                                <div className="field-detail-last-updated">
                                    Soil data recorded:{" "}
                                    {
                                        formatDateTime(
                                            soilData.recordedAt
                                        )
                                    }
                                </div>

                            </section>

                            {/* Risk */}

                            <section className="field-detail-section">

                                <div className="field-detail-section-header">

                                    <div>
                                    <span className="field-detail-section-kicker">
                                        RISK RADAR
                                    </span>

                                        <h2>
                                            {text.risk}
                                        </h2>
                                    </div>

                                    <div className="field-detail-section-icon">
                                        ⚠️
                                    </div>

                                </div>

                                {
                                    normalizedRisks.length >
                                    0 ? (
                                        <div className="field-detail-risk-list">

                                            {
                                                normalizedRisks.map(
                                                    (
                                                        risk
                                                    ) => (
                                                        <div
                                                            key={
                                                                risk.id
                                                            }
                                                            className={
                                                                `field-detail-risk-card ${getRiskLevelClass(
                                                                    risk.riskLevel
                                                                )}`
                                                            }
                                                        >

                                                            <div className="field-detail-risk-card-top">

                                                                <div>
                                                                <span className="field-detail-risk-type">
                                                                    {
                                                                        risk.riskType ??
                                                                        "—"
                                                                    }
                                                                </span>

                                                                    <h3>
                                                                        {
                                                                            risk.title ??
                                                                            "—"
                                                                        }
                                                                    </h3>
                                                                </div>

                                                                <span className="field-detail-risk-score">
                                                                {
                                                                    hasValue(
                                                                        risk.riskScore
                                                                    )
                                                                        ? formatNumber(
                                                                            risk.riskScore,
                                                                            0
                                                                        )
                                                                        : "—"
                                                                }
                                                            </span>

                                                            </div>

                                                            <p>
                                                                {
                                                                    risk.description ??
                                                                    text.unavailable
                                                                }
                                                            </p>

                                                            <div className="field-detail-risk-recommendation">

                                                                <strong>
                                                                    {
                                                                        text.recommendation
                                                                    }
                                                                </strong>

                                                                <span>
                                                                {
                                                                    risk.recommendation ??
                                                                    "—"
                                                                }
                                                            </span>

                                                            </div>

                                                            <div className="field-detail-risk-footer">

                                                            <span>
                                                                Detected:{" "}
                                                                {
                                                                    formatDateTime(
                                                                        risk.detectedAt
                                                                    )
                                                                }
                                                            </span>

                                                                <span>
                                                                Valid until:{" "}
                                                                    {
                                                                        formatDateTime(
                                                                            risk.validUntil
                                                                        )
                                                                    }
                                                            </span>

                                                            </div>

                                                        </div>
                                                    )
                                                )
                                            }

                                        </div>
                                    ) : (
                                        <div className="field-detail-empty-card">
                                        <span className="field-detail-empty-icon">
                                            ✓
                                        </span>

                                            <h3>
                                                {text.noData}
                                            </h3>

                                            <p>
                                                No weather risk records were returned for this field.
                                            </p>
                                        </div>
                                    )
                                }

                            </section>
                        </>
                    )}

                {/* =================================================
                    FIELD HEALTH
                ================================================= */}

                {activeSection ===
                    "health" && (
                        <section className="field-detail-section">

                            <div className="field-detail-section-header">

                                <div>
                                <span className="field-detail-section-kicker">
                                    HEALTH PASSPORT
                                </span>

                                    <h2>
                                        {text.health}
                                    </h2>
                                </div>

                                <div className="field-detail-section-icon">
                                    ❤️
                                </div>

                            </div>

                            <div className="field-detail-health-large-card">

                                <div className="field-detail-health-large-score">

                                <span>
                                    Health Score
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                healthScore
                                            )
                                                ? formatNumber(
                                                    healthScore,
                                                    2
                                                )
                                                : "—"
                                        }
                                    </strong>

                                    <small>
                                        / 100
                                    </small>

                                </div>

                                <div className="field-detail-health-large-status">

                                <span
                                    className={
                                        `field-detail-status-pill ${healthStatusClass}`
                                    }
                                >
                                    {
                                        healthStatus ??
                                        "—"
                                    }
                                </span>

                                    <p>
                                        {
                                            passport?.summary ??
                                            passport?.description ??
                                            "—"
                                        }
                                    </p>

                                </div>

                            </div>

                            <div className="field-detail-metric-grid">

                                <div className="field-detail-metric-card">
                                <span>
                                    Soil pH
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.pH
                                            )
                                                ? formatNumber(
                                                    soilData.pH,
                                                    2
                                                )
                                                : "—"
                                        }
                                    </strong>
                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    Organic Carbon
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.organicCarbon
                                            )
                                                ? `${formatNumber(
                                                    soilData.organicCarbon,
                                                    2
                                                )}%`
                                                : "—"
                                        }
                                    </strong>
                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    Nitrogen
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.nitrogen
                                            )
                                                ? formatNumber(
                                                    soilData.nitrogen
                                                )
                                                : "—"
                                        }
                                    </strong>
                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    Phosphorus
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.phosphorus
                                            )
                                                ? formatNumber(
                                                    soilData.phosphorus
                                                )
                                                : "—"
                                        }
                                    </strong>
                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    Potassium
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.potassium
                                            )
                                                ? formatNumber(
                                                    soilData.potassium
                                                )
                                                : "—"
                                        }
                                    </strong>
                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    Electrical Conductivity
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.electricalConductivity
                                            )
                                                ? formatNumber(
                                                    soilData.electricalConductivity,
                                                    2
                                                )
                                                : "—"
                                        }
                                    </strong>
                                </div>

                            </div>

                            <button
                                type="button"
                                className="field-detail-primary-button"
                                onClick={
                                    handlePassport
                                }
                            >
                                {text.openPassport}
                            </button>

                        </section>
                    )}

                {/* =================================================
                    SOIL
                ================================================= */}

                {activeSection ===
                    "soil" && (
                        <section className="field-detail-section">

                            <div className="field-detail-section-header">

                                <div>
                                <span className="field-detail-section-kicker">
                                    SOIL INTELLIGENCE
                                </span>

                                    <h2>
                                        {text.soil}
                                    </h2>
                                </div>

                                <div className="field-detail-section-icon">
                                    🪴
                                </div>

                            </div>

                            <div className="field-detail-metric-grid">

                                <div className="field-detail-metric-card">
                                <span>
                                    {text.soilPh}
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.pH
                                            )
                                                ? formatNumber(
                                                    soilData.pH,
                                                    2
                                                )
                                                : "—"
                                        }
                                    </strong>

                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    {text.organicCarbon}
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.organicCarbon
                                            )
                                                ? `${formatNumber(
                                                    soilData.organicCarbon,
                                                    2
                                                )}%`
                                                : "—"
                                        }
                                    </strong>

                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    {text.nitrogen}
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.nitrogen
                                            )
                                                ? formatNumber(
                                                    soilData.nitrogen
                                                )
                                                : "—"
                                        }
                                    </strong>

                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    {text.phosphorus}
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.phosphorus
                                            )
                                                ? formatNumber(
                                                    soilData.phosphorus
                                                )
                                                : "—"
                                        }
                                    </strong>

                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    {text.potassium}
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.potassium
                                            )
                                                ? formatNumber(
                                                    soilData.potassium
                                                )
                                                : "—"
                                        }
                                    </strong>

                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    {text.electricalConductivity}
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.electricalConductivity
                                            )
                                                ? formatNumber(
                                                    soilData.electricalConductivity,
                                                    2
                                                )
                                                : "—"
                                        }
                                    </strong>

                                </div>

                            </div>

                            <div className="field-detail-soil-extra-grid">

                                <div className="field-detail-info-card">

                                <span>
                                    {text.moisture}
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                soilData.moisture
                                            )
                                                ? `${formatNumber(
                                                    soilData.moisture,
                                                    1
                                                )}%`
                                                : "—"
                                        }
                                    </strong>

                                </div>

                                <div className="field-detail-info-card">

                                <span>
                                    Sample Recorded
                                </span>

                                    <strong>
                                        {
                                            formatDateTime(
                                                soilData.recordedAt
                                            )
                                        }
                                    </strong>

                                </div>

                            </div>

                        </section>
                    )}

                {/* =================================================
                    WEATHER
                ================================================= */}

                {activeSection ===
                    "weather" && (
                        <section className="field-detail-section">

                            <div className="field-detail-section-header">

                                <div>
                                <span className="field-detail-section-kicker">
                                    WEATHER RISK RADAR
                                </span>

                                    <h2>
                                        {text.weather}
                                    </h2>
                                </div>

                                <div className="field-detail-section-icon">
                                    ☁️
                                </div>

                            </div>

                            <div className="field-detail-weather-large-card">

                                <div className="field-detail-weather-large-top">

                                    <div>

                                    <span>
                                        {
                                            weatherData.weatherCondition ??
                                            "—"
                                        }
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    weatherData.temperature
                                                )
                                                    ? `${formatNumber(
                                                        weatherData.temperature,
                                                        1
                                                    )}°C`
                                                    : "—"
                                            }
                                        </strong>

                                    </div>

                                    <div>
                                    <span>
                                        Feels Like
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    weatherData.feelsLike
                                                )
                                                    ? `${formatNumber(
                                                        weatherData.feelsLike,
                                                        1
                                                    )}°C`
                                                    : "—"
                                            }
                                        </strong>
                                    </div>

                                </div>

                                <div className="field-detail-weather-grid">

                                    <div>
                                    <span>
                                        {text.humidity}
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    weatherData.humidity
                                                )
                                                    ? `${formatNumber(
                                                        weatherData.humidity,
                                                        1
                                                    )}%`
                                                    : "—"
                                            }
                                        </strong>
                                    </div>

                                    <div>
                                    <span>
                                        {text.rainfall}
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    weatherData.rainfall
                                                )
                                                    ? `${formatNumber(
                                                        weatherData.rainfall,
                                                        1
                                                    )} mm`
                                                    : "—"
                                            }
                                        </strong>
                                    </div>

                                    <div>
                                    <span>
                                        {text.windSpeed}
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    weatherData.windSpeed
                                                )
                                                    ? formatNumber(
                                                        weatherData.windSpeed,
                                                        1
                                                    )
                                                    : "—"
                                            }
                                        </strong>
                                    </div>

                                    <div>
                                    <span>
                                        Wind Direction
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    weatherData.windDirection
                                                )
                                                    ? formatNumber(
                                                        weatherData.windDirection,
                                                        0
                                                    )
                                                    : "—"
                                            }
                                        </strong>
                                    </div>

                                    <div>
                                    <span>
                                        Pressure
                                    </span>

                                        <strong>
                                            {
                                                hasValue(
                                                    weatherData.pressure
                                                )
                                                    ? formatNumber(
                                                        weatherData.pressure,
                                                        1
                                                    )
                                                    : "—"
                                            }
                                        </strong>
                                    </div>

                                    <div>
                                    <span>
                                        Recorded
                                    </span>

                                        <strong>
                                            {
                                                formatDateTime(
                                                    weatherData.recordedAt
                                                )
                                            }
                                        </strong>
                                    </div>

                                </div>

                            </div>

                            <div className="field-detail-risk-section">

                                <div className="field-detail-section-subheader">
                                    <h3>
                                        {text.risk}
                                    </h3>

                                    <span>
                                    {
                                        normalizedRisks.length
                                    }
                                </span>
                                </div>

                                {
                                    normalizedRisks.length >
                                    0 ? (
                                        <div className="field-detail-risk-list">

                                            {
                                                normalizedRisks.map(
                                                    (
                                                        risk
                                                    ) => (
                                                        <div
                                                            key={
                                                                risk.id
                                                            }
                                                            className={
                                                                `field-detail-risk-card ${getRiskLevelClass(
                                                                    risk.riskLevel
                                                                )}`
                                                            }
                                                        >

                                                            <div className="field-detail-risk-card-top">

                                                                <div>

                                                                <span className="field-detail-risk-type">
                                                                    {
                                                                        risk.riskType ??
                                                                        "—"
                                                                    }
                                                                </span>

                                                                    <h3>
                                                                        {
                                                                            risk.title ??
                                                                            "—"
                                                                        }
                                                                    </h3>

                                                                </div>

                                                                <span className="field-detail-risk-score">
                                                                {
                                                                    hasValue(
                                                                        risk.riskScore
                                                                    )
                                                                        ? formatNumber(
                                                                            risk.riskScore,
                                                                            0
                                                                        )
                                                                        : "—"
                                                                }
                                                            </span>

                                                            </div>

                                                            <p>
                                                                {
                                                                    risk.description ??
                                                                    "—"
                                                                }
                                                            </p>

                                                            <div className="field-detail-risk-recommendation">

                                                                <strong>
                                                                    {
                                                                        text.recommendation
                                                                    }
                                                                </strong>

                                                                <span>
                                                                {
                                                                    risk.recommendation ??
                                                                    "—"
                                                                }
                                                            </span>

                                                            </div>

                                                        </div>
                                                    )
                                                )
                                            }

                                        </div>
                                    ) : (
                                        <div className="field-detail-empty-card">

                                        <span className="field-detail-empty-icon">
                                            ✓
                                        </span>

                                            <h3>
                                                {text.noData}
                                            </h3>

                                            <p>
                                                No active weather risk records were returned for this field.
                                            </p>

                                        </div>
                                    )
                                }

                            </div>

                            <button
                                type="button"
                                className="field-detail-primary-button"
                                onClick={
                                    handleWeather
                                }
                            >
                                {text.openWeatherRadar}
                            </button>

                        </section>
                    )}

                {/* =================================================
                    IRRIGATION
                ================================================= */}

                {activeSection ===
                    "irrigation" && (
                        <section className="field-detail-section">

                            <div className="field-detail-section-header">

                                <div>
                                <span className="field-detail-section-kicker">
                                    SMART WATER MANAGEMENT
                                </span>

                                    <h2>
                                        {text.irrigation}
                                    </h2>
                                </div>

                                <div className="field-detail-section-icon">
                                    💧
                                </div>

                            </div>

                            <div className="field-detail-metric-grid">

                                <div className="field-detail-metric-card">
                                <span>
                                    Mode
                                </span>

                                    <strong>
                                        {
                                            irrigationData.mode ??
                                            "—"
                                        }
                                    </strong>
                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    Duration
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                irrigationData.duration
                                            )
                                                ? `${formatNumber(
                                                    irrigationData.duration,
                                                    0
                                                )} min`
                                                : "—"
                                        }
                                    </strong>
                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    Frequency
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                irrigationData.frequency
                                            )
                                                ? String(
                                                    irrigationData.frequency
                                                )
                                                : "—"
                                        }
                                    </strong>
                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    Soil Moisture
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                irrigationData.soilMoisture
                                            )
                                                ? `${formatNumber(
                                                    irrigationData.soilMoisture,
                                                    1
                                                )}%`
                                                : "—"
                                        }
                                    </strong>
                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    Flow Rate
                                </span>

                                    <strong>
                                        {
                                            hasValue(
                                                irrigationData.flowRate
                                            )
                                                ? formatNumber(
                                                    irrigationData.flowRate,
                                                    2
                                                )
                                                : "—"
                                        }
                                    </strong>
                                </div>

                                <div className="field-detail-metric-card">
                                <span>
                                    Pump Status
                                </span>

                                    <strong>
                                        {
                                            irrigationData.pumpStatus ??
                                            "—"
                                        }
                                    </strong>
                                </div>

                            </div>

                            <div className="field-detail-info-card">

                            <span>
                                Last Watered
                            </span>

                                <strong>
                                    {
                                        formatDateTime(
                                            irrigationData.lastWateredAt
                                        )
                                    }
                                </strong>

                            </div>

                            <div className="field-detail-history-block">

                                <div className="field-detail-section-subheader">

                                    <h3>
                                        {text.irrigationEvents}
                                    </h3>

                                    <span>
                                    {
                                        irrigationEvents.length
                                    }
                                </span>

                                </div>

                                {
                                    irrigationEvents.length >
                                    0 ? (
                                        <div className="field-detail-event-list">

                                            {
                                                irrigationEvents
                                                    .slice(
                                                        0,
                                                        10
                                                    )
                                                    .map(
                                                        (
                                                            event,
                                                            index
                                                        ) => (
                                                            <div
                                                                key={
                                                                    event?.id ??
                                                                    `irrigation-event-${index}`
                                                                }
                                                                className="field-detail-event-row"
                                                            >

                                                                <div>
                                                                    <strong>
                                                                        {
                                                                            firstValue(
                                                                                event?.eventType,
                                                                                event?.type,
                                                                                event?.status
                                                                            ) ??
                                                                            "—"
                                                                        }
                                                                    </strong>

                                                                    <span>
                                                                    {
                                                                        firstValue(
                                                                            event?.description,
                                                                            event?.notes,
                                                                            event?.message
                                                                        ) ??
                                                                        "—"
                                                                    }
                                                                </span>
                                                                </div>

                                                                <time>
                                                                    {
                                                                        formatDateTime(
                                                                            firstValue(
                                                                                event?.occurredAt,
                                                                                event?.eventTime,
                                                                                event?.createdAt
                                                                            )
                                                                        )
                                                                    }
                                                                </time>

                                                            </div>
                                                        )
                                                    )
                                            }

                                        </div>
                                    ) : (
                                        <div className="field-detail-empty-card">

                                        <span className="field-detail-empty-icon">
                                            💧
                                        </span>

                                            <h3>
                                                {text.noData}
                                            </h3>

                                            <p>
                                                No irrigation event records were returned for this field.
                                            </p>

                                        </div>
                                    )
                                }

                            </div>

                            <button
                                type="button"
                                className="field-detail-primary-button"
                                onClick={
                                    handleIrrigation
                                }
                            >
                                {text.openIrrigation}
                            </button>

                        </section>
                    )}

                {/* =================================================
                    FERTILIZER
                ================================================= */}

                {activeSection ===
                    "fertilizer" && (
                        <section className="field-detail-section">

                            <div className="field-detail-section-header">

                                <div>
                                <span className="field-detail-section-kicker">
                                    NUTRIENT MANAGEMENT
                                </span>

                                    <h2>
                                        {text.fertilizer}
                                    </h2>
                                </div>

                                <div className="field-detail-section-icon">
                                    🧪
                                </div>

                            </div>

                            {
                                fertilizerData.recommendation ||
                                fertilizerData.fertilizerName ? (
                                    <div className="field-detail-fertilizer-card">

                                        <div className="field-detail-fertilizer-top">

                                            <div>

                                            <span>
                                                {
                                                    fertilizerData.crop ??
                                                    "—"
                                                }
                                            </span>

                                                <h3>
                                                    {
                                                        fertilizerData.fertilizerName ??
                                                        "—"
                                                    }
                                                </h3>

                                            </div>

                                            <div className="field-detail-fertilizer-quantity">

                                                <strong>
                                                    {
                                                        fertilizerData.quantity ??
                                                        "—"
                                                    }
                                                </strong>

                                                <span>
                                                {
                                                    fertilizerData.unit ??
                                                    "—"
                                                }
                                            </span>

                                            </div>

                                        </div>

                                        <div className="field-detail-fertilizer-body">

                                            <strong>
                                                {text.recommendation}
                                            </strong>

                                            <p>
                                                {
                                                    fertilizerData.recommendation ??
                                                    "—"
                                                }
                                            </p>

                                        </div>

                                        <div className="field-detail-fertilizer-footer">

                                        <span>
                                            Timing
                                        </span>

                                            <strong>
                                                {
                                                    fertilizerData.timing ??
                                                    "—"
                                                }
                                            </strong>

                                        </div>

                                        {
                                            isCurrentCropMatch ===
                                            false && (
                                                <div className="field-detail-warning-box">

                                                    Current crop and fertilizer recommendation crop do not match. The recommendation is shown only as returned by the backend.

                                                </div>
                                            )
                                        }

                                    </div>
                                ) : (
                                    <div className="field-detail-empty-card">

                                    <span className="field-detail-empty-icon">
                                        🧪
                                    </span>

                                        <h3>
                                            {text.noData}
                                        </h3>

                                        <p>
                                            No valid real fertilizer recommendation is available for this field.
                                        </p>

                                    </div>
                                )
                            }

                            <div className="field-detail-history-block">

                                <div className="field-detail-section-subheader">

                                    <h3>
                                        {text.fertilizerSchedule}
                                    </h3>

                                    <span>
                                    {
                                        nextFertilizerSchedule
                                            ? "1"
                                            : "0"
                                    }
                                </span>

                                </div>

                                {
                                    nextFertilizerSchedule ? (
                                        <div className="field-detail-schedule-card">

                                            <div>
                                            <span>
                                                Date
                                            </span>

                                                <strong>
                                                    {
                                                        formatDate(
                                                            firstValue(
                                                                nextFertilizerSchedule?.applicationDate,
                                                                nextFertilizerSchedule?.scheduledFor,
                                                                nextFertilizerSchedule?.date
                                                            )
                                                        )
                                                    }
                                                </strong>
                                            </div>

                                            <div>
                                            <span>
                                                Status
                                            </span>

                                                <strong>
                                                    {
                                                        firstValue(
                                                            nextFertilizerSchedule?.status,
                                                            nextFertilizerSchedule?.scheduleStatus
                                                        ) ??
                                                        "—"
                                                    }
                                                </strong>
                                            </div>

                                            <div>
                                            <span>
                                                Fertilizer
                                            </span>

                                                <strong>
                                                    {
                                                        firstValue(
                                                            nextFertilizerSchedule?.fertilizerName,
                                                            nextFertilizerSchedule?.fertilizer
                                                        ) ??
                                                        "—"
                                                    }
                                                </strong>
                                            </div>

                                            <div>
                                            <span>
                                                Quantity
                                            </span>

                                                <strong>
                                                    {
                                                        firstValue(
                                                            nextFertilizerSchedule?.quantity,
                                                            nextFertilizerSchedule?.dose
                                                        ) ??
                                                        "—"
                                                    }
                                                </strong>
                                            </div>

                                        </div>
                                    ) : (
                                        <div className="field-detail-empty-card">

                                        <span className="field-detail-empty-icon">
                                            📅
                                        </span>

                                            <h3>
                                                {text.noData}
                                            </h3>

                                            <p>
                                                No upcoming fertilizer schedule was returned for this field.
                                            </p>

                                        </div>
                                    )
                                }

                            </div>

                            <button
                                type="button"
                                className="field-detail-primary-button"
                                onClick={
                                    handleFertilizer
                                }
                            >
                                {text.openFertilizer}
                            </button>

                        </section>
                    )}

                {/* =================================================
                    HISTORY
                ================================================= */}

                {activeSection ===
                    "history" && (
                        <section className="field-detail-section">

                            <div className="field-detail-section-header">

                                <div>
                                <span className="field-detail-section-kicker">
                                    CROP CYCLES
                                </span>

                                    <h2>
                                        {text.cropHistory}
                                    </h2>
                                </div>

                                <div className="field-detail-section-icon">
                                    📚
                                </div>

                            </div>

                            {
                                sortedCropHistory.length >
                                0 ? (
                                    <div className="field-detail-crop-history-list">

                                        {
                                            sortedCropHistory.map(
                                                (
                                                    record,
                                                    index
                                                ) => {

                                                    const historyCrop =
                                                        firstValue(
                                                            record?.cropName,
                                                            record?.crop?.name
                                                        );

                                                    const historySeason =
                                                        record?.season;

                                                    const historyStage =
                                                        firstValue(
                                                            record?.cropStage,
                                                            record?.stage
                                                        );

                                                    return (
                                                        <button
                                                            type="button"
                                                            key={
                                                                record?.id ??
                                                                `crop-history-${index}`
                                                            }
                                                            className="field-detail-crop-history-row"
                                                            onClick={() =>
                                                                handleCropHistoryRecord(
                                                                    record
                                                                )
                                                            }
                                                            disabled={
                                                                !record?.id
                                                            }
                                                        >

                                                            <div className="field-detail-crop-history-icon">
                                                                🌾
                                                            </div>

                                                            <div className="field-detail-crop-history-main">

                                                                <strong>
                                                                    {
                                                                        historyCrop ??
                                                                        "—"
                                                                    }
                                                                </strong>

                                                                <span>
                                                                {
                                                                    historySeason ??
                                                                    "—"
                                                                }
                                                            </span>

                                                            </div>

                                                            <div className="field-detail-crop-history-meta">

                                                            <span>
                                                                {
                                                                    historyStage ??
                                                                    "—"
                                                                }
                                                            </span>

                                                                <small>
                                                                    {
                                                                        formatDate(
                                                                            record?.sowingDate
                                                                        )
                                                                    }
                                                                </small>

                                                            </div>

                                                            <div className="field-detail-crop-history-arrow">
                                                                →
                                                            </div>

                                                        </button>
                                                    );
                                                }
                                            )
                                        }

                                    </div>
                                ) : (
                                    <div className="field-detail-empty-card">

                                    <span className="field-detail-empty-icon">
                                        📚
                                    </span>

                                        <h3>
                                            {text.noData}
                                        </h3>

                                        <p>
                                            No crop history records were returned for this field.
                                        </p>

                                    </div>
                                )
                            }

                            <button
                                type="button"
                                className="field-detail-secondary-button"
                                onClick={
                                    handleHistory
                                }
                            >
                                {text.viewHistory}
                            </button>

                        </section>
                    )}

                {/* =================================================
                    ACTIONS
                ================================================= */}

                <section className="field-detail-actions-section">

                    <div className="field-detail-section-header">

                        <div>
                            <span className="field-detail-section-kicker">
                                QUICK ACTIONS
                            </span>

                            <h2>
                                {isHindi
                                    ? "स्मार्ट मॉड्यूल"
                                    : "Smart Modules"}
                            </h2>
                        </div>

                    </div>

                    <div className="field-detail-action-grid">

                        <button
                            type="button"
                            className="field-detail-action-card"
                            onClick={
                                handlePassport
                            }
                        >
                            <span>
                                ❤️
                            </span>

                            <strong>
                                {text.health}
                            </strong>

                            <small>
                                Field Health Passport
                            </small>

                            <b>
                                →
                            </b>
                        </button>

                        <button
                            type="button"
                            className="field-detail-action-card"
                            onClick={
                                handleWeather
                            }
                        >
                            <span>
                                🌦️
                            </span>

                            <strong>
                                {text.weather}
                            </strong>

                            <small>
                                Weather Risk Radar
                            </small>

                            <b>
                                →
                            </b>
                        </button>

                        <button
                            type="button"
                            className="field-detail-action-card"
                            onClick={
                                handleIrrigation
                            }
                        >
                            <span>
                                💧
                            </span>

                            <strong>
                                {text.irrigation}
                            </strong>

                            <small>
                                Smart Irrigation
                            </small>

                            <b>
                                →
                            </b>
                        </button>

                        <button
                            type="button"
                            className="field-detail-action-card"
                            onClick={
                                handleFertilizer
                            }
                        >
                            <span>
                                🧪
                            </span>

                            <strong>
                                {text.fertilizer}
                            </strong>

                            <small>
                                Fertilizer Planning
                            </small>

                            <b>
                                →
                            </b>
                        </button>

                        <button
                            type="button"
                            className="field-detail-action-card"
                            onClick={
                                handleHistory
                            }
                        >
                            <span>
                                📚
                            </span>

                            <strong>
                                {text.history}
                            </strong>

                            <small>
                                Crop History
                            </small>

                            <b>
                                →
                            </b>
                        </button>

                    </div>

                </section>

                {/* =================================================
                    SYSTEM / REPORT STATUS
                ================================================= */}

                <section className="field-detail-section">

                    <div className="field-detail-section-header">

                        <div>
                            <span className="field-detail-section-kicker">
                                DATA STATUS
                            </span>

                            <h2>
                                {isHindi
                                    ? "सिस्टम जानकारी"
                                    : "System Information"}
                            </h2>
                        </div>

                        <div className="field-detail-section-icon">
                            📡
                        </div>

                    </div>

                    <div className="field-detail-system-grid">

                        <div className="field-detail-system-card">

                            <span>
                                Farm ID
                            </span>

                            <strong>
                                {
                                    fieldData.id ??
                                    "—"
                                }
                            </strong>

                        </div>

                        <div className="field-detail-system-card">

                            <span>
                                Farmer ID
                            </span>

                            <strong>
                                {
                                    fieldData.farmerId ??
                                    "—"
                                }
                            </strong>

                        </div>

                        <div className="field-detail-system-card">

                            <span>
                                Crop History Records
                            </span>

                            <strong>
                                {
                                    cropHistory.length
                                }
                            </strong>

                        </div>

                        <div className="field-detail-system-card">

                            <span>
                                Active Weather Risks
                            </span>

                            <strong>
                                {
                                    normalizedRisks.length
                                }
                            </strong>

                        </div>

                        <div className="field-detail-system-card">

                            <span>
                                Irrigation Events
                            </span>

                            <strong>
                                {
                                    irrigationEvents.length
                                }
                            </strong>

                        </div>

                        <div className="field-detail-system-card">

                            <span>
                                Latest Report
                            </span>

                            <strong>
                                {
                                    latestReport
                                        ? formatDate(
                                            latestReport?.createdAt
                                        )
                                        : "—"
                                }
                            </strong>

                        </div>

                    </div>

                </section>

            </main>

            {/* =================================================
                BOTTOM NAVIGATION
            ================================================= */}

            <BottomNavBar />

        </div>
    );
}