import {
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
    createCropHistory,
} from "../../api/cropHistoryApi";

import CropPicker
    from "../../components/CropPicker";

import BigSlider
    from "../../components/BigSlider";

import "./AddField.css";


function AddField() {

    const {
        language,
    } = useLanguage();


    const navigate =
        useNavigate();


    const {
        reloadFields,
    } = useFields();


    const isHindi =
        language === "hi";


    /*
     * =========================================================
     * FORM STATE
     * =========================================================
     */

    const [
        fieldName,
        setFieldName,
    ] = useState("");


    const [
        selectedCrop,
        setSelectedCrop,
    ] = useState(null);


    const [
        sizeAcres,
        setSizeAcres,
    ] = useState(2);


    const [
        location,
        setLocation,
    ] = useState(null);


    const [
        locating,
        setLocating,
    ] = useState(false);


    const [
        season,
        setSeason,
    ] = useState("");


    const [
        sowingDate,
        setSowingDate,
    ] = useState("");


    const [
        cropStage,
        setCropStage,
    ] = useState("");


    const [
        saving,
        setSaving,
    ] = useState(false);


    const [
        error,
        setError,
    ] = useState("");


    /*
     * =========================================================
     * AUTHENTICATED FARMER
     * =========================================================
     */

    const farmerId =
        localStorage.getItem(
            "agrinexus_user_id"
        );


    /*
     * =========================================================
     * TODAY
     * =========================================================
     *
     * Used to prevent a future sowing date.
     */

    const todayString =
        useMemo(() => {

            const now =
                new Date();


            const year =
                now.getFullYear();


            const month =
                String(
                    now.getMonth() + 1
                ).padStart(
                    2,
                    "0"
                );


            const day =
                String(
                    now.getDate()
                ).padStart(
                    2,
                    "0"
                );


            return `${year}-${month}-${day}`;

        }, []);


    /*
     * =========================================================
     * LOCATION
     * =========================================================
     */

    const handleUseLocation =
        () => {

            setError("");


            if (
                !navigator.geolocation
            ) {

                setError(
                    isHindi
                        ? "आपके ब्राउज़र में location सुविधा उपलब्ध नहीं है।"
                        : "Geolocation is not supported by this browser."
                );

                return;
            }


            setLocating(
                true
            );


            navigator.geolocation.getCurrentPosition(
                (position) => {

                    const latitude =
                        Number(
                            position.coords.latitude.toFixed(
                                6
                            )
                        );


                    const longitude =
                        Number(
                            position.coords.longitude.toFixed(
                                6
                            )
                        );


                    if (
                        !Number.isFinite(
                            latitude
                        ) ||
                        !Number.isFinite(
                            longitude
                        )
                    ) {

                        setLocating(
                            false
                        );


                        setError(
                            isHindi
                                ? "मान्य location प्राप्त नहीं हुई।"
                                : "A valid location could not be obtained."
                        );


                        return;
                    }


                    setLocation({
                        lat:
                        latitude,

                        lng:
                        longitude,

                        accuracy:
                            Number.isFinite(
                                position.coords.accuracy
                            )
                                ? Number(
                                    position.coords.accuracy.toFixed(
                                        1
                                    )
                                )
                                : null,
                    });


                    setLocating(
                        false
                    );
                },


                (locationError) => {

                    console.error(
                        "Geolocation error:",
                        locationError
                    );


                    setLocating(
                        false
                    );


                    let message;


                    switch (
                        locationError?.code
                        ) {

                        case 1:

                            message =
                                isHindi
                                    ? "Location permission अस्वीकार हुई। कृपया browser permission दें।"
                                    : "Location permission was denied. Please allow location access.";

                            break;


                        case 2:

                            message =
                                isHindi
                                    ? "स्थान उपलब्ध नहीं है।"
                                    : "Your location is currently unavailable.";

                            break;


                        case 3:

                            message =
                                isHindi
                                    ? "Location प्राप्त करने में समय समाप्त हो गया।"
                                    : "Location request timed out.";

                            break;


                        default:

                            message =
                                isHindi
                                    ? "स्थान प्राप्त नहीं हो सका।"
                                    : "Unable to get your location.";
                    }


                    setError(
                        message
                    );
                },


                {
                    enableHighAccuracy:
                        true,

                    timeout:
                        15000,

                    maximumAge:
                        0,
                }
            );
        };


    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */

    const normalizedArea =
        Number(
            sizeAcres
        );


    const normalizedCropId =
        selectedCrop
            ? String(
                selectedCrop
            ).trim()
            : "";


    const canSave =
        fieldName.trim().length >
        0 &&
        Boolean(
            normalizedCropId
        ) &&
        Boolean(
            season
        ) &&
        Boolean(
            sowingDate
        ) &&
        Boolean(
            cropStage
        ) &&
        Boolean(
            farmerId
        ) &&
        Boolean(
            location
        ) &&
        Number.isFinite(
            normalizedArea
        ) &&
        normalizedArea >
        0 &&
        !saving &&
        !locating;


    /*
     * =========================================================
     * VALIDATE FORM
     * =========================================================
     */

    const validateForm =
        () => {

            if (
                !farmerId
            ) {

                return (
                    isHindi
                        ? "किसान की ID नहीं मिली। कृपया दोबारा login करें।"
                        : "Farmer ID was not found. Please log in again."
                );
            }


            if (
                !fieldName.trim()
            ) {

                return (
                    isHindi
                        ? "कृपया खेत का नाम दर्ज करें।"
                        : "Please enter the farm name."
                );
            }


            if (
                !Number.isFinite(
                    normalizedArea
                ) ||
                normalizedArea <=
                0
            ) {

                return (
                    isHindi
                        ? "खेत का आकार 0 से अधिक होना चाहिए।"
                        : "Farm area must be greater than 0."
                );
            }


            if (
                !normalizedCropId
            ) {

                return (
                    isHindi
                        ? "कृपया वर्तमान फसल चुनें।"
                        : "Please select the current crop."
                );
            }


            if (
                !season
            ) {

                return (
                    isHindi
                        ? "कृपया फसल का मौसम चुनें।"
                        : "Please select the crop season."
                );
            }


            if (
                !sowingDate
            ) {

                return (
                    isHindi
                        ? "कृपया वास्तविक बुवाई की तारीख दर्ज करें।"
                        : "Please enter the actual sowing date."
                );
            }


            if (
                sowingDate >
                todayString
            ) {

                return (
                    isHindi
                        ? "बुवाई की तारीख आज या उससे पहले की होनी चाहिए।"
                        : "Sowing date cannot be in the future."
                );
            }


            if (
                !cropStage
            ) {

                return (
                    isHindi
                        ? "कृपया वर्तमान फसल अवस्था चुनें।"
                        : "Please select the current crop stage."
                );
            }


            if (
                !location ||
                !Number.isFinite(
                    Number(
                        location.lat
                    )
                ) ||
                !Number.isFinite(
                    Number(
                        location.lng
                    )
                )
            ) {

                return (
                    isHindi
                        ? "कृपया खेत का वास्तविक स्थान प्राप्त करें।"
                        : "Please provide the actual farm location."
                );
            }


            return "";
        };


    /*
     * =========================================================
     * CREATE FARM + CROP HISTORY
     * =========================================================
     */

    const handleSave =
        async () => {

            setError("");


            const validationError =
                validateForm();


            if (
                validationError
            ) {

                setError(
                    validationError
                );

                return;
            }


            setSaving(
                true
            );


            try {

                /*
                 * =====================================================
                 * STEP 1
                 * Create REAL FARM
                 * =====================================================
                 */

                const farmPayload = {

                    farmerId:
                    farmerId,

                    name:
                        fieldName.trim(),

                    areaAcres:
                    normalizedArea,

                    location:
                        `${location.lat}, ${location.lng}`,

                    latitude:
                    location.lat,

                    longitude:
                    location.lng,
                };


                console.log(
                    "Creating real farm:",
                    farmPayload
                );


                const createdFarm =
                    await apiRequest(
                        "/api/farms",
                        {
                            method:
                                "POST",

                            body:
                                JSON.stringify(
                                    farmPayload
                                ),
                        }
                    );


                if (
                    !createdFarm?.id
                ) {

                    throw new Error(
                        isHindi
                            ? "Farm बनाया गया लेकिन farm ID वापस नहीं मिली।"
                            : "Farm was created but no farm ID was returned."
                    );
                }


                const realFarmId =
                    String(
                        createdFarm.id
                    );


                /*
                 * =====================================================
                 * STEP 2
                 * Create REAL Crop History
                 * =====================================================
                 *
                 * selectedCrop must come from CropPicker's real
                 * GET /api/crops result.
                 */

                const cropHistoryPayload = {

                    farmId:
                    realFarmId,

                    cropId:
                    normalizedCropId,

                    season:
                    season,

                    sowingDate:
                    sowingDate,

                    harvestDate:
                        null,

                    cropStage:
                    cropStage,

                    residuePresent:
                        false,

                    residueType:
                        null,

                    notes:
                        "Current crop cycle created during farm registration.",
                };


                console.log(
                    "Creating real crop history:",
                    cropHistoryPayload
                );


                const createdCropHistory =
                    await createCropHistory(
                        cropHistoryPayload
                    );


                if (
                    !createdCropHistory?.id
                ) {

                    throw new Error(
                        isHindi
                            ? "फसल इतिहास रिकॉर्ड नहीं बनाया जा सका।"
                            : "Crop history was not created."
                    );
                }


                /*
                 * =====================================================
                 * STEP 3
                 * Save current farm
                 * =====================================================
                 */

                localStorage.setItem(
                    "agrinexus_current_farm_id",
                    realFarmId
                );


                /*
                 * Remove obsolete temporary values.
                 */

                localStorage.removeItem(
                    "agrinexus_pending_crop_id"
                );


                localStorage.removeItem(
                    "agrinexus_pending_farm_area_acres"
                );


                /*
                 * =====================================================
                 * STEP 4
                 * Reload backend farms
                 * =====================================================
                 */

                await reloadFields();


                /*
                 * =====================================================
                 * STEP 5
                 * Open newly created REAL farm
                 * =====================================================
                 */

                navigate(
                    `/field/${realFarmId}`
                );

            } catch (
                requestError
                ) {

                console.error(
                    "Farm/crop creation failed:",
                    requestError
                );


                if (
                    requestError?.status ===
                    401
                ) {

                    setError(
                        isHindi
                            ? "आपका login session समाप्त हो गया है। कृपया दोबारा login करें।"
                            : "Your login session has expired. Please log in again."
                    );

                    return;
                }


                if (
                    requestError?.status ===
                    403
                ) {

                    setError(
                        isHindi
                            ? "आपको यह farm बनाने की अनुमति नहीं है।"
                            : "You are not authorized to create this farm."
                    );

                    return;
                }


                if (
                    requestError?.status ===
                    409
                ) {

                    setError(
                        isHindi
                            ? "यह farm या crop record पहले से मौजूद हो सकता है।"
                            : "This farm or crop record may already exist."
                    );

                    return;
                }


                setError(
                    requestError?.message ||
                    (
                        isHindi
                            ? "खेत और फसल रिकॉर्ड बनाने में समस्या हुई।"
                            : "Unable to create the farm and crop record."
                    )
                );

            } finally {

                setSaving(
                    false
                );
            }
        };


    /*
     * =========================================================
     * UI
     * =========================================================
     */

    return (

        <div className="add-field-container">

            {/* =====================================================
                HEADER
            ===================================================== */}

            <h1 className="add-field-title">

                {
                    isHindi
                        ? "नया खेत जोड़ें"
                        : "Add New Farm"
                }

            </h1>


            <p className="add-field-subtitle">

                {
                    isHindi
                        ? "अपने खेत और वर्तमान फसल की वास्तविक जानकारी दर्ज करें"
                        : "Enter real farm and current crop information"
                }

            </p>


            {/* =====================================================
                ERROR
            ===================================================== */}

            {
                error && (

                    <div
                        className="add-field-error"
                        role="alert"
                    >

                        <span>
                            ⚠️
                        </span>


                        <span>
                            {error}
                        </span>

                    </div>
                )
            }


            {/* =====================================================
                FARM NAME
            ===================================================== */}

            <div className="add-field-section">

                <label
                    htmlFor="farm-name"
                    className="add-field-label"
                >

                    {
                        isHindi
                            ? "खेत का नाम"
                            : "Farm Name"
                    }

                </label>


                <input
                    id="farm-name"
                    type="text"
                    className="add-field-input"
                    placeholder={
                        isHindi
                            ? "जैसे: उत्तर वाला खेत"
                            : "e.g. North Field"
                    }
                    value={fieldName}
                    onChange={(
                        event
                    ) =>
                        setFieldName(
                            event.target.value
                        )
                    }
                    disabled={
                        saving
                    }
                    maxLength={100}
                />

            </div>


            {/* =====================================================
                CURRENT CROP
            ===================================================== */}

            <div className="add-field-section">

                <label className="add-field-label">

                    {
                        isHindi
                            ? "वर्तमान फसल"
                            : "Current Crop"
                    }

                </label>


                <CropPicker
                    selectedCrop={
                        selectedCrop
                    }

                    onSelect={
                        setSelectedCrop
                    }

                    language={
                        language
                    }
                />

            </div>


            {/* =====================================================
                SEASON
            ===================================================== */}

            <div className="add-field-section">

                <label
                    htmlFor="crop-season"
                    className="add-field-label"
                >

                    {
                        isHindi
                            ? "फसल का मौसम"
                            : "Crop Season"
                    }

                </label>


                <select
                    id="crop-season"
                    className="add-field-input"
                    value={season}
                    onChange={(
                        event
                    ) =>
                        setSeason(
                            event.target.value
                        )
                    }
                    disabled={
                        saving
                    }
                >

                    <option value="">

                        {
                            isHindi
                                ? "मौसम चुनें"
                                : "Select season"
                        }

                    </option>


                    <option value="Rabi">
                        Rabi
                    </option>


                    <option value="Kharif">
                        Kharif
                    </option>


                    <option value="Summer">
                        Summer
                    </option>

                </select>

            </div>


            {/* =====================================================
                SOWING DATE
            ===================================================== */}

            <div className="add-field-section">

                <label
                    htmlFor="sowing-date"
                    className="add-field-label"
                >

                    {
                        isHindi
                            ? "वास्तविक बुवाई की तारीख"
                            : "Actual Sowing Date"
                    }

                </label>


                <input
                    id="sowing-date"
                    type="date"
                    className="add-field-input"
                    value={sowingDate}
                    max={
                        todayString
                    }
                    onChange={(
                        event
                    ) =>
                        setSowingDate(
                            event.target.value
                        )
                    }
                    disabled={
                        saving
                    }
                />


                <small
                    style={{
                        display:
                            "block",

                        marginTop:
                            "7px",

                        color:
                            "#778078",

                        fontSize:
                            "12px",
                    }}
                >

                    {
                        isHindi
                            ? "भविष्य की तारीख स्वीकार नहीं की जाएगी।"
                            : "Future dates are not accepted."
                    }

                </small>

            </div>


            {/* =====================================================
                CROP STAGE
            ===================================================== */}

            <div className="add-field-section">

                <label
                    htmlFor="crop-stage"
                    className="add-field-label"
                >

                    {
                        isHindi
                            ? "वर्तमान फसल अवस्था"
                            : "Current Crop Stage"
                    }

                </label>


                <select
                    id="crop-stage"
                    className="add-field-input"
                    value={cropStage}
                    onChange={(
                        event
                    ) =>
                        setCropStage(
                            event.target.value
                        )
                    }
                    disabled={
                        saving
                    }
                >

                    <option value="">

                        {
                            isHindi
                                ? "फसल अवस्था चुनें"
                                : "Select crop stage"
                        }

                    </option>


                    <option value="SEEDLING">
                        Seedling
                    </option>


                    <option value="VEGETATIVE">
                        Vegetative
                    </option>


                    <option value="FLOWERING">
                        Flowering
                    </option>


                    <option value="GRAIN_FILLING">
                        Grain Filling
                    </option>


                    <option value="MATURITY">
                        Maturity
                    </option>

                </select>

            </div>


            {/* =====================================================
                FARM AREA
            ===================================================== */}

            <div className="add-field-section">

                <label className="add-field-label">

                    {
                        isHindi
                            ? "खेत का आकार"
                            : "Farm Area"
                    }

                </label>


                <BigSlider
                    value={
                        sizeAcres
                    }

                    onChange={
                        setSizeAcres
                    }

                    unit={
                        isHindi
                            ? "एकड़"
                            : "acres"
                    }
                />

            </div>


            {/* =====================================================
                ACTUAL LOCATION
            ===================================================== */}

            <div className="add-field-section">

                <label className="add-field-label">

                    {
                        isHindi
                            ? "खेत का वास्तविक स्थान"
                            : "Actual Farm Location"
                    }

                </label>


                <button
                    type="button"
                    className="location-button"
                    onClick={
                        handleUseLocation
                    }
                    disabled={
                        locating ||
                        saving
                    }
                >

                    📍{" "}

                    {
                        locating

                            ? (
                                isHindi
                                    ? "स्थान प्राप्त किया जा रहा है..."
                                    : "Getting location..."
                            )

                            : location

                                ? `${location.lat}, ${location.lng}`

                                : (
                                    isHindi
                                        ? "वर्तमान स्थान का उपयोग करें"
                                        : "Use Current Location"
                                )
                    }

                </button>


                {
                    location && (

                        <div className="add-field-location-preview">

                            <div>

                                <span>
                                    Latitude
                                </span>


                                <strong>
                                    {
                                        location.lat
                                    }
                                </strong>

                            </div>


                            <div>

                                <span>
                                    Longitude
                                </span>


                                <strong>
                                    {
                                        location.lng
                                    }
                                </strong>

                            </div>


                            {
                                location.accuracy != null && (

                                    <div>

                                        <span>
                                            GPS Accuracy
                                        </span>


                                        <strong>
                                            {
                                                location.accuracy
                                            } m
                                        </strong>

                                    </div>
                                )
                            }

                        </div>
                    )
                }

            </div>


            {/* =====================================================
                SAVE
            ===================================================== */}

            <button
                type="button"
                className={`add-field-save-button ${
                    !canSave
                        ? "disabled"
                        : ""
                }`}
                onClick={
                    handleSave
                }
                disabled={
                    !canSave
                }
            >

                {
                    saving

                        ? (
                            isHindi
                                ? "खेत और फसल सहेजे जा रहे हैं..."
                                : "Creating Farm & Crop..."
                        )

                        : (
                            isHindi
                                ? "खेत सहेजें"
                                : "Create Farm"
                        )
                }

            </button>


            {/* =====================================================
                CANCEL
            ===================================================== */}

            <button
                type="button"
                className="add-field-cancel-button"
                onClick={() =>
                    navigate(
                        "/home"
                    )
                }
                disabled={
                    saving
                }
            >

                {
                    isHindi
                        ? "रद्द करें"
                        : "Cancel"
                }

            </button>

        </div>
    );
}


export default AddField;