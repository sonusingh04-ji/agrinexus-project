import { apiRequest } from "./apiClient";

/**
 * Generate a new sowing decision.
 *
 * POST /api/sowing-advisor/recommend
 */
export async function getSowingRecommendation(farmId, cropId) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    if (!cropId) {
        throw new Error("cropId is required");
    }

    return apiRequest("/api/sowing-advisor/recommend", {
        method: "POST",
        body: JSON.stringify({
            farmId,
            cropId,
        }),
    });
}

/**
 * Get latest sowing decision for a farm.
 *
 * GET /api/sowing-advisor/farm/{farmId}/latest
 *
 * apiRequest already converts HTTP 204 into null.
 */
export async function getLatestSowingRecommendation(farmId) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/sowing-advisor/farm/${farmId}/latest`
    );
}

/**
 * Get all sowing decisions for a farm.
 *
 * GET /api/sowing-advisor/farm/{farmId}
 */
export async function getFarmSowingRecommendations(farmId) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/sowing-advisor/farm/${farmId}`
    );
}

/**
 * Get a sowing decision by ID.
 *
 * GET /api/sowing-advisor/{id}
 */
export async function getSowingRecommendationById(id) {
    if (!id) {
        throw new Error("decision id is required");
    }

    return apiRequest(
        `/api/sowing-advisor/${id}`
    );
}