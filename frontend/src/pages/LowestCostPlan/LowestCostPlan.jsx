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
    useFields,
} from "../../context/FieldsContext";

import {
    evaluateLowestCostSafePlan,
} from "../../api/lowestCostPlanApi";

import {
    getCurrentFarmId,
} from "../../api/apiClient";

import {
    getLatestSoilSample,
    getSoilIntelligence,
} from "../../api/cropSoilApi";

import BottomNavBar from "../../components/BottomNavBar";

import "./LowestCostPlan.css";


function LowestCostPlan() {

    const {
        language,
    } = useLanguage();

    const navigate =
        useNavigate();

    const {
        currentField,
    } = useFields();

    const isHindi =
        language === "hi";


    /*
     * =========================================================
     * CURRENT REAL FARM
     * =========================================================
     */

    const currentFarmId =
        currentField?.id ??
        getCurrentFarmId() ??
        null;


    /*
     * =========================================================
     * BACKEND OBJECT NORMALIZER
     * =========================================================
     *
     * Accept:
     *
     *   { ... }
     *
     * or:
     *
     *   { data: { ... } }
     *
     * Never creates values.
     */

    const normalizeBackendObject = (
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
     * =========================================================
     * FARM
     * =========================================================
     */

    const [
        farm,
        setFarm,
    ] = useState(
        currentField || null
    );


    /*
     * =========================================================
     * REAL SOIL DATA
     * =========================================================
     */

    const [
        soil,
        setSoil,
    ] = useState(null);


    const [
        soilSample,
        setSoilSample,
    ] = useState(null);


    const [
        soilLoading,
        setSoilLoading,
    ] = useState(false);


    /*
     * =========================================================
     * USER PLANNING INPUTS
     * =========================================================
     */

    const [
        targetSeason,
        setTargetSeason,
    ] = useState("");


    const [
        farmerConstraints,
        setFarmerConstraints,
    ] = useState([
        "LOW_COST",
    ]);


    const [
        requireSoilCompatibility,
        setRequireSoilCompatibility,
    ] = useState(true);


    const [
        availableWaterMm,
        setAvailableWaterMm,
    ] = useState("");


    /*
     * =========================================================
     * RESULT
     * =========================================================
     */

    const [
        result,
        setResult,
    ] = useState(null);


    const [
        loading,
        setLoading,
    ] = useState(false);


    const [
        error,
        setError,
    ] = useState("");


    /*
     * =========================================================
     * UPDATE FARM
     * =========================================================
     */

    useEffect(() => {

        if (
            currentField
        ) {

            setFarm(
                currentField
            );
        }

    }, [
        currentField,
    ]);


    /*
     * =========================================================
     * LOAD REAL SOIL DATA
     * =========================================================
     */

    useEffect(() => {

        let cancelled =
            false;


        async function loadSoil() {

            if (
                !currentFarmId
            ) {

                setSoil(null);
                setSoilSample(null);

                return;
            }


            setSoilLoading(true);


            try {

                const [
                    soilResult,
                    sampleResult,
                ] = await Promise.allSettled([

                    getSoilIntelligence(
                        currentFarmId
                    ),

                    getLatestSoilSample(
                        currentFarmId
                    ),

                ]);


                if (
                    cancelled
                ) {

                    return;
                }


                /*
                 * REAL SOIL INTELLIGENCE
                 */

                if (
                    soilResult.status ===
                    "fulfilled"
                ) {

                    const normalizedSoil =
                        normalizeBackendObject(
                            soilResult.value
                        );


                    console.log(
                        "REAL SOIL INTELLIGENCE:",
                        normalizedSoil
                    );


                    setSoil(
                        normalizedSoil
                    );

                } else {

                    console.warn(
                        "Real soil intelligence unavailable:",
                        soilResult.reason
                    );


                    setSoil(null);
                }


                /*
                 * REAL LATEST SOIL SAMPLE
                 */

                if (
                    sampleResult.status ===
                    "fulfilled"
                ) {

                    const normalizedSample =
                        normalizeBackendObject(
                            sampleResult.value
                        );


                    console.log(
                        "REAL LATEST SOIL SAMPLE:",
                        normalizedSample
                    );


                    setSoilSample(
                        normalizedSample
                    );

                } else {

                    console.warn(
                        "Real latest soil sample unavailable:",
                        sampleResult.reason
                    );


                    setSoilSample(null);
                }

            } catch (
                soilError
                ) {

                console.error(
                    "Failed to load real soil data:",
                    soilError
                );


                if (
                    !cancelled
                ) {

                    setSoil(null);
                    setSoilSample(null);
                }

            } finally {

                if (
                    !cancelled
                ) {

                    setSoilLoading(false);
                }
            }
        }


        void loadSoil();


        return () => {

            cancelled =
                true;
        };

    }, [
        currentFarmId,
    ]);


    /*
     * =========================================================
     * DERIVED REAL SOIL VALUES
     * =========================================================
     *
     * Soil Intelligence has priority.
     * Latest Soil Sample is fallback.
     *
     * Never creates missing values.
     */

    const soilPh =
        soil?.pH ??
        soilSample?.pH ??
        null;


    const soilType =
        soil?.soilType ??
        soilSample?.soilType ??
        null;


    const nitrogen =
        soil?.nitrogen ??
        soilSample?.nitrogen ??
        null;


    const phosphorus =
        soil?.phosphorus ??
        soilSample?.phosphorus ??
        null;


    const potassium =
        soil?.potassium ??
        soilSample?.potassium ??
        null;


    const organicCarbon =
        soil?.organicCarbon ??
        soilSample?.organicCarbon ??
        null;


    /*
     * =========================================================
     * TOGGLE CONSTRAINT
     * =========================================================
     */

    const toggleFarmerConstraint = (
        constraint
    ) => {

        setFarmerConstraints(
            (current) => {

                if (
                    current.includes(
                        constraint
                    )
                ) {

                    return current.filter(
                        (item) =>
                            item !==
                            constraint
                    );
                }


                return [
                    ...current,
                    constraint,
                ];
            }
        );
    };


    /*
     * =========================================================
     * EVALUATE PLAN
     * =========================================================
     */

    const evaluatePlan = async () => {

        try {

            setError("");
            setResult(null);


            /*
             * REAL FARM REQUIRED
             */

            if (
                !currentFarmId
            ) {

                setError(
                    isHindi
                        ? "कृपया पहले वास्तविक खेत चुनें।"
                        : "Please select a real farm first."
                );

                return;
            }


            /*
             * REAL SOIL DATA REQUIRED
             */

            const hasRealSoilData =
                soilPh !== null ||
                soilType !== null ||
                nitrogen !== null ||
                phosphorus !== null ||
                potassium !== null ||
                organicCarbon !== null;


            if (
                !hasRealSoilData
            ) {

                setError(
                    isHindi
                        ? "इस खेत के लिए वास्तविक मिट्टी डेटा उपलब्ध नहीं है। पहले मिट्टी डेटा जोड़ें।"
                        : "Real soil data is not available for this farm. Please add soil data first."
                );

                return;
            }


            /*
             * TARGET SEASON REQUIRED
             */

            if (
                !targetSeason
            ) {

                setError(
                    isHindi
                        ? "कृपया target season चुनें।"
                        : "Please select a target season."
                );

                return;
            }


            /*
             * AT LEAST ONE USER CONSTRAINT
             */

            if (
                farmerConstraints.length === 0
            ) {

                setError(
                    isHindi
                        ? "कम से कम एक प्राथमिकता चुनें।"
                        : "Please select at least one priority."
                );

                return;
            }


            /*
             * WATER VALIDATION
             */

            if (
                availableWaterMm !== "" &&
                (
                    Number.isNaN(
                        Number(
                            availableWaterMm
                        )
                    ) ||
                    Number(
                        availableWaterMm
                    ) < 0
                )
            ) {

                setError(
                    isHindi
                        ? "कृपया सही पानी बजट दर्ज करें।"
                        : "Please enter a valid water budget."
                );

                return;
            }


            setLoading(true);


            /*
             * =================================================
             * REAL BACKEND REQUEST
             * =================================================
             */

            const request = {

                farmId:
                currentFarmId,

                preferredCropId:
                    null,

                targetSeason:
                targetSeason,

                farmerConstraint:
                    farmerConstraints.join(
                        ","
                    ),

                requireSoilCompatibility:
                requireSoilCompatibility,

                soilPh:
                soilPh,

                availableWaterMm:
                    availableWaterMm.trim() === ""
                        ? null
                        : Number(
                            availableWaterMm
                        ),
            };


            console.log(
                "LOWEST COST REAL REQUEST:",
                request
            );


            const backendResponse =
                await evaluateLowestCostSafePlan(
                    request
                );


            const normalizedResponse =
                normalizeBackendObject(
                    backendResponse
                );


            console.log(
                "LOWEST COST BACKEND RESPONSE:",
                normalizedResponse
            );


            /*
             * =================================================
             * BLOCK ONLY EXPLICIT TEST/DEMO RESPONSES
             * =================================================
             *
             * ESTIMATED / CALCULATED real backend data is allowed.
             */

            const responseDataStatus =
                String(
                    normalizedResponse?.dataStatus ||
                    ""
                ).toUpperCase();


            const responseSource =
                String(
                    normalizedResponse?.source ||
                    ""
                ).toUpperCase();


            if (
                responseDataStatus ===
                "TEST" ||
                responseSource.includes(
                    "TEST"
                ) ||
                responseSource.includes(
                    "DEMO"
                )
            ) {

                setError(
                    isHindi
                        ? "Backend ने test/demo recommendation लौटाई है। इसे प्रदर्शित नहीं किया जाएगा।"
                        : "The backend returned a test/demo recommendation. It will not be displayed."
                );

                return;
            }


            /*
             * Backend response is the only result source.
             */

            setResult(
                normalizedResponse
            );

        } catch (
            requestError
            ) {

            console.error(
                "Lowest cost evaluation failed:",
                requestError
            );


            setResult(null);


            setError(
                requestError?.message ||
                (
                    isHindi
                        ? "फसल सिफारिश प्राप्त नहीं हो सकी।"
                        : "Unable to generate crop recommendation."
                )
            );

        } finally {

            setLoading(false);
        }
    };


    /*
     * =========================================================
     * FORMATTING
     * =========================================================
     */

    const money = (
        value
    ) => {

        if (
            value === null ||
            value === undefined ||
            Number.isNaN(
                Number(value)
            )
        ) {

            return "—";
        }


        return `₹${Number(
            value
        ).toLocaleString(
            "en-IN"
        )}`;
    };


    const number = (
        value,
        digits = 1
    ) => {

        if (
            value === null ||
            value === undefined ||
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


    const display = (
        value
    ) => {

        if (
            value === null ||
            value === undefined ||
            value === ""
        ) {

            return "—";
        }


        return value;
    };


    const compatibilityText = (
        value
    ) => {

        if (
            value === true
        ) {

            return isHindi
                ? "हाँ"
                : "Yes";
        }


        if (
            value === false
        ) {

            return isHindi
                ? "नहीं"
                : "No";
        }


        return "—";
    };


    const formatDate = (
        value
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


    const constraintLabel = (
        constraint
    ) => {

        switch (
            constraint
            ) {

            case "LOW_COST":

                return isHindi
                    ? "💰 कम लागत"
                    : "💰 Low Cost";


            case "LOW_WATER":

                return isHindi
                    ? "💧 कम पानी"
                    : "💧 Low Water";


            case "LOW_ENERGY":

                return isHindi
                    ? "⚡ कम ऊर्जा"
                    : "⚡ Low Energy";


            case "SHORT_DURATION":

                return isHindi
                    ? "⏱ कम अवधि"
                    : "⏱ Short Duration";


            default:

                return constraint;
        }
    };


    const nutrientStatusClass = (
        status
    ) => {

        const value =
            String(
                status ||
                ""
            ).toUpperCase();


        if (
            value.includes(
                "LOW"
            ) ||
            value.includes(
                "DEFICIENT"
            )
        ) {

            return "nutrient-low";
        }


        if (
            value.includes(
                "HIGH"
            ) ||
            value.includes(
                "EXCESS"
            )
        ) {

            return "nutrient-high";
        }


        if (
            value.includes(
                "SUFFICIENT"
            ) ||
            value.includes(
                "NORMAL"
            )
        ) {

            return "nutrient-good";
        }


        return "";
    };


    /*
     * =========================================================
     * LOADING SCREEN
     * =========================================================
     */

    if (
        loading
    ) {

        return (

            <div className="lowest-plan-page">

                <div className="lowest-plan-loading">

                    <div className="lowest-plan-loading-icon">
                        🌱
                    </div>


                    <h2>

                        {
                            isHindi
                                ? "फसल का विश्लेषण हो रहा है..."
                                : "Analyzing crop options..."
                        }

                    </h2>


                    <p>

                        {
                            isHindi
                                ? "वास्तविक मिट्टी, पानी, लागत और आपकी प्राथमिकताओं की जाँच की जा रही है।"
                                : "Checking real soil, water, cost and your selected priorities."
                        }

                    </p>

                </div>


                <BottomNavBar />

            </div>
        );
    }


    /*
     * =========================================================
     * MAIN PAGE
     * =========================================================
     */

    return (

        <div className="lowest-plan-page">


            {/* =================================================
                HEADER
            ================================================= */}

            <div className="lowest-plan-header">

                <button
                    type="button"
                    className="lowest-plan-back"
                    onClick={() =>
                        navigate(
                            "/home"
                        )
                    }
                >
                    ←
                </button>


                <div>

                    <h1>

                        {
                            isHindi
                                ? "अगली फसल सुझाव"
                                : "Next Crop Recommendation"
                        }

                    </h1>


                    <p>

                        {
                            isHindi
                                ? "कम लागत और सुरक्षित फसल योजना"
                                : "Low-cost and safe crop planning"
                        }

                    </p>

                </div>

            </div>


            {/* =================================================
                ERROR
            ================================================= */}

            {
                error && (

                    <div className="lowest-plan-error">

                        ⚠️{" "}

                        {
                            error
                        }

                    </div>
                )
            }


            {/* =================================================
                SELECTED FARM
            ================================================= */}

            <section className="lowest-plan-section">

                <h2>

                    {
                        isHindi
                            ? "चयनित खेत"
                            : "Selected Farm"
                    }

                </h2>


                <div className="lowest-plan-card">

                    <strong>

                        {
                            farm?.name ||
                            "—"
                        }

                    </strong>


                    <p>

                        {
                            farm?.location?.name ||
                            farm?.location ||
                            "—"
                        }

                    </p>


                    <small>

                        Farm ID:{" "}

                        {
                            currentFarmId ||
                            "—"
                        }

                    </small>

                </div>

            </section>


            {/* =================================================
                REAL SOIL INFORMATION
            ================================================= */}

            <section className="lowest-plan-section">

                <h2>

                    {
                        isHindi
                            ? "वर्तमान मिट्टी"
                            : "Current Soil Information"
                    }

                </h2>


                <div className="plan-summary-grid">


                    <div className="plan-summary-card">

                        <span>
                            🧪 Soil pH
                        </span>


                        <strong>

                            {
                                soilLoading
                                    ? "Loading..."
                                    : display(
                                        soilPh
                                    )
                            }

                        </strong>

                    </div>


                    <div className="plan-summary-card">

                        <span>
                            🌍 Soil Type
                        </span>


                        <strong>

                            {
                                soilLoading
                                    ? "Loading..."
                                    : display(
                                        soilType
                                    )
                            }

                        </strong>

                    </div>


                    <div className="plan-summary-card">

                        <span>
                            🌱 Nitrogen
                        </span>


                        <strong>

                            {
                                display(
                                    nitrogen
                                )
                            }

                        </strong>

                    </div>


                    <div className="plan-summary-card">

                        <span>
                            🌿 Phosphorus
                        </span>


                        <strong>

                            {
                                display(
                                    phosphorus
                                )
                            }

                        </strong>

                    </div>


                    <div className="plan-summary-card">

                        <span>
                            🥬 Potassium
                        </span>


                        <strong>

                            {
                                display(
                                    potassium
                                )
                            }

                        </strong>

                    </div>


                    <div className="plan-summary-card">

                        <span>
                            🍂 Organic Carbon
                        </span>


                        <strong>

                            {
                                display(
                                    organicCarbon
                                )
                            }

                        </strong>

                    </div>

                </div>

            </section>


            {/* =================================================
                USER PREFERENCES
            ================================================= */}

            <section className="lowest-plan-section">

                <h2>

                    {
                        isHindi
                            ? "आपकी प्राथमिकता"
                            : "Your Preference"
                    }

                </h2>


                <div className="lowest-plan-card">


                    {/* TARGET SEASON */}

                    <label>

                        {
                            isHindi
                                ? "सीजन"
                                : "Target Season"
                        }

                    </label>


                    <div className="choice-row">


                        <button
                            type="button"
                            className={
                                targetSeason ===
                                "RABI"
                                    ? "choice-button active"
                                    : "choice-button"
                            }
                            onClick={() =>
                                setTargetSeason(
                                    "RABI"
                                )
                            }
                        >
                            🌾 Rabi
                        </button>


                        <button
                            type="button"
                            className={
                                targetSeason ===
                                "KHARIF"
                                    ? "choice-button active"
                                    : "choice-button"
                            }
                            onClick={() =>
                                setTargetSeason(
                                    "KHARIF"
                                )
                            }
                        >
                            🌱 Kharif
                        </button>


                        <button
                            type="button"
                            className={
                                targetSeason ===
                                "SUMMER"
                                    ? "choice-button active"
                                    : "choice-button"
                            }
                            onClick={() =>
                                setTargetSeason(
                                    "SUMMER"
                                )
                            }
                        >
                            ☀️ Summer
                        </button>

                    </div>


                    {/* PRIORITIES */}

                    <label>

                        {
                            isHindi
                                ? "एक या अधिक प्राथमिकताएं चुनें"
                                : "Select one or more priorities"
                        }

                    </label>


                    <div className="choice-grid">

                        {[
                            "LOW_COST",
                            "LOW_WATER",
                            "LOW_ENERGY",
                            "SHORT_DURATION",
                        ].map(
                            (
                                constraint
                            ) => (

                                <button
                                    key={
                                        constraint
                                    }
                                    type="button"
                                    className={
                                        farmerConstraints.includes(
                                            constraint
                                        )
                                            ? "choice-button active"
                                            : "choice-button"
                                    }
                                    onClick={() =>
                                        toggleFarmerConstraint(
                                            constraint
                                        )
                                    }
                                >

                                    {
                                        constraintLabel(
                                            constraint
                                        )
                                    }

                                </button>
                            )
                        )}

                    </div>


                    {/* SELECTED */}

                    <div className="selected-constraints">

                        <strong>

                            {
                                isHindi
                                    ? "चयनित:"
                                    : "Selected:"
                            }

                        </strong>


                        {
                            farmerConstraints.map(
                                (
                                    constraint
                                ) => (

                                    <span
                                        key={
                                            constraint
                                        }
                                        className="selected-constraint"
                                    >

                                        {
                                            constraintLabel(
                                                constraint
                                            )
                                        }

                                    </span>

                                )
                            )
                        }

                    </div>


                    {/* WATER */}

                    <label>

                        {
                            isHindi
                                ? "उपलब्ध पानी बजट (वैकल्पिक)"
                                : "Available Water Budget (optional)"
                        }

                    </label>


                    <input
                        type="number"
                        min="0"
                        className="lowest-plan-input"
                        placeholder={
                            isHindi
                                ? "पानी का बजट mm में"
                                : "Water budget in mm"
                        }
                        value={
                            availableWaterMm
                        }
                        onChange={(
                            event
                        ) =>
                            setAvailableWaterMm(
                                event.target.value
                            )
                        }
                    />


                    {/* SOIL COMPATIBILITY */}

                    <label className="soil-check-row">

                        <input
                            type="checkbox"
                            checked={
                                requireSoilCompatibility
                            }
                            onChange={(
                                event
                            ) =>
                                setRequireSoilCompatibility(
                                    event.target.checked
                                )
                            }
                        />


                        <span>

                            {
                                isHindi
                                    ? "मिट्टी की अनुकूलता जरूरी है"
                                    : "Require soil compatibility"
                            }

                        </span>

                    </label>


                    {/* EVALUATE */}

                    <button
                        type="button"
                        className="evaluate-plan-button"
                        onClick={
                            evaluatePlan
                        }
                    >

                        🌾{" "}

                        {
                            isHindi
                                ? "फसल सुझाएं"
                                : "Recommend Crop"
                        }

                    </button>

                </div>

            </section>


            {/* =================================================
                RESULT
            ================================================= */}

            {
                result && (

                    <>

                        {/* =================================================
                            BEST OPTION
                        ================================================= */}

                        <section className="lowest-plan-section">

                            <h2>

                                {
                                    isHindi
                                        ? "सबसे अच्छा विकल्प"
                                        : "Best Option"
                                }

                            </h2>


                            <div className="best-crop-card">

                                <div className="best-crop-icon">
                                    🌾
                                </div>


                                <div className="best-crop-content">

                                    <span>

                                        {
                                            isHindi
                                                ? "सुझाई गई फसल"
                                                : "Recommended Crop"
                                        }

                                    </span>


                                    <h3>

                                        {
                                            display(
                                                result.recommendedCrop
                                            )
                                        }

                                    </h3>


                                    <strong>

                                        {
                                            result.recommendedCrop &&
                                            result.recommendationScore !==
                                            null &&
                                            result.recommendationScore !==
                                            undefined
                                                ? number(
                                                    result.recommendationScore,
                                                    1
                                                )
                                                : "—"
                                        }

                                    </strong>


                                    <small>

                                        {
                                            isHindi
                                                ? "सिफारिश स्कोर"
                                                : "Recommendation Score"
                                        }

                                    </small>

                                </div>

                            </div>

                        </section>


                        {/* =================================================
                            PLAN SUMMARY
                        ================================================= */}

                        <section className="lowest-plan-section">

                            <h2>

                                {
                                    isHindi
                                        ? "योजना सारांश"
                                        : "Plan Summary"
                                }

                            </h2>


                            <div className="plan-summary-grid">

                                <div className="plan-summary-card">

                                    <span>
                                        💰 Total Cost
                                    </span>


                                    <strong>

                                        {
                                            money(
                                                result.estimatedTotalCostInrPerHa
                                            )
                                        }

                                    </strong>


                                    <small>
                                        / ha
                                    </small>

                                </div>


                                <div className="plan-summary-card">

                                    <span>
                                        🧪 Input Cost
                                    </span>


                                    <strong>

                                        {
                                            money(
                                                result.estimatedInputCostInrPerHa
                                            )
                                        }

                                    </strong>


                                    <small>
                                        / ha
                                    </small>

                                </div>


                                <div className="plan-summary-card">

                                    <span>
                                        ⚡ Energy Cost
                                    </span>


                                    <strong>

                                        {
                                            money(
                                                result.estimatedEnergyFuelCostInrPerHa
                                            )
                                        }

                                    </strong>


                                    <small>
                                        / ha
                                    </small>

                                </div>


                                <div className="plan-summary-card">

                                    <span>
                                        🌱 Fertilizer/Pesticide
                                    </span>


                                    <strong>

                                        {
                                            money(
                                                result.estimatedFertilizerPesticideCostInrPerHa
                                            )
                                        }

                                    </strong>


                                    <small>
                                        / ha
                                    </small>

                                </div>

                            </div>

                        </section>


                        {/* =================================================
                            WATER & DURATION
                        ================================================= */}

                        <section className="lowest-plan-section">

                            <h2>

                                {
                                    isHindi
                                        ? "पानी और अवधि"
                                        : "Water & Duration"
                                }

                            </h2>


                            <div className="plan-summary-grid">

                                <div className="plan-summary-card">

                                    <span>
                                        💧 Water Demand
                                    </span>


                                    <strong>

                                        {
                                            display(
                                                result.waterDemandClass
                                            )
                                        }

                                    </strong>

                                </div>


                                <div className="plan-summary-card">

                                    <span>
                                        🌧 Minimum Water
                                    </span>


                                    <strong>

                                        {
                                            number(
                                                result.waterRequirementMmMin
                                            )
                                        }{" "}
                                        mm

                                    </strong>

                                </div>


                                <div className="plan-summary-card">

                                    <span>
                                        🌧 Maximum Water
                                    </span>


                                    <strong>

                                        {
                                            number(
                                                result.waterRequirementMmMax
                                            )
                                        }{" "}
                                        mm

                                    </strong>

                                </div>


                                <div className="plan-summary-card">

                                    <span>
                                        ⏱ Duration
                                    </span>


                                    <strong>

                                        {
                                            result.cropDurationDaysMin ??
                                            "—"
                                        }

                                        {" - "}

                                        {
                                            result.cropDurationDaysMax ??
                                            "—"
                                        }

                                        {" days"}

                                    </strong>

                                </div>

                            </div>

                        </section>


                        {/* =================================================
                            SOIL & SAFETY
                        ================================================= */}

                        <section className="lowest-plan-section">

                            <h2>

                                {
                                    isHindi
                                        ? "मिट्टी और सुरक्षा"
                                        : "Soil & Safety"
                                }

                            </h2>


                            <div className="lowest-plan-info-card">

                                <div className="info-row">

                                    <span>
                                        🧪 Soil pH
                                    </span>


                                    <strong>

                                        {
                                            display(
                                                result.soilPh
                                            )
                                        }

                                    </strong>

                                </div>


                                <div className="info-row">

                                    <span>
                                        🌍 Soil Type
                                    </span>


                                    <strong>

                                        {
                                            display(
                                                result.soilType
                                            )
                                        }

                                    </strong>

                                </div>


                                <div className="info-row">

                                    <span>
                                        🛡 Safety Tradeoff
                                    </span>


                                    <strong>

                                        {
                                            display(
                                                result.safetyTradeoff
                                            )
                                        }

                                    </strong>

                                </div>

                            </div>

                        </section>


                        {/* =================================================
                            WHY THIS CROP
                        ================================================= */}

                        <section className="lowest-plan-section">

                            <h2>

                                {
                                    isHindi
                                        ? "क्यों यह फसल?"
                                        : "Why This Crop?"
                                }

                            </h2>


                            <div className="lowest-plan-text-card">

                                <p>

                                    {
                                        display(
                                            result.reason
                                        )
                                    }

                                </p>

                            </div>

                        </section>


                        {/* =================================================
                            RECOMMENDATION
                        ================================================= */}

                        {
                            result.recommendation && (

                                <section className="lowest-plan-section">

                                    <h2>

                                        {
                                            isHindi
                                                ? "सिफारिश"
                                                : "Recommendation"
                                        }

                                    </h2>


                                    <div className="recommendation-card">

                                        <div>
                                            🌱
                                        </div>


                                        <p>

                                            {
                                                result.recommendation
                                            }

                                        </p>

                                    </div>

                                </section>

                            )
                        }


                        {/* =================================================
                            RANKED OPTIONS
                        ================================================= */}

                        <section className="lowest-plan-section">

                            <h2>

                                {
                                    isHindi
                                        ? "रैंक किए गए विकल्प"
                                        : "Ranked Crop Options"
                                }

                            </h2>


                            {
                                Array.isArray(
                                    result.rankedOptions
                                ) &&
                                result.rankedOptions.length >
                                0 ? (

                                    <div className="ranked-options">

                                        {
                                            result.rankedOptions.map(
                                                (
                                                    option,
                                                    index
                                                ) => (

                                                    <div
                                                        className="ranked-option-card"
                                                        key={
                                                            option?.cropId ||
                                                            `${option?.cropName || "crop"}-${index}`
                                                        }
                                                    >

                                                        <div>

                                                            <h3>

                                                                🌾{" "}

                                                                {
                                                                    display(
                                                                        option?.cropName
                                                                    )
                                                                }

                                                            </h3>


                                                            <p>

                                                                {
                                                                    display(
                                                                        option?.category
                                                                    )
                                                                }

                                                            </p>

                                                        </div>


                                                        <strong>

                                                            {
                                                                option?.recommendationScore !==
                                                                null &&
                                                                option?.recommendationScore !==
                                                                undefined

                                                                    ? number(
                                                                        option.recommendationScore,
                                                                        1
                                                                    )

                                                                    : "—"
                                                            }

                                                        </strong>


                                                        <div className="ranked-option-details">

                                                            <span>

                                                                💰{" "}

                                                                {
                                                                    money(
                                                                        option?.totalCostInrPerHa
                                                                    )
                                                                }

                                                            </span>


                                                            <span>

                                                                💧{" "}

                                                                {
                                                                    number(
                                                                        option?.waterRequirementMmMin
                                                                    )
                                                                }

                                                                {" - "}

                                                                {
                                                                    number(
                                                                        option?.waterRequirementMmMax
                                                                    )
                                                                }

                                                                {" mm"}

                                                            </span>


                                                            <span>

                                                                ⏱{" "}

                                                                {
                                                                    option?.durationDaysMin ??
                                                                    "—"
                                                                }

                                                                {" - "}

                                                                {
                                                                    option?.durationDaysMax ??
                                                                    "—"
                                                                }

                                                                {" days"}

                                                            </span>


                                                            <span>

                                                                🌍 Soil:{" "}

                                                                {
                                                                    compatibilityText(
                                                                        option?.soilCompatible
                                                                    )
                                                                }

                                                            </span>


                                                            <span>

                                                                💧 Water:{" "}

                                                                {
                                                                    compatibilityText(
                                                                        option?.waterCompatible
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

                                    <div className="lowest-plan-text-card">

                                        <p>

                                            {
                                                isHindi
                                                    ? "Backend ने कोई वैकल्पिक crop option नहीं लौटाया।"
                                                    : "No alternative crop options were returned by the backend."
                                            }

                                        </p>

                                    </div>

                                )
                            }

                        </section>


                        {/* =================================================
                            DATA STATUS
                        ================================================= */}

                        <section className="lowest-plan-disclaimer">

                            <strong>

                                {
                                    isHindi
                                        ? "डेटा स्थिति"
                                        : "Data Status"
                                }

                            </strong>


                            <span>

                                {
                                    display(
                                        result.dataStatus
                                    )
                                }

                            </span>


                            <p>

                                Source:{" "}

                                {
                                    display(
                                        result.source
                                    )
                                }

                            </p>


                            {
                                result.uncertaintyNote && (

                                    <p>

                                        {
                                            result.uncertaintyNote
                                        }

                                    </p>

                                )
                            }

                        </section>

                    </>
                )
            }


            <BottomNavBar />

        </div>
    );
}


export default LowestCostPlan;