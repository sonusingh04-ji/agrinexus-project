// ============================================================
// AgriNexus API Layer
// ============================================================
// This file is the ONLY place that should talk to the backend.
// Right now every function is a placeholder that simulates a
// network delay and returns fake data, so the app works fully
// offline during frontend development.
//
// Once the backend is ready: replace the inside of each function
// with a real fetch() call to the actual endpoint. Nothing in the
// rest of the app (pages/components) needs to change, since they
// only ever call these function names.
// ============================================================

const BASE_URL = "https://your-backend-url.com/api"; // TODO: update when backend is deployed

// ---------- AUTH ----------

export async function sendOtp(phone) {
  // TODO real version:
  // const res = await fetch(`${BASE_URL}/auth/send-otp`, {
  //   method: "POST",
  //   headers: { "Content-Type": "application/json" },
  //   body: JSON.stringify({ phone }),
  // });
  // return res.json();

  await delay(800);
  return { success: true };
}

export async function verifyOtp(phone, otp) {
  // TODO real version: POST { phone, otp } to /auth/verify-otp
  // Should return a token to store for authenticated requests.

  await delay(800);
  return { success: true, token: "fake-token-123" };
}

export async function registerFarmer(name, phone) {
  // TODO real version: POST { name, phone } to /auth/register

  await delay(500);
  return { success: true, farmer: { name, phone } };
}

// ---------- FIELDS ----------

export async function fetchFields() {
  // TODO real version: GET /fields (with auth token header)

  await delay(500);
  return null; // null = "use local data for now", see FieldsContext
}

export async function createField(fieldData) {
  // TODO real version: POST /fields

  await delay(500);
  return { success: true, field: fieldData };
}

export async function removeField(fieldId) {
  // TODO real version: DELETE /fields/:id

  await delay(300);
  return { success: true };
}

// ---------- CROP & SOIL ANALYSIS ----------

export async function analyzeCropAndSoil(cropImageFile, soilImageFile, fieldId) {
  // TODO real version:
  // const formData = new FormData();
  // formData.append("cropImage", cropImageFile);
  // formData.append("soilImage", soilImageFile);
  // formData.append("fieldId", fieldId);
  // const res = await fetch(`${BASE_URL}/analyze`, {
  //   method: "POST",
  //   body: formData,
  // });
  // return res.json();
  //
  // Expected real response shape should match the "report" object
  // currently hardcoded in RecommendationReport.jsx (nextCrop,
  // irrigation, fertilizer, tillage, weather — each with
  // statusColor/summaryEn/summaryHi/whyEn/whyHi).

  await delay(3000);
  return { success: true }; // real version returns the full report object
}

// ---------- REPORTS / HISTORY ----------

export async function saveReport(reportData) {
  // TODO real version: POST /reports

  await delay(400);
  return { success: true, report: reportData };
}

export async function fetchReportsForField(fieldId) {
  // TODO real version: GET /reports?fieldId=...

  await delay(400);
  return null; // null = "use local data for now", see ReportsContext
}

// ---------- AI COPILOT ----------

export async function askCopilot(question, language) {
  // TODO real version: POST { question, language } to /copilot
  // Backend should use farmer's field/report data to answer.

  await delay(1800);
  const fakeAnswer =
    language === "hi"
      ? "आपके खेत 1 में मिट्टी की नमी कम है। आज सिंचाई करने की सलाह दी जाती है।"
      : "Your Field 1 has low soil moisture. It's recommended to irrigate today.";
  return { success: true, answer: fakeAnswer };
}

// ---------- WEATHER ----------

export async function fetchWeather(lat, lng) {
  // TODO real version: GET /weather?lat=...&lng=...
  // or call a weather API directly (see project doc: Open-Meteo/IMD)

  await delay(500);
  return {
    temp: 32,
    condition: "Clear Sky",
    conditionHi: "साफ आसमान",
    humidity: 58,
    rainChance: 10,
  };
}

// ---------- helper ----------

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}