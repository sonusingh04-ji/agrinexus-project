import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useLanguage } from "../../context/LanguageContext";
import BottomNavBar from "../../components/BottomNavBar";

import {
    DEMO_FARM_ID,
    getAllCrops,
    getSoilIntelligence,
    getSoilIntelligenceForCrop,
    getLatestSoilSample,
    recommendNextCrop,
    getActiveRecoveryPlans,
    getSoilTestTrend,
} from "../../api/cropSoilApi";

import "./CropSoilAnalysis.css";

function CropSoilAnalysis() {
    const { language } = useLanguage();
    const navigate = useNavigate();
    const location = useLocation();

    const isHindi = language === "hi";

    /*
     * ---------------------------------------------------------
     * Farm ID
     * ---------------------------------------------------------
     */

    const farmId =
        location.state?.fieldId ||
        DEMO_FARM_ID;


    /*
     * ---------------------------------------------------------
     * State
     * ---------------------------------------------------------
     */

    const [crops, setCrops] =
        useState([]);

    const [selectedCropId, setSelectedCropId] =
        useState("");

    const [selectedSeason, setSelectedSeason] =
        useState("RABI");

    const [farmerConstraint, setFarmerConstraint] =
        useState("NONE");

    const [soil, setSoil] =
        useState(null);

    const [selectedCropSoil, setSelectedCropSoil] =
        useState(null);

    const [latestSample, setLatestSample] =
        useState(null);

    const [rotation, setRotation] =
        useState(null);

    const [recoveryPlans, setRecoveryPlans] =
        useState([]);

    const [soilTrend, setSoilTrend] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [analysisLoading, setAnalysisLoading] =
        useState(false);

    const [rotationLoading, setRotationLoading] =
        useState(false);

    const [error, setError] =
        useState("");

    const [analysisError, setAnalysisError] =
        useState("");

    const [rotationError, setRotationError] =
        useState("");


    /*
     * ---------------------------------------------------------
     * Initial backend data
     * ---------------------------------------------------------
     */

    useEffect(() => {
        let cancelled = false;

        async function loadData() {
            setLoading(true);
            setError("");

            try {
                const [
                    cropsResponse,
                    soilResponse,
                    sampleResponse,
                    recoveryResponse,
                    trendResponse,
                ] = await Promise.allSettled([
                    getAllCrops(),
                    getSoilIntelligence(farmId),
                    getLatestSoilSample(farmId),
                    getActiveRecoveryPlans(farmId),
                    getSoilTestTrend(farmId),
                ]);

                if (cancelled) {
                    return;
                }

                /*
                 * Crops
                 */

                if (
                    cropsResponse.status === "fulfilled"
                    && Array.isArray(
                        cropsResponse.value
                    )
                ) {
                    setCrops(
                        cropsResponse.value
                    );
                }


                /*
                 * Soil Intelligence
                 */

                if (
                    soilResponse.status === "fulfilled"
                ) {
                    setSoil(
                        soilResponse.value
                    );
                }


                /*
                 * Latest Soil Sample
                 */

                if (
                    sampleResponse.status === "fulfilled"
                ) {
                    setLatestSample(
                        sampleResponse.value
                    );
                }


                /*
                 * Recovery Plans
                 */

                if (
                    recoveryResponse.status === "fulfilled"
                    && Array.isArray(
                        recoveryResponse.value
                    )
                ) {
                    setRecoveryPlans(
                        recoveryResponse.value
                    );
                }


                /*
                 * Soil Trend
                 */

                if (
                    trendResponse.status === "fulfilled"
                ) {
                    setSoilTrend(
                        trendResponse.value
                    );
                }

            } catch (requestError) {
                if (!cancelled) {
                    setError(
                        requestError?.message ||
                        (
                            isHindi
                                ? "मिट्टी की जानकारी लोड नहीं हो सकी।"
                                : "Unable to load soil information."
                        )
                    );
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadData();

        return () => {
            cancelled = true;
        };
    }, [farmId, isHindi]);


    /*
     * ---------------------------------------------------------
     * Selected crop
     * ---------------------------------------------------------
     */

    const selectedCrop = useMemo(() => {
        if (!selectedCropId) {
            return null;
        }

        return (
            crops.find(
                (crop) =>
                    String(crop.id) ===
                    String(selectedCropId)
            ) || null
        );
    }, [
        crops,
        selectedCropId,
    ]);


    /*
     * ---------------------------------------------------------
     * Analyze selected crop
     * ---------------------------------------------------------
     */

    const analyzeSelectedCrop =
        async () => {

            if (!selectedCropId) {
                setAnalysisError(
                    isHindi
                        ? "पहले फसल चुनें।"
                        : "Please select a crop first."
                );

                return;
            }

            try {
                setAnalysisLoading(true);
                setAnalysisError("");

                const response =
                    await getSoilIntelligenceForCrop(
                        farmId,
                        selectedCropId
                    );

                setSelectedCropSoil(
                    response
                );

            } catch (requestError) {
                setAnalysisError(
                    requestError?.message ||
                    (
                        isHindi
                            ? "फसल के लिए मिट्टी विश्लेषण नहीं हो सका।"
                            : "Unable to analyze soil for this crop."
                    )
                );
            } finally {
                setAnalysisLoading(false);
            }
        };


    /*
     * ---------------------------------------------------------
     * Crop rotation
     * ---------------------------------------------------------
     */

    const runCropRotation =
        async () => {

            try {
                setRotationLoading(true);
                setRotationError("");

                const previousCropId =
                    latestSample?.cropId ||
                    soil?.cropId ||
                    null;

                const response =
                    await recommendNextCrop({
                        farmId,

                        previousCropId,

                        targetSeason:
                        selectedSeason,

                        farmerConstraint:
                        farmerConstraint,
                    });

                setRotation(
                    response
                );

            } catch (requestError) {
                setRotationError(
                    requestError?.message ||
                    (
                        isHindi
                            ? "अगली फसल की सिफारिश नहीं मिल सकी।"
                            : "Unable to generate crop rotation recommendation."
                    )
                );
            } finally {
                setRotationLoading(false);
            }
        };


    /*
     * ---------------------------------------------------------
     * Helpers
     * ---------------------------------------------------------
     */

    const valueOrDash = (
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


    const numberOrDash = (
        value,
        digits = 2
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

        return Number(value).toFixed(
            digits
        );
    };


    const dateOrDash = (
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
            return value;
        }

        return date.toLocaleDateString(
            "en-IN"
        );
    };


    const compatibilityClass =
        (value) => {
            if (value === true) {
                return "compatibility-good";
            }

            if (value === false) {
                return "compatibility-bad";
            }

            return "compatibility-neutral";
        };


    /*
     * ---------------------------------------------------------
     * Loading
     * ---------------------------------------------------------
     */

    if (loading) {
        return (
            <div className="crop-soil-page">

                <div className="crop-soil-loading">

                    <div className="crop-soil-loading-icon">
                        🌱
                    </div>

                    <h2>
                        {isHindi
                            ? "मिट्टी की जानकारी लोड हो रही है..."
                            : "Loading soil information..."}
                    </h2>

                    <p>
                        {isHindi
                            ? "कृपया प्रतीक्षा करें।"
                            : "Please wait."}
                    </p>

                </div>

                <BottomNavBar />

            </div>
        );
    }


    /*
     * ---------------------------------------------------------
     * Main
     * ---------------------------------------------------------
     */

    return (
        <div className="crop-soil-page">

            {/* =====================================================
          Header
      ===================================================== */}

            <div className="crop-soil-header">

                <button
                    className="crop-soil-back"
                    onClick={() =>
                        navigate("/home")
                    }
                    type="button"
                >
                    ←
                </button>

                <div>

                    <h1>
                        {isHindi
                            ? "मिट्टी और फसल जांच"
                            : "Soil & Crop Analysis"}
                    </h1>

                    <p>
                        {isHindi
                            ? "मिट्टी, फसल और अगली फसल की जानकारी"
                            : "Soil, crop and next-crop intelligence"}
                    </p>

                </div>

            </div>


            {/* =====================================================
          Error
      ===================================================== */}

            {error && (
                <div className="crop-soil-error">
                    ⚠️ {error}
                </div>
            )}


            {/* =====================================================
          Latest Soil Summary
      ===================================================== */}

            <section className="crop-soil-section">

                <h2>
                    {isHindi
                        ? "मिट्टी की स्थिति"
                        : "Soil Status"}
                </h2>

                <div className="soil-main-card">

                    <div className="soil-main-top">

                        <div className="soil-main-icon">
                            🌱
                        </div>

                        <div>

              <span>
                {isHindi
                    ? "मिट्टी का प्रकार"
                    : "Soil Type"}
              </span>

                            <h3>
                                {valueOrDash(
                                    soil?.soilType ||
                                    latestSample?.soilType
                                )}
                            </h3>

                        </div>

                    </div>


                    <div className="soil-metric-grid">

                        <div className="soil-metric-card">

                            <span>pH</span>

                            <strong>
                                {numberOrDash(
                                    soil?.pH ??
                                    latestSample?.pH,
                                    1
                                )}
                            </strong>

                        </div>


                        <div className="soil-metric-card">

              <span>
                {isHindi
                    ? "जैविक कार्बन"
                    : "Organic Carbon"}
              </span>

                            <strong>
                                {soil?.organicCarbon != null
                                    ? `${numberOrDash(
                                        soil.organicCarbon,
                                        2
                                    )}%`
                                    : "—"}
                            </strong>

                        </div>


                        <div className="soil-metric-card">

                            <span>N</span>

                            <strong>
                                {valueOrDash(
                                    soil?.nitrogen ??
                                    latestSample?.nitrogen
                                )}
                            </strong>

                        </div>


                        <div className="soil-metric-card">

                            <span>P</span>

                            <strong>
                                {valueOrDash(
                                    soil?.phosphorus ??
                                    latestSample?.phosphorus
                                )}
                            </strong>

                        </div>


                        <div className="soil-metric-card">

                            <span>K</span>

                            <strong>
                                {valueOrDash(
                                    soil?.potassium ??
                                    latestSample?.potassium
                                )}
                            </strong>

                        </div>


                        <div className="soil-metric-card">

                            <span>EC</span>

                            <strong>
                                {numberOrDash(
                                    soil?.electricalConductivity ??
                                    latestSample?.electricalConductivity,
                                    2
                                )}
                            </strong>

                        </div>

                    </div>

                </div>

            </section>


            {/* =====================================================
          Data Quality
      ===================================================== */}

            <section className="crop-soil-section">

                <h2>
                    {isHindi
                        ? "मिट्टी डेटा की पूर्णता"
                        : "Soil Data Completeness"}
                </h2>

                <div className="data-quality-card">

                    <div className="data-quality-header">

                        <strong>
                            {numberOrDash(
                                soil?.completenessScore,
                                1
                            )}%
                        </strong>

                        <span>
              {valueOrDash(
                  soil?.soilDataStatus
              )}
            </span>

                    </div>


                    <div className="quality-bar">

                        <div
                            className="quality-bar-fill"
                            style={{
                                width: `${Math.min(
                                    100,
                                    Math.max(
                                        0,
                                        Number(
                                            soil?.completenessScore ||
                                            0
                                        )
                                    )
                                )}%`,
                            }}
                        />

                    </div>


                    <div className="quality-counts">

            <span>
              ✅{" "}
                {isHindi
                    ? "उपलब्ध"
                    : "Measured"}:{" "}
                {valueOrDash(
                    soil?.measuredParameterCount
                )}
            </span>

                        <span>
              ⚠️{" "}
                            {isHindi
                                ? "गायब"
                                : "Missing"}:{" "}
                            {valueOrDash(
                                soil?.missingParameterCount
                            )}
            </span>

                    </div>

                </div>

            </section>


            {/* =====================================================
          Available / Missing Parameters
      ===================================================== */}

            <section className="crop-soil-section">

                <h2>
                    {isHindi
                        ? "मिट्टी की जांच"
                        : "Soil Measurements"}
                </h2>

                <div className="parameter-card">

                    <h3>
                        {isHindi
                            ? "उपलब्ध"
                            : "Available"}
                    </h3>

                    <div className="parameter-list">

                        {soil?.availableParameters?.length ? (

                            soil.availableParameters.map(
                                (parameter) => (
                                    <span
                                        key={parameter}
                                        className="parameter-good"
                                    >
                    ✓ {parameter}
                  </span>
                                )
                            )

                        ) : (

                            <span>
                —
              </span>

                        )}

                    </div>


                    <h3 className="parameter-missing-title">
                        {isHindi
                            ? "गायब"
                            : "Missing"}
                    </h3>

                    <div className="parameter-list">

                        {soil?.missingParameters?.length ? (

                            soil.missingParameters.map(
                                (parameter) => (
                                    <span
                                        key={parameter}
                                        className="parameter-missing"
                                    >
                    ⚠ {parameter}
                  </span>
                                )
                            )

                        ) : (

                            <span>
                {isHindi
                    ? "कोई नहीं"
                    : "None"}
              </span>

                        )}

                    </div>

                </div>

            </section>


            {/* =====================================================
          Findings
      ===================================================== */}

            <section className="crop-soil-section">

                <h2>
                    {isHindi
                        ? "मिट्टी से मिलने वाली जानकारी"
                        : "Soil Findings"}
                </h2>

                <div className="finding-list">

                    {soil?.findings?.length ? (

                        soil.findings.map(
                            (finding, index) => (

                                <div
                                    className="finding-card"
                                    key={`${index}-${finding}`}
                                >
                                    🌱
                                    <span>
                    {finding}
                  </span>
                                </div>

                            )
                        )

                    ) : (

                        <div className="crop-soil-text-card">

                            <p>
                                {isHindi
                                    ? "उपलब्ध डेटा से अभी कोई सुरक्षित finding नहीं मिली।"
                                    : "No supported finding is currently available from the available data."}
                            </p>

                        </div>

                    )}

                </div>


                {soil?.explanation && (

                    <div className="crop-soil-text-card">

                        <p>
                            {soil.explanation}
                        </p>

                    </div>

                )}

            </section>


            {/* =====================================================
          Crop Selection
      ===================================================== */}

            <section className="crop-soil-section">

                <h2>
                    {isHindi
                        ? "फसल चुनें"
                        : "Select a Crop"}
                </h2>

                <div className="crop-selector-card">

                    <label>
                        {isHindi
                            ? "फसल"
                            : "Crop"}
                    </label>

                    <select
                        value={selectedCropId}
                        onChange={(event) =>
                            setSelectedCropId(
                                event.target.value
                            )
                        }
                    >

                        <option value="">
                            {isHindi
                                ? "फसल चुनें"
                                : "Select crop"}
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


                    <button
                        type="button"
                        className="crop-analysis-button"
                        disabled={
                            !selectedCropId ||
                            analysisLoading
                        }
                        onClick={
                            analyzeSelectedCrop
                        }
                    >
                        {analysisLoading
                            ? (
                                isHindi
                                    ? "जांच हो रही है..."
                                    : "Analyzing..."
                            )
                            : (
                                isHindi
                                    ? "🌱 इस फसल की मिट्टी जांचें"
                                    : "🌱 Check Soil for Crop"
                            )}
                    </button>

                </div>

                {analysisError && (
                    <div className="crop-soil-error">
                        ⚠️ {analysisError}
                    </div>
                )}

            </section>


            {/* =====================================================
          Crop Compatibility
      ===================================================== */}

            {selectedCropSoil && (

                <section className="crop-soil-section">

                    <h2>
                        {isHindi
                            ? "फसल अनुकूलता"
                            : "Crop Compatibility"}
                    </h2>

                    <div className="compatibility-card">

                        <div className="compatibility-header">

                            <div>
                <span>
                  {isHindi
                      ? "चयनित फसल"
                      : "Selected Crop"}
                </span>

                                <h3>
                                    {valueOrDash(
                                        selectedCropSoil.cropName ||
                                        selectedCrop?.name
                                    )}
                                </h3>
                            </div>

                            <div className="compatibility-ph">

                <span>
                  pH
                </span>

                                <strong>
                                    {numberOrDash(
                                        selectedCropSoil.pH,
                                        1
                                    )}
                                </strong>

                            </div>

                        </div>


                        <div className="compatibility-ph-range">

              <span>
                {isHindi
                    ? "फसल pH सीमा"
                    : "Crop pH Range"}
              </span>

                            <strong>
                                {numberOrDash(
                                    selectedCropSoil.cropSoilPhMin,
                                    1
                                )}
                                {" - "}
                                {numberOrDash(
                                    selectedCropSoil.cropSoilPhMax,
                                    1
                                )}
                            </strong>

                        </div>


                        <div
                            className={
                                selectedCropSoil.cropPhInterpretation ===
                                "COMPATIBLE"
                                    ? "compatibility-result compatibility-good"
                                    : "compatibility-result compatibility-neutral"
                            }
                        >

                            🌱{" "}
                            {valueOrDash(
                                selectedCropSoil.cropPhInterpretation
                            )}

                        </div>


                        {selectedCropSoil.explanation && (

                            <p className="compatibility-explanation">
                                {selectedCropSoil.explanation}
                            </p>

                        )}

                    </div>

                </section>

            )}


            {/* =====================================================
          Crop Rotation
      ===================================================== */}

            <section className="crop-soil-section">

                <h2>
                    {isHindi
                        ? "अगली फसल"
                        : "Next Crop"}
                </h2>

                <div className="rotation-card">

                    <label>
                        {isHindi
                            ? "अगला सीजन"
                            : "Target Season"}
                    </label>

                    <div className="season-buttons">

                        <button
                            type="button"
                            className={
                                selectedSeason === "RABI"
                                    ? "season-button active"
                                    : "season-button"
                            }
                            onClick={() =>
                                setSelectedSeason(
                                    "RABI"
                                )
                            }
                        >
                            🌾 Rabi
                        </button>


                        <button
                            type="button"
                            className={
                                selectedSeason === "KHARIF"
                                    ? "season-button active"
                                    : "season-button"
                            }
                            onClick={() =>
                                setSelectedSeason(
                                    "KHARIF"
                                )
                            }
                        >
                            🌱 Kharif
                        </button>


                        <button
                            type="button"
                            className={
                                selectedSeason === "SUMMER"
                                    ? "season-button active"
                                    : "season-button"
                            }
                            onClick={() =>
                                setSelectedSeason(
                                    "SUMMER"
                                )
                            }
                        >
                            ☀️ Summer
                        </button>

                    </div>


                    <label>
                        {isHindi
                            ? "प्राथमिकता"
                            : "Priority"}
                    </label>

                    <select
                        value={farmerConstraint}
                        onChange={(event) =>
                            setFarmerConstraint(
                                event.target.value
                            )
                        }
                    >

                        <option value="NONE">
                            {isHindi
                                ? "कोई विशेष प्राथमिकता नहीं"
                                : "No special preference"}
                        </option>

                        <option value="LOW_WATER">
                            💧 Low Water
                        </option>

                        <option value="LOW_INPUT_COST">
                            💰 Low Input Cost
                        </option>

                        <option value="SHORT_DURATION">
                            ⏱ Short Duration
                        </option>

                    </select>


                    <button
                        type="button"
                        className="rotation-button"
                        disabled={
                            rotationLoading
                        }
                        onClick={
                            runCropRotation
                        }
                    >
                        {rotationLoading
                            ? (
                                isHindi
                                    ? "सिफारिश बन रही है..."
                                    : "Generating..."
                            )
                            : (
                                isHindi
                                    ? "🔄 अगली फसल सुझाएं"
                                    : "🔄 Recommend Next Crop"
                            )}
                    </button>

                </div>

                {rotationError && (
                    <div className="crop-soil-error">
                        ⚠️ {rotationError}
                    </div>
                )}

            </section>


            {/* =====================================================
          Rotation Result
      ===================================================== */}

            {rotation && (

                <section className="crop-soil-section">

                    <div className="next-crop-card">

            <span>
              {isHindi
                  ? "सुझाई गई अगली फसल"
                  : "Recommended Next Crop"}
            </span>

                        <h3>
                            {valueOrDash(
                                rotation.recommendedCropName
                            )}
                        </h3>

                        <strong>
                            {numberOrDash(
                                rotation.recommendationScore,
                                1
                            )}
                        </strong>

                        <small>
                            {isHindi
                                ? "सिफारिश स्कोर"
                                : "Recommendation Score"}
                        </small>

                    </div>


                    <div className="crop-soil-text-card">

                        <strong>
                            {isHindi
                                ? "क्यों?"
                                : "Why?"}
                        </strong>

                        <p>
                            {valueOrDash(
                                rotation.reason
                            )}
                        </p>

                        {rotation.contributingFactors && (

                            <p>
                                <strong>
                                    {isHindi
                                        ? "मुख्य कारण:"
                                        : "Contributing factors:"}
                                </strong>
                                {" "}
                                {rotation.contributingFactors}
                            </p>

                        )}

                    </div>

                </section>

            )}


            {/* =====================================================
          Soil Recovery
      ===================================================== */}

            <section className="crop-soil-section">

                <div className="section-title-row">

                    <h2>
                        {isHindi
                            ? "मिट्टी सुधार योजना"
                            : "Soil Recovery Plan"}
                    </h2>

                    <span>
            {recoveryPlans.length}
          </span>

                </div>


                {recoveryPlans.length === 0 ? (

                    <div className="crop-soil-text-card">

                        <p>
                            {isHindi
                                ? "अभी कोई सक्रिय मिट्टी सुधार योजना नहीं है।"
                                : "No active soil recovery plan is currently available."}
                        </p>

                    </div>

                ) : (

                    <div className="recovery-list">

                        {recoveryPlans.map(
                            (plan, index) => (

                                <div
                                    className="recovery-card"
                                    key={
                                        plan.id ||
                                        `${plan.focusArea}-${index}`
                                    }
                                >

                                    <div className="recovery-number">
                                        {plan.seasonNumber ||
                                            index + 1}
                                    </div>

                                    <div className="recovery-content">

                                        <h3>
                                            {valueOrDash(
                                                plan.season ||
                                                plan.cropName
                                            )}
                                        </h3>

                                        <span className="recovery-focus">
                      {valueOrDash(
                          plan.focusArea
                      )}
                    </span>

                                        <p>
                                            {valueOrDash(
                                                plan.recommendation
                                            )}
                                        </p>

                                        <div className="recovery-meta">

                      <span>
                        {valueOrDash(
                            plan.trend
                        )}
                      </span>

                                            <span>
                        {valueOrDash(
                            plan.status
                        )}
                      </span>

                                        </div>

                                    </div>

                                </div>

                            )
                        )}

                    </div>

                )}

            </section>


            {/* =====================================================
          Soil Test Trend
      ===================================================== */}

            {soilTrend && (

                <section className="crop-soil-section">

                    <h2>
                        {isHindi
                            ? "मिट्टी जांच रुझान"
                            : "Soil Test Trend"}
                    </h2>

                    <div className="trend-card">

                        <div className="trend-summary">

                            <div>

                <span>
                  {isHindi
                      ? "कुल नमूने"
                      : "Total Samples"}
                </span>

                                <strong>
                                    {valueOrDash(
                                        soilTrend.totalSoilSamples
                                    )}
                                </strong>

                            </div>


                            <div>

                <span>
                  {isHindi
                      ? "कुल रुझान"
                      : "Overall Trend"}
                </span>

                                <strong>
                                    {valueOrDash(
                                        soilTrend.overallTrend
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
                                    {dateOrDash(
                                        soilTrend.nextSuggestedTestDate
                                    )}
                                </strong>

                            </div>

                        </div>


                        <div className="trend-reminder">

                            📅{" "}
                            {valueOrDash(
                                soilTrend.testReminderStatus
                            )}

                        </div>


                        {soilTrend.findings?.length > 0 && (

                            <div className="trend-findings">

                                {soilTrend.findings.map(
                                    (
                                        finding,
                                        index
                                    ) => (

                                        <div
                                            key={`${index}-${finding}`}
                                        >
                                            📈 {finding}
                                        </div>

                                    )
                                )}

                            </div>

                        )}


                        {soilTrend.recommendedAction && (

                            <div className="trend-action">

                                <strong>
                                    {isHindi
                                        ? "सुझाव"
                                        : "Recommended Action"}
                                </strong>

                                <p>
                                    {soilTrend.recommendedAction}
                                </p>

                            </div>

                        )}

                    </div>

                </section>

            )}


            {/* =====================================================
          Important Data Note
      ===================================================== */}

            <section className="crop-soil-section">

                <div className="crop-soil-disclaimer">

                    <strong>
                        {isHindi
                            ? "महत्वपूर्ण जानकारी"
                            : "Important Information"}
                    </strong>

                    <p>
                        {soil?.uncertaintyNote ||
                            latestSample?.uncertaintyNote ||
                            (
                                isHindi
                                    ? "मिट्टी की जानकारी उपलब्ध डेटा पर आधारित है। स्थानीय कृषि सलाह और वर्तमान मिट्टी जांच के साथ निर्णय की पुष्टि करें।"
                                    : "Soil information is based on available data. Validate important decisions against current soil testing and appropriate local agricultural guidance."
                            )}
                    </p>

                    {soil?.source && (
                        <p>
                            Source:{" "}
                            {soil.source}
                        </p>
                    )}

                    {soil?.dataStatus && (
                        <p>
                            Data Status:{" "}
                            {soil.dataStatus}
                        </p>
                    )}

                </div>

            </section>


            <BottomNavBar />

        </div>
    );
}

export default CropSoilAnalysis;