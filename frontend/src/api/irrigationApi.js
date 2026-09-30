import { apiRequest } from "./apiClient";


/* =========================================================
   IRRIGATION CONFIG
   ========================================================= */

export async function getIrrigationConfig(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/irrigation-config/farm/${farmId}`
    );
}


export async function createIrrigationConfig(
    data
) {
    if (!data?.farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        "/api/irrigation-config",
        {
            method: "POST",
            body: JSON.stringify(data),
        }
    );
}


export async function updateIrrigationConfig(
    farmId,
    data
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/irrigation-config/farm/${farmId}`,
        {
            method: "PUT",
            body: JSON.stringify(data),
        }
    );
}


export async function deleteIrrigationConfig(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/irrigation-config/farm/${farmId}`,
        {
            method: "DELETE",
        }
    );
}


/* =========================================================
   IRRIGATION TELEMETRY
   ========================================================= */

export async function getFarmIrrigationTelemetry(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/irrigation-telemetry/farm/${farmId}`
    );
}


export async function getLatestIrrigationTelemetry(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/irrigation-telemetry/farm/${farmId}/latest`
    );
}


export async function getDeviceIrrigationTelemetry(
    farmId,
    deviceId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    if (!deviceId) {
        throw new Error(
            "deviceId is required"
        );
    }

    return apiRequest(
        `/api/irrigation-telemetry/farm/${farmId}/device/${encodeURIComponent(
            deviceId
        )}`
    );
}


export async function getIrrigationTelemetryRange(
    farmId,
    start,
    end
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    if (!start || !end) {
        throw new Error(
            "start and end are required"
        );
    }

    const params =
        new URLSearchParams({
            start,
            end,
        });

    return apiRequest(
        `/api/irrigation-telemetry/farm/${farmId}/range?${params.toString()}`
    );
}


export async function createIrrigationTelemetry(
    data
) {
    if (!data?.farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        "/api/irrigation-telemetry",
        {
            method: "POST",
            body: JSON.stringify(data),
        }
    );
}


export async function deleteIrrigationTelemetry(
    id
) {
    if (!id) {
        throw new Error(
            "Telemetry ID is required"
        );
    }

    return apiRequest(
        `/api/irrigation-telemetry/${id}`,
        {
            method: "DELETE",
        }
    );
}


/* =========================================================
   IRRIGATION EVENTS
   ========================================================= */

export async function getFarmIrrigationEvents(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/irrigation-events/farm/${farmId}`
    );
}


export async function getIrrigationEventById(
    eventId
) {
    if (!eventId) {
        throw new Error(
            "eventId is required"
        );
    }

    return apiRequest(
        `/api/irrigation-events/${eventId}`
    );
}


export async function getIrrigationEventsRange(
    farmId,
    start,
    end
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    if (!start || !end) {
        throw new Error(
            "start and end are required"
        );
    }

    const params =
        new URLSearchParams({
            start,
            end,
        });

    return apiRequest(
        `/api/irrigation-events/farm/${farmId}/range?${params.toString()}`
    );
}


export async function createIrrigationEvent(
    data
) {
    if (!data?.farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        "/api/irrigation-events",
        {
            method: "POST",
            body: JSON.stringify(data),
        }
    );
}


/* =========================================================
   PUMP COMPLETION
   ========================================================= */

export async function getPumpCompletion(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/pump-completion/farm/${farmId}`
    );
}


/* =========================================================
   SMART IRRIGATION
   ========================================================= */


/* =========================================================
   LIVE WEATHER FORECAST SYNC
   ========================================================= */

/*
 * Fetches the live 7-day weather forecast for the farm
 * through the AgriNexus backend.
 *
 * The frontend does not provide forecast values itself.
 * The backend is responsible for retrieving, validating,
 * and storing the real forecast data.
 */
export async function syncLiveWeatherForecast(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/weather-forecast/live/farm/${farmId}`,
        {
            method: "POST",
        }
    );
}


/*
 * Evaluates Smart Irrigation using the backend.
 *
 * Only farmId is strictly required from the frontend.
 * The hardened backend resolves the decision inputs from
 * trusted/stored farm data instead of relying on numeric
 * values supplied by the caller.
 */
export async function evaluateSmartIrrigation(
    data
) {
    if (!data?.farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        "/api/smart-irrigation/evaluate",
        {
            method: "POST",
            body: JSON.stringify(data),
        }
    );
}


/* =========================================================
   BACKWARD-COMPATIBILITY EXPORTS
   ========================================================= */


/*
 * Existing name:
 * getLatestTelemetry(farmId)
 */
export async function getLatestTelemetry(
    farmId
) {
    return getLatestIrrigationTelemetry(
        farmId
    );
}


/*
 * Existing name:
 * getFarmTelemetry(farmId)
 */
export async function getFarmTelemetry(
    farmId
) {
    return getFarmIrrigationTelemetry(
        farmId
    );
}


/*
 * Existing name:
 * getIrrigationEvents(farmId)
 */
export async function getIrrigationEvents(
    farmId
) {
    return getFarmIrrigationEvents(
        farmId
    );
}


/*
 * Existing name:
 * getIrrigationTelemetry(farmId)
 *
 * Kept so existing pages do not break.
 */
export async function getIrrigationTelemetry(
    farmId
) {
    return getFarmIrrigationTelemetry(
        farmId
    );
}


/*
 * Optional backward-compatible range name.
 */
export async function getTelemetryRange(
    farmId,
    start,
    end
) {
    return getIrrigationTelemetryRange(
        farmId,
        start,
        end
    );
}


/*
 * Optional backward-compatible event range name.
 */
export async function getIrrigationEventRange(
    farmId,
    start,
    end
) {
    return getIrrigationEventsRange(
        farmId,
        start,
        end
    );
}