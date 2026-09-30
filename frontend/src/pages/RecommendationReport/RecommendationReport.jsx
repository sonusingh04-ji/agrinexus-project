import {
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    useLocation,
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
    getLatestCropRotation,
    getLatestSoilSample,
    getSoilIntelligence,
} from "../../api/cropSoilApi";

import {
    getLatestTelemetry,
    getIrrigationConfig,
    getIrrigationEvents,
} from "../../api/irrigationApi";

import {
    getLatestFertilizerRecommendation,
    getNextFertilizerSchedule,
} from "../../api/fertilizerApi";

import {
    getWeatherRecords,
    getWeatherRadar,
    getWeatherRisks,
} from "../../api/weatherRiskApi";

import {
    getFarmCropHistory,
} from "../../api/cropHistoryApi";

import BottomNavBar from "../../components/BottomNavBar";
import RecommendationCard from "../../components/RecommendationCard";

import "./RecommendationReport.css";


/*
 * ============================================================
 * HELPERS
 * ============================================================
 */


/*
 * Validate UUID before making a farm-specific request.
 */
const isValidUuid = (
    value
) => {

    if (
        !value ||
        typeof value !== "string"
    ) {

        return false;
    }


    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
        value
    );
};


/*
 * Convert numeric values safely.
 *
 * Missing values remain null.
 * No value is invented.
 */
const safeNumber = (
    value
) => {

    if (
        typeof value === "number" &&
        Number.isFinite(
            value
        )
    ) {

        return value;
    }


    if (
        typeof value === "string" &&
        value.trim() !== ""
    ) {

        const parsed =
            Number(
                value
            );


        if (
            Number.isFinite(
                parsed
            )
        ) {

            return parsed;
        }
    }


    return null;
};


/*
 * Display numeric values.
 */
const formatNumber = (
    value,
    digits = 1
) => {

    const number =
        safeNumber(
            value
        );


    if (
        number === null
    ) {

        return "—";
    }


    return number.toFixed(
        digits
    );
};


/*
 * Format dates according to selected language.
 */
