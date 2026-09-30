// ============================================================
// Bilingual templates for DYNAMIC content (backend data).
// ============================================================
// The backend should NEVER send a full sentence. It sends
// structured data (type + values). This file turns that data
// into a Hindi or English sentence, on the frontend.
//
// Pattern: templates[type][language](data) => string
// ============================================================

export const templates = {
  irrigation_needed: {
    en: (d) => `${d.fieldName} needs irrigation within ${d.days} day${d.days > 1 ? "s" : ""}`,
    hi: (d) => `${d.fieldName} में ${d.days} दिन${d.days > 1 ? "ों" : ""} के भीतर सिंचाई आवश्यक है`,
  },

  irrigation_good: {
    en: (d) => `${d.fieldName} has enough moisture. No irrigation needed today.`,
    hi: (d) => `${d.fieldName} में पर्याप्त नमी है। आज सिंचाई की आवश्यकता नहीं है।`,
  },

  heavy_rain_forecast: {
    en: (d) => `Heavy rain expected in ${d.days} day${d.days > 1 ? "s" : ""}`,
    hi: (d) => `${d.days} दिन${d.days > 1 ? "ों" : ""} में भारी बारिश की संभावना है`,
  },

  dont_till_wet: {
    en: (d) => `Don't till ${d.fieldName} today — soil is too wet`,
    hi: (d) => `आज ${d.fieldName} में जुताई न करें — मिट्टी बहुत गीली है`,
  },

  till_ok: {
    en: (d) => `${d.fieldName} is ready for tilling`,
    hi: (d) => `${d.fieldName} जुताई के लिए तैयार है`,
  },

  next_crop_recommendation: {
    en: (d) => `${d.cropName} is the best next crop for this field`,
    hi: (d) => `इस खेत के लिए ${d.cropNameHi} अगली सबसे अच्छी फसल है`,
  },

  next_crop_reason_nitrogen: {
    en: (d) =>
      `Your last crop was ${d.previousCrop}, which uses a lot of nitrogen from the soil. ${d.cropName} naturally adds nitrogen back, making the soil healthier for future crops.`,
    hi: (d) =>
      `आपकी पिछली फसल ${d.previousCropHi} थी, जो मिट्टी से बहुत नाइट्रोजन लेती है। ${d.cropNameHi} प्राकृतिक रूप से नाइट्रोजन वापस जोड़ता है, जिससे मिट्टी भविष्य की फसलों के लिए बेहतर होती है।`,
  },

  fertilizer_apply: {
    en: (d) => `Apply ${d.fertilizerName} before ${d.stage}`,
    hi: (d) => `${d.stageHi} से पहले ${d.fertilizerNameHi} डालें`,
  },

  weather_light_rain: {
    en: (d) =>
      `Light rain expected in ${d.days} days. Plan field work accordingly.`,
    hi: (d) =>
      `${d.days} दिनों में हल्की बारिश की संभावना है। खेत का काम उसी अनुसार करें।`,
  },
};

// Helper to safely render a template — falls back to a generic
// message if backend sends an unknown "type" the frontend
// hasn't been updated to handle yet (prevents crashes).
export function renderTemplate(type, language, data) {
  const entry = templates[type];
  if (!entry || !entry[language]) {
    return language === "hi"
      ? "जानकारी उपलब्ध नहीं है"
      : "Information not available";
  }
  return entry[language](data);
}