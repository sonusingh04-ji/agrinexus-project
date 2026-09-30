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
    useFields,
} from "../../context/FieldsContext";

import {
    apiRequest,
} from "../../api/apiClient";

import {
    getCurrentCropForFarm,
} from "../../api/cropHistoryApi";

import VerifiedFieldIntelligence
    from "../../components/Home/VerifiedFieldIntelligence.jsx";

import AlertBanner
    from "../../components/AlertBanner";

import FieldCard
    from "../../components/FieldCard";

import WeatherCard
    from "../../components/WeatherCard";

import BottomNavBar
    from "../../components/BottomNavBar";

import "./Home.css";


/*
 * =========================================================
 * HELPERS
 * =========================================================
 */

const formatNumber = (
    value,
    digits = 1
) => {
    const number = Number(value);

    if (!Number.isFinite(number)) {
        return null;
    }

    return number.toFixed(digits);
};


const formatDate = (
    value,
    language
) => {
    if (!value) {
        return null;
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleDateString(
        language === "hi"
            ? "hi-IN"
            : "en-IN",
        {
            day: "numeric",
            month: "short",
            year: "numeric",
        }
    );
};


const normalizeList = (
    response
) => {
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
};


const getLatestByDate = (
    records,
    dateField
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
                    item[dateField]
            )
            .sort(
                (a, b) =>
                    new Date(b[dateField]).getTime() -
                    new Date(a[dateField]).getTime()
            )[0] || null
    );
};


const getRiskIcon = (
    riskType
) => {
    const type = String(
        riskType || ""
    ).toUpperCase();

    switch (type) {
        case "HEAT_STRESS":
            return "🔥";

        case "DROUGHT":
            return "☀️";

        case "WATERLOGGING":
            return "🌊";

        case "HIGH_WIND":
            return "💨";

        case "FROST":
            return "❄️";

        case "PEST_RISK":
            return "🐛";

        case "DISEASE_RISK":
            return "🦠";

        default:
            return "⚠️";
    }
};


const getRiskBannerType = (
    riskLevel
) => {
    const level = String(
        riskLevel || ""
    ).toUpperCase();

    if (
        level === "CRITICAL" ||
        level === "HIGH"
    ) {
        return "danger";
    }

    if (
        level === "MEDIUM" ||
        level === "MODERATE"
    ) {
        return "warning";
    }

    return "info";
};


const getRiskPriority = (
    riskLevel
) => {
    const level = String(
        riskLevel || ""
    ).toUpperCase();

    if (level === "CRITICAL") {
        return 100;
    }

    if (level === "HIGH") {
        return 80;
    }

    if (
        level === "MEDIUM" ||
        level === "MODERATE"
    ) {
        return 60;
    }

    return 40;
};


const getWeatherIcon = (
    condition
) => {
    const value = String(
        condition || ""
    ).toLowerCase();

    if (
        value.includes("storm") ||
        value.includes("thunder")
    ) {
        return "⛈️";
    }

    if (value.includes("rain")) {
        return "🌧️";
    }

    if (value.includes("cloud")) {
        return "☁️";
    }

    if (
        value.includes("clear") ||
        value.includes("sun")
    ) {
        return "☀️";
    }

    if (
        value.includes("heat") ||
        value.includes("extreme")
    ) {
        return "🔥";
    }

    return "🌤️";
};


const getHealthIcon = (
    status
) => {
    const value = String(
        status || ""
    ).toUpperCase();

    if (
        value === "HEALTHY" ||
        value === "GOOD"
    ) {
        return "✅";
    }

    if (
        value === "CRITICAL" ||
        value === "POOR"
    ) {
        return "🚨";
    }

    return "⚠️";
};


const getHealthStatusColor = (
    status
) => {
    const value = String(
        status || ""
    ).toUpperCase();

    if (
        value === "CRITICAL" ||
        value === "POOR"
    ) {
        return "danger";
    }

    if (
        value === "WARNING" ||
        value === "MODERATE"
    ) {
        return "warning";
    }

    return "good";
};


const getCropName = (
    record
) => {
    return (
        record?.cropName ??
        record?.crop?.name ??
        record?.name ??
        null
    );
};


const getCropStage = (
    record
) => {
    return (
        record?.cropStage ??
        record?.stage ??
        null
    );
};


