/**
 * Test SIN USUARIO - Funciona sin necesidad de cuenta
 * 
 * Uso:
 *   node test-quick.js <PATH_A_IMAGEN> [HORA_FOTO]
 * 
 * Ejemplo:
 *   node test-quick.js ./comida.jpg
 *   node test-quick.js ./platillo.png 14:30
 * 
 * ✅ NO requiere userId
 * ✅ NO requiere usuario en BD
 * ✅ Devuelve toda la información del platillo
 */

const fs = require("fs");
const path = require("path");
const http = require("http");

// Obtener argumentos de línea de comandos
const args = process.argv.slice(2);

if (args.length < 1) {
  console.error(`
❌ Falta la ruta de la imagen.

Uso: node test-quick.js <RUTA_IMAGEN> [HORA_FOTO]

Ejemplos:
  node test-quick.js ./comida.jpg
  node test-quick.js ./platillo.png 14:30
  `);
  process.exit(1);
}

const imagePath = args[0];
const photoTakenTime = args[1] || "12:00";

// Validar que la imagen existe
if (!fs.existsSync(imagePath)) {
  console.error(`\n❌ El archivo no existe: ${imagePath}\n`);
  process.exit(1);
}

// Leer el archivo de imagen
const imageBuffer = fs.readFileSync(imagePath);
const mimeType = getMimeType(imagePath);

console.log(`\n📸 Analizando imagen (SIN USUARIO)...\n`);
console.log(`Archivo: ${imagePath}`);
console.log(`Hora: ${photoTakenTime}`);
console.log(`Tipo MIME: ${mimeType}`);
console.log(`Tamaño: ${(imageBuffer.length / 1024).toFixed(2)} KB\n`);

// Crear multipart form data
const boundary = "----WebKitFormBoundary" + Math.random().toString(36).substring(2, 15);
const formData = createFormData(boundary, imageBuffer, photoTakenTime);

// Enviar solicitud POST al endpoint sin autenticación
const options = {
  hostname: "localhost",
  port: 4000,
  path: "/api/analysis/image-quick",
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
    console.log(`📡 Status Code: ${res.statusCode}\n`);

    try {
      const jsonData = JSON.parse(data);

      if (res.statusCode !== 200) {
        console.error("❌ Error:");
        console.error(jsonData.message || JSON.stringify(jsonData, null, 2));
        return;
      }

      console.log("✅ Análisis completado!\n");
      
      if (jsonData.dishes && Array.isArray(jsonData.dishes)) {
        console.log("🍽️  Platillos detectados:");
        jsonData.dishes.forEach((dish, i) => {
          console.log(
            `  ${i + 1}. ${dish.name}${dish.estimatedPortion ? ` (${dish.estimatedPortion})` : ""}`
          );
        });
        console.log();
      }

      if (jsonData.nutrition) {
        console.log("🔥 Información Nutricional:\n");
        console.log(`  ├─ Calorías: ${jsonData.nutrition.calories} kcal`);
        console.log(`  ├─ Proteína: ${jsonData.nutrition.proteinGrams}g`);
        console.log(`  ├─ Carbohidratos: ${jsonData.nutrition.carbsGrams}g`);
        console.log(`  └─ Grasas: ${jsonData.nutrition.fatGrams}g\n`);
      }

      if (jsonData.mealType) {
        console.log(`⏰ Tipo de comida: ${jsonData.mealType}\n`);
      }

      if (jsonData.plateAnalysis) {
        console.log(`📝 Análisis del plato:\n${jsonData.plateAnalysis}\n`);
      }

      console.log("═".repeat(60));
      console.log("✨ Test completado exitosamente\n");
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

function createFormData(boundary, imageBuffer, photoTakenTime) {
  const parts = [];

  // Agregar campo photoTakenTime
  parts.push(`--${boundary}`);
  parts.push('Content-Disposition: form-data; name="photoTakenTime"');
  parts.push("");
  parts.push(photoTakenTime);

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
