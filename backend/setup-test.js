/**
 * Script para descargar una imagen de prueba y ejecutar el test
 * 
 * Uso:
 *   node setup-test.js <USER_ID>
 * 
 * Ejemplo:
 *   node setup-test.js 507f1f77bcf86cd799439011
 */

const fs = require("fs");
const path = require("path");
const http = require("http");
const https = require("https");

const args = process.argv.slice(2);

if (args.length < 1) {
  console.error(`
❌ Falta el USER_ID.

Uso: node setup-test.js <USER_ID>

Ejemplo:
  node setup-test.js 507f1f77bcf86cd799439011
  `);
  process.exit(1);
}

const userId = args[0];
const testImagePath = path.join(__dirname, "test-food.jpg");

console.log(`\n📥 Descargando imagen de prueba...\n`);

// Descargar una imagen de comida de ejemplo
// Usar una imagen de comida simple de Unsplash
const imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&q=80";

downloadImage(imageUrl, testImagePath)
  .then(() => {
    console.log(`\n✅ Imagen descargada: ${testImagePath}`);
    console.log(`\n📸 Ejecutando test con imagen...\n`);

    // Usar child_process para ejecutar el test
    const { spawn } = require("child_process");
    const test = spawn("node", [
      "test-image-analysis.js",
      testImagePath,
      userId,
      "12:30",
      new Date().toISOString().split("T")[0],
    ]);

    test.stdout.on("data", (data) => {
      console.log(data.toString());
    });

    test.stderr.on("data", (data) => {
      console.error(data.toString());
    });

    test.on("close", (code) => {
      if (code === 0) {
        console.log(`\n✨ Test completado. Archivo guardado: ${testImagePath}`);
      } else {
        console.error(`\n❌ Test falló con código: ${code}`);
      }
    });
  })
  .catch((error) => {
    console.error(`\n❌ Error descargando imagen:\n${error.message}\n`);
    process.exit(1);
  });

function downloadImage(url, dest) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest);
    const protocol = url.startsWith("https") ? https : http;

    protocol
      .get(url, (response) => {
        if (response.statusCode !== 200) {
          reject(new Error(`HTTP ${response.statusCode}`));
          return;
        }
        response.pipe(file);
      })
      .on("error", (err) => {
        fs.unlink(dest, () => {});
        reject(err);
      });

    file.on("finish", () => {
      file.close();
      resolve();
    });

    file.on("error", (err) => {
      fs.unlink(dest, () => {});
      reject(err);
    });
  });
}
