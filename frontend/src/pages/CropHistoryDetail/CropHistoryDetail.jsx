import {
    useEffect,
    useState,
} from "react";

import {
    useNavigate,
    useParams,
    useLocation,
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

import BottomNavBar from "../../components/BottomNavBar";

import "./CropHistoryDetail.css";


function CropHistoryDetail() {

    const {
        id,
    } = useParams();

    const location =
        useLocation();

    const navigate =
        useNavigate();


    const {
        language,
    } = useLanguage();

    const {
        fields = [],
    } = useFields();

    const isHindi =
        language === "hi";


    /*
     * =========================================================
     * Historical crop record
     * =========================================================
     */

    const [
        history,
        setHistory,
    ] = useState(
        location.state?.cropHistory ||
        null
    );


    const [
        loading,
        setLoading,
    ] = useState(
        !location.state?.cropHistory
    );


    const [
        error,
        setError,
    ] = useState("");


    /*
     * =========================================================
     * Load the actual crop-history UUID
     * =========================================================
     */

    useEffect(() => {

        if (
            location.state?.cropHistory
        ) {

            setHistory(
                location.state.cropHistory
            );

            setLoading(false);

            return;
        }


        if (!id) {

            setLoading(false);

            setError(
                isHindi
                    ? "फसल इतिहास की ID उपलब्ध नहीं है।"
                    : "Crop history ID is not available."
            );

            return;
        }


        let cancelled =
            false;


        async function loadHistory() {

            setLoading(true);
            setError("");


            try {

                const response =
                    await apiRequest(
                        `/api/crop-history/${id}`
                    );


                if (cancelled) {
                    return;
                }


                setHistory(
                    response
                );

            } catch (
                requestError
                ) {

                if (!cancelled) {

                    console.error(
                        "Failed to load crop history detail:",
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


                    setHistory(
                        null
                    );
                }

            } finally {

                if (!cancelled) {

                    setLoading(false);
                }
            }
        }


        loadHistory();


        return () => {

            cancelled =
                true;
        };

    }, [
        id,
        isHindi,
        location.state,
    ]);


    /*
     * =========================================================
     * Find real farm
     * =========================================================
     */

    const farm =
        history?.farmId
            ? fields.find(
                (
                    field
                ) =>
                    String(
                        field.id
                    ) ===
                    String(
                        history.farmId
                    )
            )
            : null;


    /*
     * =========================================================
     * Helpers
     * =========================================================
     */

    const display =
        (
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


    const formatDate =
        (
            value
        ) => {

            if (!value) {

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


    const yesNo =
        (
            value
        ) => {

            if (
                value === null ||
                value === undefined
            ) {

                return "—";
            }


            return value
                ? (
                    isHindi
                        ? "हां"
                        : "Yes"
                )
                : (
                    isHindi
                        ? "नहीं"
                        : "No"
                );
        };


    const getCropIcon =
        (
            cropName
        ) => {

            const name =
                String(
                    cropName ||
                    ""
                ).toLowerCase();


            if (
                name.includes(
                    "wheat"
                ) ||
                name.includes(
                    "गेहूं"
                )
            ) {

                return "🌾";
            }


            if (
                name.includes(
                    "rice"
                ) ||
                name.includes(
                    "धान"
                )
            ) {

                return "🌾";
            }


            if (
                name.includes(
                    "maize"
                ) ||
                name.includes(
                    "corn"
                ) ||
                name.includes(
                    "मक्का"
                )
            ) {

                return "🌽";
            }


            if (
                name.includes(
                    "mustard"
                ) ||
                name.includes(
                    "सरसों"
                )
            ) {

                return "🌻";
            }


            if (
                name.includes(
                    "chickpea"
                ) ||
                name.includes(
                    "चना"
                )
            ) {

                return "🌱";
            }


            return "🌱";
        };


    /*
     * =========================================================
     * LOADING
     * =========================================================
     */

    if (loading) {

        return (

            <div className="crop-history-detail-page">

                <button
                    type="button"
                    className="crop-history-detail-back"
                    onClick={() =>
                        navigate(
                            "/history"
                        )
                    }
                >

                    ←{" "}

                    {
                        isHindi
                            ? "वापस"
                            : "Back"
                    }

                </button>


                <div className="crop-history-detail-loading">

                    <div className="crop-history-detail-loading-icon">

                        🌾

                    </div>


                    <h2>

                        {
                            isHindi
                                ? "फसल की जानकारी लोड हो रही है..."
                                : "Loading crop details..."
                        }

                    </h2>


                    <p>

                        {
                            isHindi
                                ? "वास्तविक backend रिकॉर्ड लिया जा रहा है।"
                                : "Loading the real backend crop-history record."
                        }

                    </p>

                </div>


                <BottomNavBar />

            </div>
        );
    }


    /*
     * =========================================================
     * NOT FOUND
     * =========================================================
     */

    if (!history) {

        return (

            <div className="crop-history-detail-page">

                <button
                    type="button"
                    className="crop-history-detail-back"
                    onClick={() =>
                        navigate(
                            "/history"
                        )
                    }
                >

                    ←{" "}

                    {
                        isHindi
                            ? "वापस"
                            : "Back"
                    }

                </button>


                <div className="crop-history-detail-error">

                    <div className="crop-history-detail-error-icon">

                        ⚠️

                    </div>


                    <h2>

                        {
                            isHindi
                                ? "रिकॉर्ड उपलब्ध नहीं"
                                : "Crop History Record Not Available"
                        }

                    </h2>


                    <p>

                        {
                            error ||
                            (
                                isHindi
                                    ? "यह वास्तविक crop-history record backend में नहीं मिला।"
                                    : "This real crop-history record could not be found in the backend."
                            )
                        }

                    </p>


                    <button
                        type="button"
                        className="crop-history-detail-primary-button"
                        onClick={() =>
                            navigate(
                                "/history"
                            )
                        }
                    >

                        {
                            isHindi
                                ? "फसल इतिहास देखें"
                                : "View Crop History"
                        }

                    </button>

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

        <div className="crop-history-detail-page">

            {/* BACK */}

            <button
                type="button"
                className="crop-history-detail-back"
                onClick={() =>
                    navigate(
                        "/history"
                    )
                }
            >

                ←{" "}

                {
                    isHindi
                        ? "फसल इतिहास"
                        : "Crop History"
                }

            </button>


            {/* HERO */}

            <div className="crop-history-detail-hero">

                <div className="crop-history-detail-hero-icon">

                    {
                        getCropIcon(
                            history.cropName
                        )
                    }

                </div>


                <div className="crop-history-detail-hero-content">

                    <div className="crop-history-detail-label">

                        {
                            isHindi
                                ? "ऐतिहासिक फसल रिकॉर्ड"
                                : "Historical Crop Record"
                        }

                    </div>


                    <h1>

                        {
                            display(
                                history.cropName
                            )
                        }

                    </h1>


                    <div className="crop-history-detail-season">

                        🌱{" "}

                        {
                            display(
                                history.season
                            )
                        }

                    </div>


                    {
                        farm && (

                            <div className="crop-history-detail-farm">

                                📍{" "}

                                {
                                    display(
                                        farm.name
                                    )
                                }

                            </div>
                        )
                    }

                </div>

            </div>


            {/* CROP PERIOD */}

            <section className="crop-history-detail-section">

                <h2>

                    {
                        isHindi
                            ? "फसल अवधि"
                            : "Crop Period"
                    }

                </h2>


                <div className="crop-history-detail-period">

                    <div className="crop-history-detail-period-item">

                        <span>
                            🌱
                        </span>


                        <div>

                            <small>

                                {
                                    isHindi
                                        ? "बुवाई"
                                        : "Sowing"
                                }

                            </small>


                            <strong>

                                {
                                    formatDate(
                                        history.sowingDate
                                    )
                                }

                            </strong>

                        </div>

                    </div>


                    <div className="crop-history-detail-period-arrow">

                        →

                    </div>


                    <div className="crop-history-detail-period-item">

                        <span>
                            🌾
                        </span>


                        <div>

                            <small>

                                {
                                    isHindi
                                        ? "कटाई"
                                        : "Harvest"
                                }

                            </small>


                            <strong>

                                {
                                    formatDate(
                                        history.harvestDate
                                    )
                                }

                            </strong>

                        </div>

                    </div>

                </div>

            </section>


            {/* CROP INFORMATION */}

            <section className="crop-history-detail-section">

                <h2>

                    {
                        isHindi
                            ? "फसल की जानकारी"
                            : "Crop Information"
                    }

                </h2>


                <div className="crop-history-detail-grid">

                    <div className="crop-history-detail-card">

                        <span className="crop-history-detail-card-icon">
                            🌾
                        </span>


                        <small>

                            {
                                isHindi
                                    ? "फसल"
                                    : "Crop"
                            }

                        </small>


                        <strong>

                            {
                                display(
                                    history.cropName
                                )
                            }

                        </strong>

                    </div>


                    <div className="crop-history-detail-card">

                        <span className="crop-history-detail-card-icon">
                            📅
                        </span>


                        <small>

                            {
                                isHindi
                                    ? "सीजन"
                                    : "Season"
                            }

                        </small>


                        <strong>

                            {
                                display(
                                    history.season
                                )
                            }

                        </strong>

                    </div>


                    <div className="crop-history-detail-card">

                        <span className="crop-history-detail-card-icon">
                            🌱
                        </span>


                        <small>

                            {
                                isHindi
                                    ? "फसल चरण"
                                    : "Crop Stage"
                            }

                        </small>


                        <strong>

                            {
                                display(
                                    history.cropStage
                                )
                            }

                        </strong>

                    </div>


                    <div className="crop-history-detail-card">

                        <span className="crop-history-detail-card-icon">
                            ♻️
                        </span>


                        <small>

                            {
                                isHindi
                                    ? "फसल अवशेष"
                                    : "Crop Residue"
                            }

                        </small>


                        <strong>

                            {
                                yesNo(
                                    history.residuePresent
                                )
                            }

                        </strong>

                    </div>

                </div>

            </section>


            {/* RESIDUE */}

            {
                (
                    history.residuePresent !==
                    null ||
                    history.residueType
                ) && (

                    <section className="crop-history-detail-section">

                        <h2>

                            {
                                isHindi
                                    ? "फसल अवशेष"
                                    : "Crop Residue"
                            }

                        </h2>


                        <div className="crop-history-detail-info-card">

                            <div className="crop-history-detail-info-row">

                                <span>

                                    {
                                        isHindi
                                            ? "अवशेष मौजूद"
                                            : "Residue Present"
                                    }

                                </span>


                                <strong>

                                    {
                                        yesNo(
                                            history.residuePresent
                                        )
                                    }

                                </strong>

                            </div>


                            <div className="crop-history-detail-info-row">

                                <span>

                                    {
                                        isHindi
                                            ? "अवशेष प्रकार"
                                            : "Residue Type"
                                    }

                                </span>


                                <strong>

                                    {
                                        display(
                                            history.residueType
                                        )
                                    }

                                </strong>

                            </div>

                        </div>

                    </section>
                )
            }


            {/* NOTES */}

            <section className="crop-history-detail-section">

                <h2>

                    {
                        isHindi
                            ? "नोट्स"
                            : "Notes"
                    }

                </h2>


                {
                    history.notes
                        ? (

                            <div className="crop-history-detail-notes-card">

                                <div className="crop-history-detail-notes-icon">

                                    📝

                                </div>


                                <p>

                                    {
                                        history.notes
                                    }

                                </p>

                            </div>
                        )
                        : (

                            <div className="crop-history-detail-empty">

                                {
                                    isHindi
                                        ? "इस crop-history record में कोई अतिरिक्त note नहीं है।"
                                        : "No additional notes are recorded for this crop cycle."
                                }

                            </div>
                        )
                }

            </section>


            {/* RECORD INFORMATION */}

            <section className="crop-history-detail-section">

                <h2>

                    {
                        isHindi
                            ? "रिकॉर्ड जानकारी"
                            : "Record Information"
                    }

                </h2>


                <div className="crop-history-detail-info-card">

                    <div className="crop-history-detail-info-row">

                        <span>

                            {
                                isHindi
                                    ? "रिकॉर्ड ID"
                                    : "Record ID"
                            }

                        </span>


                        <strong className="crop-history-detail-id">

                            {
                                display(
                                    history.id
                                )
                            }

                        </strong>

                    </div>


                    <div className="crop-history-detail-info-row">

                        <span>

                            {
                                isHindi
                                    ? "खेत ID"
                                    : "Farm ID"
                            }

                        </span>


                        <strong className="crop-history-detail-id">

                            {
                                display(
                                    history.farmId
                                )
                            }

                        </strong>

                    </div>


                    <div className="crop-history-detail-info-row">

                        <span>

                            {
                                isHindi
                                    ? "फसल ID"
                                    : "Crop ID"
                            }

                        </span>


                        <strong className="crop-history-detail-id">

                            {
                                display(
                                    history.cropId
                                )
                            }

                        </strong>

                    </div>

                </div>

            </section>


            {/* DATA SOURCE NOTICE */}

            <div className="crop-history-detail-current-banner">

                <div className="crop-history-detail-current-banner-icon">

                    ℹ️

                </div>


                <div>

                    <strong>

                        {
                            isHindi
                                ? "वास्तविक ऐतिहासिक रिकॉर्ड"
                                : "Real Historical Record"
                        }

                    </strong>


                    <p>

                        {
                            isHindi
                                ? "ऊपर दिखाई गई जानकारी केवल backend में stored crop-history record से ली गई है। कोई missing historical value invent नहीं की जाती।"
                                : "The information shown above comes only from the stored backend crop-history record. Missing historical values are not invented."
                        }

                    </p>

                </div>

            </div>


            {/* ACTIONS */}

            <section className="crop-history-detail-actions">

                <button
                    type="button"
                    className="crop-history-detail-primary-button"
                    onClick={() =>
                        navigate(
                            "/analysis",
                            {
                                state: {
                                    fieldId:
                                    history.farmId,
                                },
                            }
                        )
                    }
                >

                    🌾{" "}

                    {
                        isHindi
                            ? "अगली फसल जांचें"
                            : "Check Next Crop"
                    }

                </button>


                <button
                    type="button"
                    className="crop-history-detail-secondary-button"
                    onClick={() =>
                        navigate(
                            `/field/${history.farmId}`
                        )
                    }
                >

                    📍{" "}

                    {
                        isHindi
                            ? "खेत देखें"
                            : "View Field"
                    }

                </button>


                <button
                    type="button"
                    className="crop-history-detail-secondary-button"
                    onClick={() =>
                        navigate(
                            "/history"
                        )
                    }
                >

                    ←{" "}

                    {
                        isHindi
                            ? "इतिहास पर वापस जाएं"
                            : "Back to Crop History"
                    }

                </button>

            </section>


            <BottomNavBar />

        </div>
    );
}


export default CropHistoryDetail;