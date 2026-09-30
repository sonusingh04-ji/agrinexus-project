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

import BottomNavBar from "../../components/BottomNavBar";

import {
    apiRequest,
} from "../../api/apiClient";

import "./FieldHistory.css";


function History() {

    const {
        language,
    } = useLanguage();

    const navigate =
        useNavigate();

    const {
        fields = [],
    } = useFields();

    const isHindi =
        language === "hi";


    const [
        selectedFieldId,
        setSelectedFieldId,
    ] = useState(null);


    const [
        cropHistory,
        setCropHistory,
    ] = useState([]);


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
     * Select first real field
     * =========================================================
     */

    useEffect(() => {

        if (
            fields.length > 0 &&
            (
                selectedFieldId === null ||
                !fields.some(
                    (field) =>
                        String(field.id) ===
                        String(selectedFieldId)
                )
            )
        ) {

            setSelectedFieldId(
                fields[0].id
            );
        }

    }, [
        fields,
        selectedFieldId,
    ]);


    /*
     * =========================================================
     * Load REAL crop history from backend
     * =========================================================
     */

    useEffect(() => {

        if (!selectedFieldId) {

            setCropHistory([]);

            return;
        }


        let cancelled = false;


        async function loadCropHistory() {

            setLoading(true);
            setError("");


            try {

                const response =
                    await apiRequest(
                        `/api/crop-history/farm/${selectedFieldId}`
                    );


                if (cancelled) {
                    return;
                }


                if (
                    Array.isArray(response)
                ) {

                    setCropHistory(
                        response
                    );

                } else {

                    setCropHistory([]);

                }

            } catch (requestError) {

                if (!cancelled) {

                    console.error(
                        "Failed to load crop history:",
                        requestError
                    );


                    setError(
                        requestError?.message ||
                        (
                            isHindi
                                ? "फसल इतिहास लोड नहीं हो सका।"
                                : "Unable to load crop history."
                        )
                    );


                    setCropHistory([]);
                }

            } finally {

                if (!cancelled) {

                    setLoading(false);
                }
            }
        }


        loadCropHistory();


        return () => {

            cancelled = true;
        };

    }, [
        selectedFieldId,
        isHindi,
    ]);


    /*
     * =========================================================
     * Date formatter
     * =========================================================
     */

    const formatDate = (value) => {

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
            isHindi
                ? "hi-IN"
                : "en-IN",
            {
                day: "numeric",
                month: "short",
                year: "numeric",
            }
        );
    };


    /*
     * =========================================================
     * Open actual crop-history record
     * =========================================================
     */

    const openCropHistory = (
        history
    ) => {

        if (!history?.id) {

            return;
        }


        navigate(
            `/crop-history/${history.id}`,
            {
                state: {
                    cropHistory: history,
                },
            }
        );
    };


    /*
     * =========================================================
     * No fields
     * =========================================================
     */

    if (
        !fields ||
        fields.length === 0
    ) {

        return (

            <div className="history-container">

                <h1 className="history-title">

                    {
                        isHindi
                            ? "फसल इतिहास"
                            : "Crop History"
                    }

                </h1>


                <div className="history-empty">

                    {
                        isHindi
                            ? "अभी तक कोई खेत उपलब्ध नहीं है।"
                            : "No farm is available yet."
                    }

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

        <div className="history-container">

            <h1 className="history-title">

                {
                    isHindi
                        ? "फसल इतिहास"
                        : "Crop History"
                }

            </h1>


            {/* =====================================================
                FARM SELECTOR
            ====================================================== */}

            <div className="history-field-tabs">

                {fields.map(
                    (field) => (

                        <button
                            type="button"
                            key={field.id}
                            className={`history-tab ${
                                String(
                                    selectedFieldId
                                ) ===
                                String(
                                    field.id
                                )
                                    ? "history-tab-active"
                                    : ""
                            }`}
                            onClick={() =>
                                setSelectedFieldId(
                                    field.id
                                )
                            }
                        >

                            <span className="history-tab-icon">

                                {
                                    field.icon ||
                                    "🌾"
                                }

                            </span>


                            {
                                isHindi
                                    ? (
                                        field.nameHi ||
                                        field.name
                                    )
                                    : field.name
                            }

                        </button>
                    )
                )}

            </div>


            {/* =====================================================
                ERROR
            ====================================================== */}

            {error && (

                <div className="history-error">

                    ⚠️{" "}

                    {error}

                </div>

            )}


            {/* =====================================================
                LOADING
            ====================================================== */}

            {loading && (

                <div className="history-empty">

                    <div
                        style={{
                            fontSize: "42px",
                            marginBottom: "10px",
                        }}
                    >
                        🌾
                    </div>


                    {
                        isHindi
                            ? "वास्तविक crop history लोड हो रही है..."
                            : "Loading real crop history..."
                    }

                </div>

            )}


            {/* =====================================================
                EMPTY
            ====================================================== */}

            {!loading &&
                cropHistory.length === 0 && (

                    <div className="history-empty">

                        <div
                            style={{
                                fontSize: "48px",
                                marginBottom: "12px",
                            }}
                        >
                            📜
                        </div>


                        <h2>

                            {
                                isHindi
                                    ? "कोई फसल इतिहास उपलब्ध नहीं है"
                                    : "No crop history found"
                            }

                        </h2>


                        <p>

                            {
                                isHindi
                                    ? "इस खेत के लिए backend में कोई वास्तविक crop-history record उपलब्ध नहीं है।"
                                    : "No real crop-history record is currently available for this farm in the backend."
                            }

                        </p>

                    </div>

                )}


            {/* =====================================================
                REAL CROP HISTORY RECORDS
            ====================================================== */}

            {!loading &&
                cropHistory.length > 0 && (

                    <div className="history-timeline">

                        {cropHistory.map(
                            (
                                history,
                                index
                            ) => (

                                <button
                                    type="button"
                                    key={
                                        history?.id ||
                                        `${history?.cropName || "crop"}-${index}`
                                    }
                                    className="history-item"
                                    onClick={() =>
                                        openCropHistory(
                                            history
                                        )
                                    }
                                >

                                    {/* ICON */}

                                    <div className="history-item-icon">

                                        🌾

                                    </div>


                                    {/* CONTENT */}

                                    <div className="history-item-content">

                                        <div className="history-item-date">

                                            {
                                                history?.season ||
                                                "—"
                                            }

                                        </div>


                                        <div className="history-item-summary">

                                            {
                                                history?.cropName ||
                                                (
                                                    isHindi
                                                        ? "फसल"
                                                        : "Crop"
                                                )
                                            }

                                        </div>


                                        <div
                                            style={{
                                                marginTop: "6px",
                                                color: "#2e7d32",
                                                fontSize: "13px",
                                            }}
                                        >

                                            🌱{" "}

                                            {
                                                history?.cropStage ||
                                                "—"
                                            }

                                        </div>


                                        <div
                                            style={{
                                                marginTop: "4px",
                                                color: "#777",
                                                fontSize: "12px",
                                            }}
                                        >

                                            {
                                                isHindi
                                                    ? "बुवाई:"
                                                    : "Sowing:"
                                            }{" "}

                                            {
                                                formatDate(
                                                    history?.sowingDate
                                                )
                                            }

                                        </div>


                                        <div
                                            style={{
                                                marginTop: "2px",
                                                color: "#777",
                                                fontSize: "12px",
                                            }}
                                        >

                                            {
                                                isHindi
                                                    ? "कटाई:"
                                                    : "Harvest:"
                                            }{" "}

                                            {
                                                formatDate(
                                                    history?.harvestDate
                                                )
                                            }

                                        </div>


                                        {
                                            history?.residuePresent !==
                                            null &&
                                            history?.residuePresent !==
                                            undefined && (

                                                <div
                                                    style={{
                                                        marginTop: "4px",
                                                        color: "#777",
                                                        fontSize: "12px",
                                                    }}
                                                >

                                                    ♻️{" "}

                                                    {
                                                        isHindi
                                                            ? "अवशेष:"
                                                            : "Residue:"
                                                    }{" "}

                                                    {
                                                        history.residuePresent
                                                            ? (
                                                                isHindi
                                                                    ? "हां"
                                                                    : "Yes"
                                                            )
                                                            : (
                                                                isHindi
                                                                    ? "नहीं"
                                                                    : "No"
                                                            )
                                                    }

                                                </div>

                                            )
                                        }

                                    </div>


                                    {/* ARROW */}

                                    <div className="history-item-arrow">

                                        ›

                                    </div>

                                </button>

                            )
                        )}

                    </div>

                )}


            <BottomNavBar />

        </div>

    );
}


export default History;