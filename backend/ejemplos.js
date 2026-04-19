/**
 * EJEMPLOS DE USO Y PARSEO DE LA API DE ANÁLISIS DE COMIDA
 * 
 * Este archivo contiene ejemplos de cómo usar el test desde Node.js
 * y cómo parsear la respuesta
 */

// ═══════════════════════════════════════════════════════════════════════════
// EJEMPLO 1: Test Simple desde Node.js
// ═══════════════════════════════════════════════════════════════════════════

const fs = require("fs");
const http = require("http");

function sendImageAnalysisRequest(imagePath, userId, photoTime = "12:00", dishDate = null) {
  return new Promise((resolve, reject) => {
    // Leer imagen
    const imageBuffer = fs.readFileSync(imagePath);
    const boundary = "----Boundary" + Math.random().toString(36).substring(7);

    // Construir multipart form data
    const formData = createFormData(boundary, imageBuffer, userId, photoTime, dishDate);

    // Configurar request
    const options = {
      hostname: "localhost",
      port: 4000,
      path: "/api/analysis/image",
      method: "POST",
      headers: {
        "Content-Type": `multipart/form-data; boundary=${boundary}`,
        "Content-Length": formData.length,
      },
    };

    // Enviar request
    const req = http.request(options, (res) => {
      let data = "";
      res.on("data", (chunk) => (data += chunk));
      res.on("end", () => {
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          reject(new Error(`Response no es JSON válido: ${data}`));
        }
      });
    });

    req.on("error", reject);
    req.write(formData);
    req.end();
  });
}

function createFormData(boundary, imageBuffer, userId, photoTime, dishDate) {
  const parts = [];
  parts.push(`--${boundary}`);
  parts.push('Content-Disposition: form-data; name="userId"');
  parts.push("");
  parts.push(userId);

  parts.push(`--${boundary}`);
  parts.push('Content-Disposition: form-data; name="photoTakenTime"');
  parts.push("");
  parts.push(photoTime);

  if (dishDate) {
    parts.push(`--${boundary}`);
    parts.push('Content-Disposition: form-data; name="dishDate"');
    parts.push("");
    parts.push(dishDate);
  }

  parts.push(`--${boundary}`);
  parts.push(
    `Content-Disposition: form-data; name="image"; filename="food.jpg"`
  );
  parts.push("Content-Type: image/jpeg");
  parts.push("");

  const headerEnd = parts.join("\r\n") + "\r\n";
  const footer = `\r\n--${boundary}--\r\n`;

  return Buffer.concat([Buffer.from(headerEnd, "utf8"), imageBuffer, Buffer.from(footer, "utf8")]);
}

// ═══════════════════════════════════════════════════════════════════════════
// EJEMPLO 2: Usar en async/await
// ═══════════════════════════════════════════════════════════════════════════

async function exampleAsyncAwait() {
  try {
    const response = await sendImageAnalysisRequest(
      "./mi-comida.jpg",
      "507f1f77bcf86cd799439011"
    );

    console.log("✅ Análisis completado!");
    console.log("Calorías:", response.nutrition.calories);
    console.log("Platillos:", response.dishes.map((d) => d.name).join(", "));
  } catch (error) {
    console.error("❌ Error:", error.message);
  }
}

// ═══════════════════════════════════════════════════════════════════════════
// EJEMPLO 3: Extraer información específica
// ═══════════════════════════════════════════════════════════════════════════

function extractNutritionInfo(response) {
  const { nutrition, dishes, mealType, plateAnalysis } = response;

  return {
    // Datos básicos
    timeDetected: mealType,
    dishCount: dishes.length,

    // Información nutricional
    calories: nutrition.calories,
    macros: {
      protein: nutrition.proteinGrams,
      carbs: nutrition.carbsGrams,
      fat: nutrition.fatGrams,
    },

    // Cálculos útiles
    macroPercentages: {
      protein: calculatePercentage(nutrition.proteinGrams * 4, nutrition.calories),
      carbs: calculatePercentage(nutrition.carbsGrams * 4, nutrition.calories),
      fat: calculatePercentage(nutrition.fatGrams * 9, nutrition.calories),
    },

    // Listado de platillos
    dishes: dishes.map((d) => ({
      name: d.name,
      portion: d.estimatedPortion,
    })),

    // Análisis
    analysis: plateAnalysis,
  };
}

