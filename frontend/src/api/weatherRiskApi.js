import { apiRequest } from "./apiClient";

/*
 * =========================================================
 * LIVE WEATHER
 * =========================================================
 */

export async function fetchLiveWeather(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/weather/live/farm/${farmId}`,
        {
            method: "POST",
        }
    );
}

/*
 * =========================================================
 * WEATHER RISK RADAR
 * =========================================================
 */

export async function getWeatherRadar(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/weather-radar/farm/${farmId}`
    );
}

/*
 * =========================================================
 * WEATHER RISKS
 * =========================================================
 */

export async function getWeatherRisks(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/weather-risks/farm/${farmId}`
    );
}

export async function getWeatherRisksByLevel(
    farmId,
    riskLevel
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    if (!riskLevel) {
        throw new Error(
            "riskLevel is required"
        );
    }

    return apiRequest(
        `/api/weather-risks/farm/${farmId}/level/${encodeURIComponent(
            riskLevel
        )}`
    );
}

/*
 * =========================================================
 * WEATHER RECORDS
 * =========================================================
 */

export async function getWeatherRecords(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/weather/farm/${farmId}`
    );
}

/*
 * =========================================================
 * WEATHER FORECAST
 * =========================================================
 */

export async function getWeatherForecasts(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/weather-forecasts/farm/${farmId}`
    );
}

/*
 * =========================================================
 * WEATHER FORECAST DATA
 * =========================================================
 */

export async function getWeatherForecastData(
    farmId
) {
    if (!farmId) {
        throw new Error(
            "farmId is required"
        );
    }

    return apiRequest(
        `/api/weather-forecast/farm/${farmId}`
    );
}