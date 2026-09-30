
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
    useLanguage,
} from "../../context/LanguageContext";

import {
    useFields,
} from "../../context/FieldsContext";

import {
    apiRequest,
    getCurrentFarmId,
} from "../../api/apiClient";

import {
    getAllCrops,
} from "../../api/cropSoilApi";

import {
    getCurrentCropForFarm,
} from "../../api/cropHistoryApi";

import {
    generateFertilizerRecommendation,
    getLatestFertilizerRecommendation,
    createFertilizerSchedule,
    getNextFertilizerSchedule,
    getFarmFertilizerSchedules,
    getFertilizerReassessmentSchedules,
    updateFertilizerSchedule,
    deleteFertilizerSchedule,
} from "../../api/fertilizerApi";

import "./Fertilizer.css";


/* =========================================================
   HELPERS
   ========================================================= */

function safeNumber(value) {
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
    if (!safeNumber(value)) {
        return "—";
    }

    return Number(value).toFixed(decimals);
}

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

function formatDate(value) {
    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleDateString(
        "en-IN",
        {
            day: "numeric",
            month: "short",
            year: "numeric",
        }
    );
}

function formatDateInput(value) {
    if (!value) {
        return "";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value).slice(0, 10);
    }

    return date
        .toISOString()
        .slice(0, 10);
}

function normalizeStatus(value) {
    return String(value || "")
        .trim()
        .toUpperCase();
}

function getStatusTone(status) {
    const value = normalizeStatus(status);

    if (
        value === "READY" ||
        value === "COMPLETED" ||
        value === "PASS"
    ) {
        return "success";
    }

    if (
        value === "REVIEW_REQUIRED" ||
        value === "DUE" ||
        value === "REASSESS" ||
        value === "DELAYED" ||
        value === "HOLD"
    ) {
        return "warning";
    }

    if (
        value === "INSUFFICIENT_DATA" ||
        value === "NOT_AVAILABLE"
    ) {
        return "info";
    }

    if (value === "CANCELLED") {
        return "danger";
    }

    return "neutral";
}

function getStatusLabel(status) {
    if (!status) {
        return "—";
    }

    return String(status).replace(/_/g, " ");
}

function getCropName(record) {
    return (
        record?.cropName ??
        record?.crop?.name ??
        record?.name ??
        "—"
    );
}

function getCropId(record) {
    return (
        record?.cropId ??
        record?.crop?.id ??
        ""
    );
}

function hasRecommendationNutrientValues(record) {
    return [
        record?.nitrogen,
        record?.phosphorus,
        record?.potassium,
        record?.sulphur,
        record?.zinc,
    ].some((value) => safeNumber(value));
}

function isRecommendationUsable(record) {
    if (!record) {
        return false;
    }

    const status = normalizeStatus(record.status);

    if (
        status === "INSUFFICIENT_DATA" ||
        status === "NOT_AVAILABLE"
    ) {
        return false;
    }

    return hasRecommendationNutrientValues(record);
}


/*
 * Backend location can be an object like:
 *
 * {
 *   name: "Mathura",
 *   lat: 27.64,
 *   lng: 77.55
 * }
 *
 * Never render that object directly.
 */
function displayLocation(value) {
    if (
        value === null ||
        value === undefined ||
        value === ""
    ) {
        return "—";
    }

    if (typeof value === "string") {
        return value;
    }

    if (typeof value === "number") {
        return String(value);
    }

    if (typeof value === "object") {
        return (
            value.name ??
            value.location ??
            "—"
        );
    }

    return String(value);
}

function getWeatherDescription(weather) {
    if (!weather) {
        return "—";
    }

    if (typeof weather === "string") {
        return weather;
    }

    if (typeof weather === "object") {
        return (
            weather.weatherCondition ??
            weather.weatherDescription ??
            weather.condition ??
            "—"
        );
    }

    return "—";
}

function getSoilValue(
    soil,
    keys = []
) {
    for (const key of keys) {
        const value = soil?.[key];

        if (
            value !== null &&
            value !== undefined &&
            value !== ""
        ) {
            return value;
        }
    }

    return null;
}


/* =========================================================
   COMPONENT
   ========================================================= */