function calculatePercentage(calories, total) {
  return total > 0 ? Math.round((calories / total) * 100) : 0;
}

// ═══════════════════════════════════════════════════════════════════════════
// EJEMPLO 4: Validar respuesta
// ═══════════════════════════════════════════════════════════════════════════

function validateResponse(response) {
  const errors = [];

  // Validar estructura básica
  if (!response.nutrition || typeof response.nutrition !== "object") {
    errors.push("Falta la información nutricional");
  }

  if (!Array.isArray(response.dishes) || response.dishes.length === 0) {
    errors.push("No se detectaron platillos");
  }

  // Validar números
  if (response.nutrition.calories < 0 || response.nutrition.calories > 10000) {
    errors.push("Calorías fuera de rango: " + response.nutrition.calories);
  }

  // Validar que al menos existe un platillo
  if (response.nutrition.proteinGrams < 0) {
    errors.push("Proteína no puede ser negativa");
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}

// ═══════════════════════════════════════════════════════════════════════════
// EJEMPLO 5: Comparar con meta diaria
// ═══════════════════════════════════════════════════════════════════════════

function compareToDailyGoal(mealData, dailyGoal) {
  const remaining = dailyGoal - mealData.nutrition.calories;

  return {
    caloriesConsumed: mealData.nutrition.calories,
    dailyGoal,
    remaining,
    percentageOfDaily: Math.round(
      (mealData.nutrition.calories / dailyGoal) * 100
    ),
    isWithinGoal: remaining >= 0,
    message:
      remaining >= 0
        ? `✅ Te quedan ${remaining} kcal para llegar a tu meta de ${dailyGoal}`
        : `⚠️ Superaste tu meta por ${Math.abs(remaining)} kcal`,
  };
}

// ═══════════════════════════════════════════════════════════════════════════
// EJEMPLO 6: Formatear para mostrar al usuario
// ═══════════════════════════════════════════════════════════════════════════

function formatForDisplay(response) {
  const { nutrition, dishes, mealType, plateAnalysis } = response;
  const macroCalories = {
    protein: nutrition.proteinGrams * 4,
    carbs: nutrition.carbsGrams * 4,
    fat: nutrition.fatGrams * 9,
  };
  const totalCalories = Object.values(macroCalories).reduce((a, b) => a + b, 0);

  return `
╔════════════════════════════════════════════════════════════════╗
║             ANÁLISIS NUTRICIONAL DE TU PLATILLO              ║
╚════════════════════════════════════════════════════════════════╝

⏰ Tipo de comida: ${mealType.toUpperCase()}

🍽️  Platillos detectados:
${dishes.map((d) => `   • ${d.name}${d.estimatedPortion ? " (" + d.estimatedPortion + ")" : ""}`).join("\n")}

🔥 Información Nutricional:
   ├─ Calorías: ${Math.round(nutrition.calories)} kcal
   ├─ Proteína: ${nutrition.proteinGrams}g (${Math.round((macroCalories.protein / totalCalories) * 100)}%)
   ├─ Carbohidratos: ${nutrition.carbsGrams}g (${Math.round((macroCalories.carbs / totalCalories) * 100)}%)
   └─ Grasas: ${nutrition.fatGrams}g (${Math.round((macroCalories.fat / totalCalories) * 100)}%)

📝 Análisis del plato:
${plateAnalysis}

════════════════════════════════════════════════════════════════
`;
}

// ═══════════════════════════════════════════════════════════════════════════
// EJEMPLO 7: Guardar resultado en JSON
// ═══════════════════════════════════════════════════════════════════════════

function saveResultToFile(response, filename = `analysis-${Date.now()}.json`) {
  const data = {
    timestamp: new Date().toISOString(),
    analysis: response,
  };

  fs.writeFileSync(filename, JSON.stringify(data, null, 2));
  console.log(`✅ Resultado guardado en: ${filename}`);
}

// ═══════════════════════════════════════════════════════════════════════════
// EJEMPLO 8: Detectar comidas problematicas
// ═══════════════════════════════════════════════════════════════════════════

function checkNutritionWarnings(response) {
  const warnings = [];

  // Advertencia de calorías altas
  if (response.nutrition.calories > 1500) {
    warnings.push("⚠️  Alto contenido calórico");
  }

  // Advertencia de grasas altas
  if (response.nutrition.fatGrams > 50) {
    warnings.push("⚠️  Alto contenido de grasas");
  }

  // Advertencia de carbohidratos altos
  if (response.nutrition.carbsGrams > 150) {
    warnings.push("⚠️  Alto contenido de carbohidratos");
  }

  // Alerta de proteína baja
  if (response.nutrition.proteinGrams < 10) {
    warnings.push("ℹ️  Baja cantidad de proteína");
  }

  return warnings;
}

// ═══════════════════════════════════════════════════════════════════════════
// EJEMPLO 9: Integración completa
// ═══════════════════════════════════════════════════════════════════════════

async function fullIntegrationExample(imagePath, userId, dailyGoal = 2500) {
  try {
    console.log("🔄 Analizando imagen...\n");

    // 1. Enviar imagen
    const response = await sendImageAnalysisRequest(imagePath, userId);

    // 2. Validar respuesta
    const validation = validateResponse(response);
    if (!validation.valid) {
      console.error("❌ Validación fallida:", validation.errors);
      return;
    }

    // 3. Extraer información
    const extracted = extractNutritionInfo(response);
    console.log("✅ Extracción completada\n");

    // 4. Comparar con meta
    const comparison = compareToDailyGoal(response, dailyGoal);
    console.log(comparison.message);

    // 5. Buscar advertencias
    const warnings = checkNutritionWarnings(response);
    if (warnings.length > 0) {
      console.log("\n⚠️  Advertencias nutricionales:");
      warnings.forEach((w) => console.log(`   ${w}`));
    }

    // 6. Mostrar formato bonito
    console.log(formatForDisplay(response));

    // 7. Guardar resultado
    saveResultToFile(response);

    return response;
  } catch (error) {
    console.error("❌ Error:", error.message);
  }
}

// ═══════════════════════════════════════════════════════════════════════════
// EXPORTAR FUNCIONES
// ═══════════════════════════════════════════════════════════════════════════

module.exports = {
  sendImageAnalysisRequest,
  extractNutritionInfo,
  validateResponse,
  compareToDailyGoal,
  formatForDisplay,
  saveResultToFile,
  checkNutritionWarnings,
  fullIntegrationExample,
};

// ═══════════════════════════════════════════════════════════════════════════
// SI SE EJECUTA DIRECTAMENTE
// ═══════════════════════════════════════════════════════════════════════════

if (require.main === module) {
  const args = process.argv.slice(2);

  if (args.length < 2) {
    console.error(`
❌ Faltan argumentos.

Uso: node ejemplos.js <RUTA_IMAGEN> <USER_ID> [META_DIARIA_KCAL]

Ejemplo:
  node ejemplos.js ./comida.jpg 507f1f77bcf86cd799439011 2500
    `);
    process.exit(1);
  }

  const imagePath = args[0];
  const userId = args[1];
  const dailyGoal = parseInt(args[2]) || 2500;

  fullIntegrationExample(imagePath, userId, dailyGoal);
}
