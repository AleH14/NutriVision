const OpenAI = require("openai");
const env = require("../config/env");

function getClient() {
  if (!env.openAiApiKey) {
    throw new Error("OPENAI_API_KEY no esta configurada.");
  }
  return new OpenAI({ apiKey: env.openAiApiKey });
}

function buildFoodAnalysisPrompt({ photoTakenTime, userProfile }) {
  return [
    "Eres un analizador nutricional profesional.",
    "Analiza la imagen de comida y estima informacion nutrimental total del plato.",
    "Usa el perfil del usuario como contexto para mejorar la estimacion:",
    JSON.stringify(userProfile),
    `Hora en que se tomo la foto (HH:mm): ${photoTakenTime}`,
    "Determina mealType segun la hora y el tipo de plato.",
    "PRIMERO, determina si la imagen contiene comida real (platos, alimentos, bebidas, ingredientes).",
    "Si la imagen NO contiene comida (ej: persona, paisaje, documento, objeto, animal, selfie, etc.), responde con:",
     '{ "isFood": false, "message": "La imagen no parece ser de comida. Por favor, toma una foto de tu plato de comida." }',
     "",
    "Si la imagen SI contiene comida, responde SOLO JSON valido con esta estructura exacta:",
    "{",
    '  "isFood": true,',
    '  "dishes": [{"name":"string","estimatedPortion":"string"}],',
    '  "plateAnalysis": "string",',
    '  "nutrition": {',
    '    "calories": 0,',
    '    "proteinGrams": 0,',
    '    "carbsGrams": 0,',
    '    "fatGrams": 0',
    "  },",
    '  "mealType": "desayuno|almuerzo|cena|merienda"',
    "}",
    "Si no se puede estimar con precision, entrega una mejor aproximacion razonable.",
  ].join("\n");
}


function buildDailyGoalPrompt(userProfile) {
  return [
    "Eres un nutricionista profesional.",
    "Calcula la meta diaria de calorias (kcal) y macronutrientes (proteinas, carbohidratos, grasas) para este usuario segun su perfil.",
    "Entrega SOLO JSON valido con esta estructura exacta:",
    '{ "dailyCalorieGoalKcal": 0, "dailyProteinGoalGrams": 0, "dailyCarbsGoalGrams": 0, "dailyFatGoalGrams": 0, "rationale": "string" }',
    "Reglas:",
    "- Todas las metas deben ser enteros positivos.",
    "- Asegurate de que la suma calórica de los macros sea coherente con la meta calórica total (Aprox 4kcal/g proteina/carbos, 9kcal/g grasa).",
    "- rationale debe ser breve.",
    JSON.stringify(userProfile),
  ].join("\n");
}

function extractTextResponse(response) {
  if (!response || !Array.isArray(response.output)) {
    throw new Error("Respuesta inesperada de OpenAI.");
  }

  for (const outputItem of response.output) {
    if (!Array.isArray(outputItem.content)) continue;
    for (const content of outputItem.content) {
      if (content.type === "output_text" && typeof content.text === "string") {
        return content.text;
      }
    }
  }

  throw new Error("No se pudo extraer texto de la respuesta de OpenAI.");
}

