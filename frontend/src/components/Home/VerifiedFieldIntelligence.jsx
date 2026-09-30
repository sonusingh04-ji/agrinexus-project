import { useMemo } from "react";
import { useNavigate } from "react-router-dom";

import "./VerifiedFieldIntelligence.css";

const isMissing = (value) =>
    value === null ||
    value === undefined ||
    value === "";

const display = (value) =>
    isMissing(value)
        ? "—"
        : value;

const formatDate = (
    value,
    language
) => {
    if (!value) {
        return "—";
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

function VerifiedFieldIntelligence({
                                       language = "en",
                                       cropHistory = [],
                                       fertilizerRecommendation = null,
                                       fertilizerSchedule = null,
                                       soilTrend = null,
                                       recoveryPlans = [],
                                   }) {
    const navigate = useNavigate();

    const isHindi =
        language === "hi";

    /*
     * =========================================================
     * ACTIVE CROP
     * =========================================================
     *
     * Only a real, unharvested crop-history record is treated
     * as the current crop.
     */

    const activeCrop =
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
                        (record) =>
                            record &&
                            !record.harvestDate &&
                            (
                                record.cropName ||
                                record.cropId
                            )
                    )
                    .sort(
                        (a, b) =>
                            new Date(
                                b?.sowingDate ||
                                b?.createdAt ||
                                0
                            ) -
                            new Date(
                                a?.sowingDate ||
                                a?.createdAt ||
                                0
                            )
                    )[0] ||
                null
            );
        }, [
            cropHistory,
        ]);


    /*
     * =========================================================
     * FERTILIZER
     * =========================================================
     */

    const fertilizerCrop =
        fertilizerRecommendation?.cropName ??
        fertilizerRecommendation?.crop?.name ??
        null;

    const fertilizerN =
        fertilizerRecommendation?.nitrogen ??
        null;

    const fertilizerP =
        fertilizerRecommendation?.phosphorus ??
        null;

    const fertilizerK =
        fertilizerRecommendation?.potassium ??
        null;

    const fertilizerStatus =
        fertilizerRecommendation?.status ??
        fertilizerSchedule?.status ??
        null;

    const fertilizerName =
        fertilizerRecommendation?.fertilizerName ??
        fertilizerRecommendation?.productName ??
        fertilizerSchedule?.productName ??
        null;

    const fertilizerStage =
        fertilizerSchedule?.stage ??
        fertilizerRecommendation?.stage ??
        null;

    const fertilizerNextDate =
        fertilizerSchedule?.plannedDate ??
        fertilizerRecommendation?.applicationDate ??
        fertilizerRecommendation?.recommendedAt ??
        null;

    const fertilizerRecommendationText =
        fertilizerRecommendation?.recommendation ??
        null;

    const fertilizerReason =
        fertilizerRecommendation?.reason ??
        fertilizerRecommendation?.why ??
        fertilizerRecommendation?.explanation ??
        null;


    /*
     * =========================================================
     * CROP / FERTILIZER MATCH
     * =========================================================
     */

    const fertilizerCropMismatch =
        Boolean(
            activeCrop?.cropName &&
            fertilizerCrop &&
            String(
                activeCrop.cropName
            )
                .trim()
                .toLowerCase() !==
            String(
                fertilizerCrop
            )
                .trim()
                .toLowerCase()
        );


    /*
     * =========================================================
     * RECOVERY
     * =========================================================
     */

    const safeRecoveryPlans =
        Array.isArray(
            recoveryPlans
        )
            ? recoveryPlans
            : [];


    /*
     * =========================================================
     * EMPTY STATE
     * =========================================================
     */

    const hasAnyData =
        Boolean(
            fertilizerRecommendation ||
            fertilizerSchedule ||
            soilTrend ||
            safeRecoveryPlans.length > 0 ||
            activeCrop
        );


    return (
        <section className="verified-field-intelligence">

            {/* =================================================
                HEADER
            ================================================= */}

            <div className="vfi-header">

                <div className="vfi-header-copy">

                    <span className="vfi-eyebrow">
                        {isHindi
                            ? "फील्ड इंटेलिजेंस"
                            : "FIELD INTELLIGENCE"}
                    </span>

                    <h2>
                        {isHindi
                            ? "सत्यापित कृषि जानकारी"
                            : "Verified Farm Intelligence"}
                    </h2>

                    <p>
                        {isHindi
                            ? "यहां केवल आपके खेत से प्राप्त वास्तविक backend records दिखाए जाते हैं।"
                            : "Only real backend records for your selected farm are shown here."}
                    </p>

                </div>

                <div className="vfi-real-badge">
                    <span>✓</span>
                    {isHindi
                        ? "वास्तविक डेटा"
                        : "REAL DATA"}
                </div>

            </div>


            {!hasAnyData && (
                <div className="vfi-global-empty">

                    <div className="vfi-global-empty-icon">
                        ⓘ
                    </div>

                    <div>
                        <strong>
                            {isHindi
                                ? "अभी पर्याप्त backend records उपलब्ध नहीं हैं।"
                                : "No sufficient backend records are available yet."}
                        </strong>

                        <p>
                            {isHindi
                                ? "नई जानकारी उपलब्ध होते ही यह क्षेत्र स्वतः भर जाएगा।"
                                : "This section will populate automatically when real records become available."}
                        </p>
                    </div>

                </div>
            )}


            <div className="vfi-grid">

                {/* =================================================
                    ACTIVE CROP
                ================================================= */}

                <article className="vfi-card vfi-crop-card">

                    <div className="vfi-card-header">

                        <div className="vfi-icon">
                            🌾
                        </div>

                        <div>
                            <span className="vfi-card-kicker">
                                {isHindi
                                    ? "फसल चक्र"
                                    : "CROP CYCLE"}
                            </span>

                            <h3>
                                {isHindi
                                    ? "वर्तमान फसल"
                                    : "Active Crop"}
                            </h3>
                        </div>

                    </div>


                    <div className="vfi-active-crop">

                        <div className="vfi-active-crop-name">
                            {display(
                                activeCrop?.cropName
                            )}
                        </div>

                        <div className="vfi-active-crop-stage">
                            {display(
                                activeCrop?.cropStage
                            )}
                        </div>

                    </div>


                    <div className="vfi-detail-grid">

                        <div>
                            <span>
                                {isHindi
                                    ? "मौसम"
                                    : "Season"}
                            </span>

                            <strong>
                                {display(
                                    activeCrop?.season
                                )}
                            </strong>
                        </div>


                        <div>
                            <span>
                                {isHindi
                                    ? "बुवाई"
                                    : "Sowing"}
                            </span>

                            <strong>
                                {formatDate(
                                    activeCrop?.sowingDate,
                                    language
                                )}
                            </strong>
                        </div>


                        <div>
                            <span>
                                {isHindi
                                    ? "कटाई"
                                    : "Harvest"}
                            </span>

                            <strong>
                                {formatDate(
                                    activeCrop?.harvestDate,
                                    language
                                )}
                            </strong>
                        </div>

                    </div>


                    <p className="vfi-source-note">

                        {activeCrop
                            ? (
                                isHindi
                                    ? "स्रोत: सक्रिय, बिना harvest वाला crop-history record"
                                    : "Source: active unharvested crop-history record"
                            )
                            : (
                                isHindi
                                    ? "कोई सक्रिय crop-history record नहीं मिला।"
                                    : "No active crop-history record was returned."
                            )}

                    </p>

                </article>


                {/* =================================================
                    FERTILIZER
                ================================================= */}

                <article className="vfi-card vfi-fertilizer-card">

                    <div className="vfi-card-header">

                        <div className="vfi-icon vfi-icon-green">
                            🌿
                        </div>

                        <div>
                            <span className="vfi-card-kicker">
                                {isHindi
                                    ? "पोषक प्रबंधन"
                                    : "NUTRIENT MANAGEMENT"}
                            </span>

                            <h3>
                                {isHindi
                                    ? "उर्वरक सलाह"
                                    : "Fertilizer Recommendation"}
                            </h3>
                        </div>

                        <button
                            type="button"
                            className="vfi-open-button"
                            onClick={() =>
                                navigate(
                                    "/fertilizer"
                                )
                            }
                        >
                            →
                        </button>

                    </div>


                    <div className="vfi-npk-grid">

                        <div className="vfi-npk-item">
                            <span>N</span>
                            <strong>
                                {display(
                                    fertilizerN
                                )}
                            </strong>
                        </div>

                        <div className="vfi-npk-item">
                            <span>P</span>
                            <strong>
                                {display(
                                    fertilizerP
                                )}
                            </strong>
                        </div>

                        <div className="vfi-npk-item">
                            <span>K</span>
                            <strong>
                                {display(
                                    fertilizerK
                                )}
                            </strong>
                        </div>

                    </div>


                    <div className="vfi-detail-list">

                        <div>
                            <span>
                                {isHindi
                                    ? "फसल"
                                    : "Crop"}
                            </span>

                            <strong>
                                {display(
                                    fertilizerCrop
                                )}
                            </strong>
                        </div>


                        <div>
                            <span>
                                {isHindi
                                    ? "स्थिति"
                                    : "Status"}
                            </span>

                            <strong>
                                {display(
                                    fertilizerStatus
                                )}
                            </strong>
                        </div>


                        <div>
                            <span>
                                {isHindi
                                    ? "उत्पाद"
                                    : "Product"}
                            </span>

                            <strong>
                                {display(
                                    fertilizerName
                                )}
                            </strong>
                        </div>


                        <div>
                            <span>
                                {isHindi
                                    ? "अवस्था"
                                    : "Stage"}
                            </span>

                            <strong>
                                {display(
                                    fertilizerStage
                                )}
                            </strong>
                        </div>


                        <div>
                            <span>
                                {isHindi
                                    ? "अगला समय"
                                    : "Next Schedule"}
                            </span>

                            <strong>
                                {formatDate(
                                    fertilizerNextDate,
                                    language
                                )}
                            </strong>
                        </div>

                    </div>


                    {fertilizerRecommendationText && (
                        <div className="vfi-explanation">

                            <span>
                                {isHindi
                                    ? "सिफारिश"
                                    : "Recommendation"}
                            </span>

                            <p>
                                {
                                    fertilizerRecommendationText
                                }
                            </p>

                        </div>
                    )}


                    {fertilizerReason && (
                        <div className="vfi-explanation vfi-why">

                            <span>
                                {isHindi
                                    ? "क्यों?"
                                    : "Why?"}
                            </span>

                            <p>
                                {fertilizerReason}
                            </p>

                        </div>
                    )}


                    {fertilizerCropMismatch && (
                        <div className="vfi-warning">

                            ⚠️{" "}

                            {isHindi
                                ? "उर्वरक रिकॉर्ड की फसल सक्रिय फसल से मेल नहीं खाती।"
                                : "The fertilizer record crop does not match the active current crop."}

                        </div>
                    )}

                </article>


                {/* =================================================
                    SOIL TREND
                ================================================= */}

                <article className="vfi-card vfi-trend-card">

                    <div className="vfi-card-header">

                        <div className="vfi-icon vfi-icon-blue">
                            📈
                        </div>

                        <div>
                            <span className="vfi-card-kicker">
                                {isHindi
                                    ? "मिट्टी निगरानी"
                                    : "SOIL MONITORING"}
                            </span>

                            <h3>
                                {isHindi
                                    ? "मिट्टी जांच रुझान"
                                    : "Soil Test Trend"}
                            </h3>
                        </div>

                    </div>


                    <div className="vfi-detail-grid vfi-trend-grid">

                        <div>
                            <span>
                                {isHindi
                                    ? "कुल नमूने"
                                    : "Total Samples"}
                            </span>

                            <strong>
                                {display(
                                    soilTrend?.totalSoilSamples
                                )}
                            </strong>
                        </div>


                        <div>
                            <span>
                                {isHindi
                                    ? "रुझान"
                                    : "Overall Trend"}
                            </span>

                            <strong>
                                {display(
                                    soilTrend?.overallTrend
                                )}
                            </strong>
                        </div>


                        <div>
                            <span>
                                {isHindi
                                    ? "अगली जांच"
                                    : "Next Test"}
                            </span>

                            <strong>
                                {formatDate(
                                    soilTrend?.nextSuggestedTestDate,
                                    language
                                )}
                            </strong>
                        </div>


                        <div>
                            <span>
                                {isHindi
                                    ? "रिमाइंडर"
                                    : "Reminder"}
                            </span>

                            <strong>
                                {display(
                                    soilTrend?.testReminderStatus
                                )}
                            </strong>
                        </div>

                    </div>


                    {Array.isArray(
                            soilTrend?.findings
                        ) &&
                        soilTrend.findings.length > 0 && (

                            <div className="vfi-findings">

                                <span>
                                    {isHindi
                                        ? "मुख्य निष्कर्ष"
                                        : "Key Findings"}
                                </span>

                                {soilTrend.findings
                                    .slice(0, 3)
                                    .map(
                                        (
                                            finding,
                                            index
                                        ) => (
                                            <p
                                                key={
                                                    `${index}-${finding}`
                                                }
                                            >
                                                <b>•</b>{" "}
                                                {finding}
                                            </p>
                                        )
                                    )}

                            </div>

                        )}


                    {soilTrend?.recommendedAction && (
                        <div className="vfi-explanation">

                            <span>
                                {isHindi
                                    ? "अगला कदम"
                                    : "Recommended Action"}
                            </span>

                            <p>
                                {
                                    soilTrend.recommendedAction
                                }
                            </p>

                        </div>
                    )}

                </article>


                {/* =================================================
                    SOIL RECOVERY
                ================================================= */}

                <article className="vfi-card vfi-recovery-card">

                    <div className="vfi-card-header">

                        <div className="vfi-icon vfi-icon-purple">
                            🌱
                        </div>

                        <div>
                            <span className="vfi-card-kicker">
                                {isHindi
                                    ? "दीर्घकालीन मिट्टी"
                                    : "LONG-TERM SOIL"}
                            </span>

                            <h3>
                                {isHindi
                                    ? "मिट्टी सुधार रोडमैप"
                                    : "Soil Recovery Roadmap"}
                            </h3>
                        </div>

                    </div>


                    {safeRecoveryPlans.length > 0 ? (

                        <div className="vfi-recovery-list">

                            {safeRecoveryPlans
                                .slice(0, 3)
                                .map(
                                    (
                                        plan,
                                        index
                                    ) => (

                                        <div
                                            className="vfi-recovery-item"
                                            key={
                                                plan.id ||
                                                `${plan.focusArea}-${index}`
                                            }
                                        >

                                            <div className="vfi-recovery-number">
                                                {
                                                    plan.seasonNumber ||
                                                    index + 1
                                                }
                                            </div>


                                            <div className="vfi-recovery-content">

                                                <strong>
                                                    {
                                                        display(
                                                            plan.season ||
                                                            plan.cropName
                                                        )
                                                    }
                                                </strong>

                                                <span>
                                                    {
                                                        display(
                                                            plan.focusArea
                                                        )
                                                    }
                                                </span>

                                                <p>
                                                    {
                                                        display(
                                                            plan.recommendation
                                                        )
                                                    }
                                                </p>

                                                <small>
                                                    {
                                                        display(
                                                            plan.trend
                                                        )
                                                    }

                                                    {" · "}

                                                    {
                                                        display(
                                                            plan.status
                                                        )
                                                    }
                                                </small>

                                            </div>

                                        </div>

                                    )
                                )}

                        </div>

                    ) : (

                        <div className="vfi-empty-state">

                            <div className="vfi-empty-icon">
                                ✓
                            </div>

                            <strong>
                                {isHindi
                                    ? "कोई सक्रिय recovery record नहीं मिला।"
                                    : "No active recovery record was returned."}
                            </strong>

                            <p>
                                {isHindi
                                    ? "Backend में योजना उपलब्ध होने पर multi-season roadmap यहां दिखाई देगा।"
                                    : "The multi-season roadmap will appear here when backend recovery records become available."}
                            </p>

                        </div>

                    )}

                </article>

            </div>

        </section>
    );
}

export default VerifiedFieldIntelligence;