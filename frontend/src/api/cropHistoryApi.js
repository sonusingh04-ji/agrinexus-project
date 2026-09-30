import { apiRequest } from "./apiClient";

/*
 * =========================================================
 * NORMALIZE CROP HISTORY RESPONSE
 * =========================================================
 *
 * Backend may return:
 * - Array
 * - { content: [] }
 * - { data: [] }
 * - Single object
 *
 * This function always returns an array.
 */

function normalizeCropHistoryResponse(response) {
    if (Array.isArray(response)) {
        return response;
    }

    if (Array.isArray(response?.content)) {
        return response.content;
    }

    if (Array.isArray(response?.data)) {
        return response.data;
    }

    if (response?.id) {
        return [response];
    }

    return [];
}


/*
 * =========================================================
 * GET FARM CROP HISTORY
 * =========================================================
 */

export async function getFarmCropHistory(farmId) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/crop-history/farm/${farmId}`
    );
}


/*
 * =========================================================
 * CREATE CROP HISTORY
 * =========================================================
 */

export async function createCropHistory(data) {
    if (!data?.farmId) {
        throw new Error("farmId is required");
    }

    if (!data?.cropId) {
        throw new Error("cropId is required");
    }

    return apiRequest(
        "/api/crop-history",
        {
            method: "POST",
            body: JSON.stringify(data),
        }
    );
}


/*
 * =========================================================
 * GET CROP HISTORY BY ID
 * =========================================================
 */

export async function getCropHistoryById(historyId) {
    if (!historyId) {
        throw new Error("historyId is required");
    }

    return apiRequest(
        `/api/crop-history/${historyId}`
    );
}


/*
 * =========================================================
 * GET LATEST CROP HISTORY
 * =========================================================
 */

export async function getLatestCropHistory(farmId) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/crop-history/farm/${farmId}/latest`
    );
}


/*
 * =========================================================
 * CHECK WHETHER CROP HISTORY RECORD IS ACTIVE
 * =========================================================
 *
 * IMPORTANT:
 * We do NOT create any fake crop.
 *
 * A crop is considered current only when:
 * - harvestDate is missing/null
 *
 * If backend later provides an explicit active/current flag,
 * that flag is also respected.
 */

function isActiveCropRecord(record) {
    if (!record || typeof record !== "object") {
        return false;
    }

    /*
     * Explicit backend state, when available
     */
    if (
        record.active === true ||
        record.isActive === true ||
        record.current === true ||
        record.isCurrent === true
    ) {
        return true;
    }

    /*
     * Explicitly completed/harvested records
     */
    if (
        record.active === false ||
        record.isActive === false ||
        record.current === false ||
        record.isCurrent === false
    ) {
        return false;
    }

    /*
     * Normal AgriNexus crop-history rule:
     * an unharvested crop is the current crop.
     */
    return (
        record.harvestDate == null ||
        record.harvestDate === ""
    );
}


/*
 * =========================================================
 * GET CURRENT CROP FOR FARM
 * =========================================================
 *
 * SINGLE SOURCE OF TRUTH
 *
 * This function:
 * 1. Loads the real crop-history records for the farm.
 * 2. Keeps only the active/unharvested crop.
 * 3. Returns the newest matching record.
 * 4. Returns null when no real current crop exists.
 *
 * NEVER returns a fake/default crop.
 */

export async function getCurrentCropForFarm(farmId) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    const response = await getFarmCropHistory(farmId);

    const records =
        normalizeCropHistoryResponse(response);

    if (records.length === 0) {
        return null;
    }

    /*
     * Keep only records belonging to the requested farm
     * when farmId is present in the response.
     */
    const farmRecords = records.filter((record) => {
        if (!record?.farmId) {
            return true;
        }

        return (
            String(record.farmId) ===
            String(farmId)
        );
    });

    /*
     * Find active/unharvested crop records.
     */
    const activeRecords =
        farmRecords.filter(isActiveCropRecord);

    if (activeRecords.length === 0) {
        return null;
    }

    /*
     * Sort newest first.
     *
     * Primary:
     * - sowingDate
     *
     * Secondary:
     * - createdAt
     *
     * Tertiary:
     * - id
     */
    activeRecords.sort((a, b) => {
        const dateA =
            new Date(
                a?.sowingDate ||
                a?.createdAt ||
                0
            ).getTime();

        const dateB =
            new Date(
                b?.sowingDate ||
                b?.createdAt ||
                0
            ).getTime();

        if (dateB !== dateA) {
            return dateB - dateA;
        }

        return String(b?.id || "").localeCompare(
            String(a?.id || "")
        );
    });

    return activeRecords[0] || null;
}


/*
 * =========================================================
 * GET CURRENT CROP NAME
 * =========================================================
 *
 * Convenience helper for pages that only need the name.
 *
 * IMPORTANT:
 * No hardcoded crop name is returned.
 * Missing backend value => null
 */

export async function getCurrentCropName(farmId) {
    const currentCrop =
        await getCurrentCropForFarm(farmId);

    if (!currentCrop) {
        return null;
    }

    return (
        currentCrop?.cropName ??
        currentCrop?.crop?.name ??
        currentCrop?.name ??
        null
    );
}


/*
 * =========================================================
 * GET CURRENT CROP STAGE
 * =========================================================
 *
 * Convenience helper for pages that need the current stage.
 *
 * Missing backend value => null
 */

export async function getCurrentCropStage(farmId) {
    const currentCrop =
        await getCurrentCropForFarm(farmId);

    if (!currentCrop) {
        return null;
    }

    return (
        currentCrop?.cropStage ??
        currentCrop?.stage ??
        null
    );
}