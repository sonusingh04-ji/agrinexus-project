import { apiRequest } from "./apiClient";

/*
 * ============================================================
 * FERTILIZER RECOMMENDATION API
 * ============================================================
 */

export async function generateFertilizerRecommendation(payload) {
    if (!payload?.farmId) {
        throw new Error("farmId is required");
    }

    if (!payload?.cropId) {
        throw new Error("cropId is required");
    }

    if (!payload?.cropStage) {
        throw new Error("cropStage is required");
    }

    if (
        payload.fieldArea === null ||
        payload.fieldArea === undefined ||
        Number(payload.fieldArea) <= 0
    ) {
        throw new Error("fieldArea must be greater than zero");
    }

    if (!payload?.fieldAreaUnit) {
        throw new Error("fieldAreaUnit is required");
    }

    return apiRequest(
        "/api/fertilizer-recommendations",
        {
            method: "POST",
            body: JSON.stringify({
                farmId: payload.farmId,
                cropId: payload.cropId,
                cropStage: payload.cropStage,
                fieldArea: Number(payload.fieldArea),
                fieldAreaUnit: payload.fieldAreaUnit,
                ...(payload.farmerNote
                    ? {
                        farmerNote:
                        payload.farmerNote,
                    }
                    : {}),
            }),
        }
    );
}

export async function getLatestFertilizerRecommendation(
    farmId
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/fertilizer-recommendations/farm/${farmId}/latest`
    );
}

export async function getFarmFertilizerRecommendations(
    farmId
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/fertilizer-recommendations/farm/${farmId}`
    );
}

export async function getCropFertilizerRecommendations(
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
        `/api/fertilizer-recommendations/farm/${farmId}/crop/${cropId}`
    );
}

export async function getFertilizerRecommendationById(
    recommendationId
) {
    if (!recommendationId) {
        throw new Error(
            "recommendationId is required"
        );
    }

    return apiRequest(
        `/api/fertilizer-recommendations/${recommendationId}`
    );
}


/*
 * ============================================================
 * FERTILIZER SCHEDULE API
 * ============================================================
 */

export async function createFertilizerSchedule(
    payload
) {
    if (!payload?.farmId) {
        throw new Error("farmId is required");
    }

    if (!payload?.cropId) {
        throw new Error("cropId is required");
    }

    if (!payload?.stage) {
        throw new Error("stage is required");
    }

    if (!payload?.plannedDate) {
        throw new Error("plannedDate is required");
    }

    if (!payload?.actionType) {
        throw new Error("actionType is required");
    }

    return apiRequest(
        "/api/fertilizer-schedules",
        {
            method: "POST",
            body: JSON.stringify({
                farmId: payload.farmId,
                cropId: payload.cropId,

                ...(payload.recommendationId
                    ? {
                        recommendationId:
                        payload.recommendationId,
                    }
                    : {}),

                stage:
                payload.stage,

                plannedDate:
                payload.plannedDate,

                actionType:
                payload.actionType,

                ...(payload.productName
                    ? {
                        productName:
                        payload.productName,
                    }
                    : {}),

                ...(payload.nitrogen !== null &&
                payload.nitrogen !== undefined &&
                payload.nitrogen !== ""
                    ? {
                        nitrogen:
                            Number(
                                payload.nitrogen
                            ),
                    }
                    : {}),

                ...(payload.phosphorus !== null &&
                payload.phosphorus !== undefined &&
                payload.phosphorus !== ""
                    ? {
                        phosphorus:
                            Number(
                                payload.phosphorus
                            ),
                    }
                    : {}),

                ...(payload.potassium !== null &&
                payload.potassium !== undefined &&
                payload.potassium !== ""
                    ? {
                        potassium:
                            Number(
                                payload.potassium
                            ),
                    }
                    : {}),

                ...(payload.sulphur !== null &&
                payload.sulphur !== undefined &&
                payload.sulphur !== ""
                    ? {
                        sulphur:
                            Number(
                                payload.sulphur
                            ),
                    }
                    : {}),

                ...(payload.zinc !== null &&
                payload.zinc !== undefined &&
                payload.zinc !== ""
                    ? {
                        zinc:
                            Number(
                                payload.zinc
                            ),
                    }
                    : {}),

                ...(payload.status
                    ? {
                        status:
                        payload.status,
                    }
                    : {}),

                ...(payload.weatherCondition
                    ? {
                        weatherCondition:
                        payload.weatherCondition,
                    }
                    : {}),

                ...(payload.reassessmentRequired !==
                null &&
                payload.reassessmentRequired !==
                undefined
                    ? {
                        reassessmentRequired:
                            Boolean(
                                payload.reassessmentRequired
                            ),
                    }
                    : {}),

                ...(payload.source
                    ? {
                        source:
                        payload.source,
                    }
                    : {}),

                ...(payload.uncertaintyNote
                    ? {
                        uncertaintyNote:
                        payload.uncertaintyNote,
                    }
                    : {}),

                ...(payload.notes
                    ? {
                        notes:
                        payload.notes,
                    }
                    : {}),
            }),
        }
    );
}

export async function getLatestFertilizerSchedule(
    farmId
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/fertilizer-schedules/farm/${farmId}/latest`
    );
}

export async function getNextFertilizerSchedule(
    farmId,
    date = null
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    const endpoint =
        date
            ? `/api/fertilizer-schedules/farm/${farmId}/next?date=${encodeURIComponent(
                date
            )}`
            : `/api/fertilizer-schedules/farm/${farmId}/next`;

    return apiRequest(
        endpoint
    );
}

export async function getFarmFertilizerSchedules(
    farmId
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/fertilizer-schedules/farm/${farmId}`
    );
}

export async function getFertilizerSchedulesByStatus(
    farmId,
    status
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    if (!status) {
        throw new Error("status is required");
    }

    return apiRequest(
        `/api/fertilizer-schedules/farm/${farmId}/status/${encodeURIComponent(
            status
        )}`
    );
}

export async function getFertilizerSchedulesByStage(
    farmId,
    cropId,
    stage
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    if (!cropId) {
        throw new Error("cropId is required");
    }

    if (!stage) {
        throw new Error("stage is required");
    }

    return apiRequest(
        `/api/fertilizer-schedules/farm/${farmId}/crop/${cropId}/stage/${encodeURIComponent(
            stage
        )}`
    );
}

export async function getFertilizerReassessmentSchedules(
    farmId
) {
    if (!farmId) {
        throw new Error("farmId is required");
    }

    return apiRequest(
        `/api/fertilizer-schedules/farm/${farmId}/reassessment`
    );
}

export async function updateFertilizerSchedule(
    scheduleId,
    payload
) {
    if (!scheduleId) {
        throw new Error(
            "scheduleId is required"
        );
    }

    return apiRequest(
        `/api/fertilizer-schedules/${scheduleId}`,
        {
            method: "PUT",
            body: JSON.stringify(
                payload
            ),
        }
    );
}

export async function deleteFertilizerSchedule(
    scheduleId
) {
    if (!scheduleId) {
        throw new Error(
            "scheduleId is required"
        );
    }

    return apiRequest(
        `/api/fertilizer-schedules/${scheduleId}`,
        {
            method: "DELETE",
        }
    );
}