import { apiRequest } from "./apiClient";


/* =========================================================
   VALIDATION HELPERS
   ========================================================= */

const requireFarmId = (farmId) => {
    if (!farmId) {
        throw new Error("farmId is required");
    }
};

const requireBalanceId = (balanceId) => {
    if (!balanceId) {
        throw new Error("balanceId is required");
    }
};


/* =========================================================
   NUTRIENT BALANCE
   ========================================================= */

export async function getFarmNutrientBalances(
    farmId
) {
    requireFarmId(farmId);

    return apiRequest(
        `/api/nutrient-balances/farm/${farmId}`
    );
}


export async function getLatestNutrientBalance(
    farmId
) {
    requireFarmId(farmId);

    return apiRequest(
        `/api/nutrient-balances/farm/${farmId}/latest`
    );
}


export async function getCropNutrientBalances(
    farmId,
    cropId
) {
    requireFarmId(farmId);

    if (!cropId) {
        throw new Error("cropId is required");
    }

    return apiRequest(
        `/api/nutrient-balances/farm/${farmId}/crop/${cropId}`
    );
}


export async function getLatestCropNutrientBalance(
    farmId,
    cropId
) {
    requireFarmId(farmId);

    if (!cropId) {
        throw new Error("cropId is required");
    }

    return apiRequest(
        `/api/nutrient-balances/farm/${farmId}/crop/${cropId}/latest`
    );
}


export async function getNutrientBalancesByDateRange(
    farmId,
    startDate,
    endDate
) {
    requireFarmId(farmId);

    if (!startDate || !endDate) {
        throw new Error(
            "startDate and endDate are required"
        );
    }

    const params = new URLSearchParams({
        startDate,
        endDate,
    });

    return apiRequest(
        `/api/nutrient-balances/farm/${farmId}/range?${params.toString()}`
    );
}


export async function getNutrientBalanceById(
    balanceId
) {
    requireBalanceId(balanceId);

    return apiRequest(
        `/api/nutrient-balances/${balanceId}`
    );
}


export async function createNutrientBalance(
    data
) {
    requireFarmId(data?.farmId);

    return apiRequest(
        "/api/nutrient-balances",
        {
            method: "POST",
            body: JSON.stringify(data),
        }
    );
}


export async function updateNutrientBalance(
    balanceId,
    data
) {
    requireBalanceId(balanceId);
    requireFarmId(data?.farmId);

    return apiRequest(
        `/api/nutrient-balances/${balanceId}`,
        {
            method: "PUT",
            body: JSON.stringify(data),
        }
    );
}


export async function deleteNutrientBalance(
    balanceId
) {
    requireBalanceId(balanceId);

    return apiRequest(
        `/api/nutrient-balances/${balanceId}`,
        {
            method: "DELETE",
        }
    );
}


/* =========================================================
   REAL FERTILIZER INPUTS
   =========================================================

   These are not fabricated nutrient-balance values.
   They are the nutrient amounts already recorded by
   the fertilizer-input backend module.
   ========================================================= */

export async function getFarmFertilizerInputs(
    farmId
) {
    requireFarmId(farmId);

    return apiRequest(
        `/api/fertilizer-inputs/farm/${farmId}`
    );
}


/* =========================================================
   BACKWARD-COMPATIBILITY ALIASES
   ========================================================= */

export async function getLatestFarmBalance(
    farmId
) {
    return getLatestNutrientBalance(
        farmId
    );
}


export async function getFarmBalances(
    farmId
) {
    return getFarmNutrientBalances(
        farmId
    );
}


export async function getLatestCropBalance(
    farmId,
    cropId
) {
    return getLatestCropNutrientBalance(
        farmId,
        cropId
    );
}