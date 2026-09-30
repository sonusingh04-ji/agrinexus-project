import { apiRequest } from "./apiClient";
export const DEMO_FARM_ID =
    "05efa1f0-4296-43f4-bee7-d139608b4b5f";

/*
 * =========================================================
 * Crop APIs
 * =========================================================
 */

export async function getAllCrops() {
    return apiRequest("/api/crops");
}

export async function getCropById(cropId) {
    if (!cropId) {
        throw new Error("cropId is required");
    }

    return apiRequest(
        `/api/crops/${encodeURIComponent(cropId)}`
    );
}

export async function getCropsBySeason(season) {
    if (!season) {
        throw new Error("season is required");
    }

    return apiRequest(
        `/api/crops/season/${encodeURIComponent(season)}`
    );
}

export async function getCropsBySoilType(soilType) {
    if (!soilType) {
        throw new Error("soilType is required");
    }

    return apiRequest(
        `/api/crops/soil/${encodeURIComponent(soilType)}`
    );
}

export async function getCropsByPh(ph) {
    if (ph === null || ph === undefined) {
        throw new Error("pH is required");
    }

    return apiRequest(
        `/api/crops/ph/${encodeURIComponent(ph)}`
    );
}


/*
 * =========================================================
 * Soil Intelligence
 * =========================================================
 */

export async function getSoilIntelligence(farmId) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/soil-intelligence/farm/${farmId}`
    );
}

export async function getSoilIntelligenceForCrop(
    farmId,
    cropId
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    if (!cropId) {
        throw new Error("cropId is required");
    }

    return apiRequest(
        `/api/soil-intelligence/farm/${farmId}/crop/${cropId}`
    );
}


/*
 * =========================================================
 * Soil Samples
 * =========================================================
 */

export async function getLatestSoilSample(farmId) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/soil-samples/farm/${farmId}/latest`
    );
}

export async function getSoilSamples(farmId) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/soil-samples/farm/${farmId}`
    );
}


/*
 * =========================================================
 * Crop Rotation
 * =========================================================
 */

export async function recommendNextCrop({
                                            farmId,
                                            previousCropId = null,
                                            targetSeason,
                                            farmerConstraint = "NONE",
                                        }) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    if (!targetSeason) {
        throw new Error("targetSeason is required");
    }

    return apiRequest(
        "/api/crop-rotation/recommend",
        {
            method: "POST",
            body: JSON.stringify({
                farmId,
                previousCropId,
                targetSeason,
                farmerConstraint,
            }),
        }
    );
}

export async function getLatestCropRotation(
    farmId
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/crop-rotation/farm/${farmId}/latest`
    );
}

export async function getCropRotationHistory(
    farmId
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/crop-rotation/farm/${farmId}`
    );
}


/*
 * =========================================================
 * Soil Recovery
 * =========================================================
 */

export async function getActiveRecoveryPlans(
    farmId
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/soil-recovery-plans/farm/${farmId}/active`
    );
}

export async function getLatestRecoveryPlan(
    farmId
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/soil-recovery-plans/farm/${farmId}/latest`
    );
}


/*
 * =========================================================
 * Soil Test Trend
 * =========================================================
 */

export async function getSoilTestTrend(
    farmId
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/soil-test-trends/farm/${farmId}`
    );
}