export default function Fertilizer() {

    const {
        language,
    } = useLanguage();

    const navigate =
        useNavigate();

    const {
        currentField,
        fields = [],
    } = useFields();


    /* =====================================================
       FARM
       ===================================================== */

    const farmId =
        currentField?.id ??
        getCurrentFarmId() ??
        "";


    /* =====================================================
       REAL DATA
       ===================================================== */

    const [
        farm,
        setFarm,
    ] = useState(null);

    const [
        crops,
        setCrops,
    ] = useState([]);

    const [
        currentCrop,
        setCurrentCrop,
    ] = useState(null);

    const [
        recommendation,
        setRecommendation,
    ] = useState(null);

    const [
        nextSchedule,
        setNextSchedule,
    ] = useState(null);

    const [
        schedules,
        setSchedules,
    ] = useState([]);

    const [
        reassessmentSchedules,
        setReassessmentSchedules,
    ] = useState([]);

    const [
        weather,
        setWeather,
    ] = useState(null);

    const [
        soil,
        setSoil,
    ] = useState(null);


    /* =====================================================
       UI STATE
       ===================================================== */

    const [
        loading,
        setLoading,
    ] = useState(true);

    const [
        savingRecommendation,
        setSavingRecommendation,
    ] = useState(false);

    const [
        savingSchedule,
        setSavingSchedule,
    ] = useState(false);

    const [
        deletingScheduleId,
        setDeletingScheduleId,
    ] = useState("");

    const [
        error,
        setError,
    ] = useState("");

    const [
        message,
        setMessage,
    ] = useState("");


    /* =====================================================
       FERTILIZER REFERENCE LIBRARY
       ===================================================== */

    const [
        fertilizerReferences,
        setFertilizerReferences,
    ] = useState([]);

    const [
        referenceLoading,
        setReferenceLoading,
    ] = useState(false);

    const [
        savingReference,
        setSavingReference,
    ] = useState(false);

    const [
        verifyingReferenceId,
        setVerifyingReferenceId,
    ] = useState("");

    const [
        lastSubmittedReference,
        setLastSubmittedReference,
    ] = useState(null);


    /* =====================================================
       RECOMMENDATION FORM
       ===================================================== */

    const [
        selectedCropId,
        setSelectedCropId,
    ] = useState("");

    const [
        cropStage,
        setCropStage,
    ] = useState("");

    const [
        fieldArea,
        setFieldArea,
    ] = useState("");

    const [
        fieldAreaUnit,
        setFieldAreaUnit,
    ] = useState("");

    const [
        farmerNote,
        setFarmerNote,
    ] = useState("");


    /* =====================================================
       FERTILIZER REFERENCE FORM
       ===================================================== */

    const [
        referenceRegion,
        setReferenceRegion,
    ] = useState("");

    const [
        referenceVarietyOrCondition,
        setReferenceVarietyOrCondition,
    ] = useState("");

    const [
        referenceApplicationStage,
        setReferenceApplicationStage,
    ] = useState("");

    const [
        referenceN,
        setReferenceN,
    ] = useState("");

    const [
        referenceP,
        setReferenceP,
    ] = useState("");

    const [
        referenceK,
        setReferenceK,
    ] = useState("");

    const [
        referenceS,
        setReferenceS,
    ] = useState("");

    const [
        referenceZn,
        setReferenceZn,
    ] = useState("");

    const [
        referenceUnit,
        setReferenceUnit,
    ] = useState("");

    const [
        referenceSourceOrganization,
        setReferenceSourceOrganization,
    ] = useState("");

    const [
        referenceSourceDocument,
        setReferenceSourceDocument,
    ] = useState("");

    const [
        referenceSourceUrl,
        setReferenceSourceUrl,
    ] = useState("");

    const [
        referenceNotes,
        setReferenceNotes,
    ] = useState("");


    /* =====================================================
       SCHEDULE FORM
       ===================================================== */

    const [
        scheduleStage,
        setScheduleStage,
    ] = useState("");

    const [
        plannedDate,
        setPlannedDate,
    ] = useState("");

    const [
        actionType,
        setActionType,
    ] = useState(
        "FERTILIZER_APPLICATION"
    );

    const [
        productName,
        setProductName,
    ] = useState("");

    const [
        scheduleN,
        setScheduleN,
    ] = useState("");

    const [
        scheduleP,
        setScheduleP,
    ] = useState("");

    const [
        scheduleK,
        setScheduleK,
    ] = useState("");

    const [
        scheduleS,
        setScheduleS,
    ] = useState("");

    const [
        scheduleZn,
        setScheduleZn,
    ] = useState("");

    const [
        reassessmentRequired,
        setReassessmentRequired,
    ] = useState(true);

    const [
        scheduleNotes,
        setScheduleNotes,
    ] = useState("");

    /* Edit */
    const [
        editingScheduleId,
        setEditingScheduleId,
    ] = useState("");

    const [
        editDate,
        setEditDate,
    ] = useState("");

    const [
        editStatus,
        setEditStatus,
    ] = useState("");


    /* =====================================================
       ALLOWED STAGES
       ===================================================== */

    const cropStageOptions = [
        "SOWING",
        "PRE_SOWING",
        "BASAL",
        "VEGETATIVE",
        "TILLERING",
        "FLOWERING",
        "GRAIN_FILLING",
        "HARVEST",
        "REASSESSMENT",
    ];


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
                    const response =
                        await apiRequest(
                            `/api/farms/${farmId}`
                        );

                    return response || null;

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
       LOAD CROPS
       ===================================================== */

    const loadCrops =
        useCallback(
            async () => {

                try {

                    const response =
                        await getAllCrops();

                    return normalizeList(
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

                } catch (cropError) {

                    console.warn(
                        "Crop load failed:",
                        cropError
                    );

                    return [];
                }
            },
            []
        );


    /* =====================================================
       LOAD CURRENT ACTIVE CROP
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
       LOAD FERTILIZER DATA
       ===================================================== */

    const loadFertilizerData =
        useCallback(
            async () => {

                if (!farmId) {
                    return {
                        recommendation: null,
                        nextSchedule: null,
                        schedules: [],
                        reassessmentSchedules: [],
                        weather: null,
                        soil: null,
                    };
                }

                const results =
                    await Promise.allSettled([
                        getLatestFertilizerRecommendation(
                            farmId
                        ),

                        getNextFertilizerSchedule(
                            farmId
                        ),

                        getFarmFertilizerSchedules(
                            farmId
                        ),

                        getFertilizerReassessmentSchedules(
                            farmId
                        ),

                        apiRequest(
                            `/api/weather/farm/${farmId}`
                        ),

                        apiRequest(
                            `/api/soil-intelligence/farm/${farmId}`
                        ),
                    ]);


                const [
                    recommendationResult,
                    nextScheduleResult,
                    schedulesResult,
                    reassessmentResult,
                    weatherResult,
                    soilResult,
                ] = results;


                let latestRecommendation = null;

                if (
                    recommendationResult.status ===
                    "fulfilled"
                ) {
                    latestRecommendation =
                        recommendationResult.value ||
                        null;
                }


                let latestNextSchedule = null;

                if (
                    nextScheduleResult.status ===
                    "fulfilled"
                ) {
                    latestNextSchedule =
                        nextScheduleResult.value ||
                        null;
                }


                let scheduleRecords = [];

                if (
                    schedulesResult.status ===
                    "fulfilled"
                ) {
                    scheduleRecords =
                        normalizeList(
                            schedulesResult.value
                        );
                }


                let reassessmentRecords = [];

                if (
                    reassessmentResult.status ===
                    "fulfilled"
                ) {
                    reassessmentRecords =
                        normalizeList(
                            reassessmentResult.value
                        );
                }


                let latestWeather = null;

                if (
                    weatherResult.status ===
                    "fulfilled"
                ) {

                    const weatherData =
                        weatherResult.value;

                    if (
                        Array.isArray(
                            weatherData
                        )
                    ) {

                        latestWeather =
                            [...weatherData]
                                .filter(
                                    (item) =>
                                        item?.recordedAt
                                )
                                .sort(
                                    (a, b) =>
                                        new Date(
                                            b.recordedAt
                                        ).getTime() -
                                        new Date(
                                            a.recordedAt
                                        ).getTime()
                                )[0] || null;

                    } else {

                        latestWeather =
                            weatherData || null;
                    }
                }


                let soilData = null;

                if (
                    soilResult.status ===
                    "fulfilled"
                ) {
                    soilData =
                        soilResult.value ||
                        null;
                }


                return {
                    recommendation:
                    latestRecommendation,

                    nextSchedule:
                    latestNextSchedule,

                    schedules:
                    scheduleRecords,

                    reassessmentSchedules:
                    reassessmentRecords,

                    weather:
                    latestWeather,

                    soil:
                    soilData,
                };

            },
            [farmId]
        );


    /* =====================================================
       LOAD VERIFIED FERTILIZER REFERENCES
       ===================================================== */

    const loadVerifiedFertilizerReferences =
        useCallback(
            async () => {

                if (!selectedCropId) {
                    setFertilizerReferences([]);
                    return;
                }

                setReferenceLoading(true);

                try {
                    const response =
                        await apiRequest(
                            `/api/fertilizer-references/crop/${selectedCropId}`
                        );

                    setFertilizerReferences(
                        normalizeList(response)
                    );

                } catch (referenceError) {

                    console.warn(
                        "Verified fertilizer references load failed:",
                        referenceError
                    );

                    setFertilizerReferences([]);

                } finally {
                    setReferenceLoading(false);
                }
            },
            [selectedCropId]
        );


    useEffect(() => {
        void loadVerifiedFertilizerReferences();
    }, [
        loadVerifiedFertilizerReferences,
    ]);


    /* =====================================================
       INITIAL DATA LOAD
       ===================================================== */

    useEffect(() => {

        let cancelled = false;

        async function initialize() {

            if (!farmId) {

                if (!cancelled) {
                    setFarm(null);
                    setCrops([]);
                    setCurrentCrop(null);
                    setRecommendation(null);
                    setNextSchedule(null);
                    setSchedules([]);
                    setReassessmentSchedules([]);
                    setWeather(null);
                    setSoil(null);
                    setLoading(false);
                }

                return;
            }


            setLoading(true);
            setError("");


            try {

                const [
                    farmResult,
                    cropsResult,
                    currentCropResult,
                    fertilizerResult,
                ] = await Promise.all([
                    loadFarm(),
                    loadCrops(),
                    loadCurrentCrop(),
                    loadFertilizerData(),
                ]);


                if (cancelled) {
                    return;
                }


                setFarm(
                    farmResult
                );

                setReferenceRegion(
                    String(
                        farmResult?.location ??
                        currentField?.location ??
                        ""
                    )
                );

                setCrops(
                    cropsResult
                );

                setCurrentCrop(
                    currentCropResult
                );

                /*
                 * Only use the crop returned by the
                 * real active crop record. When an active
                 * crop exists, the recommendation form
                 * keeps that crop locked to the farm state.
                 */
                const activeCropIdFromRecord =
                    getCropId(currentCropResult);

                if (activeCropIdFromRecord) {
                    setSelectedCropId(
                        String(activeCropIdFromRecord)
                    );
                }


                const activeStage =
                    currentCropResult?.cropStage ??
                    currentCropResult?.stage ??
                    "";

                if (activeStage) {
                    setCropStage(
                        activeStage
                    );

                    setScheduleStage(
                        activeStage
                    );
                }


                setRecommendation(
                    fertilizerResult.recommendation
                );

                setNextSchedule(
                    fertilizerResult.nextSchedule
                );

                setSchedules(
                    fertilizerResult.schedules
                );

                setReassessmentSchedules(
                    fertilizerResult.reassessmentSchedules
                );

                setWeather(
                    fertilizerResult.weather
                );

                setSoil(
                    fertilizerResult.soil
                );


                /*
                 * Keep the form aligned with a real
                 * existing recommendation where available.
                 */
                const generatedRecommendation =
                    fertilizerResult.recommendation;

                if (
                    generatedRecommendation?.cropId
                ) {
                    setSelectedCropId(
                        String(
                            generatedRecommendation
                                .cropId
                        )
                    );
                }

                if (
                    generatedRecommendation?.cropStage
                ) {
                    setCropStage(
                        generatedRecommendation
                            .cropStage
                    );
                }


            } catch (loadError) {

                console.error(
                    "Fertilizer initialization failed:",
                    loadError
                );

                if (!cancelled) {
                    setError(
                        loadError?.message ||
                        "Unable to load fertilizer data."
                    );
                }

            } finally {

                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        void initialize();

        return () => {
            cancelled = true;
        };

    }, [
        farmId,
        loadFarm,
        loadCrops,
        loadCurrentCrop,
        loadFertilizerData,
    ]);


    /* =====================================================
       CURRENT CROP NAME
       ===================================================== */

    const currentCropName =
        useMemo(
            () =>
                getCropName(
                    currentCrop
                ),
            [
                currentCrop,
            ]
        );


    /* =====================================================
       WEATHER
       ===================================================== */

    const currentWeatherDescription =
        useMemo(
            () =>
                getWeatherDescription(
                    weather
                ),
            [
                weather,
            ]
        );


    /* =====================================================
       SORTED SCHEDULES
       ===================================================== */

    const sortedSchedules =
        useMemo(
            () =>
                [...schedules].sort(
                    (a, b) =>
                        new Date(
                            a.plannedDate || 0
                        ).getTime() -
                        new Date(
                            b.plannedDate || 0
                        ).getTime()
                ),
            [
                schedules,
            ]
        );


    /* =====================================================
       STATUS
       ===================================================== */

    const recommendationTone =
        getStatusTone(
            recommendation?.status
        );

    const nextScheduleTone =
        getStatusTone(
            nextSchedule?.status
        );

    const activeCropId =
        String(getCropId(currentCrop) || "");

    const hasActiveCrop =
        Boolean(activeCropId);

    const recommendationUsable =
        isRecommendationUsable(recommendation);

    const recommendationStatus =
        normalizeStatus(recommendation?.status);


    /* =====================================================
       GENERATE RECOMMENDATION
       ===================================================== */

    const handleGenerateRecommendation =
        async () => {

            setError("");
            setMessage("");

            if (!farmId) {
                setError(
                    "No farm is selected."
                );
                return;
            }

            if (!selectedCropId) {
                setError(
                    "Select a real crop first."
                );
                return;
            }

            if (
                hasActiveCrop &&
                String(selectedCropId) !== activeCropId
            ) {
                setError(
                    "The selected crop does not match the farm's active crop."
                );
                return;
            }

            if (!cropStage.trim()) {
                setError(
                    "Crop stage is required."
                );
                return;
            }

            if (
                !fieldArea ||
                Number(fieldArea) <= 0
            ) {
                setError(
                    "Enter a valid field area."
                );
                return;
            }

            if (!fieldAreaUnit) {
                setError(
                    "Select the field-area unit."
                );
                return;
            }


            setSavingRecommendation(
                true
            );


            try {

                const response =
                    await generateFertilizerRecommendation(
                        {
                            farmId,

                            cropId:
                            selectedCropId,

                            cropStage:
                                cropStage.trim(),

                            fieldArea:
                                Number(
                                    fieldArea
                                ),

                            fieldAreaUnit,

                            farmerNote:
                                farmerNote.trim(),
                        }
                    );


                setRecommendation(
                    response || null
                );


                if (
                    response?.cropId
                ) {
                    setSelectedCropId(
                        String(
                            response.cropId
                        )
                    );
                }


                if (
                    response?.cropStage
                ) {
                    setCropStage(
                        response.cropStage
                    );

                    setScheduleStage(
                        response.cropStage
                    );
                }


                setMessage(
                    "Fertilizer recommendation generated from the backend."
                );


            } catch (
                recommendationError
                ) {

                console.error(
                    "Fertilizer recommendation failed:",
                    recommendationError
                );

                setError(
                    recommendationError?.message ||
                    "Unable to generate fertilizer recommendation."
                );

            } finally {

                setSavingRecommendation(
                    false
                );
            }
        };


    /* =====================================================
       CREATE FERTILIZER REFERENCE
       ===================================================== */

    const handleCreateFertilizerReference =
        async () => {

            setError("");
            setMessage("");

            if (!selectedCropId) {
                setError("Select a real crop before adding a reference.");
                return;
            }

            if (!referenceRegion.trim()) {
                setError("Reference region is required.");
                return;
            }

            if (!referenceApplicationStage.trim()) {
                setError("Application stage is required.");
                return;
            }

            if (!referenceUnit.trim()) {
                setError("Reference unit is required.");
                return;
            }

            if (!referenceSourceOrganization.trim()) {
                setError("Source organization is required.");
                return;
            }

            if (!referenceSourceDocument.trim()) {
                setError("Source document is required.");
                return;
            }

            const nutrientValues = [
                referenceN,
                referenceP,
                referenceK,
                referenceS,
                referenceZn,
            ];

            if (
                !nutrientValues.some(
                    (value) =>
                        value !== ""
                        && Number.isFinite(Number(value))
                )
            ) {
                setError(
                    "Enter at least one verified nutrient value from the source document."
                );
                return;
            }

            const toNullableNumber = (value) =>
                value === ""
                    ? null
                    : Number(value);

            const invalidNumbers = nutrientValues.some(
                (value) =>
                    value !== ""
                    && (
                        !Number.isFinite(Number(value))
                        || Number(value) < 0
                    )
            );

            if (invalidNumbers) {
                setError(
                    "Nutrient values must be finite non-negative numbers."
                );
                return;
            }

            setSavingReference(true);

            try {
                const response =
                    await apiRequest(
                        "/api/fertilizer-references",
                        {
                            method: "POST",
                            body: JSON.stringify({
                                cropId: selectedCropId,
                                region: referenceRegion.trim(),
                                varietyOrCondition:
                                    referenceVarietyOrCondition.trim(),
                                applicationStage:
                                    referenceApplicationStage.trim(),
                                nitrogen:
                                    toNullableNumber(referenceN),
                                phosphorus:
                                    toNullableNumber(referenceP),
                                potassium:
                                    toNullableNumber(referenceK),
                                sulphur:
                                    toNullableNumber(referenceS),
                                zinc:
                                    toNullableNumber(referenceZn),
                                unit: referenceUnit.trim(),
                                sourceOrganization:
                                    referenceSourceOrganization.trim(),
                                sourceDocument:
                                    referenceSourceDocument.trim(),
                                sourceUrl:
                                    referenceSourceUrl.trim(),
                                notes:
                                    referenceNotes.trim(),
                            }),
                        }
                    );

                setLastSubmittedReference(
                    response || null
                );

                setMessage(
                    "Reference submitted. The backend keeps new references in REVIEW_REQUIRED until verification."
                );

            } catch (referenceError) {

                console.error(
                    "Create fertilizer reference failed:",
                    referenceError
                );

                setError(
                    referenceError?.message ||
                    "Unable to create fertilizer reference."
                );

            } finally {
                setSavingReference(false);
            }
        };


    /* =====================================================
       VERIFY FERTILIZER REFERENCE
       ===================================================== */

    const handleVerifyFertilizerReference =
        async (referenceId) => {

            if (!referenceId) {
                return;
            }

            setError("");
            setMessage("");
            setVerifyingReferenceId(
                String(referenceId)
            );

            try {
                const response =
                    await apiRequest(
                        `/api/fertilizer-references/${referenceId}/verify`,
                        {
                            method: "PATCH",
                        }
                    );

                if (response) {
                    setFertilizerReferences(
                        (previous) => [
                            response,
                            ...previous.filter(
                                (item) =>
                                    String(item?.id) !==
                                    String(response?.id)
                            ),
                        ]
                    );

                    setLastSubmittedReference(
                        response
                    );
                }

                setMessage(
                    "Fertilizer reference verified successfully. It is now eligible for verified-reference lookup."
                );

            } catch (referenceError) {

                console.error(
                    "Verify fertilizer reference failed:",
                    referenceError
                );

                setError(
                    referenceError?.message ||
                    "Unable to verify fertilizer reference."
                );

            } finally {
                setVerifyingReferenceId("");
            }
        };


    /* =====================================================
       USE RECOMMENDATION
       ===================================================== */

    const handleUseRecommendation =
        () => {

            if (!recommendationUsable) {
                setMessage(
                    "Schedule values are unavailable because the backend has not returned a usable fertilizer recommendation."
                );
                return;
            }


            if (
                recommendation.cropId
            ) {

                setSelectedCropId(
                    String(
                        recommendation.cropId
                    )
                );
            }


            if (
                recommendation.cropStage
            ) {

                setScheduleStage(
                    recommendation.cropStage
                );
            }


            setScheduleN(
                recommendation.nitrogen ??
                ""
            );

            setScheduleP(
                recommendation.phosphorus ??
                ""
            );

            setScheduleK(
                recommendation.potassium ??
                ""
            );

            setScheduleS(
                recommendation.sulphur ??
                ""
            );

            setScheduleZn(
                recommendation.zinc ??
                ""
            );


            setMessage(
                "Recommendation values were copied into the schedule form."
            );


            window.scrollTo({
                top:
                    document.body.scrollHeight *
                    0.62,
                behavior:
                    "smooth",
            });
        };


    /* =====================================================
       CREATE SCHEDULE
       ===================================================== */

    const handleCreateSchedule =
        async () => {

            setError("");
            setMessage("");


            if (!farmId) {
                setError(
                    "No farm is selected."
                );
                return;
            }

            if (!selectedCropId) {
                setError(
                    "Select a real crop."
                );
                return;
            }

            if (!scheduleStage.trim()) {
                setError(
                    "Schedule stage is required."
                );
                return;
            }

            if (!plannedDate) {
                setError(
                    "Planned date is required."
                );
                return;
            }

            if (!actionType) {
                setError(
                    "Action type is required."
                );
                return;
            }


            setSavingSchedule(true);


            try {

                const response =
                    await createFertilizerSchedule(
                        {
                            farmId,

                            cropId:
                            selectedCropId,

                            recommendationId:
                                recommendation?.id ??
                                null,

                            stage:
                                scheduleStage.trim(),

                            plannedDate,

                            actionType,

                            productName:
                                productName.trim(),

                            nitrogen:
                                scheduleN !== ""
                                    ? Number(
                                        scheduleN
                                    )
                                    : null,

                            phosphorus:
                                scheduleP !== ""
                                    ? Number(
                                        scheduleP
                                    )
                                    : null,

                            potassium:
                                scheduleK !== ""
                                    ? Number(
                                        scheduleK
                                    )
                                    : null,

                            sulphur:
                                scheduleS !== ""
                                    ? Number(
                                        scheduleS
                                    )
                                    : null,

                            zinc:
                                scheduleZn !== ""
                                    ? Number(
                                        scheduleZn
                                    )
                                    : null,

                            weatherCondition:
                            currentWeatherDescription,

                            reassessmentRequired,

                            source:
                                recommendation?.source ??
                                "",

                            uncertaintyNote:
                                recommendation?.uncertaintyNote ??
                                "",

                            notes:
                                scheduleNotes.trim(),
                        }
                    );


                if (response) {

                    const updatedSchedules =
                        [
                            ...schedules,
                            response,
                        ].sort(
                            (a, b) =>
                                new Date(
                                    a.plannedDate || 0
                                ).getTime() -
                                new Date(
                                    b.plannedDate || 0
                                ).getTime()
                        );

                    setSchedules(
                        updatedSchedules
                    );

                    setNextSchedule(
                        response
                    );
                }


                setMessage(
                    "Fertilizer schedule created successfully."
                );

                setScheduleNotes("");


            } catch (
                scheduleError
                ) {

                console.error(
                    "Create schedule failed:",
                    scheduleError
                );

                setError(
                    scheduleError?.message ||
                    "Unable to create fertilizer schedule."
                );

            } finally {

                setSavingSchedule(false);
            }
        };


    /* =====================================================
       EDIT SCHEDULE
       ===================================================== */

    const beginEditSchedule =
        (schedule) => {

            setEditingScheduleId(
                schedule.id
            );

            setEditDate(
                formatDateInput(
                    schedule.plannedDate
                )
            );

            setEditStatus(
                schedule.status ||
                "PLANNED"
            );
        };


    const cancelEditSchedule =
        () => {

            setEditingScheduleId("");
            setEditDate("");
            setEditStatus("");
        };


    const saveScheduleEdit =
        async (
            schedule
        ) => {

            if (!schedule?.id) {
                return;
            }

            if (!editDate) {
                setError(
                    "A planned date is required."
                );
                return;
            }


            setError("");
            setMessage("");


            try {

                const response =
                    await updateFertilizerSchedule(
                        schedule.id,
                        {
                            farmId:
                            schedule.farmId,

                            cropId:
                            schedule.cropId,

                            recommendationId:
                                schedule.recommendationId ??
                                null,

                            stage:
                            schedule.stage,

                            plannedDate:
                            editDate,

                            actionType:
                            schedule.actionType,

                            productName:
                                schedule.productName ??
                                "",

                            nitrogen:
                                schedule.nitrogen ??
                                null,

                            phosphorus:
                                schedule.phosphorus ??
                                null,

                            potassium:
                                schedule.potassium ??
                                null,

                            sulphur:
                                schedule.sulphur ??
                                null,

                            zinc:
                                schedule.zinc ??
                                null,

                            status:
                                editStatus ||
                                schedule.status,

                            weatherCondition:
                                schedule.weatherCondition ??
                                "",

                            reassessmentRequired:
                                schedule.reassessmentRequired ??
                                true,

                            source:
                                schedule.source ??
                                "",

                            uncertaintyNote:
                                schedule.uncertaintyNote ??
                                "",

                            notes:
                                schedule.notes ??
                                "",
                        }
                    );


                setSchedules(
                    (previous) =>
                        previous.map(
                            (item) =>
                                String(
                                    item.id
                                ) ===
                                String(
                                    schedule.id
                                )
                                    ? response
                                    : item
                        )
                );


                if (
                    String(
                        nextSchedule?.id
                    ) ===
                    String(
                        schedule.id
                    )
                ) {
                    setNextSchedule(
                        response
                    );
                }


                cancelEditSchedule();


                setMessage(
                    "Fertilizer schedule updated."
                );


            } catch (
                updateError
                ) {

                console.error(
                    "Schedule update failed:",
                    updateError
                );

                setError(
                    updateError?.message ||
                    "Unable to update fertilizer schedule."
                );
            }
        };


    /* =====================================================
       DELETE SCHEDULE
       ===================================================== */

    const handleDeleteSchedule =
        async (
            scheduleId
        ) => {

            if (!scheduleId) {
                return;
            }


            const confirmed =
                window.confirm(
                    "Delete this fertilizer schedule?"
                );

            if (!confirmed) {
                return;
            }


            setError("");
            setMessage("");


            setDeletingScheduleId(
                scheduleId
            );


            try {

                await deleteFertilizerSchedule(
                    scheduleId
                );


                setSchedules(
                    (previous) =>
                        previous.filter(
                            (item) =>
                                String(
                                    item.id
                                ) !==
                                String(
                                    scheduleId
                                )
                        )
                );


                if (
                    String(
                        nextSchedule?.id
                    ) ===
                    String(
                        scheduleId
                    )
                ) {

                    try {

                        const refreshed =
                            await getNextFertilizerSchedule(
                                farmId
                            );

                        setNextSchedule(
                            refreshed ||
                            null
                        );

                    } catch {
                        setNextSchedule(
                            null
                        );
                    }
                }


                setMessage(
                    "Fertilizer schedule deleted."
                );


            } catch (
                deleteError
                ) {

                console.error(
                    "Schedule delete failed:",
                    deleteError
                );

                setError(
                    deleteError?.message ||
                    "Unable to delete fertilizer schedule."
                );

            } finally {

                setDeletingScheduleId("");
            }
        };


    /* =====================================================
       FARM ABSENT
       ===================================================== */

    if (!farmId) {

        return (
            <div className="fertilizer-page">

                <div className="fertilizer-empty-page">

                    <div className="fertilizer-empty-icon">
                        🌿
                    </div>

                    <h1>
                        {language === "hi"
                            ? "उर्वरक सलाह"
                            : "Fertilizer Intelligence"}
                    </h1>

                    <p>
                        {language === "hi"
                            ? "पहले एक खेत चुनें।"
                            : "Select a farm before using fertilizer intelligence."}
                    </p>

                </div>

            </div>
        );
    }


    /* =====================================================
       RENDER
       ===================================================== */

    return (
        <div className="fertilizer-page">

            {/* =================================================
                HERO
            ================================================= */}

            <section className="fertilizer-hero">

                <div>

                    <span className="fertilizer-eyebrow">
                        AGRINEXUS FARM INTELLIGENCE
                    </span>

                    <h1>
                        Fertilizer
                        <span>
                            Recommendation & Scheduling
                        </span>
                    </h1>

                    <p>
                        Turn real soil and crop records into
                        transparent fertilizer decision support
                        and planned field activities.
                    </p>

                </div>

                <div className="fertilizer-hero-mark">
                    🌿
                </div>

            </section>


            {/* =================================================
                FARM CONTEXT
            ================================================= */}

            <section className="fertilizer-farm-context">

                <div className="fertilizer-farm-icon">
                    🚜
                </div>

                <div className="fertilizer-farm-main">

                    <span>
                        CURRENT FARM
                    </span>

                    <strong>
                        {farm?.name ??
                            currentField?.name ??
                            "—"}
                    </strong>

                    <small>
                        {displayLocation(
                            farm?.location ??
                            currentField?.location
                        )}
                    </small>

                </div>

                <div className="fertilizer-farm-crop">

                    <span>
                        ACTIVE CROP
                    </span>

                    <strong>
                        {currentCropName}
                    </strong>

                    <small>
                        {currentCrop?.cropStage ??
                            currentCrop?.stage ??
                            "—"}
                    </small>

                </div>

                <button
                    type="button"
                    className="fertilizer-back-btn"
                    onClick={() =>
                        navigate("/home")
                    }
                >
                    ← Home
                </button>

            </section>


            {/* =================================================
                ALERTS
            ================================================= */}

            {error && (

                <div className="fertilizer-alert fertilizer-alert-error">

                    <span>
                        ⚠️
                    </span>

                    <div>

                        <strong>
                            Request needs attention
                        </strong>

                        <p>
                            {error}
                        </p>

                    </div>

                </div>
            )}


            {message && (

                <div className="fertilizer-alert fertilizer-alert-success">

                    <span>
                        ✓
                    </span>

                    <div>

                        <strong>
                            Fertilizer module updated
                        </strong>

                        <p>
                            {message}
                        </p>

                    </div>

                </div>
            )}


            {/* =================================================
                EVIDENCE
            ================================================= */}

            <section className="fertilizer-evidence-grid">

                <article className="fertilizer-evidence-card">

                    <div className="fertilizer-card-icon">
                        🧪
                    </div>

                    <div>

                        <span>
                            SOIL EVIDENCE
                        </span>

                        <strong>
                            pH{" "}
                            {
                                displayNumber(
                                    getSoilValue(
                                        soil,
                                        [
                                            "pH",
                                            "soilPh",
                                        ]
                                    ),
                                    2
                                )
                            }
                        </strong>

                        <small>
                            N{" "}
                            {
                                displayNumber(
                                    getSoilValue(
                                        soil,
                                        [
                                            "nitrogen",
                                        ]
                                    ),
                                    2
                                )
                            }

                            {" · "}

                            P{" "}
                            {
                                displayNumber(
                                    getSoilValue(
                                        soil,
                                        [
                                            "phosphorus",
                                        ]
                                    ),
                                    2
                                )
                            }

                            {" · "}

                            K{" "}
                            {
                                displayNumber(
                                    getSoilValue(
                                        soil,
                                        [
                                            "potassium",
                                        ]
                                    ),
                                    2
                                )
                            }
                        </small>

                    </div>

                </article>


                <article className="fertilizer-evidence-card">

                    <div className="fertilizer-card-icon">
                        🌦️
                    </div>

                    <div>

                        <span>
                            WEATHER CONTEXT
                        </span>

                        <strong>
                            {
                                safeNumber(
                                    weather?.temperature
                                )
                                    ? `${displayNumber(
                                        weather.temperature,
                                        1
                                    )} °C`
                                    : "—"
                            }
                        </strong>

                        <small>
                            {
                                currentWeatherDescription
                            }
                        </small>

                    </div>

                </article>


                <article className="fertilizer-evidence-card">

                    <div className="fertilizer-card-icon">
                        📚
                    </div>

                    <div>

                        <span>
                            REGISTERED FIELDS
                        </span>

                        <strong>
                            {fields.length}
                        </strong>

                        <small>
                            Real fields available to this account
                        </small>

                    </div>

                </article>

            </section>


            {/* =================================================
                FERTILIZER REFERENCE LIBRARY
            ================================================= */}

            <section className="fertilizer-section">

                <div className="fertilizer-section-heading">

                    <div>

                        <span>
                            REFERENCE DATA · VERIFY BEFORE USE
                        </span>

                        <h2>
                            Fertilizer Reference Library
                        </h2>

                        <p>
                            Register an authoritative fertilizer reference, keep it in review, then verify it before recommendation logic can use it.
                        </p>

                    </div>

                    <div className="fertilizer-step-badge">
                        PROVENANCE FIRST
                    </div>

                </div>


                <div className="fertilizer-card fertilizer-form-card">

                    <div className="fertilizer-form-grid">

                        <div className="fertilizer-field">
                            <label>
                                Crop
                            </label>

                            <input
                                value={
                                    crops.find(
                                        (crop) =>
                                            String(crop?.id) ===
                                            String(selectedCropId)
                                    )?.name ||
                                    currentCropName ||
                                    "—"
                                }
                                readOnly
                            />

                            <small>
                                Uses the real crop selected for this fertilizer workflow.
                            </small>
                        </div>


                        <div className="fertilizer-field">
                            <label>
                                Region
                            </label>

                            <input
                                value={referenceRegion}
                                onChange={(event) =>
                                    setReferenceRegion(
                                        event.target.value
                                    )
                                }
                                placeholder="Farm / reference region"
                            />
                        </div>


                        <div className="fertilizer-field">
                            <label>
                                Application Stage
                            </label>

                            <select
                                value={
                                    referenceApplicationStage
                                }
                                onChange={(event) =>
                                    setReferenceApplicationStage(
                                        event.target.value
                                    )
                                }
                            >
                                <option value="">
                                    Select stage
                                </option>

                                {cropStageOptions.map(
                                    (stage) => (
                                        <option
                                            key={stage}
                                            value={stage}
                                        >
                                            {stage.replace(
                                                /_/g,
                                                " "
                                            )}
                                        </option>
                                    )
                                )}
                            </select>
                        </div>


                        <div className="fertilizer-field">
                            <label>
                                Variety / Condition
                            </label>

                            <input
                                value={
                                    referenceVarietyOrCondition
                                }
                                onChange={(event) =>
                                    setReferenceVarietyOrCondition(
                                        event.target.value
                                    )
                                }
                                placeholder="Optional"
                            />
                        </div>


                        <div className="fertilizer-field">
                            <label>
                                Nitrogen (N)
                            </label>

                            <input
                                type="number"
                                min="0"
                                step="0.01"
                                value={referenceN}
                                onChange={(event) =>
                                    setReferenceN(
                                        event.target.value
                                    )
                                }
                                placeholder="From source"
                            />
                        </div>


                        <div className="fertilizer-field">
                            <label>
                                Phosphorus (P)
                            </label>

                            <input
                                type="number"
                                min="0"
                                step="0.01"
                                value={referenceP}
                                onChange={(event) =>
                                    setReferenceP(
                                        event.target.value
                                    )
                                }
                                placeholder="From source"
                            />
                        </div>


                        <div className="fertilizer-field">
                            <label>
                                Potassium (K)
                            </label>

                            <input
                                type="number"
                                min="0"
                                step="0.01"
                                value={referenceK}
                                onChange={(event) =>
                                    setReferenceK(
                                        event.target.value
                                    )
                                }
                                placeholder="From source"
                            />
                        </div>


                        <div className="fertilizer-field">
                            <label>
                                Sulphur (S)
                            </label>

                            <input
                                type="number"
                                min="0"
                                step="0.01"
                                value={referenceS}
                                onChange={(event) =>
                                    setReferenceS(
                                        event.target.value
                                    )
                                }
                                placeholder="From source"
                            />
                        </div>


                        <div className="fertilizer-field">
                            <label>
                                Zinc (Zn)
                            </label>

                            <input
                                type="number"
                                min="0"
                                step="0.01"
                                value={referenceZn}
                                onChange={(event) =>
                                    setReferenceZn(
                                        event.target.value
                                    )
                                }
                                placeholder="From source"
                            />
                        </div>


                        <div className="fertilizer-field">
                            <label>
                                Unit
                            </label>

                            <input
                                value={referenceUnit}
                                onChange={(event) =>
                                    setReferenceUnit(
                                        event.target.value
                                    )
                                }
                                placeholder="Use the source unit exactly"
                            />
                        </div>


                        <div className="fertilizer-field">
                            <label>
                                Source Organization
                            </label>

                            <input
                                value={
                                    referenceSourceOrganization
                                }
                                onChange={(event) =>
                                    setReferenceSourceOrganization(
                                        event.target.value
                                    )
                                }
                                placeholder="Official organization"
                            />
                        </div>


                        <div className="fertilizer-field">
                            <label>
                                Source Document
                            </label>

                            <input
                                value={
                                    referenceSourceDocument
                                }
                                onChange={(event) =>
                                    setReferenceSourceDocument(
                                        event.target.value
                                    )
                                }
                                placeholder="Notification / bulletin / guideline"
                            />
                        </div>


                        <div className="fertilizer-field full-width">
                            <label>
                                Source URL
                                <span>Optional</span>
                            </label>

                            <input
                                type="url"
                                value={
                                    referenceSourceUrl
                                }
                                onChange={(event) =>
                                    setReferenceSourceUrl(
                                        event.target.value
                                    )
                                }
                                placeholder="Official source link"
                            />
                        </div>


                        <div className="fertilizer-field full-width">
                            <label>
                                Notes
                                <span>Optional</span>
                            </label>

                            <textarea
                                rows="3"
                                value={referenceNotes}
                                onChange={(event) =>
                                    setReferenceNotes(
                                        event.target.value
                                    )
                                }
                                placeholder="Context, variety, soil condition, or citation notes"
                            />
                        </div>

                    </div>


                    <div className="fertilizer-form-footer">

                        <div>
                            <span>
                                Backend lifecycle
                            </span>

                            <strong>
                                REVIEW_REQUIRED → VERIFIED
                            </strong>
                        </div>

                        <button
                            type="button"
                            className="fertilizer-primary-btn"
                            disabled={
                                savingReference
                                || !selectedCropId
                            }
                            onClick={
                                handleCreateFertilizerReference
                            }
                        >
                            {savingReference
                                ? "Submitting..."
                                : "Submit Reference →"}
                        </button>

                    </div>

                </div>


                <div className="fertilizer-card fertilizer-history-card">

                    <div className="fertilizer-section-heading">
                        <div>
                            <span>
                                VERIFIED REFERENCES
                            </span>

                            <h3>
                                Available for Recommendation
                            </h3>
                        </div>

                        <div className="fertilizer-history-count">
                            {referenceLoading
                                ? "…"
                                : fertilizerReferences.length}
                            <span>
                                records
                            </span>
                        </div>
                    </div>


                    {lastSubmittedReference ? (
                        <div className="fertilizer-review-card">
                            <span className="fertilizer-card-label">
                                LAST SUBMISSION
                            </span>

                            <h3>
                                {getCropName(lastSubmittedReference)}
                            </h3>

                            <p>
                                Status: {getStatusLabel(
                                lastSubmittedReference.dataStatus
                                || lastSubmittedReference.status
                                || "REVIEW_REQUIRED"
                            )}
                                {" · "}
                                {lastSubmittedReference.sourceOrganization || "—"}
                            </p>

                            <small>
                                {lastSubmittedReference.sourceDocument || "—"}
                            </small>

                            {normalizeStatus(
                                lastSubmittedReference.dataStatus
                                || lastSubmittedReference.status
                            ) === "REVIEW_REQUIRED" && (
                                <div style={{ marginTop: "14px" }}>
                                    <button
                                        type="button"
                                        className="fertilizer-secondary-btn"
                                        disabled={
                                            verifyingReferenceId ===
                                            String(lastSubmittedReference.id)
                                        }
                                        onClick={() =>
                                            handleVerifyFertilizerReference(
                                                lastSubmittedReference.id
                                            )
                                        }
                                    >
                                        {verifyingReferenceId ===
                                        String(lastSubmittedReference.id)
                                            ? "Verifying..."
                                            : "Verify Reference →"}
                                    </button>
                                </div>
                            )}
                        </div>
                    ) : null}


                    {fertilizerReferences.length === 0 ? (

                        <div className="fertilizer-inline-empty">
                            <span>
                                📚
                            </span>

                            <strong>
                                No verified fertilizer references returned
                            </strong>

                            <p>
                                Submit a real source above. New references remain in review until explicitly verified by the backend.
                            </p>
                        </div>

                    ) : (

                        <div className="fertilizer-schedule-list">
                            {fertilizerReferences.map(
                                (reference) => (
                                    <article
                                        key={reference.id}
                                        className="fertilizer-schedule-row"
                                    >
                                        <div className="fertilizer-schedule-date">
                                            <strong>
                                                {reference.cropName || "—"}
                                            </strong>
                                            <span>
                                                {reference.applicationStage || "—"}
                                            </span>
                                        </div>

                                        <div className="fertilizer-schedule-info">
                                            <strong>
                                                {reference.sourceOrganization || "—"}
                                            </strong>
                                            <span>
                                                {reference.sourceDocument || "—"}
                                            </span>
                                            <span>
                                                Region: {reference.region || "—"}
                                            </span>
                                        </div>

                                        <div className="fertilizer-schedule-nutrients">
                                            <span>N {displayNumber(reference.nitrogen)}</span>
                                            <span>P {displayNumber(reference.phosphorus)}</span>
                                            <span>K {displayNumber(reference.potassium)}</span>
                                            <span>S {displayNumber(reference.sulphur)}</span>
                                            <span>Zn {displayNumber(reference.zinc)}</span>
                                        </div>

                                        <div className="fertilizer-schedule-status">
                                            <span className="fertilizer-status-mini success">
                                                VERIFIED
                                            </span>
                                            <small>
                                                {reference.verifiedDate || "—"}
                                            </small>
                                        </div>
                                    </article>
                                )
                            )}
                        </div>
                    )}

                </div>

            </section>


            {/* =================================================
                GENERATE
            ================================================= */}

            <section className="fertilizer-section">

                <div className="fertilizer-section-heading">

                    <div>

                        <span>
                            01 · GENERATE
                        </span>

                        <h2>
                            Fertilizer Recommendation
                        </h2>

                        <p>
                            Generate a backend recommendation
                            using the real farm and crop records.
                        </p>

                    </div>

                    <div className="fertilizer-step-badge">
                        REAL DATA
                    </div>

                </div>


                <div className="fertilizer-card fertilizer-form-card">

                    <div className="fertilizer-form-grid">

                        <div className="fertilizer-field full-width">

                            <label>
                                Crop
                            </label>

                            <select
                                value={
                                    selectedCropId
                                }
                                disabled={hasActiveCrop}
                                onChange={(event) =>
                                    setSelectedCropId(
                                        event.target.value
                                    )
                                }
                            >

                                <option value="">
                                    Select real crop
                                </option>

                                {crops.map(
                                    (crop) => (

                                        <option
                                            key={crop.id}
                                            value={crop.id}
                                        >
                                            {crop.name}
                                        </option>

                                    )
                                )}

                            </select>

                            <small>
                                Active crop:
                                {" "}
                                {currentCropName}
                                {hasActiveCrop
                                    ? " · locked to farm active crop"
                                    : " · no active crop returned by backend"}
                            </small>

                        </div>


                        <div className="fertilizer-field">

                            <label>
                                Crop Stage
                            </label>

                            <select
                                value={
                                    cropStage
                                }
                                onChange={(event) =>
                                    setCropStage(
                                        event.target.value
                                    )
                                }
                            >

                                <option value="">
                                    Select stage
                                </option>

                                {cropStageOptions.map(
                                    (stage) => (

                                        <option
                                            key={stage}
                                            value={stage}
                                        >
                                            {stage.replace(
                                                /_/g,
                                                " "
                                            )}
                                        </option>

                                    )
                                )}

                            </select>

                        </div>


                        <div className="fertilizer-field">

                            <label>
                                Field Area
                            </label>

                            <input
                                type="number"
                                min="0"
                                step="0.01"
                                value={
                                    fieldArea
                                }
                                onChange={(event) =>
                                    setFieldArea(
                                        event.target.value
                                    )
                                }
                                placeholder="Enter area"
                            />

                        </div>


                        <div className="fertilizer-field">

                            <label>
                                Area Unit
                            </label>

                            <select
                                value={
                                    fieldAreaUnit
                                }
                                onChange={(event) =>
                                    setFieldAreaUnit(
                                        event.target.value
                                    )
                                }
                            >

                                <option value="">
                                    Select unit
                                </option>

                                <option value="ACRE">
                                    ACRE
                                </option>

                                <option value="HECTARE">
                                    HECTARE
                                </option>

                            </select>

                        </div>


                        <div className="fertilizer-field full-width">

                            <label>
                                Farmer Note
                                <span>
                                    Optional
                                </span>
                            </label>

                            <textarea
                                value={
                                    farmerNote
                                }
                                onChange={(event) =>
                                    setFarmerNote(
                                        event.target.value
                                    )
                                }
                                rows="3"
                                placeholder="Describe the current fertilizer situation..."
                            />

                        </div>

                    </div>


                    <div className="fertilizer-form-footer">

                        <div>

                            <span>
                                Required
                            </span>

                            <strong>
                                Crop + Stage + Area + Unit
                            </strong>

                        </div>

                        <button
                            type="button"
                            className="fertilizer-primary-btn"
                            disabled={
                                savingRecommendation
                            }
                            onClick={
                                handleGenerateRecommendation
                            }
                        >

                            {savingRecommendation
                                ? "Generating..."
                                : "Generate Recommendation →"}

                        </button>

                    </div>

                </div>

            </section>


            {/* =================================================
                RECOMMENDATION
            ================================================= */}

            <section className="fertilizer-section">

                <div className="fertilizer-section-heading">

                    <div>

                        <span>
                            02 · DECISION
                        </span>

                        <h2>
                            Latest Recommendation
                        </h2>

                    </div>

                    {recommendation?.generatedAt && (
                        <small>
                            Generated{" "}
                            {formatDate(
                                recommendation.generatedAt
                            )}
                        </small>
                    )}

                </div>


                {loading ? (

                    <div className="fertilizer-card fertilizer-loading-card">

                        <div className="fertilizer-spinner" />

                        <span>
                            Loading fertilizer data...
                        </span>

                    </div>

                ) : !recommendation ? (

                    <div className="fertilizer-card fertilizer-empty-card">

                        <div className="fertilizer-empty-icon">
                            🌱
                        </div>

                        <h3>
                            No fertilizer recommendation available
                        </h3>

                        <p>
                            Generate a recommendation using
                            the real farm and crop information.
                        </p>

                    </div>

                ) : (

                    <>
                        {recommendationStatus === "INSUFFICIENT_DATA" ? (
                            <div className="fertilizer-card fertilizer-empty-card">
                                <div className="fertilizer-empty-icon">⚠️</div>
                                <h3>Recommendation cannot be used yet</h3>
                                <p>
                                    The backend returned insufficient data for a reliable fertilizer recommendation.
                                    No missing nutrient quantity has been invented or estimated in the frontend.
                                </p>
                            </div>
                        ) : null}

                        <div className="fertilizer-recommendation-layout">

                            <article className="fertilizer-card fertilizer-decision-card">

                                <div className="fertilizer-decision-header">

                                    <div>

                                    <span className="fertilizer-card-label">
                                        STATUS
                                    </span>

                                        <div
                                            className={`fertilizer-status-pill ${recommendationTone}`}
                                        >
                                            {
                                                getStatusLabel(
                                                    recommendation.status
                                                )
                                            }
                                        </div>

                                    </div>

                                    <div className="fertilizer-decision-crop">

                                    <span>
                                        CROP
                                    </span>

                                        <strong>
                                            {
                                                recommendation.cropName ||
                                                "—"
                                            }
                                        </strong>

                                    </div>

                                </div>


                                <div className="fertilizer-nutrient-grid">

                                    <NutrientBox
                                        name="N"
                                        title="Nitrogen"
                                        value={
                                            recommendation.nitrogen
                                        }
                                    />

                                    <NutrientBox
                                        name="P"
                                        title="Phosphorus"
                                        value={
                                            recommendation.phosphorus
                                        }
                                    />

                                    <NutrientBox
                                        name="K"
                                        title="Potassium"
                                        value={
                                            recommendation.potassium
                                        }
                                    />

                                    <NutrientBox
                                        name="S"
                                        title="Sulphur"
                                        value={
                                            recommendation.sulphur
                                        }
                                    />

                                    <NutrientBox
                                        name="Zn"
                                        title="Zinc"
                                        value={
                                            recommendation.zinc
                                        }
                                    />

                                </div>


                                <div className="fertilizer-unit-note">
                                    Nutrient values are displayed exactly
                                    as returned by the backend. Missing
                                    values remain unavailable.
                                </div>


                                <div className="fertilizer-detail-box">

                                <span>
                                    FARMER RECOMMENDATION
                                </span>

                                    <p>
                                        {
                                            recommendation.recommendation ||
                                            "—"
                                        }
                                    </p>

                                </div>

                            </article>


                            <article className="fertilizer-card fertilizer-explanation-card">

                                <div className="fertilizer-explanation-header">

                                    <div>

                                    <span className="fertilizer-card-label">
                                        EXPLAINABILITY
                                    </span>

                                        <h3>
                                            Why this recommendation?
                                        </h3>

                                    </div>

                                    <span className="fertilizer-ai-mark">
                                    WHY
                                </span>

                                </div>


                                <div className="fertilizer-reason-box">

                                <span>
                                    💡
                                </span>

                                    <p>
                                        {
                                            recommendation.reason ||
                                            "—"
                                        }
                                    </p>

                                </div>


                                <div className="fertilizer-meta-list">

                                    <MetaRow
                                        label="Crop stage"
                                        value={
                                            recommendation.cropStage
                                        }
                                    />

                                    <MetaRow
                                        label="Field area"
                                        value={
                                            safeNumber(
                                                recommendation.fieldArea
                                            )
                                                ? `${recommendation.fieldArea} ${recommendation.fieldAreaUnit || ""}`
                                                : "—"
                                        }
                                    />

                                    <MetaRow
                                        label="Data status"
                                        value={
                                            recommendation.dataStatus
                                        }
                                    />

                                    <MetaRow
                                        label="Source"
                                        value={
                                            recommendation.source
                                        }
                                    />

                                </div>


                                <button
                                    type="button"
                                    className="fertilizer-secondary-btn"
                                    disabled={!recommendationUsable}
                                    onClick={
                                        handleUseRecommendation
                                    }
                                >
                                    {recommendationUsable
                                        ? "Use Values for Schedule →"
                                        : "Schedule Values Unavailable"}
                                </button>

                                {!recommendationUsable && (
                                    <small className="fertilizer-unit-note">
                                        This action stays disabled until the backend provides at least one usable nutrient value and the recommendation is not marked as insufficient data.
                                    </small>
                                )}

                            </article>


                            <article className="fertilizer-card fertilizer-uncertainty-card">

                            <span>
                                ⚠️
                            </span>

                                <div>

                                <span className="fertilizer-card-label">
                                    UNCERTAINTY
                                </span>

                                    <p>
                                        {
                                            recommendation.uncertaintyNote ||
                                            "—"
                                        }
                                    </p>

                                </div>

                            </article>

                        </div>
                    </>
                )}

            </section>


            {/* =================================================
                NEXT SCHEDULE
            ================================================= */}

            <section className="fertilizer-section">

                <div className="fertilizer-section-heading">

                    <div>

                        <span>
                            03 · NEXT ACTION
                        </span>

                        <h2>
                            Next Fertilizer Activity
                        </h2>

                    </div>

                </div>


                {!nextSchedule ? (

                    <div className="fertilizer-card fertilizer-empty-card compact">

                        <span>
                            📅
                        </span>

                        <div>

                            <h3>
                                No upcoming schedule
                            </h3>

                            <p>
                                No backend schedule was returned
                                for this farm.
                            </p>

                        </div>

                    </div>

                ) : (

                    <div className="fertilizer-next-card">

                        <div className="fertilizer-next-icon">
                            📅
                        </div>

                        <div className="fertilizer-next-main">

                            <span>
                                NEXT ACTIVITY
                            </span>

                            <h3>
                                {
                                    nextSchedule.actionType ||
                                    "—"
                                }
                            </h3>

                            <p>
                                {
                                    nextSchedule.cropName ||
                                    "—"
                                }
                                {" · "}
                                {
                                    nextSchedule.stage ||
                                    "—"
                                }
                            </p>

                        </div>

                        <div className="fertilizer-next-date">

                            <span>
                                DATE
                            </span>

                            <strong>
                                {
                                    formatDate(
                                        nextSchedule.plannedDate
                                    )
                                }
                            </strong>

                            <em
                                className={`fertilizer-status-mini ${nextScheduleTone}`}
                            >
                                {
                                    getStatusLabel(
                                        nextSchedule.status
                                    )
                                }
                            </em>

                        </div>

                        <div className="fertilizer-next-meta">

                            <span>
                                Product
                            </span>

                            <strong>
                                {
                                    nextSchedule.productName ||
                                    "—"
                                }
                            </strong>

                            <span>
                                Reassessment
                            </span>

                            <strong>
                                {
                                    nextSchedule
                                        .reassessmentRequired ===
                                    true
                                        ? "Required"
                                        : nextSchedule
                                            .reassessmentRequired ===
                                        false
                                            ? "Not required"
                                            : "—"
                                }
                            </strong>

                        </div>

                    </div>

                )}

            </section>


            {/* =================================================
                CREATE SCHEDULE
            ================================================= */}

            <section className="fertilizer-section">

                <div className="fertilizer-section-heading">

                    <div>

                        <span>
                            04 · PLAN
                        </span>

                        <h2>
                            Create Fertilizer Schedule
                        </h2>

                        <p>
                            Create a real schedule record in
                            the AgriNexus backend.
                        </p>

                    </div>

                </div>


                <div className="fertilizer-card fertilizer-form-card">

                    <div className="fertilizer-form-grid">

                        <div className="fertilizer-field">

                            <label>
                                Crop
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
                                    Select crop
                                </option>

                                {crops.map(
                                    (crop) => (

                                        <option
                                            key={crop.id}
                                            value={crop.id}
                                        >
                                            {crop.name}
                                        </option>

                                    )
                                )}

                            </select>

                        </div>


                        <div className="fertilizer-field">

                            <label>
                                Stage
                            </label>

                            <select
                                value={
                                    scheduleStage
                                }
                                onChange={(event) =>
                                    setScheduleStage(
                                        event.target.value
                                    )
                                }
                            >

                                <option value="">
                                    Select stage
                                </option>

                                {cropStageOptions.map(
                                    (stage) => (

                                        <option
                                            key={stage}
                                            value={stage}
                                        >
                                            {stage.replace(
                                                /_/g,
                                                " "
                                            )}
                                        </option>

                                    )
                                )}

                            </select>

                        </div>


                        <div className="fertilizer-field">

                            <label>
                                Planned Date
                            </label>

                            <input
                                type="date"
                                value={
                                    plannedDate
                                }
                                onChange={(event) =>
                                    setPlannedDate(
                                        event.target.value
                                    )
                                }
                            />

                        </div>


                        <div className="fertilizer-field">

                            <label>
                                Action Type
                            </label>

                            <select
                                value={
                                    actionType
                                }
                                onChange={(event) =>
                                    setActionType(
                                        event.target.value
                                    )
                                }
                            >

                                <option value="FERTILIZER_APPLICATION">
                                    FERTILIZER APPLICATION
                                </option>

                                <option value="REASSESSMENT">
                                    REASSESSMENT
                                </option>

                                <option value="SOIL_TEST">
                                    SOIL TEST
                                </option>

                                <option value="REVIEW">
                                    REVIEW
                                </option>

                            </select>

                        </div>


                        <div className="fertilizer-field">

                            <label>
                                Product
                                <span>
                                    Optional
                                </span>
                            </label>

                            <input
                                type="text"
                                value={
                                    productName
                                }
                                onChange={(event) =>
                                    setProductName(
                                        event.target.value
                                    )
                                }
                                placeholder="Product name"
                            />

                        </div>


                        <div className="fertilizer-field">

                            <label>
                                Nitrogen
                            </label>

                            <input
                                type="number"
                                step="0.01"
                                value={
                                    scheduleN
                                }
                                onChange={(event) =>
                                    setScheduleN(
                                        event.target.value
                                    )
                                }
                                placeholder="—"
                            />

                        </div>


                        <div className="fertilizer-field">

                            <label>
                                Phosphorus
                            </label>

                            <input
                                type="number"
                                step="0.01"
                                value={
                                    scheduleP
                                }
                                onChange={(event) =>
                                    setScheduleP(
                                        event.target.value
                                    )
                                }
                                placeholder="—"
                            />

                        </div>


                        <div className="fertilizer-field">

                            <label>
                                Potassium
                            </label>

                            <input
                                type="number"
                                step="0.01"
                                value={
                                    scheduleK
                                }
                                onChange={(event) =>
                                    setScheduleK(
                                        event.target.value
                                    )
                                }
                                placeholder="—"
                            />

                        </div>


                        <div className="fertilizer-field">

                            <label>
                                Sulphur
                            </label>

                            <input
                                type="number"
                                step="0.01"
                                value={
                                    scheduleS
                                }
                                onChange={(event) =>
                                    setScheduleS(
                                        event.target.value
                                    )
                                }
                                placeholder="—"
                            />

                        </div>


                        <div className="fertilizer-field">

                            <label>
                                Zinc
                            </label>

                            <input
                                type="number"
                                step="0.01"
                                value={
                                    scheduleZn
                                }
                                onChange={(event) =>
                                    setScheduleZn(
                                        event.target.value
                                    )
                                }
                                placeholder="—"
                            />

                        </div>


                        <label className="fertilizer-checkbox">

                            <input
                                type="checkbox"
                                checked={
                                    reassessmentRequired
                                }
                                onChange={(event) =>
                                    setReassessmentRequired(
                                        event.target.checked
                                    )
                                }
                            />

                            <span>
                                Reassessment required before activity
                            </span>

                        </label>


                        <div className="fertilizer-field full-width">

                            <label>
                                Notes
                            </label>

                            <textarea
                                value={
                                    scheduleNotes
                                }
                                onChange={(event) =>
                                    setScheduleNotes(
                                        event.target.value
                                    )
                                }
                                rows="3"
                                placeholder="Additional schedule notes..."
                            />

                        </div>

                    </div>


                    <div className="fertilizer-schedule-context">

                        <span>
                            Weather context
                        </span>

                        <strong>
                            {
                                currentWeatherDescription
                            }
                        </strong>

                    </div>


                    <div className="fertilizer-form-footer">

                        <div>

                            <span>
                                Linked recommendation
                            </span>

                            <strong>
                                {
                                    recommendation?.id
                                        ? "Yes"
                                        : "No"
                                }
                            </strong>

                        </div>

                        <button
                            type="button"
                            className="fertilizer-primary-btn"
                            disabled={
                                savingSchedule
                            }
                            onClick={
                                handleCreateSchedule
                            }
                        >

                            {savingSchedule
                                ? "Creating..."
                                : "Create Schedule →"}

                        </button>

                    </div>

                </div>

            </section>


            {/* =================================================
                SCHEDULE HISTORY
            ================================================= */}

            <section className="fertilizer-section">

                <div className="fertilizer-section-heading">

                    <div>

                        <span>
                            05 · HISTORY
                        </span>

                        <h2>
                            Fertilizer Schedule History
                        </h2>

                    </div>

                    <div className="fertilizer-history-count">

                        {sortedSchedules.length}

                        <span>
                            records
                        </span>

                    </div>

                </div>


                <div className="fertilizer-card fertilizer-history-card">

                    {sortedSchedules.length === 0 ? (

                        <div className="fertilizer-inline-empty">

                            <span>
                                📅
                            </span>

                            <strong>
                                No fertilizer schedules returned
                            </strong>

                            <p>
                                Create a schedule above and
                                it will appear here.
                            </p>

                        </div>

                    ) : (

                        <div className="fertilizer-schedule-list">

                            {sortedSchedules.map(
                                (schedule) => (

                                    <article
                                        key={schedule.id}
                                        className="fertilizer-schedule-row"
                                    >

                                        <div className="fertilizer-schedule-date">

                                            <strong>
                                                {
                                                    formatDate(
                                                        schedule.plannedDate
                                                    )
                                                }
                                            </strong>

                                            <span>
                                                {
                                                    schedule.actionType ||
                                                    "—"
                                                }
                                            </span>

                                        </div>


                                        <div className="fertilizer-schedule-info">

                                            <strong>
                                                {
                                                    schedule.cropName ||
                                                    "—"
                                                }
                                            </strong>

                                            <span>
                                                Stage:
                                                {" "}
                                                {
                                                    schedule.stage ||
                                                    "—"
                                                }
                                            </span>

                                            <span>
                                                Product:
                                                {" "}
                                                {
                                                    schedule.productName ||
                                                    "—"
                                                }
                                            </span>

                                        </div>


                                        <div className="fertilizer-schedule-nutrients">

                                            <span>
                                                N{" "}
                                                {
                                                    safeNumber(
                                                        schedule.nitrogen
                                                    )
                                                        ? displayNumber(
                                                            schedule.nitrogen,
                                                            2
                                                        )
                                                        : "—"
                                                }
                                            </span>

                                            <span>
                                                P{" "}
                                                {
                                                    safeNumber(
                                                        schedule.phosphorus
                                                    )
                                                        ? displayNumber(
                                                            schedule.phosphorus,
                                                            2
                                                        )
                                                        : "—"
                                                }
                                            </span>

                                            <span>
                                                K{" "}
                                                {
                                                    safeNumber(
                                                        schedule.potassium
                                                    )
                                                        ? displayNumber(
                                                            schedule.potassium,
                                                            2
                                                        )
                                                        : "—"
                                                }
                                            </span>

                                        </div>


                                        <div className="fertilizer-schedule-status">

                                            <span
                                                className={`fertilizer-status-mini ${getStatusTone(
                                                    schedule.status
                                                )}`}
                                            >
                                                {
                                                    getStatusLabel(
                                                        schedule.status
                                                    )
                                                }
                                            </span>

                                            {
                                                schedule.reassessmentRequired !==
                                                null &&
                                                schedule.reassessmentRequired !==
                                                undefined && (
                                                    <small>
                                                        {
                                                            schedule.reassessmentRequired
                                                                ? "Reassessment"
                                                                : "No reassessment"
                                                        }
                                                    </small>
                                                )
                                            }

                                        </div>


                                        <div className="fertilizer-schedule-actions">

                                            {editingScheduleId ===
                                            schedule.id ? (

                                                <>

                                                    <input
                                                        type="date"
                                                        value={
                                                            editDate
                                                        }
                                                        onChange={(
                                                            event
                                                        ) =>
                                                            setEditDate(
                                                                event.target.value
                                                            )
                                                        }
                                                    />

                                                    <select
                                                        value={
                                                            editStatus
                                                        }
                                                        onChange={(
                                                            event
                                                        ) =>
                                                            setEditStatus(
                                                                event.target.value
                                                            )
                                                        }
                                                    >

                                                        <option value="PLANNED">
                                                            PLANNED
                                                        </option>

                                                        <option value="DUE">
                                                            DUE
                                                        </option>

                                                        <option value="COMPLETED">
                                                            COMPLETED
                                                        </option>

                                                        <option value="DELAYED">
                                                            DELAYED
                                                        </option>

                                                        <option value="CANCELLED">
                                                            CANCELLED
                                                        </option>

                                                        <option value="REASSESS">
                                                            REASSESS
                                                        </option>

                                                    </select>

                                                    <button
                                                        type="button"
                                                        onClick={() =>
                                                            saveScheduleEdit(
                                                                schedule
                                                            )
                                                        }
                                                    >
                                                        Save
                                                    </button>

                                                    <button
                                                        type="button"
                                                        onClick={
                                                            cancelEditSchedule
                                                        }
                                                    >
                                                        Cancel
                                                    </button>

                                                </>

                                            ) : (

                                                <>

                                                    <button
                                                        type="button"
                                                        onClick={() =>
                                                            beginEditSchedule(
                                                                schedule
                                                            )
                                                        }
                                                    >
                                                        Edit
                                                    </button>

                                                    <button
                                                        type="button"
                                                        className="danger"
                                                        disabled={
                                                            deletingScheduleId ===
                                                            schedule.id
                                                        }
                                                        onClick={() =>
                                                            handleDeleteSchedule(
                                                                schedule.id
                                                            )
                                                        }
                                                    >
                                                        {
                                                            deletingScheduleId ===
                                                            schedule.id
                                                                ? "..."
                                                                : "Delete"
                                                        }
                                                    </button>

                                                </>

                                            )}

                                        </div>

                                    </article>

                                )
                            )}

                        </div>

                    )}

                </div>

            </section>


            {/* =================================================
                REASSESSMENT
            ================================================= */}

            <section className="fertilizer-section">

                <div className="fertilizer-section-heading">

                    <div>

                        <span>
                            06 · REVIEW
                        </span>

                        <h2>
                            Reassessment Required
                        </h2>

                    </div>

                </div>


                {reassessmentSchedules.length ===
                0 ? (

                    <div className="fertilizer-card fertilizer-empty-card compact">

                        <span>
                            ✓
                        </span>

                        <div>

                            <h3>
                                No reassessment records returned
                            </h3>

                            <p>
                                The backend did not return any
                                schedules marked for reassessment.
                            </p>

                        </div>

                    </div>

                ) : (

                    <div className="fertilizer-review-grid">

                        {reassessmentSchedules.map(
                            (schedule) => (

                                <article
                                    key={schedule.id}
                                    className="fertilizer-review-card"
                                >

                                    <span className="fertilizer-card-label">
                                        {
                                            formatDate(
                                                schedule.plannedDate
                                            )
                                        }
                                    </span>

                                    <h3>
                                        {
                                            schedule.cropName ||
                                            "—"
                                        }
                                    </h3>

                                    <p>
                                        {
                                            schedule.reason ||
                                            schedule.recommendation ||
                                            "—"
                                        }
                                    </p>

                                    <small>
                                        {
                                            schedule.status ||
                                            "—"
                                        }
                                    </small>

                                </article>

                            )
                        )}

                    </div>

                )}

            </section>


            {/* =================================================
                DATA NOTE
            ================================================= */}

            <section className="fertilizer-data-note">

                <div className="fertilizer-data-note-icon">
                    ✓
                </div>

                <div>

                    <strong>
                        Real backend fertilizer intelligence
                    </strong>

                    <p>
                        Fertilizer recommendations and schedules
                        are loaded from the AgriNexus backend.
                        The page does not calculate or invent missing
                        nutrient quantities. When the backend cannot
                        support a usable recommendation, the schedule
                        action remains disabled.
                    </p>

                </div>

            </section>

        </div>
    );
}


/* =========================================================
   SMALL COMPONENTS
   ========================================================= */

function NutrientBox({
                         name,
                         title,
                         value,
                     }) {

    return (
        <div className="fertilizer-nutrient-box">

            <span>
                {name}
            </span>

            <strong>
                {
                    safeNumber(value)
                        ? displayNumber(
                            value,
                            2
                        )
                        : "—"
                }
            </strong>

            <small>
                {title}
            </small>

        </div>
    );
}


function MetaRow({
                     label,
                     value,
                 }) {

    return (
        <div className="fertilizer-meta-row">

            <span>
                {label}
            </span>

            <strong>
                {value || "—"}
            </strong>

        </div>
    );
}
