const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL ||
    "http://localhost:8080";


/*
 * =========================================================
 * AUTH ENDPOINT DETECTION
 * =========================================================
 *
 * Authentication endpoints are public.
 *
 * IMPORTANT:
 * Never attach an old/expired JWT to:
 *
 * /api/auth/login
 * /api/auth/register
 * /api/auth/forgot-password
 * /api/auth/reset-password
 *
 * Otherwise the JWT filter can reject the request before
 * the public authentication controller gets a chance to
 * process it.
 * =========================================================
 */

const isAuthEndpoint = (
    endpoint
) => {
    return String(
        endpoint || ""
    ).startsWith(
        "/api/auth/"
    );
};


/*
 * =========================================================
 * Generic API Request
 * =========================================================
 */

export async function apiRequest(
    endpoint,
    options = {}
) {

    /*
     * Read the current JWT.
     */
    const token =
        localStorage.getItem(
            "agrinexus_token"
        );


    /*
     * Detect authentication endpoints.
     */
    const authEndpoint =
        isAuthEndpoint(
            endpoint
        );


    /*
     * ---------------------------------------------------------
     * Headers
     * ---------------------------------------------------------
     */

    const headers = {
        Accept:
            "application/json",

        /*
         * Do not set Content-Type for FormData.
         */
        ...(options.body instanceof FormData
            ? {}
            : {
                "Content-Type":
                    "application/json",
            }),

        /*
         * Allow individual requests to provide
         * additional/override headers.
         */
        ...(options.headers || {}),
    };


    /*
     * ---------------------------------------------------------
     * Authorization
     * ---------------------------------------------------------
     *
     * Attach JWT only to protected endpoints.
     *
     * NEVER send an old JWT to /api/auth/*.
     * ---------------------------------------------------------
     */

    if (
        token &&
        !authEndpoint
    ) {
        headers.Authorization =
            `Bearer ${token}`;
    }


    /*
     * ---------------------------------------------------------
     * HTTP request
     * ---------------------------------------------------------
     */

    let response;


    try {

        response =
            await fetch(
                `${API_BASE_URL}${endpoint}`,
                {
                    ...options,
                    headers,
                }
            );

    } catch (
        networkError
        ) {

        console.error(
            "AgriNexus API connection failed:",
            networkError
        );


        const error =
            new Error(
                "Unable to connect to AgriNexus backend."
            );


        error.status = 0;

        error.data = null;


        throw error;
    }


    /*
     * =========================================================
     * 204 NO CONTENT
     * =========================================================
     */

    if (
        response.status ===
        204
    ) {
        return null;
    }


    /*
     * =========================================================
     * RESPONSE PARSING
     * =========================================================
     */

    let data = null;


    const contentType =
        response.headers.get(
            "content-type"
        ) || "";


    try {

        if (
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

    } catch (
        parseError
        ) {

        console.error(
            "Failed to parse API response:",
            parseError
        );


        data = null;
    }


    /*
     * =========================================================
     * HTTP ERROR
     * =========================================================
     */

    if (
        !response.ok
    ) {

        /*
         * The backend's authentication contract says:
         *
         * 401 = invalid/expired JWT.
         *
         * Clear stale authentication state for protected
         * requests so the application does not keep sending
         * the same invalid token again and again.
         *
         * We intentionally DO NOT do this for /api/auth/*
         * because a login 401 may simply mean:
         *
         * "Invalid email or password"
         *
         * rather than an expired session.
         */

        if (
            response.status === 401 &&
            !authEndpoint
        ) {
            clearAuthData();

            /*
             * Current farm selection belongs to the authenticated
             * session. Clear it as well so the next successful
             * login starts from a valid farm selection.
             */
            clearCurrentFarmId();
        }


        /*
         * Read the backend error message.
         */

        const message =
            data?.message ||
            data?.error ||
            data?.detail ||
            (
                response.status === 401
                    ? "Invalid or expired authentication token"
                    : `Request failed with status ${response.status}`
            );


        const error =
            new Error(
                message
            );


        error.status =
            response.status;


        error.data =
            data;


        throw error;
    }


    /*
     * =========================================================
     * SUCCESS
     * =========================================================
     */

    return data;
}


/*
 * =========================================================
 * AUTHENTICATION
 * =========================================================
 */


/*
 * Store JWT + authenticated user information.
 *
 * Backend AuthResponse:
 *
 * userId
 * firstName
 * lastName
 * email
 * role
 * active
 * token
 * tokenType
 */

export function setAuthData(
    authResponse
) {

    if (
        !authResponse
    ) {
        return;
    }


    /*
     * JWT
     */

    if (
        authResponse.token
    ) {

        localStorage.setItem(
            "agrinexus_token",
            String(
                authResponse.token
            )
        );
    }


    /*
     * User ID
     */

    if (
        authResponse.userId
    ) {

        localStorage.setItem(
            "agrinexus_user_id",
            String(
                authResponse.userId
            )
        );
    }


    /*
     * Email
     */

    if (
        authResponse.email
    ) {

        localStorage.setItem(
            "agrinexus_user_email",
            String(
                authResponse.email
            )
        );
    }


    /*
     * First name
     */

    if (
        authResponse.firstName
    ) {

        localStorage.setItem(
            "agrinexus_user_first_name",
            String(
                authResponse.firstName
            )
        );
    }


    /*
     * Last name
     */

    if (
        authResponse.lastName
    ) {

        localStorage.setItem(
            "agrinexus_user_last_name",
            String(
                authResponse.lastName
            )
        );
    }


    /*
     * Role
     */

    if (
        authResponse.role
    ) {

        localStorage.setItem(
            "agrinexus_user_role",
            String(
                authResponse.role
            )
        );
    }


    /*
     * Active state
     */

    localStorage.setItem(
        "agrinexus_user_active",
        String(
            authResponse.active ??
            true
        )
    );
}


/*
 * =========================================================
 * CLEAR AUTHENTICATION
 * =========================================================
 */

export function clearAuthData() {

    localStorage.removeItem(
        "agrinexus_token"
    );

    localStorage.removeItem(
        "agrinexus_user_id"
    );

    localStorage.removeItem(
        "agrinexus_user_email"
    );

    localStorage.removeItem(
        "agrinexus_user_first_name"
    );

    localStorage.removeItem(
        "agrinexus_user_last_name"
    );

    localStorage.removeItem(
        "agrinexus_user_role"
    );

    localStorage.removeItem(
        "agrinexus_user_active"
    );
}


/*
 * =========================================================
 * AUTHENTICATION CHECK
 * =========================================================
 */

export function isAuthenticated() {

    return Boolean(
        localStorage.getItem(
            "agrinexus_token"
        )
    );
}


/*
 * =========================================================
 * CURRENT FARM
 * =========================================================
 */

const CURRENT_FARM_KEY =
    "agrinexus_current_farm_id";


/*
 * Set current farm.
 */

export function setCurrentFarmId(
    farmId
) {

    if (
        !farmId
    ) {
        return;
    }


    localStorage.setItem(
        CURRENT_FARM_KEY,
        String(
            farmId
        )
    );
}


/*
 * Get current farm.
 */

export function getCurrentFarmId() {

    return localStorage.getItem(
        CURRENT_FARM_KEY
    );
}


/*
 * Clear current farm.
 */

export function clearCurrentFarmId() {

    localStorage.removeItem(
        CURRENT_FARM_KEY
    );
}


/*
 * =========================================================
 * API BASE URL
 * =========================================================
 */

export {
    API_BASE_URL
};