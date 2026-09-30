import { apiRequest } from "./apiClient";

/*
 * =========================================================
 * LAND COVER MAPPING / LULC API
 * =========================================================
 */

/**
 * Synchronize the latest LULC data for a farm.
 *
 * Backend:
 * POST /api/lulc/farm/{farmId}/sync
 */
export async function syncLulc(farmId) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/lulc/farm/${farmId}/sync`,
        {
            method: "POST",
        }
    );
}


/**
 * Get the latest stored LULC observation.
 *
 * Backend:
 * GET /api/lulc/farm/{farmId}
 */
export async function getLatestLulc(farmId) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/lulc/farm/${farmId}`
    );
}


/**
 * Get LULC observation history.
 *
 * Backend:
 * GET /api/lulc/farm/{farmId}/history
 */
export async function getLulcHistory(farmId) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/lulc/farm/${farmId}/history`
    );
}