import {
    useEffect,
    useMemo,
    useState,
} from "react";

import { useNavigate } from "react-router-dom";

import {
    useLanguage,
} from "../../context/LanguageContext";

import {
    useFields,
} from "../../context/FieldsContext";

import {
    clearAuthData,
    API_BASE_URL,
} from "../../api/apiClient";

import {
    getMyProfile,
    updateMyProfile,
    uploadProfilePhoto,
    deleteProfilePhoto,
} from "../../api/profileApi";

import "./Profile.css";


function Profile() {

    const navigate =
        useNavigate();


    /*
     * =========================================================
     * Global language system
     * =========================================================
     *
     * languages:
     *     Complete language list from LanguageContext
     *
     * currentLanguage:
     *     Currently selected language object
     *
     * t:
     *     Translation function
     */

    const {
        language,
        setLanguage,
        t,
        languages,
        currentLanguage,
    } = useLanguage();


    const {
        fields,
    } = useFields();


    const isHindi =
        language === "hi";


    /*
     * =========================================================
     * Profile state
     * =========================================================
     */

    const [
        profile,
        setProfile,
    ] = useState(null);


    const [
        loading,
        setLoading,
    ] = useState(true);


    const [
        saving,
        setSaving,
    ] = useState(false);


    const [
        uploadingPhoto,
        setUploadingPhoto,
    ] = useState(false);


    const [
        error,
        setError,
    ] = useState("");


    const [
        success,
        setSuccess,
    ] = useState("");


    const [
        editMode,
        setEditMode,
    ] = useState(false);


    /*
     * =========================================================
     * Edit form state
     * =========================================================
     */

    const [
        firstName,
        setFirstName,
    ] = useState("");


    const [
        lastName,
        setLastName,
    ] = useState("");


    const [
        email,
        setEmail,
    ] = useState("");


    const [
        mobile,
        setMobile,
    ] = useState("");


    const [
        preferredLanguage,
        setPreferredLanguage,
    ] = useState("EN");


    /*
     * =========================================================
     * Protected profile photo
     * =========================================================
     */

    const [
        photoDisplayUrl,
        setPhotoDisplayUrl,
    ] = useState(null);


    /*
     * =========================================================
     * Convert backend photo path → full URL
     * =========================================================
     */

    const buildPhotoUrl =
        (photoUrl) => {

            if (!photoUrl) {
                return null;
            }


            if (
                photoUrl.startsWith("http://") ||
                photoUrl.startsWith("https://")
            ) {
                return photoUrl;
            }


            return (
                `${API_BASE_URL}` +
                `${photoUrl.startsWith("/") ? "" : "/"}` +
                `${photoUrl}`
            );
        };


    /*
     * =========================================================
     * Load protected profile photo
     * =========================================================
     */

    const loadProfilePhoto =
        async (photoUrl) => {

            if (!photoUrl) {

                setPhotoDisplayUrl(
                    null
                );

                return;
            }


            const token =
                localStorage.getItem(
                    "agrinexus_token"
                );


            if (!token) {

                console.error(
                    "JWT token not found while loading profile photo."
                );

                setPhotoDisplayUrl(
                    null
                );

                return;
            }


            const fullPhotoUrl =
                buildPhotoUrl(
                    photoUrl
                );


            try {

                const response =
                    await fetch(
                        fullPhotoUrl,
                        {
                            method: "GET",

                            headers: {
                                Authorization:
                                    `Bearer ${token}`,

                                Accept:
                                    "image/*",
                            },
                        }
                    );


                if (!response.ok) {

                    throw new Error(
                        `Profile photo request failed with status ${response.status}`
                    );
                }


                const blob =
                    await response.blob();


                const objectUrl =
                    URL.createObjectURL(
                        blob
                    );


                setPhotoDisplayUrl(
                    (previousUrl) => {

                        if (previousUrl) {

                            URL.revokeObjectURL(
                                previousUrl
                            );
                        }


                        return objectUrl;
                    }
                );

            } catch (photoError) {

                console.error(
                    "Unable to load profile photo:",
                    photoError
                );


                setPhotoDisplayUrl(
                    null
                );
            }
        };


    /*
     * =========================================================
     * Load profile
     * =========================================================
     */

    useEffect(() => {

        let cancelled =
            false;


        async function loadProfile() {

            setLoading(true);
            setError("");


            try {

                const data =
                    await getMyProfile();


                if (cancelled) {
                    return;
                }


                setProfile(
                    data
                );


                /*
                 * Form values
                 */

                setFirstName(
                    data?.firstName || ""
                );


                setLastName(
                    data?.lastName || ""
                );


                setEmail(
                    data?.email || ""
                );


                setMobile(
                    data?.mobileNumber ||
                    data?.phone ||
                    ""
                );


                const backendLanguage =
                    String(
                        data?.preferredLanguage ||
                        "EN"
                    ).toUpperCase();


                setPreferredLanguage(
                    backendLanguage
                );


                /*
                 * Synchronize global language context.
                 *
                 * Backend:
                 *     EN / HI / MR / TA ...
                 *
                 * Frontend:
                 *     en / hi / mr / ta ...
                 */

                setLanguage(
                    backendLanguage.toLowerCase()
                );


                /*
                 * Local storage sync
                 */

                if (data?.firstName) {

                    localStorage.setItem(
                        "agrinexus_user_first_name",
                        data.firstName
                    );
                }


                if (data?.lastName) {

                    localStorage.setItem(
                        "agrinexus_user_last_name",
                        data.lastName
                    );
                }


                if (data?.email) {

                    localStorage.setItem(
                        "agrinexus_user_email",
                        data.email
                    );
                }


                if (data?.role) {

                    localStorage.setItem(
                        "agrinexus_user_role",
                        data.role
                    );
                }


                if (
                    data?.preferredLanguage
                ) {

                    localStorage.setItem(
                        "agrinexus_preferred_language",
                        backendLanguage
                    );
                }


                /*
                 * Load profile photo.
                 */

                if (
                    data?.profilePhotoUrl
                ) {

                    await loadProfilePhoto(
                        data.profilePhotoUrl
                    );
                }

            } catch (requestError) {

                console.error(
                    "Profile loading failed:",
                    requestError
                );


                /*
                 * Local fallback.
                 */

                const fallbackLanguage =
                    localStorage.getItem(
                        "agrinexus_preferred_language"
                    ) || "EN";


                const fallback = {

                    firstName:
                        localStorage.getItem(
                            "agrinexus_user_first_name"
                        ) || "",

                    lastName:
                        localStorage.getItem(
                            "agrinexus_user_last_name"
                        ) || "",

                    email:
                        localStorage.getItem(
                            "agrinexus_user_email"
                        ) || "",

                    mobile:
                        "",

                    preferredLanguage:
                    fallbackLanguage,

                    role:
                        localStorage.getItem(
                            "agrinexus_user_role"
                        ) || "FARMER",

                    active:
                        localStorage.getItem(
                            "agrinexus_user_active"
                        ) !== "false",

                    profilePhotoUrl:
                        null,
                };


                if (!cancelled) {

                    setProfile(
                        fallback
                    );


                    setFirstName(
                        fallback.firstName
                    );


                    setLastName(
                        fallback.lastName
                    );


                    setEmail(
                        fallback.email
                    );


                    setMobile(
                        fallback.mobile
                    );


                    setPreferredLanguage(
                        fallback.preferredLanguage
                    );


                    setLanguage(
                        fallbackLanguage.toLowerCase()
                    );
                }

            } finally {

                if (!cancelled) {

                    setLoading(false);
                }
            }
        }


        loadProfile();


        return () => {

            cancelled = true;
        };

    }, []);


    /*
     * =========================================================
     * Cleanup photo blob
     * =========================================================
     */

    useEffect(() => {

        return () => {

            if (photoDisplayUrl) {

                URL.revokeObjectURL(
                    photoDisplayUrl
                );
            }
        };

    }, [
        photoDisplayUrl,
    ]);


    /*
     * =========================================================
     * Derived profile information
     * =========================================================
     */

    const fullName =
        `${firstName} ${lastName}`
            .trim() ||
        "AgriNexus Farmer";


    const initials =
        useMemo(() => {

            const first =
                firstName?.charAt(0) ||
                "";


            const last =
                lastName?.charAt(0) ||
                "";


            return (
                `${first}${last}`
                    .toUpperCase() ||
                "AF"
            );

        }, [
            firstName,
            lastName,
        ]);


    /*
     * =========================================================
     * Save Profile
     * =========================================================
     */

    const handleSaveProfile =
        async () => {

            setError("");
            setSuccess("");


            if (
                !firstName.trim()
            ) {

                setError(
                    t(
                        "profile.firstName"
                    )
                );

                return;
            }


            if (
                !lastName.trim()
            ) {

                setError(
                    t(
                        "profile.lastName"
                    )
                );

                return;
            }


            if (
                !email.trim()
            ) {

                setError(
                    t(
                        "profile.email"
                    )
                );

                return;
            }


            setSaving(true);


            try {

                const payload = {

                    firstName:
                        firstName.trim(),

                    lastName:
                        lastName.trim(),

                    email:
                        email.trim(),

                    mobileNumber:
                        mobile.trim() ||
                        null,

                    preferredLanguage:
                        String(
                            preferredLanguage
                        ).toUpperCase(),
                };


                const updated =
                    await updateMyProfile(
                        payload
                    );


                setProfile(
                    (previous) => ({
                        ...previous,
                        ...updated,
                        ...payload,
                    })
                );


                localStorage.setItem(
                    "agrinexus_user_first_name",
                    payload.firstName
                );


                localStorage.setItem(
                    "agrinexus_user_last_name",
                    payload.lastName
                );


                localStorage.setItem(
                    "agrinexus_user_email",
                    payload.email
                );


                localStorage.setItem(
                    "agrinexus_preferred_language",
                    payload.preferredLanguage
                );


                /*
                 * Update global application language.
                 */

                setLanguage(
                    payload.preferredLanguage
                        .toLowerCase()
                );


                setSuccess(
                    t(
                        "common.saved"
                    ) ||
                    "Profile updated successfully."
                );


                setEditMode(
                    false
                );

            } catch (requestError) {

                console.error(
                    "Profile update failed:",
                    requestError
                );


                setError(
                    requestError?.message ||
                    "Unable to update profile."
                );

            } finally {

                setSaving(false);
            }
        };


    /*
     * =========================================================
     * Upload Profile Photo
     * =========================================================
     */

    const handlePhotoChange =
        async (event) => {

            const file =
                event.target.files?.[0];


            if (!file) {
                return;
            }


            setError("");
            setSuccess("");


            /*
             * Image validation
             */

            if (
                !file.type.startsWith(
                    "image/"
                )
            ) {

                setError(
                    "Please select an image file."
                );


                event.target.value = "";


                return;
            }


            /*
             * Maximum 5 MB
             */

            if (
                file.size >
                5 * 1024 * 1024
            ) {

                setError(
                    "Profile photo must be smaller than 5 MB."
                );


                event.target.value = "";


                return;
            }


            setUploadingPhoto(
                true
            );


            try {

                const response =
                    await uploadProfilePhoto(
                        file
                    );


                const newPhotoUrl =
                    response?.profilePhotoUrl ||
                    response?.photoUrl ||
                    response?.url;


                setProfile(
                    (previous) => ({
                        ...previous,
                        profilePhotoUrl:
                            newPhotoUrl ||
                            previous?.profilePhotoUrl ||
                            null,
                    })
                );


                /*
                 * Immediately load protected photo.
                 */

                if (
                    newPhotoUrl
                ) {

                    await loadProfilePhoto(
                        newPhotoUrl
                    );

                } else {

                    const refreshedProfile =
                        await getMyProfile();


                    setProfile(
                        refreshedProfile
                    );


                    if (
                        refreshedProfile?.profilePhotoUrl
                    ) {

                        await loadProfilePhoto(
                            refreshedProfile.profilePhotoUrl
                        );
                    }
                }


                setSuccess(
                    "Profile photo updated successfully."
                );

            } catch (requestError) {

                console.error(
                    "Photo upload failed:",
                    requestError
                );


                setError(
                    requestError?.message ||
                    "Unable to update profile photo."
                );

            } finally {

                setUploadingPhoto(
                    false
                );


                event.target.value =
                    "";
            }
        };


    /*
     * =========================================================
     * Delete Profile Photo
     * =========================================================
     */

    const handleDeletePhoto =
        async () => {

            setError("");
            setSuccess("");


            try {

                await deleteProfilePhoto();


                setPhotoDisplayUrl(
                    (previousUrl) => {

                        if (previousUrl) {

                            URL.revokeObjectURL(
                                previousUrl
                            );
                        }


                        return null;
                    }
                );


                setProfile(
                    (previous) => ({
                        ...previous,
                        profilePhotoUrl:
                            null,
                    })
                );


                setSuccess(
                    "Profile photo removed."
                );

            } catch (requestError) {

                console.error(
                    "Delete profile photo failed:",
                    requestError
                );


                setError(
                    requestError?.message ||
                    "Unable to remove profile photo."
                );
            }
        };


    /*
     * =========================================================
     * Change Preferred Language
     * =========================================================
     */

    const handleLanguageChange =
        async (code) => {

            const frontendCode =
                String(
                    code || ""
                ).toLowerCase();


            const backendCode =
                frontendCode.toUpperCase();


            setPreferredLanguage(
                backendCode
            );


            setError("");
            setSuccess("");


            /*
             * Update UI immediately.
             */

            setLanguage(
                frontendCode
            );


            localStorage.setItem(
                "agrinexus_preferred_language",
                backendCode
            );


            try {

                const updated =
                    await updateMyProfile({

                        firstName:
                            firstName.trim(),

                        lastName:
                            lastName.trim(),

                        email:
                            email.trim(),

                        mobileNumber:
                            mobile.trim() ||
                            null,

                        preferredLanguage:
                        backendCode,
                    });


                setProfile(
                    (previous) => ({
                        ...previous,
                        ...updated,
                        preferredLanguage:
                        backendCode,
                    })
                );


                setSuccess(
                    "Language preference updated."
                );

            } catch (requestError) {

                console.error(
                    "Language preference update failed:",
                    requestError
                );


                setError(
                    requestError?.message ||
                    "Unable to update language preference."
                );
            }
        };


    /*
     * =========================================================
     * Logout
     * =========================================================
     */

    const handleLogout =
        () => {

            clearAuthData();


            localStorage.removeItem(
                "agrinexus_current_farm_id"
            );


            localStorage.removeItem(
                "agrinexus_preferred_language"
            );


            navigate(
                "/login",
                {
                    replace: true,
                }
            );
        };


    /*
     * =========================================================
     * Loading
     * =========================================================
     */

    if (loading) {

        return (

            <div className="profile-container">

                <div className="profile-loading">

                    <div>
                        🌱
                    </div>

                    <p>
                        {t(
                            "common.loading"
                        )}
                    </p>

                </div>

            </div>
        );
    }


    /*
     * =========================================================
     * Render
     * =========================================================
     */

    return (

        <div className="profile-container">


            {/* =================================================
                Messages
            ================================================= */}

            {error && (

                <div
                    className="
                        profile-message
                        profile-message-error
                    "
                >

                    <span>
                        ⚠️
                    </span>

                    <span>
                        {error}
                    </span>

                </div>

            )}


            {success && (

                <div
                    className="
                        profile-message
                        profile-message-success
                    "
                >

                    <span>
                        ✅
                    </span>

                    <span>
                        {success}
                    </span>

                </div>

            )}


            {/* =================================================
                Profile Header
            ================================================= */}

            <section
                className="profile-header-card"
            >

                <div
                    className="
                        profile-photo-wrapper
                    "
                >

                    {photoDisplayUrl ? (

                        <img
                            src={
                                photoDisplayUrl
                            }
                            alt={
                                t(
                                    "profile.title"
                                )
                            }
                            className="
                                profile-photo
                            "
                            onError={() => {

                                setPhotoDisplayUrl(
                                    null
                                );
                            }}
                        />

                    ) : (

                        <div
                            className="
                                profile-avatar
                            "
                        >
                            {initials}
                        </div>

                    )}


                    <label
                        className="
                            profile-photo-edit
                        "
                        aria-label={
                            t(
                                "profile.changePhoto"
                            )
                        }
                    >

                        {
                            uploadingPhoto
                                ? "⏳"
                                : "📷"
                        }


                        <input
                            type="file"
                            accept="
                                image/png,
                                image/jpeg,
                                image/webp
                            "
                            onChange={
                                handlePhotoChange
                            }
                            className="
                                profile-photo-file-input
                            "
                            disabled={
                                uploadingPhoto
                            }
                        />

                    </label>

                </div>


                <h1
                    className="
                        profile-name
                    "
                >
                    {
                        fullName
                    }
                </h1>


                <div
                    className="
                        profile-role
                    "
                >
                    {
                        profile?.role ||
                        "FARMER"
                    }
                </div>


                {email && (

                    <div
                        className="
                            profile-email
                        "
                    >
                        ✉️{" "}
                        {email}
                    </div>

                )}


                {mobile && (

                    <div
                        className="
                            profile-email
                        "
                    >
                        📱{" "}
                        {mobile}
                    </div>

                )}


                <div
                    className="
                        profile-account-status
                    "
                >

                    <span>
                        ●
                    </span>

                    {
                        profile?.active !==
                        false

                            ? t(
                                "profile.accountActive"
                            )

                            : t(
                                "profile.accountInactive"
                            )
                    }

                </div>


                {photoDisplayUrl && (

                    <button
                        type="button"
                        className="
                            profile-remove-photo
                        "
                        onClick={
                            handleDeletePhoto
                        }
                        disabled={
                            uploadingPhoto
                        }
                    >

                        {
                            t(
                                "profile.removePhoto"
                            )
                        }

                    </button>

                )}

            </section>


            {/* =================================================
                Farm Statistics
            ================================================= */}

            <section
                className="
                    profile-summary-card
                "
            >

                <div
                    className="
                        profile-stat
                    "
                >

                    <div
                        className="
                            profile-stat-value
                        "
                    >
                        {
                            fields.length
                        }
                    </div>

                    <div
                        className="
                            profile-stat-label
                        "
                    >
                        {
                            t(
                                "profile.totalFarms"
                            )
                        }
                    </div>

                </div>


                <div
                    className="
                        profile-stat-divider
                    "
                />


                <div
                    className="
                        profile-stat
                    "
                >

                    <div
                        className="
                            profile-stat-value
                        "
                    >

                        {
                            fields.filter(
                                (field) =>
                                    field?.statusColor ===
                                    "warning"
                            ).length
                        }

                    </div>


                    <div
                        className="
                            profile-stat-label
                        "
                    >
                        {
                            t(
                                "profile.attention"
                            )
                        }
                    </div>

                </div>


                <div
                    className="
                        profile-stat-divider
                    "
                />


                <div
                    className="
                        profile-stat
                    "
                >

                    <div
                        className="
                            profile-stat-value
                        "
                    >

                        {
                            fields.filter(
                                (field) =>
                                    field?.statusColor ===
                                    "good"
                            ).length
                        }

                    </div>


                    <div
                        className="
                            profile-stat-label
                        "
                    >
                        {
                            t(
                                "profile.healthy"
                            )
                        }
                    </div>

                </div>

            </section>


            {/* =================================================
                Personal Information
            ================================================= */}

            <section
                className="
                    profile-section
                "
            >

                <div
                    className="
                        profile-section-title
                    "
                >

                    <span>
                        👤{" "}
                        {
                            t(
                                "profile.personalInformation"
                            )
                        }
                    </span>


                    {!editMode && (

                        <button
                            type="button"
                            className="
                                profile-inline-edit
                            "
                            onClick={() =>
                                setEditMode(
                                    true
                                )
                            }
                        >

                            ✏️{" "}
                            {
                                t(
                                    "profile.edit"
                                )
                            }

                        </button>

                    )}

                </div>


                <div
                    className="
                        profile-info-card
                    "
                >

                    {editMode ? (

                        <div
                            className="
                                profile-edit-form
                            "
                        >

                            <label>

                                {
                                    t(
                                        "profile.firstName"
                                    )
                                }

                                <input
                                    type="text"
                                    value={
                                        firstName
                                    }
                                    onChange={(event) =>
                                        setFirstName(
                                            event.target.value
                                        )
                                    }
                                    disabled={
                                        saving
                                    }
                                />

                            </label>


                            <label>

                                {
                                    t(
                                        "profile.lastName"
                                    )
                                }

                                <input
                                    type="text"
                                    value={
                                        lastName
                                    }
                                    onChange={(event) =>
                                        setLastName(
                                            event.target.value
                                        )
                                    }
                                    disabled={
                                        saving
                                    }
                                />

                            </label>


                            <label>

                                {
                                    t(
                                        "profile.mobile"
                                    )
                                }

                                <input
                                    type="tel"
                                    inputMode="numeric"
                                    value={
                                        mobile
                                    }
                                    onChange={(event) =>
                                        setMobile(
                                            event.target.value
                                        )
                                    }
                                    placeholder={
                                        t(
                                            "profile.mobilePlaceholder"
                                        )
                                    }
                                    disabled={
                                        saving
                                    }
                                />

                            </label>


                            <label>

                                {
                                    t(
                                        "profile.email"
                                    )
                                }

                                <input
                                    type="email"
                                    value={
                                        email
                                    }
                                    onChange={(event) =>
                                        setEmail(
                                            event.target.value
                                        )
                                    }
                                    disabled={
                                        saving
                                    }
                                />

                            </label>


                            <div
                                className="
                                    profile-edit-buttons
                                "
                            >

                                <button
                                    type="button"
                                    className="
                                        profile-save-button
                                    "
                                    onClick={
                                        handleSaveProfile
                                    }
                                    disabled={
                                        saving
                                    }
                                >

                                    {
                                        saving
                                            ? "Saving..."
                                            : t(
                                                "profile.saveChanges"
                                            )
                                    }

                                </button>


                                <button
                                    type="button"
                                    className="
                                        profile-cancel-button
                                    "
                                    onClick={() =>
                                        setEditMode(
                                            false
                                        )
                                    }
                                    disabled={
                                        saving
                                    }
                                >

                                    {
                                        t(
                                            "common.cancel"
                                        )
                                    }

                                </button>

                            </div>

                        </div>

                    ) : (

                        <>

                            <div
                                className="
                                    profile-info-row
                                "
                            >

                                <span>
                                    {
                                        t(
                                            "profile.name"
                                        )
                                    }
                                </span>

                                <strong>
                                    {
                                        fullName
                                    }
                                </strong>

                            </div>


                            <div
                                className="
                                    profile-info-row
                                "
                            >

                                <span>
                                    {
                                        t(
                                            "profile.mobile"
                                        )
                                    }
                                </span>

                                <strong>
                                    {
                                        mobile ||
                                        "—"
                                    }
                                </strong>

                            </div>


                            <div
                                className="
                                    profile-info-row
                                "
                            >

                                <span>
                                    {
                                        t(
                                            "profile.email"
                                        )
                                    }
                                </span>

                                <strong>
                                    {
                                        email ||
                                        "—"
                                    }
                                </strong>

                            </div>


                            <div
                                className="
                                    profile-info-row
                                "
                            >

                                <span>
                                    {
                                        t(
                                            "profile.role"
                                        )
                                    }
                                </span>

                                <strong>
                                    {
                                        profile?.role ||
                                        "FARMER"
                                    }
                                </strong>

                            </div>

                        </>

                    )}

                </div>

            </section>


            {/* =================================================
                Preferred Language
            ================================================= */}

            <section
                className="
                    profile-section
                "
            >

                <div
                    className="
                        profile-section-title
                    "
                >

                    🌐{" "}
                    {
                        t(
                            "profile.preferredLanguage"
                        )
                    }

                </div>


                <div
                    className="
                        profile-language-card
                    "
                >

                    <div
                        className="
                            profile-current-language
                        "
                    >

                        <span>

                            {
                                currentLanguage?.nativeName ||
                                "English"
                            }

                        </span>


                        <small>

                            {
                                currentLanguage?.name ||
                                "English"
                            }

                        </small>

                    </div>


                    <select
                        className="
                            profile-language-select
                        "
                        value={
                            String(
                                preferredLanguage
                            ).toLowerCase()
                        }
                        onChange={(event) =>
                            handleLanguageChange(
                                event.target.value
                            )
                        }
                        disabled={
                            saving ||
                            uploadingPhoto
                        }
                    >

                        {languages.map(
                            (item) => (

                                <option
                                    key={
                                        item.code
                                    }
                                    value={
                                        item.code
                                    }
                                >

                                    {
                                        item.nativeName
                                    }

                                    {" — "}

                                    {
                                        item.name
                                    }

                                </option>

                            )
                        )}

                    </select>


                    <div
                        className="
                            profile-language-count
                        "
                    >

                        22 languages available

                    </div>

                </div>

            </section>


            {/* =================================================
                Quick Actions
            ================================================= */}

            <section
                className="
                    profile-section
                "
            >

                <div
                    className="
                        profile-section-title
                    "
                >

                    ⚡{" "}
                    {
                        t(
                            "profile.quickActions"
                        )
                    }

                </div>


                <div
                    className="
                        profile-actions-card
                    "
                >

                    <button
                        type="button"
                        className="
                            profile-action-row
                        "
                        onClick={() =>
                            navigate(
                                "/history"
                            )
                        }
                    >

                        <span
                            className="
                                profile-action-icon
                            "
                        >
                            📜
                        </span>


                        <span
                            className="
                                profile-action-content
                            "
                        >

                            <strong>
                                {
                                    t(
                                        "navigation.history"
                                    )
                                }
                            </strong>


                            <small>
                                {
                                    t(
                                        "profile.viewCropHistory"
                                    )
                                }
                            </small>

                        </span>


                        <span
                            className="
                                profile-action-arrow
                            "
                        >
                            →
                        </span>

                    </button>


                    <button
                        type="button"
                        className="
                            profile-action-row
                        "
                        onClick={() =>
                            navigate(
                                "/add-field"
                            )
                        }
                    >

                        <span
                            className="
                                profile-action-icon
                            "
                        >
                            ➕
                        </span>


                        <span
                            className="
                                profile-action-content
                            "
                        >

                            <strong>
                                {
                                    t(
                                        "farm.addNew"
                                    )
                                }
                            </strong>


                            <small>
                                {
                                    t(
                                        "profile.registerAnotherFarm"
                                    )
                                }
                            </small>

                        </span>


                        <span
                            className="
                                profile-action-arrow
                            "
                        >
                            →
                        </span>

                    </button>

                </div>

            </section>


            {/* =================================================
                Logout
            ================================================= */}

            <button
                type="button"
                className="
                    profile-logout-button
                "
                onClick={
                    handleLogout
                }
            >

                🚪{" "}

                {
                    t(
                        "profile.logout"
                    )
                }

            </button>


            {/* =================================================
                Footer
            ================================================= */}

            <div
                className="
                    profile-footer
                "
            >

                AgriNexus

                <span>
                    {" • "}
                </span>

                {
                    t(
                        "profile.footer"
                    )
                }

            </div>

        </div>
    );
}


export default Profile;