const formatDate = (
    value,
    language = "en"
) => {

    if (
        !value
    ) {

        return "—";
    }


    const date =
        new Date(
            value
        );


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
        language === "hi"
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


/*
 * Get newest record from an array.
 */
const latestByDate = (
    records,
    dateField
) => {

    if (
        !Array.isArray(
            records
        ) ||
        records.length === 0
    ) {

        return null;
    }


    return [
        ...records,
    ].sort(
        (
            a,
            b
        ) => {

            const first =
                new Date(
                    a?.[
                        dateField
                        ] || 0
                ).getTime();


            const second =
                new Date(
                    b?.[
                        dateField
                        ] || 0
                ).getTime();


            return (
                second -
                first
            );
        }
    )[0];
};


/*
 * Display text safely.
 */
const textValue = (
    value
) => {

    if (
        value === null ||
        value === undefined ||
        value === ""
    ) {

        return "—";
    }


    return String(
        value
    );
};


/*
 * Normalize backend list responses.
 *
 * Supports:
 * []
 * { content: [] }
 * { data: [] }
 *
 * Does not create records.
 */
const normalizeList = (
    response
) => {

    if (
        Array.isArray(
            response
        )
    ) {

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
};


/*
 * Normalize backend object responses.
 */
const normalizeObject = (
    response
) => {

    if (
        !response ||
        typeof response !== "object"
    ) {

        return null;
    }


    if (
        response.data &&
        typeof response.data === "object" &&
        !Array.isArray(
            response.data
        )
    ) {

        return response.data;
    }


    return response;
};


/*
 * Explicitly reject test/demo records.
 *
 * Calculated / estimated real backend data is allowed.
 */
const isTestOrDemo = (
    record
) => {

    if (
        !record
    ) {

        return false;
    }


    const dataStatus =
        String(
            record.dataStatus ||
            ""
        ).toUpperCase();


    const source =
        String(
            record.source ||
            ""
        ).toUpperCase();


    return (
        dataStatus ===
        "TEST" ||
        source.includes(
            "TEST"
        ) ||
        source.includes(
            "DEMO"
        )
    );
};


/*
 * ============================================================
 * MAIN COMPONENT
 * ============================================================
 */

function RecommendationReport() {

    const {
        language,
    } = useLanguage();


    const navigate =
        useNavigate();


    const location =
        useLocation();


    const {
        fields,
    } = useFields();


    const isHindi =
        language === "hi";


    /*
     * ========================================================
     * DETERMINE ACTIVE REAL FARM
     * ========================================================
     */

    const navigationFarmId =
        location.state?.farmId ||
        location.state?.fieldId ||
        location.state?.savedReport?.farmId ||
        null;


    const activeField =
        useMemo(
            () => {

                if (
                    !Array.isArray(
                        fields
                    ) ||
                    fields.length === 0
                ) {

                    return null;
                }


                if (
                    navigationFarmId
                ) {

                    const matchedField =
                        fields.find(
                            (
                                field
                            ) =>
                                String(
                                    field.id
                                ) ===
                                String(
                                    navigationFarmId
                                )
                        );


                    if (
                        matchedField
                    ) {

                        return matchedField;
                    }
                }


                /*
                 * Existing application behaviour:
                 * use first backend field only when no explicit
                 * farm was passed.
                 *
                 * This is still a real backend field.
                 */

                return fields[0] || null;

            },
            [
                fields,
                navigationFarmId,
            ]
        );


    const farmId =
        activeField?.id ||
        navigationFarmId ||
        null;


    /*
     * ========================================================
     * STATE
     * ========================================================
     */

    const [
        loading,
        setLoading,
    ] = useState(
        true
    );


    const [
        error,
        setError,
    ] = useState(
        ""
    );


    const [
        report,
        setReport,
    ] = useState({

        cropRotation:
            null,

        soil:
            null,

        latestSoilSample:
            null,

        irrigationConfig:
            null,

        irrigationTelemetry:
            null,

        irrigationEvents:
            [],

        weather:
            null,

        weatherRisks:
            [],

        weatherRadar:
            null,

        fertilizerRecommendation:
            null,

        fertilizerSchedule:
            null,

        cropHistory:
            [],
    });


    /*
     * ========================================================
     * OPTIONAL API LOADER
     * ========================================================
     *
     * One failed optional section must not destroy
     * the entire report.
     */

    const loadOptional =
        async (
            requestFunction
        ) => {

            try {

                return await requestFunction();

            } catch (
                requestError
                ) {

                console.warn(
                    "AgriNexus report request failed:",
                    requestError
                );


                return null;
            }
        };


    /*
     * ========================================================
     * LOAD REPORT
     * ========================================================
     */

    useEffect(() => {

        let cancelled =
            false;


        async function loadReport() {

            /*
             * A valid real farm is mandatory.
             */

            if (
                !isValidUuid(
                    String(
                        farmId
                    )
                )
            ) {

                if (
                    !cancelled
                ) {

                    setLoading(
                        false
                    );


                    setError(
                        isHindi
                            ? "मान्य वास्तविक खेत ID उपलब्ध नहीं है।"
                            : "A valid real farm ID is not available."
                    );
                }


                return;
            }


            setLoading(
                true
            );


            setError("");


            try {

                /*
                 * =================================================
                 * ALL DATA COMES FROM BACKEND
                 * =================================================
                 */

                const [
                    cropRotationResponse,
                    soilResponse,
                    latestSoilSampleResponse,
                    irrigationConfigResponse,
                    irrigationTelemetryResponse,
                    irrigationEventsResponse,
                    weatherRecordsResponse,
                    weatherRisksResponse,
                    weatherRadarResponse,
                    fertilizerRecommendationResponse,
                    fertilizerScheduleResponse,
                    cropHistoryResponse,
                ] = await Promise.all([

                    loadOptional(
                        () =>
                            getLatestCropRotation(
                                farmId
                            )
                    ),

                    loadOptional(
                        () =>
                            getSoilIntelligence(
                                farmId
                            )
                    ),

                    loadOptional(
                        () =>
                            getLatestSoilSample(
                                farmId
                            )
                    ),

                    loadOptional(
                        () =>
                            getIrrigationConfig(
                                farmId
                            )
                    ),

                    loadOptional(
                        () =>
                            getLatestTelemetry(
                                farmId
                            )
                    ),

                    loadOptional(
                        () =>
                            getIrrigationEvents(
                                farmId
                            )
                    ),

                    loadOptional(
                        () =>
                            getWeatherRecords(
                                farmId
                            )
                    ),

                    loadOptional(
                        () =>
                            getWeatherRisks(
                                farmId
                            )
                    ),

                    loadOptional(
                        () =>
                            getWeatherRadar(
                                farmId
                            )
                    ),

                    loadOptional(
                        () =>
                            getLatestFertilizerRecommendation(
                                farmId
                            )
                    ),

                    loadOptional(
                        () =>
                            getNextFertilizerSchedule(
                                farmId
                            )
                    ),

                    loadOptional(
                        () =>
                            getFarmCropHistory(
                                farmId
                            )
                    ),

                ]);


                if (
                    cancelled
                ) {

                    return;
                }


                /*
                 * Normalize responses.
                 */

                const cropRotation =
                    normalizeObject(
                        cropRotationResponse
                    );


                const soil =
                    normalizeObject(
                        soilResponse
                    );


                const latestSoilSample =
                    normalizeObject(
                        latestSoilSampleResponse
                    );


                const irrigationConfig =
                    normalizeObject(
                        irrigationConfigResponse
                    );


                const irrigationTelemetry =
                    normalizeObject(
                        irrigationTelemetryResponse
                    );


                const irrigationEvents =
                    normalizeList(
                        irrigationEventsResponse
                    );


                const weatherRecords =
                    normalizeList(
                        weatherRecordsResponse
                    );


                const weatherRisks =
                    normalizeList(
                        weatherRisksResponse
                    );


                const weatherRadar =
                    normalizeObject(
                        weatherRadarResponse
                    );


                const fertilizerRecommendation =
                    normalizeObject(
                        fertilizerRecommendationResponse
                    );


                const fertilizerSchedule =
                    normalizeObject(
                        fertilizerScheduleResponse
                    );


                const cropHistory =
                    normalizeList(
                        cropHistoryResponse
                    );


                /*
                 * Latest weather record.
                 */
                const latestWeather =
                    latestByDate(
                        weatherRecords,
                        "recordedAt"
                    );


                /*
                 * Explicitly remove test/demo records.
                 */

                const safeCropRotation =
                    isTestOrDemo(
                        cropRotation
                    )
                        ? null
                        : cropRotation;


                const safeSoil =
                    isTestOrDemo(
                        soil
                    )
                        ? null
                        : soil;


                const safeLatestSoilSample =
                    isTestOrDemo(
                        latestSoilSample
                    )
                        ? null
                        : latestSoilSample;


                const safeWeatherRadar =
                    isTestOrDemo(
                        weatherRadar
                    )
                        ? null
                        : weatherRadar;


                const safeFertilizerRecommendation =
                    isTestOrDemo(
                        fertilizerRecommendation
                    )
                        ? null
                        : fertilizerRecommendation;


                const safeFertilizerSchedule =
                    isTestOrDemo(
                        fertilizerSchedule
                    )
                        ? null
                        : fertilizerSchedule;


                const safeWeather =
                    isTestOrDemo(
                        latestWeather
                    )
                        ? null
                        : latestWeather;


                const safeWeatherRisks =
                    weatherRisks.filter(
                        (
                            risk
                        ) =>
                            !isTestOrDemo(
                                risk
                            )
                    );


                const safeCropHistory =
                    cropHistory.filter(
                        (
                            record
                        ) =>
                            !isTestOrDemo(
                                record
                            )
                    );


                setReport({

                    cropRotation:
                    safeCropRotation,

                    soil:
                    safeSoil,

                    latestSoilSample:
                    safeLatestSoilSample,

                    irrigationConfig,

                    irrigationTelemetry,

                    irrigationEvents,

                    weather:
                    safeWeather,

                    weatherRisks:
                    safeWeatherRisks,

                    weatherRadar:
                    safeWeatherRadar,

                    fertilizerRecommendation:
                    safeFertilizerRecommendation,

                    fertilizerSchedule:
                    safeFertilizerSchedule,

                    cropHistory:
                    safeCropHistory,
                });


            } catch (
                requestError
                ) {

                console.error(
                    "Failed to load AgriNexus report:",
                    requestError
                );


                if (
                    !cancelled
                ) {

                    setError(
                        requestError?.message ||
                        (
                            isHindi
                                ? "रिपोर्ट लोड नहीं हो सकी।"
                                : "Unable to load the report."
                        )
                    );
                }

            } finally {

                if (
                    !cancelled
                ) {

                    setLoading(
                        false
                    );
                }
            }
        }


        void loadReport();


        return () => {

            cancelled =
                true;
        };

    }, [
        farmId,
        isHindi,
    ]);


    /*
     * ========================================================
     * REAL CURRENT CROP
     * ========================================================
     *
     * IMPORTANT:
     *
     * Do NOT use:
     * - next crop recommendation
     * - previous crop
     * - first crop-history record
     * - soil crop name
     *
     * A current crop must come from an active crop record.
     */

    const currentCropRecord =
        useMemo(
            () => {

                if (
                    !Array.isArray(
                        report.cropHistory
                    )
                ) {

                    return null;
                }


                const activeRecords =
                    report.cropHistory
                        .filter(
                            (
                                record
                            ) => {

                                if (
                                    !record
                                ) {

                                    return false;
                                }


                                /*
                                 * A harvested record is historical,
                                 * not current.
                                 */

                                if (
                                    record.harvestDate
                                ) {

                                    return false;
                                }


                                /*
                                 * If backend supplies farmId,
                                 * verify it.
                                 */

                                if (
                                    record.farmId
                                ) {

                                    if (
                                        String(
                                            record.farmId
                                        ) !==
                                        String(
                                            farmId
                                        )
                                    ) {

                                        return false;
                                    }
                                }


                                /*
                                 * A crop name is mandatory.
                                 */

                                if (
                                    !record.cropName ||
                                    String(
                                        record.cropName
                                    ).trim() === ""
                                ) {

                                    return false;
                                }


                                return true;
                            }
                        )
                        .sort(
                            (
                                a,
                                b
                            ) => {

                                const first =
                                    new Date(
                                        a?.sowingDate ||
                                        a?.createdAt ||
                                        0
                                    ).getTime();


                                const second =
                                    new Date(
                                        b?.sowingDate ||
                                        b?.createdAt ||
                                        0
                                    ).getTime();


                                return (
                                    second -
                                    first
                                );
                            }
                        );


                return (
                    activeRecords[0] ||
                    null
                );

            },
            [
                report.cropHistory,
                farmId,
            ]
        );


    /*
     * Only a confirmed active crop is displayed.
     */

    const currentCrop =
        currentCropRecord?.cropName ||
        "—";


    const farmName =
        activeField?.name ||
        "—";


    /*
     * ========================================================
     * RECOMMENDED NEXT CROP
     * ========================================================
     *
     * This is NOT current crop.
     */

    const recommendedCrop =
        report.cropRotation
            ?.recommendedCropName ||
        "—";


    const recommendationScore =
        report.cropRotation
            ?.recommendationScore;


    /*
     * ========================================================
     * LATEST WEATHER RISK
     * ========================================================
     */

    const latestRisk =
        report.weatherRadar?.mainRisk ||
        latestByDate(
            report.weatherRisks,
            "detectedAt"
        );


    /*
     * ========================================================
     * SMART RECOMMENDATION SECTIONS
     * ========================================================
     */

    const recommendationSections =
        useMemo(
            () => {

                const sections = [];


                /*
                 * ------------------------------------------------
                 * CROP ROTATION
                 * ------------------------------------------------
                 */

                if (
                    report.cropRotation
                ) {

                    const rotation =
                        report.cropRotation;


                    const score =
                        safeNumber(
                            rotation.recommendationScore
                        );


                    sections.push({

                        key:
                            "crop-rotation",

                        icon:
                            "🌱",

                        titleEn:
                            "Next Crop Recommendation",

                        titleHi:
                            "अगली फसल की सिफारिश",

                        statusColor:
                            score !== null &&
                            score >= 70
                                ? "good"
                                : "warning",

                        summary:
                            isHindi
                                ? `अगली सुझाई गई फसल: ${textValue(
                                    rotation.recommendedCropName
                                )}`
                                : `Recommended next crop: ${textValue(
                                    rotation.recommendedCropName
                                )}`,

                        whyText:
                            [
                                rotation.reason,
                                rotation.contributingFactors,
                                rotation.uncertaintyNote,
                            ]
                                .filter(
                                    Boolean
                                )
                                .join(
                                    " "
                                ),
                    });
                }


                /*
                 * ------------------------------------------------
                 * WEATHER
                 * ------------------------------------------------
                 */

                if (
                    report.weatherRadar ||
                    latestRisk ||
                    report.weather
                ) {

                    const radar =
                        report.weatherRadar;


                    const riskName =
                        radar?.riskType ||
                        latestRisk?.riskType ||
                        radar?.mainRisk ||
                        "WEATHER";


                    const riskScore =
                        radar?.riskScore ??
                        latestRisk?.riskScore ??
                        radar?.combinedRiskScore;


                    const riskLevel =
                        radar?.riskLevel ||
                        latestRisk?.riskLevel ||
                        "INFO";


                    const normalizedLevel =
                        String(
                            riskLevel
                        ).toUpperCase();


                    sections.push({

                        key:
                            "weather",

                        icon:
                            "⚠️",

                        titleEn:
                            "Weather Risk",

                        titleHi:
                            "मौसम जोखिम",

                        statusColor:
                            normalizedLevel ===
                            "CRITICAL"
                                ? "danger"
                                : normalizedLevel ===
                                "HIGH"
                                    ? "warning"
                                    : "good",

                        summary:
                            isHindi

                                ? `मौसम जोखिम: ${textValue(
                                    riskName
                                )}${
                                    riskScore !==
                                    null &&
                                    riskScore !==
                                    undefined
                                        ? ` (${formatNumber(
                                            riskScore,
                                            0
                                        )})`
                                        : ""
                                }`

                                : `Weather risk: ${textValue(
                                    riskName
                                )}${
                                    riskScore !==
                                    null &&
                                    riskScore !==
                                    undefined
                                        ? ` (${formatNumber(
                                            riskScore,
                                            0
                                        )})`
                                        : ""
                                }`,

                        whyText:
                            [
                                radar?.summary,
                                latestRisk?.description,
                                latestRisk?.recommendation,
                                radar?.uncertaintyNote,
                            ]
                                .filter(
                                    Boolean
                                )
                                .join(
                                    " "
                                ),
                    });
                }


                /*
                 * ------------------------------------------------
                 * IRRIGATION
                 * ------------------------------------------------
                 */

                if (
                    report.irrigationTelemetry ||
                    report.irrigationEvents.length >
                    0 ||
                    report.irrigationConfig
                ) {

                    const telemetry =
                        report.irrigationTelemetry;


                    const latestEvent =
                        latestByDate(
                            report.irrigationEvents,
                            "startTime"
                        );


                    const moisture =
                        telemetry?.vwc30cm ??
                        telemetry?.vwc15cm ??
                        telemetry?.vwc60cm ??
                        latestEvent?.moistureAfter ??
                        null;


                    const pumpState =
                        telemetry?.pumpState ??
                        (
                            latestEvent?.pumpWasOn ===
                            true
                                ? 1
                                : latestEvent?.pumpWasOn ===
                                false
                                    ? 0
                                    : null
                        );


                    sections.push({

                        key:
                            "irrigation",

                        icon:
                            "💧",

                        titleEn:
                            "Smart Irrigation",

                        titleHi:
                            "स्मार्ट सिंचाई",

                        statusColor:
                            moisture !==
                            null &&
                            moisture !==
                            undefined
                                ? "good"
                                : "warning",

                        summary:

                            isHindi

                                ? `मिट्टी की नमी: ${
                                    moisture !==
                                    null &&
                                    moisture !==
                                    undefined
                                        ? `${formatNumber(
                                            moisture
                                        )}%`
                                        : "डेटा उपलब्ध नहीं"
                                }`

                                : `Soil moisture: ${
                                    moisture !==
                                    null &&
                                    moisture !==
                                    undefined
                                        ? `${formatNumber(
                                            moisture
                                        )}%`
                                        : "Data unavailable"
                                }`,

                        whyText:

                            isHindi

                                ? [
                                    telemetry
                                        ? `पंप स्थिति: ${
                                            pumpState ===
                                            1
                                                ? "चालू"
                                                : pumpState ===
                                                0
                                                    ? "बंद"
                                                    : "उपलब्ध नहीं"
                                        }`
                                        : null,

                                    telemetry?.flowRateLpm !=
                                    null
                                        ? `फ्लो: ${formatNumber(
                                            telemetry.flowRateLpm
                                        )} L/min`
                                        : null,

                                    telemetry?.pumpRuntimeMinutes !=
                                    null
                                        ? `रनटाइम: ${formatNumber(
                                            telemetry.pumpRuntimeMinutes
                                        )} मिनट`
                                        : null,

                                    report.irrigationConfig
                                        ?.notes,

                                ]
                                    .filter(
                                        Boolean
                                    )
                                    .join(
                                        " "
                                    )

                                : [

                                    telemetry
                                        ? `Pump: ${
                                            pumpState ===
                                            1
                                                ? "ON"
                                                : pumpState ===
                                                0
                                                    ? "OFF"
                                                    : "Unavailable"
                                        }`
                                        : null,

                                    telemetry?.flowRateLpm !=
                                    null
                                        ? `Flow: ${formatNumber(
                                            telemetry.flowRateLpm
                                        )} L/min`
                                        : null,

                                    telemetry?.pumpRuntimeMinutes !=
                                    null
                                        ? `Runtime: ${formatNumber(
                                            telemetry.pumpRuntimeMinutes
                                        )} minutes`
                                        : null,

                                    report.irrigationConfig
                                        ?.notes,

                                ]
                                    .filter(
                                        Boolean
                                    )
                                    .join(
                                        " "
                                    ),
                    });
                }


                /*
                 * ------------------------------------------------
                 * FERTILIZER
                 * ------------------------------------------------
                 *
                 * Recommendation is shown only when it is
                 * associated with a real current crop.
                 */

                const fertilizerRecommendation =
                    report.fertilizerRecommendation;


                let fertilizerIsCurrentCropSafe =
                    false;


                if (
                    fertilizerRecommendation &&
                    currentCropRecord
                ) {

                    const recommendationCropId =
                        fertilizerRecommendation.cropId ??
                        null;


                    const currentCropId =
                        currentCropRecord.cropId ??
                        currentCropRecord.id ??
                        null;


                    const recommendationCropName =
                        String(
                            fertilizerRecommendation.cropName ||
                            ""
                        )
                            .trim()
                            .toLowerCase();


                    const currentCropName =
                        String(
                            currentCropRecord.cropName ||
                            ""
                        )
                            .trim()
                            .toLowerCase();


                    /*
                     * When both IDs exist, they must match.
                     */

                    if (
                        recommendationCropId &&
                        currentCropId
                    ) {

                        fertilizerIsCurrentCropSafe =
                            String(
                                recommendationCropId
                            ) ===
                            String(
                                currentCropId
                            );

                    } else if (
                        recommendationCropName &&
                        currentCropName
                    ) {

                        fertilizerIsCurrentCropSafe =
                            recommendationCropName ===
                            currentCropName;

                    } else {

                        /*
                         * Without enough crop identity information,
                         * do not present recommendation as current-crop
                         * fertilizer guidance.
                         */

                        fertilizerIsCurrentCropSafe =
                            false;
                    }
                }


                if (
                    fertilizerIsCurrentCropSafe
                ) {

                    const schedule =
                        report.fertilizerSchedule;


                    sections.push({

                        key:
                            "fertilizer",

                        icon:
                            "🌿",

                        titleEn:
                            "Fertilizer Recommendation",

                        titleHi:
                            "उर्वरक सिफारिश",

                        statusColor:
                            fertilizerRecommendation?.status ===
                            "REVIEW_REQUIRED"
                                ? "warning"
                                : "good",

                        summary:

                            isHindi

                                ? `उर्वरक स्थिति: ${textValue(
                                    fertilizerRecommendation?.status
                                )}`

                                : `Fertilizer status: ${textValue(
                                    fertilizerRecommendation?.status
                                )}`,

                        whyText:
                            [
                                fertilizerRecommendation?.recommendation,
                                fertilizerRecommendation?.reason,
                                schedule?.recommendation,
                                schedule?.reason,
                                fertilizerRecommendation?.uncertaintyNote,
                            ]
                                .filter(
                                    Boolean
                                )
                                .join(
                                    " "
                                ),
                    });
                }


                /*
                 * ------------------------------------------------
                 * SOIL
                 * ------------------------------------------------
                 */

                if (
                    report.soil
                ) {

                    sections.push({

                        key:
                            "soil",

                        icon:
                            "🧪",

                        titleEn:
                            "Soil Intelligence",

                        titleHi:
                            "मिट्टी की जानकारी",

                        statusColor:
                            String(
                                report.soil.dataStatus ||
                                ""
                            ).toUpperCase() ===
                            "UNKNOWN"
                                ? "warning"
                                : "good",

                        summary:

                            isHindi

                                ? `मिट्टी: ${textValue(
                                    report.soil.soilType
                                )}, pH ${formatNumber(
                                    report.soil.pH,
                                    1
                                )}`

                                : `Soil: ${textValue(
                                    report.soil.soilType
                                )}, pH ${formatNumber(
                                    report.soil.pH,
                                    1
                                )}`,

                        whyText:
                            [
                                report.soil.explanation,
                                report.soil.uncertaintyNote,
                                report.soil.cropPhInterpretation,
                            ]
                                .filter(
                                    Boolean
                                )
                                .join(
                                    " "
                                ),
                    });
                }


                return sections;

            },
            [
                report,
                isHindi,
                latestRisk,
                currentCropRecord,
            ]
        );


    /*
     * ========================================================
     * LOADING
     * ========================================================
     */

    if (
        loading
    ) {

        return (

            <div className="report-container">

                <h1 className="report-title">

                    {
                        isHindi
                            ? "रिपोर्ट तैयार हो रही है..."
                            : "Preparing Your Report..."
                    }

                </h1>


                <p className="report-subtitle">

                    {
                        isHindi
                            ? "आपके खेत का वास्तविक डेटा लोड किया जा रहा है"
                            : "Loading real data from your farm..."
                    }

                </p>


                <div className="report-loading-card">

                    <div className="report-loading-icon">
                        🌾
                    </div>


                    <div>

                        {
                            isHindi
                                ? "कृपया प्रतीक्षा करें..."
                                : "Please wait..."
                        }

                    </div>

                </div>


                <BottomNavBar />

            </div>
        );
    }


    /*
     * ========================================================
     * ERROR
     * ========================================================
     */

    const hasAnyReportData =
        Boolean(
            report.soil ||
            report.weather ||
            report.cropRotation ||
            report.latestSoilSample ||
            report.irrigationTelemetry ||
            report.irrigationConfig ||
            report.fertilizerRecommendation ||
            report.fertilizerSchedule ||
            report.cropHistory.length > 0 ||
            report.weatherRisks.length > 0
        );


    if (
        error &&
        !hasAnyReportData
    ) {

        return (

            <div className="report-container">

                <h1 className="report-title">

                    {
                        isHindi
                            ? "रिपोर्ट उपलब्ध नहीं"
                            : "Report Unavailable"
                    }

                </h1>


                <div className="report-error-card">

                    <strong>
                        ⚠️
                    </strong>


                    <span>
                        {error}
                    </span>

                </div>


                <button
                    type="button"
                    className="report-home-button"
                    onClick={() =>
                        navigate(
                            "/home"
                        )
                    }
                >

                    {
                        isHindi
                            ? "होम पर जाएं"
                            : "Go to Home"
                    }

                </button>


                <BottomNavBar />

            </div>
        );
    }


    /*
     * ========================================================
     * RENDER
     * ========================================================
     */

    return (

        <div className="report-container">


            {/* =================================================
                HEADER
            ================================================= */}

            <header className="report-header">

                <div className="report-header-badge">
                    🌾
                </div>


                <h1 className="report-title">

                    {
                        isHindi
                            ? "आपकी रिपोर्ट तैयार है"
                            : "Your Report is Ready"
                    }

                </h1>


                <p className="report-subtitle">

                    {
                        farmName
                    }

                </p>


                <p className="report-subtitle">

                    {
                        isHindi
                            ? "वास्तविक खेत डेटा पर आधारित"
                            : "Based on real farm data"
                    }

                </p>

            </header>


            {/* =================================================
                CURRENT CROP
            ================================================= */}

            <div className="report-data-card">

                <div className="report-data-card-icon">
                    🌾
                </div>


                <div className="report-data-card-content">

                    <div className="report-data-card-label">

                        {
                            isHindi
                                ? "वर्तमान फसल"
                                : "Current Crop"
                        }

                    </div>


                    <div className="report-data-card-value">

                        {
                            currentCrop
                        }

                    </div>


                    {
                        currentCropRecord?.cropStage && (

                            <div className="report-data-card-sub">

                                {
                                    isHindi
                                        ? `स्टेज: ${currentCropRecord.cropStage}`
                                        : `Stage: ${currentCropRecord.cropStage}`
                                }

                            </div>

                        )
                    }

                </div>

            </div>


            {/* =================================================
                NEXT CROP
            ================================================= */}

            {
                report.cropRotation && (

                    <div className="report-data-card">

                        <div className="report-data-card-icon">
                            🌱
                        </div>


                        <div className="report-data-card-content">

                            <div className="report-data-card-label">

                                {
                                    isHindi
                                        ? "अगली फसल"
                                        : "Next Crop"
                                }

                            </div>


                            <div className="report-data-card-value">

                                {
                                    recommendedCrop
                                }

                            </div>


                            <div className="report-data-card-sub">

                                {
                                    isHindi

                                        ? `स्कोर: ${
                                            recommendationScore !=
                                            null
                                                ? formatNumber(
                                                    recommendationScore,
                                                    1
                                                )
                                                : "—"
                                        }`

                                        : `Score: ${
                                            recommendationScore !=
                                            null
                                                ? formatNumber(
                                                    recommendationScore,
                                                    1
                                                )
                                                : "—"
                                        }`

                                }

                            </div>

                        </div>

                    </div>
                )
            }


            {/* =================================================
                SMART RECOMMENDATIONS
            ================================================= */}

            {
                recommendationSections.length >
                0 && (

                    <section className="report-recommendation-section">

                        <div className="report-section-heading">

                            <span>
                                ✨
                            </span>


                            <h2>

                                {
                                    isHindi
                                        ? "स्मार्ट सिफारिशें"
                                        : "Smart Recommendations"
                                }

                            </h2>

                        </div>


                        {
                            recommendationSections.map(
                                (
                                    section
                                ) => (

                                    <RecommendationCard
                                        key={
                                            section.key
                                        }

                                        icon={
                                            section.icon
                                        }

                                        title={

                                            isHindi
                                                ? section.titleHi
                                                : section.titleEn
                                        }

                                        statusColor={
                                            section.statusColor
                                        }

                                        summary={
                                            section.summary ||
                                            (
                                                isHindi
                                                    ? "डेटा उपलब्ध नहीं है"
                                                    : "Data unavailable"
                                            )
                                        }

                                        whyText={
                                            section.whyText ||
                                            (
                                                isHindi
                                                    ? "इस सेक्शन के लिए पर्याप्त डेटा उपलब्ध नहीं है।"
                                                    : "There is not enough data available for this section."
                                            )
                                        }

                                        language={
                                            language
                                        }
                                    />

                                )
                            )
                        }

                    </section>
                )
            }


            {/* =================================================
                WEATHER
            ================================================= */}

            {
                report.weather && (

                    <section className="report-detail-section">

                        <h2>

                            🌤️{" "}

                            {
                                isHindi
                                    ? "मौसम"
                                    : "Weather"
                            }

                        </h2>


                        <div className="report-detail-grid">

                            <div className="report-detail-item">

                                <span>

                                    🌡️{" "}

                                    {
                                        isHindi
                                            ? "तापमान"
                                            : "Temperature"
                                    }

                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.weather.temperature,
                                            1
                                        )
                                    }°C

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>

                                    💧{" "}

                                    {
                                        isHindi
                                            ? "नमी"
                                            : "Humidity"
                                    }

                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.weather.humidity,
                                            0
                                        )
                                    }%

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>

                                    🌧️{" "}

                                    {
                                        isHindi
                                            ? "बारिश"
                                            : "Rainfall"
                                    }

                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.weather.rainfall,
                                            1
                                        )
                                    } mm

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>

                                    💨{" "}

                                    {
                                        isHindi
                                            ? "हवा"
                                            : "Wind"
                                    }

                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.weather.windSpeed,
                                            1
                                        )
                                    } km/h

                                </strong>

                            </div>

                        </div>

                    </section>
                )
            }


            {/* =================================================
                SOIL
            ================================================= */}

            {
                report.soil && (

                    <section className="report-detail-section">

                        <h2>

                            🧪{" "}

                            {
                                isHindi
                                    ? "मिट्टी"
                                    : "Soil"
                            }

                        </h2>


                        <div className="report-detail-grid">

                            <div className="report-detail-item">

                                <span>
                                    pH
                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.soil.pH,
                                            1
                                        )
                                    }

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>

                                    {
                                        isHindi
                                            ? "जैविक कार्बन"
                                            : "Organic Carbon"
                                    }

                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.soil.organicCarbon,
                                            2
                                        )
                                    }%

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>
                                    N
                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.soil.nitrogen,
                                            0
                                        )
                                    }

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>
                                    P
                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.soil.phosphorus,
                                            0
                                        )
                                    }

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>
                                    K
                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.soil.potassium,
                                            0
                                        )
                                    }

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>

                                    {
                                        isHindi
                                            ? "डेटा स्थिति"
                                            : "Data Status"
                                    }

                                </span>


                                <strong>

                                    {
                                        textValue(
                                            report.soil.dataStatus
                                        )
                                    }

                                </strong>

                            </div>

                        </div>

                    </section>
                )
            }


            {/* =================================================
                LATEST SOIL SAMPLE
            ================================================= */}

            {
                report.latestSoilSample && (

                    <section className="report-detail-section">

                        <h2>

                            📋{" "}

                            {
                                isHindi
                                    ? "नवीनतम मिट्टी परीक्षण"
                                    : "Latest Soil Sample"
                            }

                        </h2>


                        <div className="report-detail-grid">

                            <div className="report-detail-item">

                                <span>

                                    {
                                        isHindi
                                            ? "नमूना दिनांक"
                                            : "Sample Date"
                                    }

                                </span>


                                <strong>

                                    {
                                        formatDate(
                                            report.latestSoilSample.sampledAt ||
                                            report.latestSoilSample.createdAt,
                                            language
                                        )
                                    }

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>

                                    {
                                        isHindi
                                            ? "मिट्टी प्रकार"
                                            : "Soil Type"
                                    }

                                </span>


                                <strong>

                                    {
                                        textValue(
                                            report.latestSoilSample.soilType ||
                                            report.soil?.soilType
                                        )
                                    }

                                </strong>

                            </div>

                        </div>

                    </section>
                )
            }


            {/* =================================================
                IRRIGATION
            ================================================= */}

            {
                (
                    report.irrigationTelemetry ||
                    report.irrigationConfig
                ) && (

                    <section className="report-detail-section">

                        <h2>

                            💧{" "}

                            {
                                isHindi
                                    ? "स्मार्ट सिंचाई"
                                    : "Smart Irrigation"
                            }

                        </h2>


                        <div className="report-detail-grid">

                            <div className="report-detail-item">

                                <span>

                                    💧{" "}

                                    {
                                        isHindi
                                            ? "30 सेमी नमी"
                                            : "30 cm Moisture"
                                    }

                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.irrigationTelemetry
                                                ?.vwc30cm,
                                            1
                                        )
                                    }%

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>

                                    🚰{" "}

                                    {
                                        isHindi
                                            ? "फ्लो"
                                            : "Flow"
                                    }

                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.irrigationTelemetry
                                                ?.flowRateLpm,
                                            1
                                        )
                                    }{" "}

                                    L/min

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>

                                    ⚙️{" "}

                                    {
                                        isHindi
                                            ? "प्रेशर"
                                            : "Pressure"
                                    }

                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.irrigationTelemetry
                                                ?.linePressureBar,
                                            1
                                        )
                                    }{" "}

                                    bar

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>

                                    ⏱️{" "}

                                    {
                                        isHindi
                                            ? "रनटाइम"
                                            : "Runtime"
                                    }

                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.irrigationTelemetry
                                                ?.pumpRuntimeMinutes,
                                            0
                                        )
                                    }{" "}

                                    min

                                </strong>

                            </div>

                        </div>

                    </section>
                )
            }


            {/* =================================================
                FERTILIZER
            ================================================= */}

            {
                report.fertilizerRecommendation &&
                currentCropRecord && (

                    <section className="report-detail-section">

                        <h2>

                            🌿{" "}

                            {
                                isHindi
                                    ? "उर्वरक"
                                    : "Fertilizer"
                            }

                        </h2>


                        <div className="report-detail-grid">

                            <div className="report-detail-item">

                                <span>
                                    N
                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.fertilizerRecommendation
                                                .nitrogen,
                                            1
                                        )
                                    }

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>
                                    P
                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.fertilizerRecommendation
                                                .phosphorus,
                                            1
                                        )
                                    }

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>
                                    K
                                </span>


                                <strong>

                                    {
                                        formatNumber(
                                            report.fertilizerRecommendation
                                                .potassium,
                                            1
                                        )
                                    }

                                </strong>

                            </div>


                            <div className="report-detail-item">

                                <span>

                                    {
                                        isHindi
                                            ? "स्थिति"
                                            : "Status"
                                    }

                                </span>


                                <strong>

                                    {
                                        textValue(
                                            report.fertilizerRecommendation
                                                .status
                                        )
                                    }

                                </strong>

                            </div>

                        </div>

                    </section>
                )
            }


            {/* =================================================
                CROP HISTORY
            ================================================= */}

            {
                report.cropHistory.length >
                0 && (

                    <section className="report-detail-section">

                        <h2>

                            📚{" "}

                            {
                                isHindi
                                    ? "फसल इतिहास"
                                    : "Crop History"
                            }

                        </h2>


                        <div className="report-history-list">

                            {
                                report.cropHistory
                                    .slice(
                                        0,
                                        5
                                    )
                                    .map(
                                        (
                                            history,
                                            index
                                        ) => (

                                            <div
                                                className="report-history-item"
                                                key={
                                                    history.id ||
                                                    index
                                                }
                                            >

                                                <strong>

                                                    🌾{" "}

                                                    {
                                                        textValue(
                                                            history.cropName
                                                        )
                                                    }

                                                </strong>


                                                <span>

                                                    {
                                                        textValue(
                                                            history.season
                                                        )
                                                    }

                                                </span>


                                                <span>

                                                    {
                                                        formatDate(
                                                            history.sowingDate,
                                                            language
                                                        )
                                                    }

                                                    {" → "}

                                                    {
                                                        formatDate(
                                                            history.harvestDate,
                                                            language
                                                        )
                                                    }

                                                </span>

                                            </div>

                                        )
                                    )
                            }

                        </div>

                    </section>
                )
            }


            {/* =================================================
                DATA SOURCE
            ================================================= */}

            <section className="report-provenance-card">

                <strong>

                    {
                        isHindi
                            ? "डेटा स्रोत"
                            : "Data Source"
                    }

                </strong>


                <p>

                    {
                        isHindi
                            ? `यह रिपोर्ट आपके चुने हुए खेत के वास्तविक backend records पर आधारित है। Farm ID: ${farmId}`
                            : `This report is based on real backend records for your selected farm. Farm ID: ${farmId}`
                    }

                </p>


                {
                    report.soil?.source && (

                        <p>

                            {
                                isHindi
                                    ? `मिट्टी स्रोत: ${report.soil.source}`
                                    : `Soil source: ${report.soil.source}`
                            }

                        </p>
                    )
                }


                <p>

                    {
                        isHindi
                            ? "जहाँ वास्तविक डेटा उपलब्ध नहीं है, वहाँ मान के स्थान पर — दिखाया जाता है।"
                            : "Where real data is unavailable, the value is shown as — rather than being invented."
                    }

                </p>

            </section>


            {/* =================================================
                HOME BUTTON
            ================================================= */}

            <button
                type="button"
                className="report-home-button"
                onClick={() =>
                    navigate(
                        "/home"
                    )
                }
            >

                {
                    isHindi
                        ? "होम पर जाएं"
                        : "Go to Home"
                }

            </button>


            <BottomNavBar />

        </div>
    );
}


export default RecommendationReport;