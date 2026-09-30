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
    getFarmCropHistory,
} from "../../api/cropHistoryApi";

import BottomNavBar from "../../components/BottomNavBar";

import "./FieldHistory.css";


function FieldHistory() {

    const {
        language,
    } = useLanguage();

    const navigate =
        useNavigate();

    const {
        fields = [],
        currentField,
        loading: fieldsLoading,
    } = useFields();

    const isHindi =
        language === "hi";


    /*
     * =========================================================
     * Selected real farm
     * =========================================================
     */

    const [
        selectedFieldId,
        setSelectedFieldId,
    ] = useState(
        currentField?.id ??
        fields[0]?.id ??
        null
    );


    /*
     * =========================================================
     * Keep selected farm synchronized
     * =========================================================
     */

    useEffect(() => {

        if (
            currentField?.id &&
            !selectedFieldId
        ) {

            setSelectedFieldId(
                currentField.id
            );

            return;
        }


        if (
            fields.length > 0 &&
            !selectedFieldId
        ) {

            setSelectedFieldId(
                fields[0].id
            );
        }

    }, [
        currentField,
        fields,
        selectedFieldId,
    ]);


    /*
     * =========================================================
     * Real crop-history state
     * =========================================================
     */

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
     * Selected field
     * =========================================================
     */

    const selectedField =
        fields.find(
            (field) =>
                String(field.id) ===
                String(selectedFieldId)
        ) ||
        null;


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
                    await getFarmCropHistory(
                        selectedFieldId
                    );


                if (cancelled) {
                    return;
                }


                let records = [];


                /*
                 * Backend may return:
                 *
                 * 1. Array
                 * 2. { content: [...] }
                 * 3. Single record
                 */

                if (
                    Array.isArray(response)
                ) {

                    records =
                        response;

                } else if (
                    Array.isArray(
                        response?.content
                    )
                ) {

                    records =
                        response.content;

                } else if (
                    response &&
                    typeof response === "object"
                ) {

                    records = [
                        response,
                    ];
                }


                /*
                 * Only backend records are used.
                 * No demo/fake history is created.
                 */

                setCropHistory(
                    records
                );

            } catch (requestError) {

                if (cancelled) {
                    return;
                }


                console.error(
                    "Failed to load crop history:",
                    requestError
                );


                setCropHistory(
                    []
                );


                setError(
                    requestError?.message ||
                    (
                        isHindi
                            ? "फसल इतिहास लोड नहीं हो सका।"
                            : "Unable to load crop history."
                    )
                );

            } finally {

                if (!cancelled) {

                    setLoading(
                        false
                    );
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
     * Sort newest crop first
     * =========================================================
     */

    const sortedHistory =
        useMemo(() => {

            return [
                ...cropHistory,
            ].sort(
                (a, b) => {

                    const dateA =
                        new Date(
                            a?.harvestDate ||
                            a?.sowingDate ||
                            0
                        );


                    const dateB =
                        new Date(
                            b?.harvestDate ||
                            b?.sowingDate ||
                            0
                        );


                    return (
                        dateB.getTime() -
                        dateA.getTime()
                    );
                }
            );

        }, [
            cropHistory,
        ]);


    /*
     * =========================================================
     * Format date
     * =========================================================
     */

    const formatDate = (
        value
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

            return String(
                value
            );
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
     * Crop icon
     * =========================================================
     *
     * Icons are only visual labels.
     * They do not create crop data.
     */

    const getCropIcon = (
        cropName
    ) => {

        const name =
            String(
                cropName || ""
            ).toLowerCase();


        if (
            name.includes("wheat") ||
            name.includes("गेहूं")
        ) {

            return "🌾";
        }


        if (
            name.includes("rice") ||
            name.includes("धान")
        ) {

            return "🌾";
        }


        if (
            name.includes("mustard") ||
            name.includes("सरसों")
        ) {

            return "🌻";
        }


        if (
            name.includes("maize") ||
            name.includes("corn") ||
            name.includes("मक्का")
        ) {

            return "🌽";
        }


        if (
            name.includes("chickpea") ||
            name.includes("चना")
        ) {

            return "🌱";
        }


        if (
            name.includes("potato") ||
            name.includes("आलू")
        ) {

            return "🥔";
        }


        return "🌱";
    };


    /*
     * =========================================================
     * Open real crop-history detail
     * =========================================================
     */

    const openCropHistory = (
        record
    ) => {

        if (!record?.id) {

            return;
        }


        navigate(
            `/crop-history/${record.id}`,
            {
                state: {
                    cropHistory:
                    record,
                },
            }
        );
    };


    /*
     * =========================================================
     * Loading farms
     * =========================================================
     */

    if (fieldsLoading) {

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

                    <div className="history-loading-icon">
                        🔄
                    </div>


                    {
                        isHindi
                            ? "खेत की जानकारी लोड हो रही है..."
                            : "Loading farms..."
                    }

                </div>


                <BottomNavBar />

            </div>
        );
    }


    /*
     * =========================================================
     * No real farms
     * =========================================================
     */

    if (fields.length === 0) {

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

                    <div className="history-empty-icon">
                        📜
                    </div>


                    <strong>

                        {
                            isHindi
                                ? "कोई खेत उपलब्ध नहीं है"
                                : "No farms available"
                        }

                    </strong>


                    <p>

                        {
                            isHindi
                                ? "पहले backend में एक वास्तविक farm रिकॉर्ड उपलब्ध होना चाहिए।"
                                : "A real farm record must be available in the backend first."
                        }

                    </p>

                </div>


                <BottomNavBar />

            </div>
        );
    }


    /*
     * =========================================================
     * MAIN UI
     * =========================================================
     */

    return (

        <div className="history-container">


            {/* =====================================================
          PAGE TITLE
      ====================================================== */}

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

                {
                    fields.map(
                        (field) => (

                            <button
                                key={field.id}
                                type="button"
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
                      getCropIcon(
                          field.cropName
                      )
                  }

                </span>


                                <span>

                  {
                      isHindi
                          ? (
                              field.nameHi ||
                              field.name
                          )
                          : field.name
                  }

                </span>

                            </button>

                        )
                    )
                }

            </div>


            {/* =====================================================
          SELECTED FARM
      ====================================================== */}

            {
                selectedField && (

                    <div className="history-selected-farm">

            <span>
              📍
            </span>


                        <div>

                            <strong>
                                {
                                    selectedField.name
                                }
                            </strong>


                            {
                                selectedField.location?.name &&
                                (
                                    <small>
                                        {
                                            selectedField.location.name
                                        }
                                    </small>
                                )
                            }

                        </div>

                    </div>

                )
            }


            {/* =====================================================
          API ERROR
      ====================================================== */}

            {
                error && (

                    <div className="history-api-error">

                        ⚠️{" "}

                        {
                            error
                        }

                    </div>

                )
            }


            {/* =====================================================
          LOADING
      ====================================================== */}

            {
                loading ? (

                    <div className="history-empty">

                        <div className="history-loading-icon">
                            🔄
                        </div>


                        {
                            isHindi
                                ? "वास्तविक फसल इतिहास लोड हो रहा है..."
                                : "Loading real crop history..."
                        }

                    </div>

                ) : (

                    <>
                        {/* =================================================
                EMPTY HISTORY
            ================================================== */}

                        {
                            sortedHistory.length === 0 && (

                                <div className="history-empty">

                                    <div className="history-empty-icon">
                                        📜
                                    </div>


                                    <strong>

                                        {
                                            isHindi
                                                ? "कोई फसल इतिहास नहीं मिला"
                                                : "No crop history found"
                                        }

                                    </strong>


                                    <p>

                                        {
                                            isHindi
                                                ? "इस खेत के लिए backend में अभी कोई वास्तविक crop-history record उपलब्ध नहीं है।"
                                                : "There are no real crop-history records available for this farm in the backend."
                                        }

                                    </p>

                                </div>

                            )
                        }


                        {/* =================================================
                REAL HISTORY RECORDS
            ================================================== */}

                        {
                            sortedHistory.length > 0 && (

                                <div className="history-timeline">

                                    {
                                        sortedHistory.map(
                                            (
                                                record,
                                                index
                                            ) => (

                                                <button
                                                    key={
                                                        record?.id ||
                                                        `crop-history-${index}`
                                                    }
                                                    type="button"
                                                    className="history-item"
                                                    onClick={() =>
                                                        openCropHistory(
                                                            record
                                                        )
                                                    }
                                                >

                                                    {/* ICON */}

                                                    <div className="history-item-icon">

                                                        {
                                                            getCropIcon(
                                                                record?.cropName
                                                            )
                                                        }

                                                    </div>


                                                    {/* CONTENT */}

                                                    <div className="history-item-content">

                                                        <div className="history-item-season">

                                                            {
                                                                record?.season ||
                                                                "—"
                                                            }

                                                        </div>


                                                        <div className="history-item-name">

                                                            {
                                                                record?.cropName ||
                                                                (
                                                                    isHindi
                                                                        ? "अज्ञात फसल"
                                                                        : "Unknown Crop"
                                                                )
                                                            }

                                                        </div>


                                                        <div className="history-item-stage">

                                                            🌱{" "}

                                                            {
                                                                record?.cropStage ||
                                                                "—"
                                                            }

                                                        </div>


                                                        <div className="history-item-dates">

                              <span>

                                🌱{" "}

                                  {
                                      isHindi
                                          ? "बुवाई:"
                                          : "Sowing:"
                                  }{" "}

                                  {
                                      formatDate(
                                          record?.sowingDate
                                      )
                                  }

                              </span>


                                                            <span>

                                🌾{" "}

                                                                {
                                                                    isHindi
                                                                        ? "कटाई:"
                                                                        : "Harvest:"
                                                                }{" "}

                                                                {
                                                                    formatDate(
                                                                        record?.harvestDate
                                                                    )
                                                                }

                              </span>

                                                        </div>


                                                        {
                                                            (
                                                                record?.residuePresent !==
                                                                null ||
                                                                record?.residueType
                                                            ) && (

                                                                <div className="history-item-residue">

                                                                    ♻️{" "}

                                                                    {
                                                                        isHindi
                                                                            ? "अवशेष:"
                                                                            : "Residue:"
                                                                    }{" "}


                                                                    {
                                                                        record?.residuePresent ===
                                                                        true
                                                                            ? (
                                                                                isHindi
                                                                                    ? "हां"
                                                                                    : "Yes"
                                                                            )
                                                                            : record?.residuePresent ===
                                                                            false
                                                                                ? (
                                                                                    isHindi
                                                                                        ? "नहीं"
                                                                                        : "No"
                                                                                )
                                                                                : "—"
                                                                    }


                                                                    {
                                                                        record?.residueType && (
                                                                            <>
                                                                                {" · "}

                                                                                {
                                                                                    record.residueType
                                                                                }
                                                                            </>
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
                                        )
                                    }

                                </div>

                            )
                        }


                        {/* =================================================
                REAL RECORD COUNT
            ================================================== */}

                        {
                            sortedHistory.length > 0 && (

                                <div className="history-summary">

                  <span>
                    📊
                  </span>


                                    <div>

                                        <strong>
                                            {
                                                sortedHistory.length
                                            }
                                        </strong>


                                        <small>

                                            {
                                                isHindi
                                                    ? " फसल रिकॉर्ड उपलब्ध"
                                                    : " crop records available"
                                            }

                                        </small>

                                    </div>

                                </div>

                            )
                        }

                    </>

                )
            }


            {/* =====================================================
          BOTTOM NAVIGATION
      ====================================================== */}

            <BottomNavBar />

        </div>
    );
}


export default FieldHistory;