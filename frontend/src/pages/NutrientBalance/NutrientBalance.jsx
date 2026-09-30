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
    getFarmNutrientBalances,
    getLatestNutrientBalance,
    getFarmFertilizerInputs,
} from "../../api/nutrientBalanceApi";

import {
    getCurrentCropForFarm,
} from "../../api/cropHistoryApi";

import BottomNavBar from "../../components/BottomNavBar";

import "./NutrientBalance.css";


/* =========================================================
   HELPERS
   ========================================================= */

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

    return String(value);
};


const numberValue = (
    value,
    digits = 2
) => {

    if (
        value === null ||
        value === undefined ||
        value === ""
    ) {
        return "—";
    }

    const number =
        Number(value);

    if (!Number.isFinite(number)) {
        return "—";
    }

    return number.toFixed(
        digits
    );
};


const formatDate = (
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


const getLocationText = (
    location
) => {

    if (
        location === null ||
        location === undefined ||
        location === ""
    ) {
        return "—";
    }

    if (
        typeof location ===
        "string"
    ) {
        return location;
    }

    if (
        typeof location ===
        "object"
    ) {

        return (
            location.name ??
            location.label ??
            location.address ??
            location.location ??
            "—"
        );
    }

    return String(
        location
    );
};


const safeNumber = (
    value
) => {

    const number =
        Number(value);

    if (
        value === null ||
        value === undefined ||
        value === "" ||
        !Number.isFinite(number)
    ) {
        return 0;
    }

    return number;
};


const hasValue = (
    value
) => {

    return (
        value !== null &&
        value !== undefined &&
        value !== ""
    );
};


const getStatusClass = (
    status
) => {

    const normalized =
        String(status || "")
            .toUpperCase();

    if (
        normalized ===
        "POSITIVE"
    ) {
        return "positive";
    }

    if (
        normalized ===
        "NEGATIVE"
    ) {
        return "negative";
    }

    if (
        normalized ===
        "BALANCED"
    ) {
        return "balanced";
    }

    return "unavailable";
};


/* =========================================================
   COMPONENT
   ========================================================= */

function NutrientBalance() {

    const {
        language,
    } = useLanguage();

    const navigate =
        useNavigate();

    const {
        currentField,
        loading: fieldsLoading,
    } = useFields();


    /* =====================================================
       STATE
       ===================================================== */

    const [
        latestBalance,
        setLatestBalance,
    ] = useState(null);

    const [
        balances,
        setBalances,
    ] = useState([]);

    const [
        fertilizerInputs,
        setFertilizerInputs,
    ] = useState([]);

    const [
        currentCrop,
        setCurrentCrop,
    ] = useState(null);

    const [
        loading,
        setLoading,
    ] = useState(true);

    const [
        error,
        setError,
    ] = useState("");


    /* =====================================================
       FARM
       ===================================================== */

    const farmId =
        currentField?.id ??
        null;


    /* =====================================================
       LOAD REAL DATA
       ===================================================== */

    useEffect(() => {

        let cancelled =
            false;


        async function loadData() {

            if (fieldsLoading) {
                return;
            }

            setLoading(true);
            setError("");


            if (!farmId) {

                if (!cancelled) {

                    setLatestBalance(
                        null
                    );

                    setBalances(
                        []
                    );

                    setFertilizerInputs(
                        []
                    );

                    setCurrentCrop(
                        null
                    );

                    setError(
                        language === "hi"
                            ? "कृपया पहले एक खेत चुनें।"
                            : "Please select a farm first."
                    );

                    setLoading(
                        false
                    );
                }

                return;
            }


            const [
                latestResult,
                historyResult,
                fertilizerResult,
                cropResult,
            ] =
                await Promise.allSettled([

                    getLatestNutrientBalance(
                        farmId
                    ),

                    getFarmNutrientBalances(
                        farmId
                    ),

                    getFarmFertilizerInputs(
                        farmId
                    ),

                    getCurrentCropForFarm(
                        farmId
                    ),

                ]);


            if (cancelled) {
                return;
            }


            if (
                latestResult.status ===
                "fulfilled"
            ) {

                setLatestBalance(
                    latestResult.value
                );

            } else {

                setLatestBalance(
                    null
                );
            }


            if (
                historyResult.status ===
                "fulfilled"
            ) {

                setBalances(
                    normalizeList(
                        historyResult.value
                    )
                );

            } else {

                setBalances(
                    []
                );
            }


            if (
                fertilizerResult.status ===
                "fulfilled"
            ) {

                setFertilizerInputs(
                    normalizeList(
                        fertilizerResult.value
                    )
                );

            } else {

                setFertilizerInputs(
                    []
                );
            }


            if (
                cropResult.status ===
                "fulfilled"
            ) {

                setCurrentCrop(
                    cropResult.value
                );

            } else {

                setCurrentCrop(
                    null
                );
            }


            const dataFailed =
                latestResult.status ===
                "rejected" &&
                historyResult.status ===
                "rejected" &&
                fertilizerResult.status ===
                "rejected";


            if (dataFailed) {

                setError(
                    language === "hi"
                        ? "पोषक तत्व डेटा लोड नहीं हो सका।"
                        : "Unable to load nutrient balance data."
                );
            }


            setLoading(
                false
            );
        }


        loadData();


        return () => {
            cancelled = true;
        };

    }, [
        farmId,
        fieldsLoading,
        language,
    ]);


    /* =====================================================
       REAL FERTILIZER INPUT TOTALS
       ===================================================== */

    const fertilizerTotals =
        useMemo(() => {

            return fertilizerInputs.reduce(
                (
                    totals,
                    input
                ) => {

                    totals.nitrogen +=
                        safeNumber(
                            input?.nitrogen
                        );

                    totals.phosphorus +=
                        safeNumber(
                            input?.phosphorus
                        );

                    totals.potassium +=
                        safeNumber(
                            input?.potassium
                        );

                    totals.sulphur +=
                        safeNumber(
                            input?.sulphur
                        );

                    totals.zinc +=
                        safeNumber(
                            input?.zinc
                        );

                    return totals;

                },
                {
                    nitrogen: 0,
                    phosphorus: 0,
                    potassium: 0,
                    sulphur: 0,
                    zinc: 0,
                }
            );

        }, [
            fertilizerInputs,
        ]);


    /* =====================================================
       WHETHER A TOTAL EXISTS
       ===================================================== */

    const hasFertilizerNutrients =
        useMemo(() => {

            return fertilizerInputs.some(
                (
                    input
                ) => (
                    hasValue(
                        input?.nitrogen
                    ) ||
                    hasValue(
                        input?.phosphorus
                    ) ||
                    hasValue(
                        input?.potassium
                    ) ||
                    hasValue(
                        input?.sulphur
                    ) ||
                    hasValue(
                        input?.zinc
                    )
                )
            );

        }, [
            fertilizerInputs,
        ]);


    /* =====================================================
       LATEST BALANCE
       ===================================================== */

    const activeBalance =
        latestBalance;


    /* =====================================================
       BALANCE ROWS
       ===================================================== */

    const nutrientRows =
        useMemo(() => {

            if (!activeBalance) {
                return [];
            }

            return [

                {
                    short: "N",
                    name: "Nitrogen",
                    balance:
                    activeBalance.nitrogenBalance,
                    status:
                    activeBalance.nitrogenStatus,
                },

                {
                    short: "P",
                    name: "Phosphorus",
                    balance:
                    activeBalance.phosphorusBalance,
                    status:
                    activeBalance.phosphorusStatus,
                },

                {
                    short: "K",
                    name: "Potassium",
                    balance:
                    activeBalance.potassiumBalance,
                    status:
                    activeBalance.potassiumStatus,
                },

                {
                    short: "S",
                    name: "Sulphur",
                    balance:
                    activeBalance.sulphurBalance,
                    status:
                    activeBalance.sulphurStatus,
                },

                {
                    short: "Zn",
                    name: "Zinc",
                    balance:
                    activeBalance.zincBalance,
                    status:
                    activeBalance.zincStatus,
                },

            ];

        }, [
            activeBalance,
        ]);


    /* =====================================================
       LOADING
       ===================================================== */

    if (loading) {

        return (
            <main className="nutrient-page">

                <section className="nutrient-loading">

                    <div className="nutrient-loading-spinner" />

                    <h2>
                        {language === "hi"
                            ? "पोषक तत्व डेटा लोड हो रहा है..."
                            : "Loading nutrient intelligence..."}
                    </h2>

                    <p>
                        {language === "hi"
                            ? "वास्तविक खेत रिकॉर्ड प्राप्त किए जा रहे हैं।"
                            : "Fetching real records from the AgriNexus backend."}
                    </p>

                </section>

            </main>
        );
    }


    /* =====================================================
       RENDER
       ===================================================== */

    return (
        <main className="nutrient-page">


            {/* =================================================
                HERO
            ================================================= */}

            <section className="nutrient-hero">

                <div>

                    <div className="nutrient-eyebrow">
                        AGRINEXUS FARM INTELLIGENCE
                    </div>

                    <h1>
                        Nutrient Balance
                    </h1>

                    <p>
                        Track real nutrient inputs,
                        crop uptake, residue return
                        and calculated nutrient balance.
                    </p>

                </div>


                <div className="nutrient-hero-icon">
                    🧬
                </div>

            </section>


            {/* =================================================
                FARM
            ================================================= */}

            <section className="nutrient-farm-bar">

                <div className="nutrient-farm-icon">
                    🚜
                </div>

                <div>

                    <span>
                        CURRENT FARM
                    </span>

                    <strong>
                        {textValue(
                            currentField?.name
                        )}
                    </strong>

                    <small>
                        {getLocationText(
                            currentField?.location
                        )}
                    </small>

                </div>


                <button
                    type="button"
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

                <section className="nutrient-error">

                    <span>
                        ⚠️
                    </span>

                    <span>
                        {error}
                    </span>

                </section>

            )}


            {/* =================================================
                CONTEXT
            ================================================= */}

            <section className="nutrient-context-grid">


                <article className="nutrient-context-card">

                    <span>
                        CURRENT CROP
                    </span>

                    <strong>
                        {textValue(
                            currentCrop?.cropName ??
                            currentCrop?.crop?.name
                        )}
                    </strong>

                    <small>
                        {textValue(
                            currentCrop?.season
                        )}
                    </small>

                </article>


                <article className="nutrient-context-card">

                    <span>
                        FERTILIZER INPUT RECORDS
                    </span>

                    <strong>
                        {fertilizerInputs.length}
                    </strong>

                    <small>
                        Real farm input records returned
                        by the backend.
                    </small>

                </article>


                <article className="nutrient-context-card">

                    <span>
                        LATEST BALANCE RECORD
                    </span>

                    <strong>
                        {activeBalance
                            ? "AVAILABLE"
                            : "—"}
                    </strong>

                    <small>
                        {activeBalance
                            ? textValue(
                                activeBalance.dataStatus
                            )
                            : "No backend balance record"}
                    </small>

                </article>


            </section>


            {/* =================================================
                REAL FERTILIZER INPUT EVIDENCE
            ================================================= */}

            <section className="nutrient-section">

                <div className="nutrient-section-heading">

                    <div>

                        <span>
                            REAL INPUT EVIDENCE
                        </span>

                        <h2>
                            Fertilizer Nutrient Inputs
                        </h2>

                        <p>
                            Totals below are aggregated only
                            from nutrient amounts already stored
                            in the fertilizer-input backend.
                        </p>

                    </div>


                    <div className="nutrient-record-badge">
                        BACKEND INPUT DATA
                    </div>

                </div>


                {!hasFertilizerNutrients ? (

                    <div className="nutrient-history-empty">

                        <h3>
                            No nutrient input amounts recorded
                        </h3>

                        <p>
                            Fertilizer input records may exist,
                            but nutrient quantities were not
                            returned for them.
                        </p>

                    </div>

                ) : (

                    <div className="nutrient-grid">

                        <article className="nutrient-card">

                            <div className="nutrient-card-top">

                                <span>
                                    N
                                </span>

                                <small>
                                    Nitrogen Input
                                </small>

                            </div>

                            <strong>
                                {numberValue(
                                    fertilizerTotals.nitrogen
                                )}
                            </strong>

                        </article>


                        <article className="nutrient-card">

                            <div className="nutrient-card-top">

                                <span>
                                    P
                                </span>

                                <small>
                                    Phosphorus Input
                                </small>

                            </div>

                            <strong>
                                {numberValue(
                                    fertilizerTotals.phosphorus
                                )}
                            </strong>

                        </article>


                        <article className="nutrient-card">

                            <div className="nutrient-card-top">

                                <span>
                                    K
                                </span>

                                <small>
                                    Potassium Input
                                </small>

                            </div>

                            <strong>
                                {numberValue(
                                    fertilizerTotals.potassium
                                )}
                            </strong>

                        </article>


                        <article className="nutrient-card">

                            <div className="nutrient-card-top">

                                <span>
                                    S
                                </span>

                                <small>
                                    Sulphur Input
                                </small>

                            </div>

                            <strong>
                                {numberValue(
                                    fertilizerTotals.sulphur
                                )}
                            </strong>

                        </article>


                        <article className="nutrient-card">

                            <div className="nutrient-card-top">

                                <span>
                                    Zn
                                </span>

                                <small>
                                    Zinc Input
                                </small>

                            </div>

                            <strong>
                                {numberValue(
                                    fertilizerTotals.zinc
                                )}
                            </strong>

                        </article>

                    </div>

                )}

            </section>


            {/* =================================================
                INPUT RECORD HISTORY
            ================================================= */}

            <section className="nutrient-section">

                <div className="nutrient-section-heading">

                    <div>

                        <span>
                            INPUT HISTORY
                        </span>

                        <h2>
                            Recorded Fertilizer Applications
                        </h2>

                    </div>

                </div>


                {fertilizerInputs.length === 0 ? (

                    <div className="nutrient-history-empty">

                        <h3>
                            No fertilizer input records
                        </h3>

                        <p>
                            The backend returned no fertilizer
                            input records for this farm.
                        </p>

                    </div>

                ) : (

                    <div className="nutrient-history-list">

                        {fertilizerInputs.map(
                            (
                                input,
                                index
                            ) => (

                                <article
                                    key={
                                        input?.id ??
                                        `${input?.applicationDate ?? "record"}-${index}`
                                    }
                                    className="nutrient-history-card"
                                >

                                    <div>

                                        <strong>
                                            {textValue(
                                                input?.productName
                                            )}
                                        </strong>

                                        <small>
                                            {formatDate(
                                                input?.applicationDate,
                                                language
                                            )}

                                            {" · "}

                                            {textValue(
                                                input?.inputType
                                            )}
                                        </small>

                                    </div>


                                    <div className="nutrient-history-values">

                                        <span>
                                            N{" "}
                                            {numberValue(
                                                input?.nitrogen
                                            )}
                                        </span>

                                        <span>
                                            P{" "}
                                            {numberValue(
                                                input?.phosphorus
                                            )}
                                        </span>

                                        <span>
                                            K{" "}
                                            {numberValue(
                                                input?.potassium
                                            )}
                                        </span>

                                        <span>
                                            S{" "}
                                            {numberValue(
                                                input?.sulphur
                                            )}
                                        </span>

                                        <span>
                                            Zn{" "}
                                            {numberValue(
                                                input?.zinc
                                            )}
                                        </span>

                                    </div>


                                    <span className="nutrient-history-status">

                                        {textValue(
                                            input?.source
                                        )}

                                    </span>

                                </article>

                            )
                        )}

                    </div>

                )}

            </section>


            {/* =================================================
                ACTUAL BACKEND BALANCE
            ================================================= */}

            {activeBalance ? (

                <>

                    <section className="nutrient-section">

                        <div className="nutrient-section-heading">

                            <div>

                                <span>
                                    CALCULATED BALANCE
                                </span>

                                <h2>
                                    Net Nutrient Balance
                                </h2>

                                <p>
                                    These values are shown exactly
                                    from the NutrientBalance backend
                                    record.
                                </p>

                            </div>

                            <div className="nutrient-record-badge">
                                {textValue(
                                    activeBalance.dataStatus
                                )}
                            </div>

                        </div>


                        <div className="nutrient-grid">

                            {nutrientRows.map(
                                (
                                    row
                                ) => (

                                    <article
                                        key={
                                            row.short
                                        }
                                        className={`nutrient-card ${getStatusClass(
                                            row.status
                                        )}`}
                                    >

                                        <div className="nutrient-card-top">

                                            <span>
                                                {row.short}
                                            </span>

                                            <small>
                                                {row.name}
                                            </small>

                                        </div>


                                        <strong>
                                            {numberValue(
                                                row.balance
                                            )}
                                        </strong>


                                        <div className="nutrient-status">
                                            {textValue(
                                                row.status
                                            )}
                                        </div>

                                    </article>

                                )
                            )}

                        </div>

                    </section>


                    {/* =========================================
                        ACCOUNTING COMPONENTS
                    ========================================= */}

                    <section className="nutrient-section">

                        <div className="nutrient-section-heading">

                            <div>

                                <span>
                                    ACCOUNTING COMPONENTS
                                </span>

                                <h2>
                                    How this balance was formed
                                </h2>

                            </div>

                        </div>


                        <div className="nutrient-flow-grid">


                            <article className="nutrient-flow-card">

                                <h3>
                                    Nutrient Inputs
                                </h3>

                                <div>
                                    <span>N</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.nitrogenInput
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <span>P</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.phosphorusInput
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <span>K</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.potassiumInput
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <span>S</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.sulphurInput
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <span>Zn</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.zincInput
                                        )}
                                    </strong>
                                </div>

                            </article>


                            <article className="nutrient-flow-card">

                                <h3>
                                    Crop Uptake
                                </h3>

                                <div>
                                    <span>N</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.nitrogenUptake
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <span>P</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.phosphorusUptake
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <span>K</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.potassiumUptake
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <span>S</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.sulphurUptake
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <span>Zn</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.zincUptake
                                        )}
                                    </strong>
                                </div>

                            </article>


                            <article className="nutrient-flow-card">

                                <h3>
                                    Residue Return
                                </h3>

                                <div>
                                    <span>N</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.nitrogenResidueReturn
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <span>P</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.phosphorusResidueReturn
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <span>K</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.potassiumResidueReturn
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <span>S</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.sulphurResidueReturn
                                        )}
                                    </strong>
                                </div>

                                <div>
                                    <span>Zn</span>
                                    <strong>
                                        {numberValue(
                                            activeBalance.zincResidueReturn
                                        )}
                                    </strong>
                                </div>

                            </article>


                            <article className="nutrient-flow-card">

                                <h3>
                                    Biological N Fixation
                                </h3>

                                <div className="nutrient-single-value">

                                    <span>
                                        N
                                    </span>

                                    <strong>
                                        {numberValue(
                                            activeBalance
                                                .nitrogenBiologicalFixation
                                        )}
                                    </strong>

                                </div>

                                <p>
                                    Shown only when supplied
                                    by the backend nutrient record.
                                </p>

                            </article>

                        </div>

                    </section>


                    {/* =========================================
                        PERIOD
                    ========================================= */}

                    <section className="nutrient-meta">

                        <div>
                            <span>
                                Balance Period
                            </span>

                            <strong>
                                {formatDate(
                                    activeBalance.periodStart,
                                    language
                                )}
                                {" → "}
                                {formatDate(
                                    activeBalance.periodEnd,
                                    language
                                )}
                            </strong>
                        </div>


                        <div>
                            <span>
                                Crop
                            </span>

                            <strong>
                                {textValue(
                                    activeBalance.cropName
                                )}
                            </strong>
                        </div>


                        <div>
                            <span>
                                Source
                            </span>

                            <strong>
                                {textValue(
                                    activeBalance.source
                                )}
                            </strong>
                        </div>

                    </section>

                </>

            ) : (

                /* =================================================
                   NO ACTUAL BALANCE
                ================================================= */

                <section className="nutrient-empty">

                    <div>
                        🧬
                    </div>

                    <h2>
                        No nutrient balance record
                    </h2>

                    <p>
                        The backend did not return a stored
                        NutrientBalance record for this farm.
                    </p>


                    {hasFertilizerNutrients && (

                        <div
                            style={{
                                marginTop: "26px",
                                padding: "18px",
                                borderRadius: "18px",
                                background: "#f3f9f4",
                                border: "1px solid #d8e9dc",
                            }}
                        >

                            <strong>
                                Real fertilizer input evidence is available.
                            </strong>

                            <p
                                style={{
                                    marginTop: "8px",
                                }}
                            >
                                AgriNexus has nutrient amounts from
                                recorded fertilizer inputs, but it
                                will not present them as a net nutrient
                                balance until the backend balance record
                                contains the required accounting
                                components.
                            </p>

                        </div>

                    )}

                </section>

            )}


            {/* =================================================
                HISTORY OF BALANCE RECORDS
            ================================================= */}

            <section className="nutrient-section">

                <div className="nutrient-section-heading">

                    <div>

                        <span>
                            BALANCE HISTORY
                        </span>

                        <h2>
                            Nutrient Balance History
                        </h2>

                    </div>

                    <strong>
                        {balances.length}
                    </strong>

                </div>


                {balances.length === 0 ? (

                    <div className="nutrient-history-empty">

                        <h3>
                            No historical nutrient balances
                        </h3>

                        <p>
                            No NutrientBalance records were returned
                            by the backend for this farm.
                        </p>

                    </div>

                ) : (

                    <div className="nutrient-history-list">

                        {balances.map(
                            (
                                balance,
                                index
                            ) => (

                                <article
                                    key={
                                        balance?.id ??
                                        `balance-${index}`
                                    }
                                    className="nutrient-history-card"
                                >

                                    <div>

                                        <strong>
                                            {textValue(
                                                balance?.cropName
                                            )}
                                        </strong>

                                        <small>
                                            {formatDate(
                                                balance?.periodStart,
                                                language
                                            )}
                                            {" → "}
                                            {formatDate(
                                                balance?.periodEnd,
                                                language
                                            )}
                                        </small>

                                    </div>


                                    <div className="nutrient-history-values">

                                        <span>
                                            N{" "}
                                            {numberValue(
                                                balance?.nitrogenBalance
                                            )}
                                        </span>

                                        <span>
                                            P{" "}
                                            {numberValue(
                                                balance?.phosphorusBalance
                                            )}
                                        </span>

                                        <span>
                                            K{" "}
                                            {numberValue(
                                                balance?.potassiumBalance
                                            )}
                                        </span>

                                    </div>


                                    <span className="nutrient-history-status">

                                        {textValue(
                                            balance?.dataStatus
                                        )}

                                    </span>

                                </article>

                            )
                        )}

                    </div>

                )}

            </section>


            {/* =================================================
                EXPLANATION
            ================================================= */}

            <section className="nutrient-explanation">

                <div className="nutrient-explanation-icon">
                    💡
                </div>

                <div>

                    <span>
                        AGRINEXUS ACCOUNTING RULE
                    </span>

                    <h2>
                        Real records first, calculations second
                    </h2>

                    <p>
                        Fertilizer input amounts can be shown
                        directly from recorded farm applications.
                        A net nutrient balance is shown only when
                        the backend has stored the required balance
                        components.
                    </p>

                    <small>
                        Missing nutrient values remain unavailable.
                        No soil deficiency or nutrient requirement
                        is inferred from missing records.
                    </small>

                </div>

            </section>


            {/* =================================================
                FOOTER
            ================================================= */}

            <section className="nutrient-footer-note">

                <span>
                    ✓
                </span>

                <div>

                    <strong>
                        Real backend nutrient intelligence
                    </strong>

                    <p>
                        Nutrient input evidence comes from the
                        fertilizer-input records. Net balances come
                        only from NutrientBalance records.
                    </p>

                </div>

            </section>


            <BottomNavBar />

        </main>
    );
}


export default NutrientBalance;