/**
 * Test simple para analizar una imagen de comida
 * 
 * Uso:
 *   node test-image-analysis.js <PATH_A_IMAGEN> <USER_ID> [HORA_FOTO] [FECHA_PLATO]
 * 
 * Ejemplo:
 *   node test-image-analysis.js ./test-food.jpg 507f1f77bcf86cd799439011
 *   node test-image-analysis.js ./comida.png 507f1f77bcf86cd799439011 14:30 2026-04-17
 * 
 * Nota: 
 * - Asegúrate que el servidor esté corriendo (npm run dev)
 * - El USER_ID debe existir en MongoDB
 */

const fs = require("fs");
const path = require("path");
const http = require("http");

// Obtener argumentos de línea de comandos
const args = process.argv.slice(2);

if (args.length < 2) {
  console.error(`
❌ Faltan argumentos. 

Uso: node test-image-analysis.js <RUTA_IMAGEN> <USER_ID> [HORA_FOTO] [FECHA_PLATO]

Ejemplos:
  node test-image-analysis.js ./test-food.jpg 507f1f77bcf86cd799439011
  node test-image-analysis.js ./comida.png 507f1f77bcf86cd799439011 14:30 2026-04-17
  `);
  process.exit(1);
}

const imagePath = args[0];
const userId = args[1];
const photoTakenTime = args[2] || "12:00";
const dishDate = args[3] || new Date().toISOString().split("T")[0];

// Validar que la imagen existe
if (!fs.existsSync(imagePath)) {
  console.error(`\n❌ El archivo no existe: ${imagePath}\n`);
  process.exit(1);
}

// Leer el archivo de imagen
const imageBuffer = fs.readFileSync(imagePath);
const mimeType = getMimeType(imagePath);

console.log(`\n📸 Analizando imagen...\n`);
console.log(`Archivo: ${imagePath}`);
console.log(`User ID: ${userId}`);
console.log(`Hora: ${photoTakenTime}`);
console.log(`Fecha: ${dishDate}`);
console.log(`Tipo MIME: ${mimeType}`);
console.log(`Tamaño: ${(imageBuffer.length / 1024).toFixed(2)} KB\n`);

// Crear multipart form data
const boundary = "----WebKitFormBoundary" + Math.random().toString(36).substring(2, 15);
const formData = createFormData(boundary, imageBuffer, userId, photoTakenTime, dishDate);

// Enviar solicitud POST
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

const req = http.request(options, (res) => {
  let data = "";

  res.on("data", (chunk) => {
    data += chunk;
  });

  res.on("end", () => {
    console.log(`\n📡 Status Code: ${res.statusCode}\n`);

    try {
      const jsonData = JSON.parse(data);
      console.log("✅ Respuesta (formateada):\n");
      console.log(JSON.stringify(jsonData, null, 2));
      
      console.log("\n📊 Información Extraída:\n");
      
      if (jsonData.dishes && Array.isArray(jsonData.dishes)) {
        console.log("🍽️  Platillos detectados:");
        jsonData.dishes.forEach((dish, i) => {
          console.log(
            `  ${i + 1}. ${dish.name} (${dish.estimatedPortion || "porción no especificada"})`
          );
        });
      }

      if (jsonData.nutrition) {
        console.log("\n🔥 Información Nutricional (Total):");
        console.log(`  Calorías: ${jsonData.nutrition.calories} kcal`);
        console.log(`  Proteína: ${jsonData.nutrition.proteinGrams}g`);
        console.log(`  Carbohidratos: ${jsonData.nutrition.carbsGrams}g`);
        console.log(`  Grasas: ${jsonData.nutrition.fatGrams}g`);
      }

      if (jsonData.mealType) {
        console.log(`\n⏰ Tipo de comida: ${jsonData.mealType}`);
      }

      if (jsonData.plateAnalysis) {
        console.log(`\n📝 Análisis del plato:\n${jsonData.plateAnalysis}`);
      }

      if (jsonData.todayNutritionSummary) {
        console.log("\n📈 Resumen de hoy:");
        console.log(`  Proteína consumida: ${jsonData.todayNutritionSummary.proteinGramsConsumed}g`);
        console.log(`  Carbohidratos consumidos: ${jsonData.todayNutritionSummary.carbsGramsConsumed}g`);
        console.log(`  Grasas consumidas: ${jsonData.todayNutritionSummary.fatGramsConsumed}g`);
      }

      console.log("\n✨ Test completado exitosamente\n");
    } catch (error) {
      console.error("❌ Error al parsear JSON:\n", data);
    }
  });
});

req.on("error", (error) => {
  console.error("\n❌ Error al conectar con el servidor:\n", error.message);
  console.error(
    "\nAsegúrate de que el servidor esté corriendo en http://localhost:4000"
  );
  console.error("Ejecuta: npm run dev\n");
});

req.write(formData);
req.end();

// Funciones auxiliares
function getMimeType(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  const mimeTypes = {
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".png": "image/png",
    ".gif": "image/gif",
    ".webp": "image/webp",
  };
  return mimeTypes[ext] || "image/jpeg";
}

function createFormData(boundary, imageBuffer, userId, photoTakenTime, dishDate) {
  const parts = [];

  // Agregar campo userId
  parts.push(`--${boundary}`);
  parts.push('Content-Disposition: form-data; name="userId"');
  parts.push("");
  parts.push(userId);

  // Agregar campo photoTakenTime
  parts.push(`--${boundary}`);
  parts.push('Content-Disposition: form-data; name="photoTakenTime"');
  parts.push("");
  parts.push(photoTakenTime);

  // Agregar campo dishDate
  parts.push(`--${boundary}`);
  parts.push('Content-Disposition: form-data; name="dishDate"');
  parts.push("");
  parts.push(dishDate);

  // Agregar archivo de imagen
  parts.push(`--${boundary}`);
  parts.push(
    `Content-Disposition: form-data; name="image"; filename="test-food-${Date.now()}.jpg"`
  );
  parts.push(`Content-Type: ${getMimeType("temp.jpg")}`);
  parts.push("");

  const headersEnd = parts.join("\r\n") + "\r\n";
  const footerStart = `\r\n--${boundary}--\r\n`;

  const binParts = [];
  binParts.push(Buffer.from(headersEnd, "utf8"));
  binParts.push(imageBuffer);
  binParts.push(Buffer.from(footerStart, "utf8"));

  return Buffer.concat(binParts);
}