function parseJsonResponse(rawText, invalidJsonErrorMessage) {
  const text = String(rawText || "").trim().replace(/^```json\s*/i, "").replace(/^```\s*/i, "").replace(/\s*```$/, "");
  try {
    return JSON.parse(text);
  } catch {
    throw new Error(invalidJsonErrorMessage);
  }
}

function normalizeProfile(userProfile) {
  return {
    age: userProfile.age,
    heightCm: userProfile.heightCm,
    currentWeightLb: userProfile.currentWeightLb,
    gender: userProfile.gender,
    physicalActivity: userProfile.physicalActivity,
    personalGoal: userProfile.personalGoal,
  };
}

function normalizeMealType(value) {
  const mealType = String(value || "")
    .trim()
    .toLowerCase();
  if (["desayuno", "almuerzo", "cena", "merienda"].includes(mealType)) {
    return mealType;
  }
  return "merienda";
}

async function calculateDailyCalorieGoal(userProfile) {
  const client = getClient();

  const response = await client.responses.create({
    model: env.openAiModel,
    input: [
      {
        role: "user",
        content: [{ type: "input_text", text: buildDailyGoalPrompt(normalizeProfile(userProfile)) }],
      },
    ],
  });

  const rawText = extractTextResponse(response);
  const parsed = parseJsonResponse(rawText, "OpenAI no devolvio JSON valido para la meta diaria.");

  if (!Number.isFinite(parsed?.dailyCalorieGoalKcal) || parsed.dailyCalorieGoalKcal <= 0) {
    throw new Error("OpenAI no devolvio una meta diaria valida.");
  }

  return {
    dailyCalorieGoalKcal: Math.round(parsed.dailyCalorieGoalKcal),
    dailyProteinGoalGrams: Math.round(parsed.dailyProteinGoalGrams || (parsed.dailyCalorieGoalKcal * 0.25 / 4)),
    dailyCarbsGoalGrams: Math.round(parsed.dailyCarbsGoalGrams || (parsed.dailyCalorieGoalKcal * 0.45 / 4)),
    dailyFatGoalGrams: Math.round(parsed.dailyFatGoalGrams || (parsed.dailyCalorieGoalKcal * 0.30 / 9)),
    rationale: typeof parsed.rationale === "string" ? parsed.rationale : "",
    rawResponse: response,
  };
}

async function analyzeFoodImageBuffer(imageBuffer, mimeType, options = {}) {
  const client = getClient();
  const imageBase64 = imageBuffer.toString("base64");
  const { photoTakenTime = "12:00", userProfile = {} } = options;

  const response = await client.responses.create({
    model: env.openAiModel,
    input: [
      {
        role: "user",
        content: [
          { type: "input_text", text: buildFoodAnalysisPrompt({ photoTakenTime, userProfile: normalizeProfile(userProfile) }) },
          {
            type: "input_image",
            image_url: `data:${mimeType};base64,${imageBase64}`,
          },
        ],
      },
    ],
  });

  const rawText = extractTextResponse(response);
  const parsed = parseJsonResponse(rawText, "OpenAI no devolvio JSON valido para el analisis de comida.");

 // Validar si la imagen no es comida
  if (parsed.isFood === false) {
    return {
      isFood: false,
      message: parsed.message || "La imagen no parece ser de comida. Por favor, toma una foto de tu plato de comida.",
      parsed: null,
      rawResponse: response
    };
  }

  // Si es comida, validar estructura normal
  if (!parsed?.nutrition || !Array.isArray(parsed?.dishes) || !parsed?.mealType) {
    throw new Error("Respuesta de OpenAI sin estructura nutrimental esperada.");
  }

  const normalizedNutrition = {
    calories: Number(parsed.nutrition.calories || 0),
    proteinGrams: Number(parsed.nutrition.proteinGrams || 0),
    carbsGrams: Number(parsed.nutrition.carbsGrams || 0),
    fatGrams: Number(parsed.nutrition.fatGrams || 0),
  };

  const normalizedDishes = parsed.dishes
    .filter((dish) => dish && typeof dish.name === "string")
    .map((dish) => ({
      name: dish.name.trim(),
      estimatedPortion: typeof dish.estimatedPortion === "string" ? dish.estimatedPortion.trim() : "",
    }));

  if (normalizedDishes.length === 0) {
    throw new Error("OpenAI no devolvio platos detectados.");
  }

  const normalizedParsed = {
    dishes: normalizedDishes,
    plateAnalysis: typeof parsed.plateAnalysis === "string" ? parsed.plateAnalysis.trim() : "",
    nutrition: normalizedNutrition,
    mealType: normalizeMealType(parsed.mealType),
  };

  return { isFood: true, parsed: normalizedParsed, message: null, rawResponse: response };
}

module.exports = { analyzeFoodImageBuffer, calculateDailyCalorieGoal };
