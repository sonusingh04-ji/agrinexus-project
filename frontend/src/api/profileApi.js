import { apiRequest, API_BASE_URL } from "./apiClient";

/*
 * =========================================================
 * Profile APIs
 * =========================================================
 */

/*
 * Get currently logged-in user profile.
 *
 * Expected:
 * GET /api/users/me
 */
export async function getMyProfile() {
    return apiRequest("/api/users/me");
}


/*
 * Update profile details.
 *
 * Expected:
 * PUT /api/users/me
 */
export async function updateMyProfile(data) {
    return apiRequest(
        "/api/users/me",
        {
            method: "PUT",
            body: JSON.stringify(data),
        }
    );
}


/*
 * =========================================================
 * Profile Photo
 * =========================================================
 *
 * We intentionally do not use apiRequest here because
 * this request uses multipart/form-data.
 */
export async function uploadProfilePhoto(file) {

    if (!file) {
        throw new Error(
            "Profile photo is required."
        );
    }

    const token =
        localStorage.getItem(
            "agrinexus_token"
        );

    const formData =
        new FormData();

    formData.append(
        "file",
        file
    );

    const response =
        await fetch(
            `${API_BASE_URL}/api/users/me/photo`,
            {
                method: "POST",

                headers: token
                    ? {
                        Authorization:
                            `Bearer ${token}`,
                    }
                    : {},

                body: formData,
            }
        );

    let data = null;

    const contentType =
        response.headers.get(
            "content-type"
        );

    if (
        contentType &&
        contentType.includes(
            "application/json"
        )
    ) {
        data =
            await response.json();
    } else {

        const text =
            await response.text();

        data =
            text || null;
    }

    if (!response.ok) {

        const message =
            data?.message ||
            data?.error ||
            `Photo upload failed with status ${response.status}`;

        const error =
            new Error(message);

        error.status =
            response.status;

        error.data =
            data;

        throw error;
    }

    return data;
}


/*
 * =========================================================
 * Remove Profile Photo
 * =========================================================
 *
 * Expected:
 * DELETE /api/users/me/photo
 */
export async function deleteProfilePhoto() {
    return apiRequest(
        "/api/users/me/photo",
        {
            method: "DELETE",
        }
    );
}