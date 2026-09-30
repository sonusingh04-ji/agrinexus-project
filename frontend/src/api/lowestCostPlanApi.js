import { apiRequest } from "./apiClient";

export const DEMO_FARM_ID =
    "05efa1f0-4296-43f4-bee7-d139608b4b5f";

export async function evaluateLowestCostSafePlan(
    request
) {
    return apiRequest(
        "/api/lowest-cost-safe-plan/evaluate",
        {
            method: "POST",
            body: JSON.stringify(request),
        }
    );
}