const getBackendValue = (
    object,
    ...keys
) => {
    for (const key of keys) {
        const value =
            object?.[key];

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


/*
 * =========================================================
 * HOME
 * =========================================================
 */

function Home() {
    const {
        language,
    } = useLanguage();

    const navigate =
        useNavigate();

    const {
        fields = [],
        currentField,
        loading: fieldsLoading,
        error: fieldsError,
        deleteField,
    } = useFields();


    /*
     * =========================================================
     * SELECTED REAL FARM
     * =========================================================
     */

    const selectedFarmId =
        currentField?.id ??
        localStorage.getItem(
            "agrinexus_current_farm_id"
        ) ??
        null;


    /*
     * =========================================================
     * STATE
     * =========================================================
     */

    const [
        farm,
        setFarm,
    ] = useState(null);

    const [
        passport,
        setPassport,
    ] = useState(null);

    const [
        soil,
        setSoil,
    ] = useState(null);

    const [
        weather,
        setWeather,
    ] = useState(null);

    const [
        weatherRisks,
        setWeatherRisks,
    ] = useState([]);

    const [
        weatherRadar,
        setWeatherRadar,
    ] = useState(null);

    const [
        irrigationConfig,
        setIrrigationConfig,
    ] = useState(null);

    const [
        irrigationTelemetry,
        setIrrigationTelemetry,
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
        cropRotation,
        setCropRotation,
    ] = useState(null);

    const [
        cropHistory,
        setCropHistory,
    ] = useState([]);

    const [
        currentCropRecord,
        setCurrentCropRecord,
    ] = useState(null);

    const [
        soilTrend,
        setSoilTrend,
    ] = useState(null);

    const [
        recoveryPlans,
        setRecoveryPlans,
    ] = useState([]);

    /*
     * =========================================================
     * NEW REAL BACKEND DATA
     * =========================================================
     */

    const [
        satelliteObservation,
        setSatelliteObservation,
    ] = useState(null);

    const [
        nutrientBalance,
        setNutrientBalance,
    ] = useState(null);

    const [
        satelliteFieldData,
        setSatelliteFieldData,
    ] = useState(null);

    const [
        diseasePestRisks,
        setDiseasePestRisks,
    ] = useState([]);

    const [
        sustainability,
        setSustainability,
    ] = useState(null);

    const [
        pumpCompletion,
        setPumpCompletion,
    ] = useState(null);

    const [
        digitalMemory,
        setDigitalMemory,
    ] = useState(null);

    const [
        rainfallHarvesting,
        setRainfallHarvesting,
    ] = useState(null);

    const [
        solarPump,
        setSolarPump,
    ] = useState(null);

    const [
        dontDoToday,
        setDontDoToday,
    ] = useState(null);

    const [
        sowingAdvisor,
        setSowingAdvisor,
    ] = useState(null);

    const [
        aiExplanation,
        setAiExplanation,
    ] = useState(null);

    const [
        farmerAlerts,
        setFarmerAlerts,
    ] = useState([]);

    const [
        loading,
        setLoading,
    ] = useState(true);

    const [
        dataErrors,
        setDataErrors,
    ] = useState([]);

    const [
        confirmDeleteId,
        setConfirmDeleteId,
    ] = useState(null);

    const [
        deletingFarm,
        setDeletingFarm,
    ] = useState(false);

    const [
        deleteError,
        setDeleteError,
    ] = useState("");


    /*
     * =========================================================
     * DELETE FARM
     * =========================================================
     */

    const handleDeleteFarm = async () => {
        if (
            !confirmDeleteId ||
            deletingFarm
        ) {
            return;
        }

        try {
            setDeletingFarm(true);
            setDeleteError("");

            await deleteField(
                confirmDeleteId
            );

            setConfirmDeleteId(null);
            setDeleteError("");
        } catch (error) {
            console.error(
                "Farm deletion failed:",
                error
            );

            setDeleteError(
                error?.message ||
                (
                    language === "hi"
                        ? "खेत हटाया नहीं जा सका।"
                        : "Unable to delete this farm."
                )
            );
        } finally {
            setDeletingFarm(false);
        }
    };


    /*
     * =========================================================
     * LOAD DASHBOARD
     * =========================================================
     */

    useEffect(() => {
        let cancelled = false;


        const optionalRequest = async (
            url,
            label
        ) => {
            try {
                const value =
                    await apiRequest(url);

                return {
                    value,
                    label,
                    error: null,
                };
            } catch (error) {
                console.warn(
                    `${label} request failed:`,
                    error
                );

                return {
                    value: null,
                    label,
                    error:
                        error?.message ||
                        `Unable to load ${label}`,
                };
            }
        };


        const loadDashboard = async () => {
            if (fieldsLoading) {
                return;
            }


            if (!selectedFarmId) {
                setFarm(null);
                setPassport(null);
                setSoil(null);
                setWeather(null);
                setWeatherRisks([]);
                setWeatherRadar(null);
                setIrrigationConfig(null);
                setIrrigationTelemetry(null);
                setIrrigationEvents([]);
                setFertilizerRecommendation(null);
                setFertilizerSchedule(null);
                setCropRotation(null);
                setCropHistory([]);
                setCurrentCropRecord(null);
                setSoilTrend(null);
                setRecoveryPlans([]);

                setSatelliteObservation(null);
                setNutrientBalance(null);
                setSatelliteFieldData(null);
                setDiseasePestRisks([]);
                setSustainability(null);
                setPumpCompletion(null);
                setDigitalMemory(null);
                setRainfallHarvesting(null);
                setSolarPump(null);
                setDontDoToday(null);
                setSowingAdvisor(null);
                setAiExplanation(null);
                setFarmerAlerts([]);

                setDataErrors([]);
                setLoading(false);

                return;
            }


            setLoading(true);
            setDataErrors([]);


            try {
                const results =
                    await Promise.all([
                        /*
                         * 0
                         * FARM
                         */
                        optionalRequest(
                            `/api/farms/${selectedFarmId}`,
                            "Farm"
                        ),

                        /*
                         * 1
                         * FIELD HEALTH PASSPORT
                         */
                        optionalRequest(
                            `/api/field-health-passport/farm/${selectedFarmId}`,
                            "Field Health"
                        ),

                        /*
                         * 2
                         * SOIL INTELLIGENCE
                         */
                        optionalRequest(
                            `/api/soil-intelligence/farm/${selectedFarmId}`,
                            "Soil Intelligence"
                        ),

                        /*
                         * 3
                         * WEATHER
                         */
                        optionalRequest(
                            `/api/weather/farm/${selectedFarmId}`,
                            "Weather"
                        ),

                        /*
                         * 4
                         * WEATHER RISKS
                         */
                        optionalRequest(
                            `/api/weather-risks/farm/${selectedFarmId}`,
                            "Weather Risks"
                        ),

                        /*
                         * 5
                         * WEATHER RADAR
                         */
                        optionalRequest(
                            `/api/weather-radar/farm/${selectedFarmId}`,
                            "Weather Radar"
                        ),

                        /*
                         * 6
                         * IRRIGATION CONFIGURATION
                         */
                        optionalRequest(
                            `/api/irrigation-config/farm/${selectedFarmId}`,
                            "Irrigation Configuration"
                        ),

                        /*
                         * 7
                         * LATEST IRRIGATION TELEMETRY
                         */
                        optionalRequest(
                            `/api/irrigation-telemetry/farm/${selectedFarmId}/latest`,
                            "Irrigation Telemetry"
                        ),

                        /*
                         * 8
                         * IRRIGATION EVENTS
                         */
                        optionalRequest(
                            `/api/irrigation-events/farm/${selectedFarmId}`,
                            "Irrigation Events"
                        ),

                        /*
                         * 9
                         * LATEST FERTILIZER RECOMMENDATION
                         */
                        optionalRequest(
                            `/api/fertilizer-recommendations/farm/${selectedFarmId}/latest`,
                            "Fertilizer Recommendation"
                        ),

                        /*
                         * 10
                         * NEXT FERTILIZER SCHEDULE
                         */
                        optionalRequest(
                            `/api/fertilizer-schedules/farm/${selectedFarmId}/next`,
                            "Fertilizer Schedule"
                        ),

                        /*
                         * 11
                         * CROP ROTATION
                         */
                        optionalRequest(
                            `/api/crop-rotation/farm/${selectedFarmId}/latest`,
                            "Crop Rotation"
                        ),

                        /*
                         * 12
                         * CROP HISTORY
                         */
                        optionalRequest(
                            `/api/crop-history/farm/${selectedFarmId}`,
                            "Crop History"
                        ),

                        /*
                         * 13
                         * SOIL TEST TREND
                         */
                        optionalRequest(
                            `/api/soil-test-trends/farm/${selectedFarmId}`,
                            "Soil Test Trend"
                        ),

                        /*
                         * 14
                         * SOIL RECOVERY
                         */
                        optionalRequest(
                            `/api/soil-recovery-plans/farm/${selectedFarmId}/active`,
                            "Soil Recovery"
                        ),

                        /*
                         * 15
                         * SATELLITE OBSERVATION
                         */
                        optionalRequest(
                            `/api/satellite-observations/farm/${selectedFarmId}/latest`,
                            "Satellite Observation"
                        ),

                        /*
                         * 16
                         * NUTRIENT BALANCE
                         */
                        optionalRequest(
                            `/api/nutrient-balances/farm/${selectedFarmId}/latest`,
                            "Nutrient Balance"
                        ),

                        /*
                         * 17
                         * SATELLITE FIELD DATA
                         */
                        optionalRequest(
                            `/api/satellite/farm/${selectedFarmId}`,
                            "Satellite Field Data"
                        ),

                        /*
                         * 18
                         * DISEASE / PEST
                         */
                        optionalRequest(
                            `/api/disease-pest/farm/${selectedFarmId}`,
                            "Disease and Pest Risk"
                        ),

                        /*
                         * 19
                         * SUSTAINABILITY
                         */
                        optionalRequest(
                            `/api/sustainability/farm/${selectedFarmId}/latest`,
                            "Sustainability Score"
                        ),

                        /*
                         * 20
                         * PUMP COMPLETION
                         */
                        optionalRequest(
                            `/api/pump-completion/farm/${selectedFarmId}`,
                            "Pump Completion"
                        ),

                        /*
                         * 21
                         * DIGITAL MEMORY
                         */
                        optionalRequest(
                            `/api/digital-memory/farm/${selectedFarmId}`,
                            "Digital Memory"
                        ),

                        /*
                         * 22
                         * RAINFALL HARVESTING
                         */
                        optionalRequest(
                            `/api/rainfall-harvesting/farm/${selectedFarmId}`,
                            "Rainfall Harvesting"
                        ),

                        /*
                         * 23
                         * SOLAR PUMP
                         */
                        optionalRequest(
                            `/api/solar-pump/farm/${selectedFarmId}`,
                            "Solar Pump"
                        ),

                        /*
                         * 24
                         * DON'T DO TODAY
                         */
                        optionalRequest(
                            `/api/dont-do-today/farm/${selectedFarmId}`,
                            "Dont Do Today"
                        ),

                        /*
                         * 25
                         * SOWING ADVISOR
                         */
                        optionalRequest(
                            `/api/sowing-advisor/farm/${selectedFarmId}/latest`,
                            "Sowing Advisor"
                        ),

                        /*
                         * 26
                         * EXPLAINABLE AI
                         */
                        optionalRequest(
                            `/api/ai/explanations/farm/${selectedFarmId}/latest`,
                            "AI Explanation"
                        ),

                        /*
                         * 27
                         * FARMER ALERTS
                         */
                        optionalRequest(
                            `/api/farmer-alerts/farm/${selectedFarmId}/unread`,
                            "Farmer Alerts"
                        ),
                    ]);


                /*
                 * =====================================================
                 * CURRENT CROP
                 *
                 * Dedicated crop-history source of truth.
                 * =====================================================
                 */

                let realCurrentCrop = null;

                try {
                    realCurrentCrop =
                        await getCurrentCropForFarm(
                            selectedFarmId
                        );
                } catch (error) {
                    console.warn(
                        "Current crop request failed:",
                        error
                    );
                }


                if (cancelled) {
                    return;
                }


                /*
                 * =====================================================
                 * LOAD ERRORS
                 * =====================================================
                 */

                setDataErrors(
                    results
                        .filter(
                            (result) =>
                                result.error
                        )
                        .map(
                            (result) =>
                                result.label
                        )
                );


                /*
                 * =====================================================
                 * EXISTING CORE DATA
                 * =====================================================
                 */

                setFarm(
                    results[0].value
                );

                setPassport(
                    results[1].value
                );

                setSoil(
                    results[2].value
                );


                /*
                 * WEATHER
                 */

                const weatherData =
                    results[3].value;

                if (
                    Array.isArray(weatherData)
                ) {
                    setWeather(
                        getLatestByDate(
                            weatherData,
                            "recordedAt"
                        )
                    );
                } else {
                    setWeather(
                        weatherData
                    );
                }


                /*
                 * WEATHER RISKS
                 */

                setWeatherRisks(
                    normalizeList(
                        results[4].value
                    )
                );


                /*
                 * WEATHER RADAR
                 */

                setWeatherRadar(
                    results[5].value
                );


                /*
                 * IRRIGATION
                 */

                setIrrigationConfig(
                    results[6].value
                );

                setIrrigationTelemetry(
                    results[7].value
                );

                setIrrigationEvents(
                    normalizeList(
                        results[8].value
                    )
                );


                /*
                 * FERTILIZER
                 */

                setFertilizerRecommendation(
                    results[9].value
                );

                setFertilizerSchedule(
                    results[10].value
                );


                /*
                 * CROP ROTATION
                 */

                setCropRotation(
                    results[11].value
                );


                /*
                 * CROP HISTORY
                 */

                setCropHistory(
                    normalizeList(
                        results[12].value
                    )
                );


                /*
                 * CURRENT CROP
                 */

                setCurrentCropRecord(
                    realCurrentCrop
                );


                /*
                 * SOIL TEST TREND
                 */

                setSoilTrend(
                    results[13].value
                );


                /*
                 * SOIL RECOVERY
                 */

                setRecoveryPlans(
                    normalizeList(
                        results[14].value
                    )
                );


                /*
                 * =====================================================
                 * NEW DATA
                 * =====================================================
                 */

                /*
                 * Satellite Observation
                 */
                setSatelliteObservation(
                    results[15].value
                );


                /*
                 * Nutrient Balance
                 */
                setNutrientBalance(
                    results[16].value
                );


                /*
                 * Satellite Field Data
                 */
                const satelliteRecords =
                    normalizeList(
                        results[17].value
                    );

                setSatelliteFieldData(
                    getLatestByDate(
                        satelliteRecords,
                        "recordedAt"
                    ) ??
                    (
                        results[17].value &&
                        !Array.isArray(
                            results[17].value
                        )
                            ? results[17].value
                            : null
                    )
                );


                /*
                 * Disease / Pest
                 */
                setDiseasePestRisks(
                    normalizeList(
                        results[18].value
                    )
                );


                /*
                 * Sustainability
                 */
                setSustainability(
                    results[19].value
                );


                /*
                 * Pump Completion
                 */
                setPumpCompletion(
                    results[20].value
                );


                /*
                 * Digital Memory
                 */
                setDigitalMemory(
                    results[21].value
                );


                /*
                 * Rainfall Harvesting
                 */
                setRainfallHarvesting(
                    results[22].value
                );


                /*
                 * Solar Pump
                 */
                setSolarPump(
                    results[23].value
                );


                /*
                 * Don't Do Today
                 */
                setDontDoToday(
                    results[24].value
                );


                /*
                 * Sowing Advisor
                 */
                setSowingAdvisor(
                    results[25].value
                );


                /*
                 * Explainable AI
                 */
                setAiExplanation(
                    results[26].value
                );


                /*
                 * Farmer Alerts
                 */
                setFarmerAlerts(
                    normalizeList(
                        results[27].value
                    )
                );

            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        };


        void loadDashboard();


        return () => {
            cancelled = true;
        };
    }, [
        selectedFarmId,
        fieldsLoading,
    ]);


    /*
     * =========================================================
     * FARM INFORMATION
     * =========================================================
     */

    const farmName =
        getBackendValue(
            farm,
            "name"
        ) ??
        getBackendValue(
            currentField,
            "name",
            "fieldName"
        ) ??
        "—";


    const farmLocation =
        getBackendValue(
            farm,
            "location"
        ) ??
        getBackendValue(
            currentField,
            "location"
        ) ??
        "—";


    const farmLatitude =
        getBackendValue(
            farm,
            "latitude"
        );


    const farmLongitude =
        getBackendValue(
            farm,
            "longitude"
        );


    /*
     * =========================================================
     * CURRENT CROP
     * =========================================================
     *
     * IMPORTANT:
     * NO passport fallback
     * NO farm fallback
     * NO field fallback
     * NO crop rotation fallback
     * NO hardcoded crop
     * =========================================================
     */

    const currentCrop =
        getCropName(
            currentCropRecord
        ) ??
        "—";


    const currentCropStage =
        getCropStage(
            currentCropRecord
        ) ??
        "—";


    /*
     * =========================================================
     * SOIL
     * =========================================================
     */

    const soilPh =
        soil?.pH ??
        null;


    const soilType =
        soil?.soilType ??
        "—";


    const soilNitrogen =
        soil?.nitrogen ??
        null;


    const soilPhosphorus =
        soil?.phosphorus ??
        null;


    const soilPotassium =
        soil?.potassium ??
        null;


    const soilOrganicCarbon =
        soil?.organicCarbon ??
        null;


    /*
     * =========================================================
     * SOIL MOISTURE
     * =========================================================
     */

    const currentMoisture =
        irrigationTelemetry?.vwc30cm ??
        irrigationTelemetry?.vwc15cm ??
        irrigationTelemetry?.vwc60cm ??
        passport?.currentVwc30cm ??
        passport?.currentVwc15cm ??
        passport?.currentVwc60cm ??
        null;


    /*
     * =========================================================
     * FIELD HEALTH
     * =========================================================
     */

    const healthStatus =
        passport?.overallHealthStatus ??
        passport?.healthStatus ??
        currentField?.statusText ??
        "UNKNOWN";


    const healthScore =
        passport?.healthScore ??
        passport?.overallHealthScore ??
        currentField?.healthScore ??
        null;


    /*
     * =========================================================
     * WEATHER
     * =========================================================
     */

    const temperature =
        weather?.temperature ??
        null;


    const feelsLike =
        weather?.feelsLike ??
        null;


    const humidity =
        weather?.humidity ??
        null;


    const rainfall =
        weather?.rainfall ??
        null;


    const windSpeed =
        weather?.windSpeed ??
        null;


    const pressure =
        weather?.pressure ??
        null;


    const weatherCondition =
        weather?.weatherCondition ??
        weather?.weatherDescription ??
        "—";


    const weatherIcon =
        useMemo(
            () =>
                getWeatherIcon(
                    weatherCondition
                ),
            [weatherCondition]
        );


    /*
     * =========================================================
     * WEATHER RISKS
     * =========================================================
     */

    const sortedRisks =
        useMemo(() => {
            return [...weatherRisks].sort(
                (a, b) => {
                    const scoreA =
                        Number(
                            a?.riskScore ??
                            0
                        );

                    const scoreB =
                        Number(
                            b?.riskScore ??
                            0
                        );

                    return scoreB - scoreA;
                }
            );
        }, [
            weatherRisks,
        ]);


    const radarRisks =
        normalizeList(
            weatherRadar?.activeRisks
        );


    const highestWeatherRisk =
        sortedRisks[0] ??
        radarRisks[0] ??
        null;


    const activeRiskCount =
        weatherRadar?.activeRiskCount ??
        radarRisks.length ??
        sortedRisks.length ??
        passport?.activeWeatherRiskCount ??
        0;


    const highestRiskType =
        highestWeatherRisk?.riskType ??
        weatherRadar?.highestRiskType ??
        weatherRadar?.highestWeatherRisk ??
        passport?.highestWeatherRisk ??
        null;


    const highestRiskScore =
        highestWeatherRisk?.riskScore ??
        weatherRadar?.highestRiskScore ??
        passport?.highestWeatherRiskScore ??
        null;


    /*
     * =========================================================
     * IRRIGATION
     * =========================================================
     */

    const pumpState =
        irrigationTelemetry?.pumpState ??
        irrigationTelemetry?.pumpStatus ??
        null;


    const pumpStatus =
        pumpState === 1 ||
        pumpState === true ||
        String(
            pumpState
        ).toUpperCase() === "ON"
            ? "ON"
            : pumpState === 0 ||
            pumpState === false ||
            String(
                pumpState
            ).toUpperCase() === "OFF"
                ? "OFF"
                : "—";


    const irrigationFlow =
        irrigationTelemetry?.flowRateLpm ??
        null;


    const irrigationPressure =
        irrigationTelemetry?.linePressureBar ??
        null;


    const irrigationRuntime =
        irrigationTelemetry?.pumpRuntimeMinutes ??
        null;


    const irrigationMoisture =
        irrigationTelemetry?.vwc30cm ??
        irrigationTelemetry?.vwc15cm ??
        irrigationTelemetry?.vwc60cm ??
        null;


    /*
     * =========================================================
     * FERTILIZER
     * =========================================================
     */

    const fertilizerCrop =
        fertilizerRecommendation?.cropName ??
        fertilizerRecommendation?.crop?.name ??
        (
            currentCrop !== "—"
                ? currentCrop
                : "—"
        );


    const fertilizerStatus =
        fertilizerRecommendation?.status ??
        fertilizerSchedule?.status ??
        "—";


    /*
     * =========================================================
     * CROP ROTATION
     * =========================================================
     */

    const recommendedNextCrop =
        cropRotation?.recommendedCropName ??
        cropRotation?.recommendedCrop?.name ??
        null;


    const cropRotationScore =
        cropRotation?.recommendationScore ??
        null;


    /*
     * =========================================================
     * LATEST CROP HISTORY
     * =========================================================
     */

    const latestCropHistory =
        useMemo(() => {
            if (
                !Array.isArray(
                    cropHistory
                ) ||
                cropHistory.length === 0
            ) {
                return null;
            }

            return (
                [...cropHistory]
                    .filter(
                        (item) =>
                            item &&
                            (
                                item.sowingDate ||
                                item.harvestDate ||
                                item.createdAt
                            )
                    )
                    .sort(
                        (a, b) => {
                            const dateA =
                                new Date(
                                    a.sowingDate ??
                                    a.harvestDate ??
                                    a.createdAt ??
                                    0
                                ).getTime();

                            const dateB =
                                new Date(
                                    b.sowingDate ??
                                    b.harvestDate ??
                                    b.createdAt ??
                                    0
                                ).getTime();

                            return dateB - dateA;
                        }
                    )[0] ?? null
            );
        }, [
            cropHistory,
        ]);


    /*
     * =========================================================
     * DISEASE / PEST
     * =========================================================
     */

    const diseasePestSorted =
        useMemo(() => {
            return [
                ...diseasePestRisks,
            ].sort(
                (a, b) => {
                    const scoreA =
                        Number(
                            a?.riskScore ??
                            0
                        );

                    const scoreB =
                        Number(
                            b?.riskScore ??
                            0
                        );

                    return scoreB - scoreA;
                }
            );
        }, [
            diseasePestRisks,
        ]);


    const highestDiseasePestRisk =
        diseasePestSorted[0] ??
        null;


    /*
     * =========================================================
     * NUTRIENT BALANCE
     * =========================================================
     */

    const nutrientStatuses = [
        [
            "N",
            nutrientBalance?.nitrogenBalance,
            nutrientBalance?.nitrogenStatus,
        ],
        [
            "P",
            nutrientBalance?.phosphorusBalance,
            nutrientBalance?.phosphorusStatus,
        ],
        [
            "K",
            nutrientBalance?.potassiumBalance,
            nutrientBalance?.potassiumStatus,
        ],
        [
            "S",
            nutrientBalance?.sulphurBalance,
            nutrientBalance?.sulphurStatus,
        ],
        [
            "Zn",
            nutrientBalance?.zincBalance,
            nutrientBalance?.zincStatus,
        ],
    ];


    const nutrientBalanceAvailable =
        nutrientStatuses.some(
            ([, value, status]) =>
                (
                    value !== null &&
                    value !== undefined &&
                    value !== ""
                ) ||
                (
                    status !== null &&
                    status !== undefined &&
                    status !== ""
                )
        );


    /*
     * =========================================================
     * SATELLITE
     * =========================================================
     */

    const satelliteNdvi =
        satelliteObservation?.ndvi ??
        satelliteFieldData?.ndvi ??
        null;


    const satelliteNdwi =
        satelliteObservation?.ndwi ??
        satelliteFieldData?.ndwi ??
        null;


    const satelliteHealth =
        satelliteFieldData?.vegetationHealth ??
        satelliteObservation?.vegetationHealth ??
        null;


    const satelliteSoilMoisture =
        satelliteFieldData?.soilMoisture ??
        satelliteObservation?.soilMoisture ??
        null;


    const satelliteCloudCover =
        satelliteObservation?.cloudCoverPercent ??
        satelliteFieldData?.cloudCoverage ??
        null;


    const satelliteObservedAt =
        satelliteObservation?.observedAt ??
        satelliteFieldData?.recordedAt ??
        null;


    /*
     * =========================================================
     * SUSTAINABILITY
     * =========================================================
     */

    const sustainabilityScore =
        sustainability?.overallScore ??
        null;


    const sustainabilityLevel =
        sustainability?.sustainabilityLevel ??
        null;


    /*
     * =========================================================
     * DIGITAL MEMORY
     * =========================================================
     */

    const digitalMemoryCounts = [
        [
            language === "hi"
                ? "फसल"
                : "Crops",
            digitalMemory?.cropHistoryCount,
        ],
        [
            language === "hi"
                ? "मिट्टी नमूने"
                : "Soil Samples",
            digitalMemory?.soilSampleCount,
        ],
        [
            language === "hi"
                ? "सिंचाई"
                : "Irrigation",
            digitalMemory?.irrigationEventCount,
        ],
        [
            language === "hi"
                ? "मौसम रिकॉर्ड"
                : "Weather",
            digitalMemory?.weatherRecordCount,
        ],
    ];


    /*
     * =========================================================
     * ALERTS
     * =========================================================
     */

    const unreadAlertCount =
        farmerAlerts.length;


    /*
     * =========================================================
     * FIELD CARD
     * =========================================================
     */

    const fieldCardData =
        useMemo(() => {
            if (
                !farm &&
                !currentField
            ) {
                return null;
            }

            const realFarmId =
                farm?.id ??
                currentField?.id ??
                null;

            if (!realFarmId) {
                return null;
            }

            return {
                id: realFarmId,

                name:
                    farm?.name ??
                    currentField?.name ??
                    "—",

                nameHi:
                    farm?.name ??
                    currentField?.nameHi ??
                    farm?.name ??
                    "—",

                icon:
                    currentField?.icon ??
                    "🌾",

                statusText:
                    String(
                        healthStatus
                    ).toUpperCase(),

                statusTextHi:
                    String(
                        healthStatus
                    ).toUpperCase(),

                statusColor:
                    getHealthStatusColor(
                        healthStatus
                    ),

                stage:
                currentCropStage,

                stageLabel:
                currentCropStage,

                stageLabelHi:
                currentCropStage,

                moisture:
                    currentMoisture !== null
                        ? `${formatNumber(
                            currentMoisture,
                            1
                        )}%`
                        : "—",

                moistureHi:
                    currentMoisture !== null
                        ? `${formatNumber(
                            currentMoisture,
                            1
                        )}%`
                        : "—",

                lastUpdated:
                    passport?.generatedAt ??
                    weather?.recordedAt ??
                    farm?.updatedAt ??
                    farm?.createdAt ??
                    null,
            };
        }, [
            farm,
            currentField,
            healthStatus,
            currentCropStage,
            currentMoisture,
            passport,
            weather,
        ]);


    /*
     * =========================================================
     * QUICK STATISTICS
     * =========================================================
     */

    const fieldsCount =
        fields.length;


    const fieldsNeedingAttention =
        [
            "WARNING",
            "MODERATE",
            "POOR",
            "CRITICAL",
        ].includes(
            String(
                healthStatus
            ).toUpperCase()
        )
            ? 1
            : 0;


    /*
     * =========================================================
     * TODAY ACTIONS
     * =========================================================
     */

    const todayActions =
        useMemo(() => {
            const actions = [];


            /*
             * WEATHER RISKS
             */

            sortedRisks
                .slice(0, 3)
                .forEach(
                    (risk) => {
                        const level =
                            String(
                                risk?.riskLevel ??
                                ""
                            ).toUpperCase();

                        const riskType =
                            String(
                                risk?.riskType ??
                                ""
                            ).toUpperCase();

                        const title =
                            risk?.title ??
                            (
                                riskType
                                    ? riskType.replace(
                                        /_/g,
                                        " "
                                    )
                                    : "Weather Risk"
                            );

                        const backendRecommendation =
                            risk?.recommendation ??
                            risk?.description ??
                            null;

                        actions.push({
                            id:
                                `risk-${
                                    risk?.id ??
                                    riskType
                                }`,

                            priority:
                                getRiskPriority(
                                    level
                                ),

                            type:
                                getRiskPriority(
                                    level
                                ) >= 80
                                    ? "danger"
                                    : "warning",

                            icon:
                                getRiskIcon(
                                    riskType
                                ),

                            titleEn:
                            title,

                            titleHi:
                            title,

                            textEn:
                                backendRecommendation ??
                                "—",

                            textHi:
                                backendRecommendation ??
                                "—",

                            buttonEn:
                                "View Weather Risk",

                            buttonHi:
                                "मौसम जोखिम देखें",

                            action:
                                () =>
                                    navigate(
                                        "/weather-radar"
                                    ),
                        });
                    }
                );


            /*
             * DISEASE / PEST ACTION
             */

            if (
                highestDiseasePestRisk
            ) {
                actions.push({
                    id:
                        `disease-pest-${
                            highestDiseasePestRisk.id ??
                            "risk"
                        }`,

                    priority:
                        getRiskPriority(
                            highestDiseasePestRisk.riskLevel
                        ) + 1,

                    type:
                        getRiskPriority(
                            highestDiseasePestRisk.riskLevel
                        ) >= 80
                            ? "danger"
                            : "warning",

                    icon:
                        "🐛",

                    titleEn:
                        highestDiseasePestRisk.riskName ??
                        "Disease / Pest Risk",

                    titleHi:
                        highestDiseasePestRisk.riskName ??
                        "रोग / कीट जोखिम",

                    textEn:
                        highestDiseasePestRisk.recommendation ??
                        highestDiseasePestRisk.reason ??
                        "—",

                    textHi:
                        highestDiseasePestRisk.recommendation ??
                        highestDiseasePestRisk.reason ??
                        "—",

                    buttonEn:
                        "View Disease & Pest",

                    buttonHi:
                        "रोग और कीट देखें",

                    action:
                        () =>
                            navigate(
                                "/disease-pest"
                            ),
                });
            }


            /*
             * IRRIGATION
             */

            const latestIrrigationEvent =
                getLatestByDate(
                    irrigationEvents,
                    "startTime"
                );


            if (
                irrigationTelemetry ||
                irrigationConfig ||
                latestIrrigationEvent
            ) {
                let irrigationTextEn =
                    "Check the current irrigation status.";

                let irrigationTextHi =
                    "सिंचाई की वर्तमान स्थिति जांचें।";


                if (
                    pumpStatus === "ON"
                ) {
                    irrigationTextEn =
                        "The irrigation pump is currently ON. Check the field moisture and irrigation status.";

                    irrigationTextHi =
                        "सिंचाई पंप अभी चालू है। खेत की नमी और सिंचाई की स्थिति जांचें।";
                } else if (
                    pumpStatus === "OFF"
                ) {
                    irrigationTextEn =
                        "The irrigation pump is currently OFF. Check moisture before starting irrigation.";

                    irrigationTextHi =
                        "सिंचाई पंप अभी बंद है। सिंचाई शुरू करने से पहले मिट्टी की नमी जांचें।";
                }


                actions.push({
                    id:
                        "irrigation",

                    priority:
                        55,

                    type:
                        "info",

                    icon:
                        "💧",

                    titleEn:
                        "Check irrigation",

                    titleHi:
                        "सिंचाई जांचें",

                    textEn:
                    irrigationTextEn,

                    textHi:
                    irrigationTextHi,

                    buttonEn:
                        "Open Irrigation",

                    buttonHi:
                        "सिंचाई खोलें",

                    action:
                        () =>
                            navigate(
                                "/irrigation"
                            ),
                });
            }


            /*
             * DON'T DO TODAY
             */

            if (
                dontDoToday
            ) {
                const decision =
                    String(
                        dontDoToday?.decision ??
                        ""
                    );

                actions.push({
                    id:
                        "dont-do-today",

                    priority:
                        72,

                    type:
                        "warning",

                    icon:
                        "🛑",

                    titleEn:
                        dontDoToday?.title ??
                        dontDoToday?.action ??
                        "Don't Do This Today",

                    titleHi:
                        dontDoToday?.title ??
                        dontDoToday?.action ??
                        "आज यह न करें",

                    textEn:
                        dontDoToday?.message ??
                        dontDoToday?.reason ??
                        (
                            decision
                                ? decision
                                : "—"
                        ),

                    textHi:
                        dontDoToday?.message ??
                        dontDoToday?.reason ??
                        (
                            decision
                                ? decision
                                : "—"
                        ),

                    buttonEn:
                        "Review Decision",

                    buttonHi:
                        "निर्णय देखें",

                    action:
                        () =>
                            navigate(
                                "/irrigation"
                            ),
                });
            }


            /*
             * FERTILIZER REVIEW
             */

            if (
                fertilizerRecommendation &&
                String(
                    fertilizerRecommendation.status ??
                    ""
                ).toUpperCase() ===
                "REVIEW_REQUIRED"
            ) {
                actions.push({
                    id:
                        "fertilizer-review",

                    priority:
                        60,

                    type:
                        "warning",

                    icon:
                        "🌿",

                    titleEn:
                        "Review fertilizer advice",

                    titleHi:
                        "खाद की सलाह जांचें",

                    textEn:
                        fertilizerRecommendation.recommendation ??
                        "Review the fertilizer recommendation.",

                    textHi:
                        fertilizerRecommendation.recommendation ??
                        "उर्वरक की सलाह जांचें।",

                    buttonEn:
                        "Open Fertilizer",

                    buttonHi:
                        "खाद खोलें",

                    action:
                        () =>
                            navigate(
                                "/fertilizer"
                            ),
                });
            }


            /*
             * FERTILIZER SCHEDULE
             */

            if (
                fertilizerSchedule?.plannedDate
            ) {
                const today =
                    new Date();

                today.setHours(
                    0,
                    0,
                    0,
                    0
                );


                const scheduleDate =
                    new Date(
                        fertilizerSchedule.plannedDate
                    );


                if (
                    !Number.isNaN(
                        scheduleDate.getTime()
                    )
                ) {
                    scheduleDate.setHours(
                        0,
                        0,
                        0,
                        0
                    );


                    const diffDays =
                        Math.ceil(
                            (
                                scheduleDate -
                                today
                            ) /
                            (
                                1000 *
                                60 *
                                60 *
                                24
                            )
                        );


                    if (
                        diffDays >= 0 &&
                        diffDays <= 3
                    ) {
                        actions.push({
                            id:
                                "fertilizer-schedule",

                            priority:
                                55,

                            type:
                                "info",

                            icon:
                                "📅",

                            titleEn:
                                "Fertilizer schedule",

                            titleHi:
                                "खाद का अगला समय",

                            textEn:
                                fertilizerSchedule.recommendation ??
                                `Next fertilizer activity: ${formatDate(
                                    fertilizerSchedule.plannedDate,
                                    "en"
                                )}.`,

                            textHi:
                                fertilizerSchedule.recommendation ??
                                `अगली खाद की गतिविधि: ${formatDate(
                                    fertilizerSchedule.plannedDate,
                                    "hi"
                                )}।`,

                            buttonEn:
                                "View Fertilizer",

                            buttonHi:
                                "खाद देखें",

                            action:
                                () =>
                                    navigate(
                                        "/fertilizer"
                                    ),
                        });
                    }
                }
            }


            /*
             * SOIL TEST REMINDER
             */

            const soilReminder =
                String(
                    soilTrend?.testReminderStatus ??
                    ""
                ).toUpperCase();


            if (
                soilReminder === "OVERDUE"
            ) {
                actions.push({
                    id:
                        "soil-test-overdue",

                    priority:
                        70,

                    type:
                        "danger",

                    icon:
                        "🧪",

                    titleEn:
                        "Soil test is overdue",

                    titleHi:
                        "मिट्टी की जांच बाकी है",

                    textEn:
                        soilTrend?.recommendedAction ??
                        "A new soil test is needed.",

                    textHi:
                        soilTrend?.recommendedAction ??
                        "नई मिट्टी जांच की आवश्यकता है।",

                    buttonEn:
                        "Open Soil Analysis",

                    buttonHi:
                        "मिट्टी जांचें",

                    action:
                        () =>
                            navigate(
                                "/analysis",
                                {
                                    state: {
                                        fieldId:
                                        selectedFarmId,
                                    },
                                }
                            ),
                });
            } else if (
                soilReminder === "DUE_SOON"
            ) {
                actions.push({
                    id:
                        "soil-test-due-soon",

                    priority:
                        50,

                    type:
                        "info",

                    icon:
                        "🧪",

                    titleEn:
                        "Soil test is due soon",

                    titleHi:
                        "मिट्टी की जांच जल्द करें",

                    textEn:
                        soilTrend?.recommendedAction ??
                        "Plan the next soil test.",

                    textHi:
                        soilTrend?.recommendedAction ??
                        "अगली मिट्टी जांच की योजना बनाएं।",

                    buttonEn:
                        "View Soil",

                    buttonHi:
                        "मिट्टी देखें",

                    action:
                        () =>
                            navigate(
                                "/analysis",
                                {
                                    state: {
                                        fieldId:
                                        selectedFarmId,
                                    },
                                }
                            ),
                });
            }


            /*
             * SOIL RECOVERY
             */

            if (
                recoveryPlans.length > 0
            ) {
                const activePlan =
                    recoveryPlans[0];

                actions.push({
                    id:
                        "soil-recovery",

                    priority:
                        45,

                    type:
                        "info",

                    icon:
                        "🌱",

                    titleEn:
                        "Soil recovery plan",

                    titleHi:
                        "मिट्टी सुधार योजना",

                    textEn:
                        activePlan?.recommendation ??
                        "A soil recovery plan is active for this field.",

                    textHi:
                        activePlan?.recommendation ??
                        "इस खेत के लिए मिट्टी सुधार योजना सक्रिय है।",

                    buttonEn:
                        "View Soil Plan",

                    buttonHi:
                        "मिट्टी योजना देखें",

                    action:
                        () =>
                            navigate(
                                "/field-health-passport"
                            ),
                });
            }


            /*
             * FIELD HEALTH
             */

            const normalizedHealth =
                String(
                    healthStatus ||
                    ""
                ).toUpperCase();


            if (
                normalizedHealth === "CRITICAL" ||
                normalizedHealth === "POOR"
            ) {
                actions.push({
                    id:
                        "field-health",

                    priority:
                        85,

                    type:
                        "danger",

                    icon:
                        "🚨",

                    titleEn:
                        "Field health needs attention",

                    titleHi:
                        "खेत के स्वास्थ्य पर ध्यान दें",

                    textEn:
                        passport?.summary ??
                        passport?.healthSummary ??
                        "Review the field health passport.",

                    textHi:
                        passport?.summary ??
                        passport?.healthSummary ??
                        "फील्ड हेल्थ पासपोर्ट जांचें।",

                    buttonEn:
                        "View Field Health",

                    buttonHi:
                        "फील्ड हेल्थ देखें",

                    action:
                        () =>
                            navigate(
                                "/field-health-passport"
                            ),
                });
            }


            return actions
                .sort(
                    (a, b) =>
                        b.priority -
                        a.priority
                )
                .slice(
                    0,
                    4
                );

        }, [
            sortedRisks,
            highestDiseasePestRisk,
            irrigationTelemetry,
            irrigationConfig,
            irrigationEvents,
            pumpStatus,
            dontDoToday,
            fertilizerRecommendation,
            fertilizerSchedule,
            soilTrend,
            recoveryPlans,
            healthStatus,
            passport,
            selectedFarmId,
            navigate,
        ]);


    /*
     * =========================================================
     * NO FARM
     * =========================================================
     */

    if (
        !fieldsLoading &&
        !loading &&
        !selectedFarmId
    ) {
        return (
            <div className="home-container">

                <div className="home-header">
                    <h1 className="home-title">
                        {language === "hi"
                            ? "नमस्ते 👋"
                            : "Namaste 👋"}
                    </h1>
                </div>


                <div className="home-section">
                    <div className="last-report-card">
                        <div className="last-report-icon">
                            🌾
                        </div>

                        <div className="last-report-text">
                            <div className="last-report-title">
                                {language === "hi"
                                    ? "कोई खेत चयनित नहीं"
                                    : "No field selected"}
                            </div>

                            <div className="last-report-sub">
                                {language === "hi"
                                    ? "पहले अपना खेत जोड़ें या चयन करें।"
                                    : "Please add or select a field first."}
                            </div>
                        </div>
                    </div>
                </div>


                <button
                    type="button"
                    className="add-field-button"
                    onClick={() =>
                        navigate(
                            "/add-field"
                        )
                    }
                >
                    +{" "}
                    {language === "hi"
                        ? "खेत जोड़ें"
                        : "Add Field"}
                </button>


                <BottomNavBar />

            </div>
        );
    }


    /*
     * =========================================================
     * LOADING
     * =========================================================
     */

    if (
        loading ||
        fieldsLoading
    ) {
        return (
            <div className="home-container">

                <div className="home-header">
                    <h1 className="home-title">
                        {language === "hi"
                            ? "लोड हो रहा है..."
                            : "Loading..."}
                    </h1>
                </div>


                <div className="home-section">
                    <div className="last-report-card">
                        <div className="last-report-icon">
                            🌱
                        </div>

                        <div className="last-report-text">
                            <div className="last-report-title">
                                {language === "hi"
                                    ? "AgriNexus डेटा लोड हो रहा है"
                                    : "Loading AgriNexus data"}
                            </div>

                            <div className="last-report-sub">
                                {language === "hi"
                                    ? "मौसम, मिट्टी, सिंचाई और फसल डेटा लिया जा रहा है..."
                                    : "Loading weather, soil, irrigation and crop data..."}
                            </div>
                        </div>
                    </div>
                </div>


                <BottomNavBar />

            </div>
        );
    }


    /*
     * =========================================================
     * MAIN HOME
     * =========================================================
     */

    return (
        <div className="home-container">

            {/* =====================================================
                HEADER
            ===================================================== */}

            <div className="home-header">

                <h1 className="home-title">
                    {language === "hi"
                        ? "नमस्ते 👋"
                        : "Namaste 👋"}
                </h1>


                <div>
                    <strong>
                        {farmName}
                    </strong>

                    <div>
                        {farmLocation}
                    </div>

                    {farmLatitude !== null &&
                        farmLongitude !== null && (
                            <div>
                                {farmLatitude}
                                {", "}
                                {farmLongitude}
                            </div>
                        )}
                </div>

            </div>


            {/* =====================================================
                DATA WARNING
            ===================================================== */}

            {(
                fieldsError ||
                dataErrors.length > 0
            ) && (
                <div className="home-section">

                    <div className="last-report-card">

                        <div className="last-report-icon">
                            ⚠️
                        </div>

                        <div className="last-report-text">

                            <div className="last-report-title">
                                {language === "hi"
                                    ? "कुछ डेटा उपलब्ध नहीं है"
                                    : "Some data is unavailable"}
                            </div>

                            <div className="last-report-sub">
                                {fieldsError ??
                                    (
                                        language === "hi"
                                            ? `${dataErrors.length} मॉड्यूल का डेटा उपलब्ध नहीं है।`
                                            : `${dataErrors.length} modules could not be loaded.`
                                    )}
                            </div>

                        </div>

                    </div>

                </div>
            )}


            {/* =====================================================
                TODAY ACTIONS
            ===================================================== */}

            <div className="home-today-actions-section">

                <div className="home-today-actions-header">

                    <div>
                        <div className="home-today-actions-title">
                            {language === "hi"
                                ? "आज क्या करें?"
                                : "What should I do today?"}
                        </div>

                        <div className="home-today-actions-subtitle">
                            {language === "hi"
                                ? "आपके खेत के आज के जरूरी काम"
                                : "Important actions for your field today"}
                        </div>
                    </div>


                    <div className="home-today-actions-badge">
                        {todayActions.length}
                    </div>

                </div>


                {todayActions.length === 0 ? (

                    <div className="home-today-empty">

                        <div className="home-today-empty-icon">
                            ✅
                        </div>

                        <div>

                            <div className="home-today-empty-title">
                                {language === "hi"
                                    ? "आज कोई जरूरी कार्रवाई नहीं"
                                    : "No urgent action today"}
                            </div>

                            <div className="home-today-empty-text">
                                {language === "hi"
                                    ? "उपलब्ध खेत डेटा के अनुसार अभी कोई जरूरी काम नहीं मिला।"
                                    : "Based on the available field data, no urgent action was identified."}
                            </div>

                        </div>

                    </div>

                ) : (

                    <div className="home-today-actions-list">

                        {todayActions.map(
                            (item) => (
                                <div
                                    key={item.id}
                                    className={`home-today-action home-today-action-${item.type}`}
                                >

                                    <div className="home-today-action-icon">
                                        {item.icon}
                                    </div>


                                    <div className="home-today-action-content">

                                        <div className="home-today-action-title">
                                            {language === "hi"
                                                ? item.titleHi
                                                : item.titleEn}
                                        </div>


                                        <div className="home-today-action-text">
                                            {language === "hi"
                                                ? item.textHi
                                                : item.textEn}
                                        </div>


                                        <button
                                            type="button"
                                            className="home-today-action-button"
                                            onClick={
                                                item.action
                                            }
                                        >
                                            {language === "hi"
                                                ? item.buttonHi
                                                : item.buttonEn}
                                            {" →"}
                                        </button>

                                    </div>

                                </div>
                            )
                        )}

                    </div>

                )}

            </div>


            {/* =====================================================
                WEATHER
            ===================================================== */}

            <WeatherCard
                icon={
                    weatherIcon
                }

                temp={
                    temperature !== null
                        ? Number(
                            temperature
                        )
                        : "—"
                }

                condition={
                    weatherCondition
                }

                humidity={
                    humidity !== null
                        ? Number(
                            humidity
                        )
                        : "—"
                }

                rainChance={
                    rainfall !== null
                        ? Number(
                            rainfall
                        )
                        : "—"
                }

                language={
                    language
                }
            />


            {/* =====================================================
                TODAY'S WEATHER
            ===================================================== */}

            <div className="home-section">

                <div className="last-report-card">

                    <div className="last-report-icon">
                        🌤️
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "आज का मौसम"
                                : "Today's Weather"}
                        </div>


                        <div className="last-report-sub">

                            {temperature !== null
                                ? `${formatNumber(
                                    temperature,
                                    1
                                )}°C`
                                : "—"}

                            {" · "}

                            {humidity !== null
                                ? `${formatNumber(
                                    humidity,
                                    0
                                )}%`
                                : "—"}

                            {" · "}

                            {rainfall !== null
                                ? `${formatNumber(
                                    rainfall,
                                    1
                                )} mm`
                                : "—"}

                        </div>


                        {feelsLike !== null && (
                            <div className="last-report-sub">
                                {language === "hi"
                                    ? `महसूस तापमान: ${formatNumber(
                                        feelsLike,
                                        1
                                    )}°C`
                                    : `Feels like: ${formatNumber(
                                        feelsLike,
                                        1
                                    )}°C`}
                            </div>
                        )}


                        {windSpeed !== null && (
                            <div className="last-report-sub">
                                {language === "hi"
                                    ? `हवा: ${formatNumber(
                                        windSpeed,
                                        1
                                    )} km/h`
                                    : `Wind: ${formatNumber(
                                        windSpeed,
                                        1
                                    )} km/h`}
                            </div>
                        )}


                        {pressure !== null && (
                            <div className="last-report-sub">
                                {language === "hi"
                                    ? `दबाव: ${formatNumber(
                                        pressure,
                                        1
                                    )} hPa`
                                    : `Pressure: ${formatNumber(
                                        pressure,
                                        1
                                    )} hPa`}
                            </div>
                        )}

                    </div>

                </div>

            </div>


            {/* =====================================================
                WEATHER RISK
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card home-weather-risk-card"
                    onClick={() =>
                        navigate(
                            "/weather-radar"
                        )
                    }
                    role="button"
                    tabIndex={0}
                    onKeyDown={(event) => {
                        if (
                            event.key === "Enter" ||
                            event.key === " "
                        ) {
                            event.preventDefault();

                            navigate(
                                "/weather-radar"
                            );
                        }
                    }}
                >

                    <div className="last-report-icon">
                        {getRiskIcon(
                            highestRiskType
                        )}
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "मौसम जोखिम"
                                : "Weather Risk"}
                        </div>


                        <div className="last-report-sub">
                            {highestRiskType
                                ? `${String(
                                    highestRiskType
                                ).replace(
                                    /_/g,
                                    " "
                                )}${
                                    highestRiskScore !== null &&
                                    highestRiskScore !== undefined
                                        ? ` · Score ${highestRiskScore}`
                                        : ""
                                }`
                                : (
                                    language === "hi"
                                        ? "कोई प्रमुख जोखिम उपलब्ध नहीं"
                                        : "No major risk available"
                                )}
                        </div>


                        <div className="last-report-sub">
                            {activeRiskCount}{" "}
                            {language === "hi"
                                ? "सक्रिय जोखिम"
                                : "active risks"}
                        </div>

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                QUICK STATS
            ===================================================== */}

            <div className="quick-stats">

                <div className="quick-stat-box">

                    <div className="quick-stat-number">
                        {fieldsCount}
                    </div>

                    <div className="quick-stat-label">
                        {language === "hi"
                            ? "कुल खेत"
                            : "Total Fields"}
                    </div>

                </div>


                <div className="quick-stat-box quick-stat-warning">

                    <div className="quick-stat-number">
                        {fieldsNeedingAttention}
                    </div>

                    <div className="quick-stat-label">
                        {language === "hi"
                            ? "ध्यान चाहिए"
                            : "Need Attention"}
                    </div>

                </div>


                <div className="quick-stat-box">

                    <div className="quick-stat-number">
                        {activeRiskCount}
                    </div>

                    <div className="quick-stat-label">
                        {language === "hi"
                            ? "जोखिम"
                            : "Risks"}
                    </div>

                </div>

            </div>
            {/* =====================================================
    SOWING ADVISOR
===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card"
                    onClick={() =>
                        navigate(
                            "/sowing-advisor"
                        )
                    }
                    role="button"
                    tabIndex={0}
                    onKeyDown={(event) => {
                        if (
                            event.key === "Enter" ||
                            event.key === " "
                        ) {
                            event.preventDefault();

                            navigate(
                                "/sowing-advisor"
                            );
                        }
                    }}
                >

                    <div className="last-report-icon">
                        🌱
                    </div>

                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "बुवाई सलाहकार"
                                : "Sowing Advisor"}
                        </div>

                        <div className="last-report-sub">
                            {sowingAdvisor?.cropName ??
                                (language === "hi"
                                    ? "कोई फसल निर्णय नहीं"
                                    : "No crop decision available")}
                            {" · "}
                            {sowingAdvisor?.decision ??
                                "—"}
                        </div>

                        <div className="last-report-sub">
                            {sowingAdvisor?.recommendedWindow
                                ? `Window: ${sowingAdvisor.recommendedWindow}`
                                : "—"}
                        </div>

                    </div>

                    <div className="field-card-arrow"></div>

                </div>

            </div>

            {/* =====================================================
                SOIL & CROP
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card home-soil-analysis-card"
                    onClick={() =>
                        navigate(
                            "/analysis",
                            {
                                state: {
                                    fieldId:
                                    selectedFarmId,
                                },
                            }
                        )
                    }
                    role="button"
                    tabIndex={0}
                >

                    <div className="last-report-icon">
                        🧪
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "मिट्टी और फसल जांच"
                                : "Soil & Crop Analysis"}
                        </div>


                        <div className="last-report-sub">
                            {soilType !== "—"
                                ? `${soilType} · pH ${
                                    formatNumber(
                                        soilPh,
                                        1
                                    ) ?? "—"
                                }`
                                : (
                                    language === "hi"
                                        ? "मिट्टी की जानकारी देखें"
                                        : "View soil information"
                                )}
                        </div>


                        <div className="last-report-sub">
                            N{" "}
                            {soilNitrogen ?? "—"}

                            {" · "}

                            P{" "}
                            {soilPhosphorus ?? "—"}

                            {" · "}

                            K{" "}
                            {soilPotassium ?? "—"}
                        </div>


                        {soilOrganicCarbon !== null && (
                            <div className="last-report-sub">
                                {language === "hi"
                                    ? `ऑर्गेनिक कार्बन: ${soilOrganicCarbon}`
                                    : `Organic Carbon: ${soilOrganicCarbon}`}
                            </div>
                        )}

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                CURRENT CROP
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card home-current-crop-card"
                    onClick={() =>
                        navigate(
                            "/history"
                        )
                    }
                    role="button"
                    tabIndex={0}
                    onKeyDown={(event) => {
                        if (
                            event.key === "Enter" ||
                            event.key === " "
                        ) {
                            event.preventDefault();

                            navigate(
                                "/history"
                            );
                        }
                    }}
                >

                    <div className="last-report-icon">
                        🌾
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "वर्तमान फसल"
                                : "Current Crop"}
                        </div>


                        <div className="last-report-sub">
                            {currentCrop}
                            {" — "}
                            {currentCropStage}
                        </div>


                        {currentMoisture !== null && (
                            <div className="last-report-sub">
                                {language === "hi"
                                    ? `मिट्टी की नमी: ${formatNumber(
                                        currentMoisture,
                                        1
                                    )}%`
                                    : `Soil Moisture: ${formatNumber(
                                        currentMoisture,
                                        1
                                    )}%`}
                            </div>
                        )}

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                SMART IRRIGATION
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card home-irrigation-card"
                    onClick={() =>
                        navigate(
                            "/irrigation"
                        )
                    }
                    role="button"
                    tabIndex={0}
                >

                    <div className="last-report-icon">
                        💧
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "स्मार्ट सिंचाई"
                                : "Smart Irrigation"}
                        </div>


                        <div className="last-report-sub">
                            {pumpStatus !== "—"
                                ? (
                                    language === "hi"
                                        ? `पंप: ${
                                            pumpStatus === "ON"
                                                ? "चालू"
                                                : "बंद"
                                        }`
                                        : `Pump: ${pumpStatus}`
                                )
                                : (
                                    language === "hi"
                                        ? "सिंचाई स्थिति देखें"
                                        : "View irrigation status"
                                )}
                        </div>


                        <div className="last-report-sub">

                            {irrigationMoisture !== null
                                ? (
                                    language === "hi"
                                        ? `नमी: ${formatNumber(
                                            irrigationMoisture,
                                            1
                                        )}%`
                                        : `Moisture: ${formatNumber(
                                            irrigationMoisture,
                                            1
                                        )}%`
                                )
                                : "—"}


                            {irrigationFlow !== null && (
                                <>
                                    {" · "}
                                    {
                                        formatNumber(
                                            irrigationFlow,
                                            1
                                        )
                                    }
                                    {" L/min"}
                                </>
                            )}

                        </div>

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                IRRIGATION DETAILS
            ===================================================== */}

            <div className="home-section">

                <div className="quick-stats">

                    <div className="quick-stat-box">

                        <div className="quick-stat-number">
                            {irrigationFlow !== null
                                ? formatNumber(
                                    irrigationFlow,
                                    0
                                )
                                : "—"}
                        </div>

                        <div className="quick-stat-label">
                            {language === "hi"
                                ? "फ्लो L/min"
                                : "Flow L/min"}
                        </div>

                    </div>


                    <div className="quick-stat-box">

                        <div className="quick-stat-number">
                            {irrigationPressure !== null
                                ? formatNumber(
                                    irrigationPressure,
                                    1
                                )
                                : "—"}
                        </div>

                        <div className="quick-stat-label">
                            {language === "hi"
                                ? "दबाव bar"
                                : "Pressure bar"}
                        </div>

                    </div>


                    <div className="quick-stat-box">

                        <div className="quick-stat-number">
                            {irrigationRuntime !== null
                                ? formatNumber(
                                    irrigationRuntime,
                                    0
                                )
                                : "—"}
                        </div>

                        <div className="quick-stat-label">
                            {language === "hi"
                                ? "रनटाइम min"
                                : "Runtime min"}
                        </div>

                    </div>

                </div>

            </div>


            {/* =====================================================
                FERTILIZER
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card home-fertilizer-card"
                    onClick={() =>
                        navigate(
                            "/fertilizer"
                        )
                    }
                    role="button"
                    tabIndex={0}
                >

                    <div className="last-report-icon">
                        🌿
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "उर्वरक"
                                : "Fertilizer"}
                        </div>


                        <div className="last-report-sub">
                            {fertilizerCrop}
                            {" · "}
                            {fertilizerStatus}
                        </div>


                        {fertilizerRecommendation?.nitrogen !== null &&
                            fertilizerRecommendation?.nitrogen !== undefined && (
                                <div className="last-report-sub">

                                    N:
                                    {" "}
                                    {
                                        fertilizerRecommendation.nitrogen
                                    }

                                    {" · "}

                                    P:
                                    {" "}
                                    {
                                        fertilizerRecommendation.phosphorus ??
                                        "—"
                                    }

                                    {" · "}

                                    K:
                                    {" "}
                                    {
                                        fertilizerRecommendation.potassium ??
                                        "—"
                                    }

                                </div>
                            )}


                        {fertilizerSchedule?.plannedDate && (
                            <div className="last-report-sub">

                                📅{" "}

                                {language === "hi"
                                    ? "अगला:"
                                    : "Next:"}

                                {" "}

                                {formatDate(
                                    fertilizerSchedule.plannedDate,
                                    language
                                )}

                            </div>
                        )}

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                VERIFIED FIELD INTELLIGENCE
            ===================================================== */}

            <VerifiedFieldIntelligence
                language={language}
                cropHistory={
                    cropHistory
                }
                fertilizerRecommendation={
                    fertilizerRecommendation
                }
                fertilizerSchedule={
                    fertilizerSchedule
                }
                soilTrend={
                    soilTrend
                }
                recoveryPlans={
                    recoveryPlans
                }
            />


            {/* =====================================================
                NUTRIENT BALANCE
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card"
                    onClick={() =>
                        navigate(
                            "/nutrient-balance"
                        )
                    }
                    role="button"
                    tabIndex={0}
                >

                    <div className="last-report-icon">
                        🧬
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "पोषक तत्व संतुलन"
                                : "Nutrient Balance"}
                        </div>


                        <div className="last-report-sub">
                            {nutrientBalance?.cropName ??
                                currentCrop}
                            {" · "}
                            {nutrientBalance?.dataStatus ??
                                "—"}
                        </div>


                        <div className="last-report-sub">

                            {nutrientBalanceAvailable
                                ? nutrientStatuses
                                    .filter(
                                        ([, value, status]) =>
                                            (
                                                value !== null &&
                                                value !== undefined &&
                                                value !== ""
                                            ) ||
                                            (
                                                status !== null &&
                                                status !== undefined &&
                                                status !== ""
                                            )
                                    )
                                    .map(
                                        ([name, value, status]) =>
                                            `${name}: ${
                                                value !== null &&
                                                value !== undefined &&
                                                value !== ""
                                                    ? formatNumber(
                                                        value,
                                                        2
                                                    )
                                                    : status ?? "—"
                                            }`
                                    )
                                    .join(" · ")
                                : "—"}

                        </div>

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                WEATHER-AWARE SOWING
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card"
                    onClick={() =>
                        navigate(
                            "/sowing-advisor"
                        )
                    }
                    role="button"
                    tabIndex={0}
                >

                    <div className="last-report-icon">
                        🌱
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "मौसम आधारित बुवाई सलाह"
                                : "Weather-Aware Sowing"}
                        </div>


                        <div className="last-report-sub">
                            {sowingAdvisor?.cropName ??
                                "—"}
                            {" · "}
                            {sowingAdvisor?.decision ??
                                "—"}
                        </div>


                        <div className="last-report-sub">
                            {sowingAdvisor?.recommendedWindow ??
                                "—"}
                        </div>


                        <div className="last-report-sub">
                            {sowingAdvisor?.recommendation ??
                                "—"}
                        </div>

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                DISEASE / PEST
            ===================================================== */}

            <div className="home-section">

                <div className="last-report-card">

                    <div className="last-report-icon">
                        🐛
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "रोग और कीट चेतावनी"
                                : "Disease & Pest Warning"}
                        </div>


                        <div className="last-report-sub">
                            {highestDiseasePestRisk?.riskName ??
                                "—"}

                            {" · "}

                            {highestDiseasePestRisk?.riskLevel ??
                                "—"}

                            {
                                highestDiseasePestRisk?.riskScore !==
                                null &&
                                highestDiseasePestRisk?.riskScore !==
                                undefined
                                    ? ` · Score ${highestDiseasePestRisk.riskScore}`
                                    : ""
                            }
                        </div>


                        <div className="last-report-sub">
                            {highestDiseasePestRisk?.recommendation ??
                                highestDiseasePestRisk?.reason ??
                                "—"}
                        </div>

                    </div>

                </div>

            </div>


            {/* =====================================================
                FIELD HEALTH
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card home-passport-card"
                    onClick={() =>
                        navigate(
                            "/field-health-passport"
                        )
                    }
                    role="button"
                    tabIndex={0}
                >

                    <div className="last-report-icon">
                        {
                            getHealthIcon(
                                healthStatus
                            )
                        }
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "फील्ड हेल्थ"
                                : "Field Health"}
                        </div>


                        <div className="last-report-sub">
                            {language === "hi"
                                ? `स्थिति: ${healthStatus}`
                                : `Status: ${healthStatus}`}
                        </div>


                        {healthScore !== null && (
                            <div className="last-report-sub">
                                {language === "hi"
                                    ? `स्कोर: ${formatNumber(
                                        healthScore,
                                        2
                                    )}`
                                    : `Score: ${formatNumber(
                                        healthScore,
                                        2
                                    )}`}
                            </div>
                        )}

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                NEXT CROP / LOW COST
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card home-lowest-plan-card"
                    onClick={() =>
                        navigate(
                            "/lowest-cost-plan"
                        )
                    }
                    role="button"
                    tabIndex={0}
                >

                    <div className="last-report-icon">
                        🌾
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "अगली फसल और कम लागत योजना"
                                : "Next Crop & Low-Cost Plan"}
                        </div>


                        <div className="last-report-sub">
                            {recommendedNextCrop
                                ? (
                                    language === "hi"
                                        ? `अगली फसल: ${recommendedNextCrop}`
                                        : `Next crop: ${recommendedNextCrop}`
                                )
                                : (
                                    language === "hi"
                                        ? "अगली फसल की सिफारिश प्राप्त करें"
                                        : "Get the next crop recommendation"
                                )}
                        </div>


                        {cropRotationScore !== null && (
                            <div className="last-report-sub">
                                {language === "hi"
                                    ? `स्कोर: ${formatNumber(
                                        cropRotationScore,
                                        1
                                    )}`
                                    : `Score: ${formatNumber(
                                        cropRotationScore,
                                        1
                                    )}`}
                            </div>
                        )}

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                SOIL RECOVERY
            ===================================================== */}

            {recoveryPlans.length > 0 && (
                <div className="home-section">

                    <div
                        className="last-report-card"
                        onClick={() =>
                            navigate(
                                "/field-health-passport"
                            )
                        }
                        role="button"
                        tabIndex={0}
                    >

                        <div className="last-report-icon">
                            🌿
                        </div>


                        <div className="last-report-text">

                            <div className="last-report-title">
                                {language === "hi"
                                    ? "मिट्टी सुधार योजना"
                                    : "Soil Recovery Plan"}
                            </div>


                            <div className="last-report-sub">
                                {
                                    recoveryPlans.length
                                }{" "}

                                {language === "hi"
                                    ? "सक्रिय योजना"
                                    : "active plan(s)"}
                            </div>


                            <div className="last-report-sub">
                                {recoveryPlans[0]?.focusArea ??
                                    recoveryPlans[0]?.recommendation ??
                                    "—"}
                            </div>

                        </div>

                    </div>

                </div>
            )}


            {/* =====================================================
                SATELLITE FIELD SIGNALS
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card"
                    onClick={() =>
                        navigate(
                            "/satellite"
                        )
                    }
                    role="button"
                    tabIndex={0}
                >

                    <div className="last-report-icon">
                        🛰️
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "सैटेलाइट फील्ड सिग्नल"
                                : "Satellite Field Signals"}
                        </div>


                        <div className="last-report-sub">
                            NDVI:{" "}
                            {
                                satelliteNdvi !== null
                                    ? formatNumber(
                                        satelliteNdvi,
                                        3
                                    )
                                    : "—"
                            }

                            {" · "}

                            NDWI:{" "}
                            {
                                satelliteNdwi !== null
                                    ? formatNumber(
                                        satelliteNdwi,
                                        3
                                    )
                                    : "—"
                            }
                        </div>


                        <div className="last-report-sub">
                            {language === "hi"
                                ? "वनस्पति स्वास्थ्य"
                                : "Vegetation Health"}
                            {": "}
                            {satelliteHealth ?? "—"}
                        </div>


                        <div className="last-report-sub">
                            {language === "hi"
                                ? "सैटेलाइट मिट्टी नमी"
                                : "Satellite Soil Moisture"}
                            {": "}
                            {
                                satelliteSoilMoisture !== null
                                    ? formatNumber(
                                        satelliteSoilMoisture,
                                        2
                                    )
                                    : "—"
                            }
                        </div>


                        {satelliteCloudCover !== null && (
                            <div className="last-report-sub">
                                {language === "hi"
                                    ? `क्लाउड कवरेज: ${formatNumber(
                                        satelliteCloudCover,
                                        1
                                    )}%`
                                    : `Cloud cover: ${formatNumber(
                                        satelliteCloudCover,
                                        1
                                    )}%`}
                            </div>
                        )}


                        {satelliteObservedAt && (
                            <div className="last-report-sub">
                                {language === "hi"
                                    ? `निरीक्षण: ${formatDate(
                                        satelliteObservedAt,
                                        language
                                    )}`
                                    : `Observed: ${formatDate(
                                        satelliteObservedAt,
                                        language
                                    )}`}
                            </div>
                        )}

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                SUSTAINABILITY
            ===================================================== */}

            <div className="home-section">

                <div className="last-report-card">

                    <div className="last-report-icon">
                        ♻️
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "फार्म सस्टेनेबिलिटी"
                                : "Farm Sustainability"}
                        </div>


                        <div className="last-report-sub">
                            {
                                sustainabilityScore !==
                                null &&
                                sustainabilityScore !==
                                undefined
                                    ? `Score: ${formatNumber(
                                        sustainabilityScore,
                                        1
                                    )}`
                                    : "—"
                            }

                            {" · "}

                            {sustainabilityLevel ??
                                "—"}
                        </div>


                        <div className="last-report-sub">
                            {sustainability?.recommendation ??
                                "—"}
                        </div>

                    </div>

                </div>

            </div>


            {/* =====================================================
                WATER & PUMP INTELLIGENCE
            ===================================================== */}

            <div className="home-section">

                <h2 className="home-section-title">
                    {language === "hi"
                        ? "पानी और पंप इंटेलिजेंस"
                        : "Water & Pump Intelligence"}
                </h2>


                <div className="quick-stats">

                    <div className="quick-stat-box">

                        <div className="quick-stat-number">

                            {
                                pumpCompletion?.targetReached !==
                                null &&
                                pumpCompletion?.targetReached !==
                                undefined
                                    ? (
                                        pumpCompletion.targetReached
                                            ? "✓"
                                            : "—"
                                    )
                                    : "—"
                            }

                        </div>


                        <div className="quick-stat-label">
                            {language === "hi"
                                ? "पंप लक्ष्य"
                                : "Pump Target"}
                        </div>

                    </div>


                    <div className="quick-stat-box">

                        <div className="quick-stat-number">

                            {
                                rainfallHarvesting?.estimatedCaptureLiters !==
                                null &&
                                rainfallHarvesting?.estimatedCaptureLiters !==
                                undefined
                                    ? formatNumber(
                                        rainfallHarvesting.estimatedCaptureLiters,
                                        0
                                    )
                                    : "—"
                            }

                        </div>


                        <div className="quick-stat-label">
                            {language === "hi"
                                ? "संभावित संग्रह L"
                                : "Estimated Capture L"}
                        </div>

                    </div>


                    <div className="quick-stat-box">

                        <div className="quick-stat-number">

                            {
                                solarPump?.solarPowerKw !==
                                null &&
                                solarPump?.solarPowerKw !==
                                undefined
                                    ? formatNumber(
                                        solarPump.solarPowerKw,
                                        2
                                    )
                                    : "—"
                            }

                        </div>


                        <div className="quick-stat-label">
                            {language === "hi"
                                ? "सोलर kW"
                                : "Solar kW"}
                        </div>

                    </div>

                </div>


                <div className="last-report-card">

                    <div className="last-report-icon">
                        💧
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "पानी निर्णय"
                                : "Water Decision"}
                        </div>


                        <div className="last-report-sub">
                            {rainfallHarvesting?.decision ??
                                "—"}

                            {" · "}

                            {solarPump?.decision ??
                                "—"}
                        </div>


                        <div className="last-report-sub">
                            {pumpCompletion?.message ??
                                pumpCompletion?.pumpAction ??
                                "—"}
                        </div>


                        <div className="last-report-sub">
                            {rainfallHarvesting?.recommendation ??
                                solarPump?.recommendation ??
                                "—"}
                        </div>

                    </div>

                </div>

            </div>


            {/* =====================================================
                DON'T DO TODAY
            ===================================================== */}

            <div className="home-section">

                <div className="last-report-card">

                    <div className="last-report-icon">
                        🛑
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "आज यह न करें"
                                : "Don't Do This Today"}
                        </div>


                        <div className="last-report-sub">
                            {dontDoToday?.title ??
                                dontDoToday?.action ??
                                "—"}

                            {" · "}

                            {dontDoToday?.decision ??
                                "—"}
                        </div>


                        <div className="last-report-sub">
                            {dontDoToday?.message ??
                                dontDoToday?.reason ??
                                "—"}
                        </div>


                        <div className="last-report-sub">
                            {dontDoToday?.forecastRainfall !==
                            null &&
                            dontDoToday?.forecastRainfall !==
                            undefined
                                ? `Forecast rainfall: ${dontDoToday.forecastRainfall}`
                                : "—"}
                        </div>

                    </div>

                </div>

            </div>


            {/* =====================================================
                EXPLAINABLE AI
            ===================================================== */}

            <div className="home-section">

                <div className="last-report-card">

                    <div className="last-report-icon">
                        🤖
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "AI ने यह क्यों कहा?"
                                : "Why did AI recommend this?"}
                        </div>


                        <div className="last-report-sub">
                            {aiExplanation?.decision ??
                                "—"}
                        </div>


                        <div className="last-report-sub">
                            {aiExplanation?.why ??
                                "—"}
                        </div>


                        <div className="last-report-sub">
                            Confidence:{" "}
                            {
                                aiExplanation?.confidence !==
                                null &&
                                aiExplanation?.confidence !==
                                undefined
                                    ? formatNumber(
                                        aiExplanation.confidence,
                                        1
                                    )
                                    : "—"
                            }
                        </div>


                        <div className="last-report-sub">
                            {
                                aiExplanation?.dataUsed
                                    ? Array.isArray(
                                        aiExplanation.dataUsed
                                    )
                                        ? aiExplanation.dataUsed.join(
                                            " · "
                                        )
                                        : String(
                                            aiExplanation.dataUsed
                                        )
                                    : "—"
                            }
                        </div>

                    </div>

                </div>

            </div>


            {/* =====================================================
                DIGITAL MEMORY
            ===================================================== */}

            <div className="home-section">

                <h2 className="home-section-title">
                    {language === "hi"
                        ? "डिजिटल फार्म मेमोरी"
                        : "Digital Farm Memory"}
                </h2>


                <div className="quick-stats">

                    {digitalMemoryCounts.map(
                        ([label, value]) => (
                            <div
                                className="quick-stat-box"
                                key={label}
                            >

                                <div className="quick-stat-number">
                                    {
                                        value !== null &&
                                        value !== undefined
                                            ? value
                                            : "—"
                                    }
                                </div>


                                <div className="quick-stat-label">
                                    {label}
                                </div>

                            </div>
                        )
                    )}

                </div>


                <div className="last-report-card">

                    <div className="last-report-icon">
                        🧠
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "फार्म रिकॉर्ड"
                                : "Farm Record"}
                        </div>


                        <div className="last-report-sub">
                            {digitalMemory?.farmName ??
                                farmName}
                        </div>


                        <div className="last-report-sub">
                            {digitalMemory?.dataStatus ??
                                "—"}
                        </div>


                        <div className="last-report-sub">
                            {digitalMemory?.uncertaintyNote ??
                                "—"}
                        </div>

                    </div>

                </div>

            </div>


            {/* =====================================================
                FARMER ALERTS
            ===================================================== */}

            <div className="home-section">

                <div className="last-report-card">

                    <div className="last-report-icon">
                        🔔
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "किसान अलर्ट"
                                : "Farmer Alerts"}
                        </div>


                        <div className="last-report-sub">
                            {unreadAlertCount}{" "}

                            {language === "hi"
                                ? "अपठित अलर्ट"
                                : "unread alerts"}
                        </div>


                        <div className="last-report-sub">
                            {farmerAlerts[0]?.title ??
                                farmerAlerts[0]?.message ??
                                "—"}
                        </div>


                        <div className="last-report-sub">
                            {farmerAlerts[0]?.reason ??
                                farmerAlerts[0]?.description ??
                                "—"}
                        </div>

                    </div>

                </div>

            </div>


            {/* =====================================================
                IMPORTANT ALERTS
            ===================================================== */}

            <div className="home-section">

                <h2 className="home-section-title">

                    {language === "hi"
                        ? "महत्वपूर्ण सूचनाएं"
                        : "Important Alerts"}

                </h2>


                <div className="home-alerts">

                    {sortedRisks.length === 0 ? (

                        <div className="last-report-card">

                            <div className="last-report-icon">
                                ✅
                            </div>


                            <div className="last-report-text">

                                <div className="last-report-title">
                                    {language === "hi"
                                        ? "कोई सक्रिय मौसम जोखिम नहीं"
                                        : "No active weather risks"}
                                </div>


                                <div className="last-report-sub">
                                    {language === "hi"
                                        ? "वर्तमान उपलब्ध मौसम डेटा के अनुसार।"
                                        : "Based on the currently available weather data."}
                                </div>

                            </div>

                        </div>

                    ) : (

                        sortedRisks
                            .slice(
                                0,
                                3
                            )
                            .map(
                                (
                                    risk,
                                    index
                                ) => (

                                    <AlertBanner
                                        key={
                                            risk.id ??
                                            `${risk.riskType}-${index}`
                                        }

                                        type={
                                            getRiskBannerType(
                                                risk.riskLevel
                                            )
                                        }

                                        icon={
                                            getRiskIcon(
                                                risk.riskType
                                            )
                                        }

                                        message={
                                            risk.title ??
                                            risk.description ??
                                            risk.riskType ??
                                            (
                                                language === "hi"
                                                    ? "मौसम जोखिम"
                                                    : "Weather risk"
                                            )
                                        }
                                    />

                                )
                            )
                    )}


                    {highestDiseasePestRisk && (
                        <AlertBanner
                            type={
                                getRiskBannerType(
                                    highestDiseasePestRisk.riskLevel
                                )
                            }
                            icon="🐛"
                            message={
                                highestDiseasePestRisk.riskName ??
                                highestDiseasePestRisk.reason ??
                                (
                                    language === "hi"
                                        ? "रोग / कीट जोखिम"
                                        : "Disease / Pest risk"
                                )
                            }
                        />
                    )}


                    {String(
                            fertilizerRecommendation?.status ??
                            ""
                        ).toUpperCase() ===
                        "REVIEW_REQUIRED" && (
                            <AlertBanner
                                type="warning"
                                icon="🌿"
                                message={
                                    language === "hi"
                                        ? "उर्वरक सिफारिश की समीक्षा आवश्यक है।"
                                        : "Fertilizer recommendation requires review."
                                }
                            />
                        )}


                    {String(
                            soilTrend?.testReminderStatus ??
                            ""
                        ).toUpperCase() ===
                        "OVERDUE" && (
                            <AlertBanner
                                type="danger"
                                icon="🧪"
                                message={
                                    language === "hi"
                                        ? "मिट्टी की जांच लंबित है।"
                                        : "Soil testing is overdue."
                                }
                            />
                        )}

                </div>

            </div>


            {/* =====================================================
                MY FIELDS
            ===================================================== */}

            <div className="home-section">

                <div className="home-fields-header">

                    <h2 className="home-section-title">
                        {language === "hi"
                            ? "मेरे खेत"
                            : "My Fields"}
                    </h2>


                    <button
                        type="button"
                        className="add-field-button"
                        onClick={() =>
                            navigate(
                                "/add-field"
                            )
                        }
                    >
                        +{" "}
                        {language === "hi"
                            ? "खेत जोड़ें"
                            : "Add Field"}
                    </button>

                </div>


                <div className="home-fields">

                    {fieldCardData && (
                        <FieldCard
                            key={
                                fieldCardData.id
                            }

                            fieldName={
                                language === "hi"
                                    ? fieldCardData.nameHi
                                    : fieldCardData.name
                            }

                            cropIcon={
                                fieldCardData.icon
                            }

                            statusText={
                                language === "hi"
                                    ? fieldCardData.statusTextHi
                                    : fieldCardData.statusText
                            }

                            statusColor={
                                fieldCardData.statusColor
                            }

                            stage={
                                fieldCardData.stage
                            }

                            stageLabel={
                                language === "hi"
                                    ? fieldCardData.stageLabelHi
                                    : fieldCardData.stageLabel
                            }

                            moisture={
                                language === "hi"
                                    ? fieldCardData.moistureHi
                                    : fieldCardData.moisture
                            }

                            lastUpdated={
                                fieldCardData.lastUpdated
                                    ? formatDate(
                                        fieldCardData.lastUpdated,
                                        language
                                    )
                                    : "—"
                            }

                            onClick={() =>
                                navigate(
                                    `/field/${fieldCardData.id}`
                                )
                            }

                            onDelete={() =>
                                setConfirmDeleteId(
                                    fieldCardData.id
                                )
                            }
                        />
                    )}

                </div>

            </div>


            {/* =====================================================
                CROP HISTORY
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card"
                    onClick={() =>
                        navigate(
                            "/history"
                        )
                    }
                    role="button"
                    tabIndex={0}
                >

                    <div className="last-report-icon">
                        📜
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "फसल इतिहास"
                                : "Crop History"}
                        </div>


                        <div className="last-report-sub">
                            {latestCropHistory
                                ? (
                                    getCropName(
                                        latestCropHistory
                                    ) ??
                                    "—"
                                )
                                : "—"}
                        </div>


                        {latestCropHistory?.season && (
                            <div className="last-report-sub">
                                {language === "hi"
                                    ? `मौसम: ${latestCropHistory.season}`
                                    : `Season: ${latestCropHistory.season}`}
                            </div>
                        )}


                        {latestCropHistory?.harvestDate && (
                            <div className="last-report-sub">
                                {language === "hi"
                                    ? `कटाई: ${formatDate(
                                        latestCropHistory.harvestDate,
                                        language
                                    )}`
                                    : `Harvest: ${formatDate(
                                        latestCropHistory.harvestDate,
                                        language
                                    )}`}
                            </div>
                        )}

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                FIELD HEALTH PASSPORT
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card home-passport-card"
                    onClick={() =>
                        navigate(
                            "/field-health-passport"
                        )
                    }
                    role="button"
                    tabIndex={0}
                >

                    <div className="last-report-icon">
                        🩺
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "फील्ड हेल्थ पासपोर्ट"
                                : "Field Health Passport"}
                        </div>


                        <div className="last-report-sub">
                            {healthScore !== null
                                ? (
                                    language === "hi"
                                        ? `हेल्थ स्कोर: ${formatNumber(
                                            healthScore,
                                            2
                                        )}`
                                        : `Health Score: ${formatNumber(
                                            healthScore,
                                            2
                                        )}`
                                )
                                : (
                                    language === "hi"
                                        ? "दीर्घकालिक फील्ड स्वास्थ्य देखें"
                                        : "View long-term field health"
                                )}
                        </div>

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                SATELLITE MONITORING
            ===================================================== */}

            <div className="home-section">

                <div
                    className="last-report-card"
                    onClick={() =>
                        navigate(
                            "/satellite"
                        )
                    }
                    role="button"
                    tabIndex={0}
                >

                    <div className="last-report-icon">
                        🛰️
                    </div>


                    <div className="last-report-text">

                        <div className="last-report-title">
                            {language === "hi"
                                ? "सैटेलाइट मॉनिटरिंग"
                                : "Satellite Monitoring"}
                        </div>


                        <div className="last-report-sub">
                            {resultsSatelliteAvailable(
                                selectedFarmId,
                                satelliteObservation,
                                satelliteFieldData
                            )}
                        </div>

                    </div>


                    <div className="field-card-arrow"></div>

                </div>

            </div>


            {/* =====================================================
                MAIN CTA
            ===================================================== */}

            <button
                type="button"
                className="home-cta-button"
                onClick={() =>
                    navigate(
                        "/lowest-cost-plan"
                    )
                }
            >
                🌾{" "}
                {language === "hi"
                    ? "अगली फसल सुझाएं"
                    : "Recommend Next Crop"}
            </button>


            <BottomNavBar />


            {/* =====================================================
                DELETE CONFIRMATION
            ===================================================== */}

            {confirmDeleteId && (

                <div className="confirm-overlay">

                    <div className="confirm-box">

                        <div className="confirm-icon">
                            ⚠️
                        </div>


                        <div className="confirm-text">
                            {language === "hi"
                                ? "क्या आप इस खेत को हटाना चाहते हैं?"
                                : "Delete this field?"}
                        </div>


                        {deleteError && (
                            <div
                                className="delete-error"
                                role="alert"
                            >
                                {deleteError}
                            </div>
                        )}


                        <div className="confirm-buttons">

                            <button
                                type="button"
                                className="confirm-cancel"
                                disabled={
                                    deletingFarm
                                }
                                onClick={() => {

                                    if (
                                        deletingFarm
                                    ) {
                                        return;
                                    }

                                    setConfirmDeleteId(
                                        null
                                    );

                                    setDeleteError(
                                        ""
                                    );
                                }}
                            >
                                {language === "hi"
                                    ? "रद्द करें"
                                    : "Cancel"}
                            </button>


                            <button
                                type="button"
                                className="confirm-delete"
                                disabled={
                                    deletingFarm
                                }
                                onClick={
                                    handleDeleteFarm
                                }
                            >
                                {deletingFarm
                                    ? (
                                        language === "hi"
                                            ? "हटा रहे हैं..."
                                            : "Deleting..."
                                    )
                                    : (
                                        language === "hi"
                                            ? "हटाएं"
                                            : "Delete"
                                    )}
                            </button>

                        </div>

                    </div>

                </div>

            )}

        </div>
    );
}


/*
 * =========================================================
 * SATELLITE DISPLAY HELPER
 * =========================================================
 *
 * IMPORTANT:
 * This helper only displays backend data.
 * It does not create a satellite measurement.
 * =========================================================
 */

function resultsSatelliteAvailable(
    farmId,
    satelliteObservation,
    satelliteFieldData
) {
    if (!farmId) {
        return "—";
    }

    const ndvi =
        satelliteObservation?.ndvi ??
        satelliteFieldData?.ndvi ??
        null;

    const health =
        satelliteFieldData?.vegetationHealth ??
        satelliteObservation?.vegetationHealth ??
        null;

    if (
        ndvi === null ||
        ndvi === undefined
    ) {
        return health ??
            "—";
    }

    return `NDVI: ${formatNumber(
        ndvi,
        3
    )}${
        health
            ? ` · ${health}`
            : ""
    }`;
}


export default Home;