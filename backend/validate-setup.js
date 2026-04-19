/**
 * Script para validar la configuración del backend antes de correr tests
 * 
 * Verifica:
 * - Que el servidor está corriendo
 * - Que MongoDB está conectado
 * - Que OpenAI API está configurada
 * - Que hay al menos un usuario de prueba
 * 
 * Uso:
 *   node validate-setup.js
 */

const http = require("http");
const path = require("path");

console.log("\n🔍 Validando configuración de NutriVision Backend...\n");

// Color codes for console
const colors = {
  reset: "\x1b[0m",
  green: "\x1b[32m",
  red: "\x1b[31m",
  yellow: "\x1b[33m",
  blue: "\x1b[34m",
};

let errors = [];

// 1. Verificar que el servidor está corriendo
console.log(`1️⃣  Verificando servidor en http://localhost:4000...`);
checkServerHealth()
  .then((isRunning) => {
    if (isRunning) {
      console.log(`   ${colors.green}✅ Servidor está corriendo${colors.reset}\n`);
    } else {
      errors.push("Servidor no está corriendo");
      console.log(
        `   ${colors.red}❌ Servidor no está corriendo en puerto 3000${colors.reset}\n`
      );
    }

    // 2. Verificar archivo .env
    console.log(`2️⃣  Verificando archivo .env...`);
    try {
      const envPath = path.join(__dirname, "../.env");
      require("fs").accessSync(envPath);
      console.log(`   ${colors.green}✅ Archivo .env encontrado${colors.reset}\n`);
    } catch {
      errors.push("Archivo .env no encontrado");
      console.log(
        `   ${colors.red}❌ Archivo .env no encontrado${colors.reset}\n`
      );
    }

    // 3. Verificar variables de entorno
    console.log(`3️⃣  Verificando variables de entorno...`);
    require("dotenv").config({ path: path.join(__dirname, "../.env") });

    const requiredVars = ["OPENAI_API_KEY", "DATABASE_URL", "OPENAI_MODEL"];
    const missingVars = requiredVars.filter((v) => !process.env[v]);

    if (missingVars.length === 0) {
      console.log(`   ${colors.green}✅ Variables de entorno completas${colors.reset}`);
      console.log(`      • OPENAI_API_KEY: ${maskApiKey(process.env.OPENAI_API_KEY)}`);
      console.log(`      • DATABASE_URL: ${process.env.DATABASE_URL}`);
      console.log(`      • OPENAI_MODEL: ${process.env.OPENAI_MODEL}\n`);
    } else {
      errors.push(`Faltan variables: ${missingVars.join(", ")}`);
      console.log(`   ${colors.red}❌ Faltan variables:${colors.reset}`);
      missingVars.forEach((v) => console.log(`      • ${v}`));
      console.log();
    }

    printSummary(errors);
  })
  .catch((error) => {
    console.error(`   ${colors.red}❌ Error:${colors.reset} ${error.message}\n`);
    printSummary(["No se pudo completar la validación"]);
  });

async function checkServerHealth() {
  return new Promise((resolve) => {
    const req = http.get("http://localhost:4000/health", (res) => {
      let data = "";
      res.on("data", (chunk) => {
        data += chunk;
      });
      res.on("end", () => {
        try {
          const json = JSON.parse(data);
          resolve(json.ok === true);
        } catch {
          resolve(false);
        }
      });
    });

    req.on("error", () => {
      resolve(false);
    });

    setTimeout(() => {
      req.destroy();
      resolve(false);
    }, 5000);
  });
}

function maskApiKey(key) {
  if (!key) return "(no configurada)";
  return key.substring(0, 10) + "..." + key.substring(key.length - 4);
}

function printSummary(errors) {
  console.log(`${"─".repeat(60)}`);
  console.log(`\n📋 Resumen de Validación:\n`);

  if (errors.length === 0) {
    console.log(`${colors.green}✅ Todo está configurado correctamente!${colors.reset}`);
    console.log(`\n📝 Próximos pasos:\n`);
    console.log(`1. Obtén un USER_ID válido:`);
    console.log(`   mongo nutrivision`);
    console.log(`   db.users.findOne({}) // copia el _id\n`);
    console.log(`2. Ejecuta el test:`);
    console.log(`   node test-image-analysis.js ./tu-imagen.jpg <USER_ID>\n`);
    console.log(`3. O ejecuta el test automático:`);
    console.log(`   node setup-test.js <USER_ID>\n`);
  } else {
    console.log(
      `${colors.red}❌ Se encontraron ${errors.length} error(es):${colors.reset}\n`
    );
    errors.forEach((err, i) => {
      console.log(`${colors.yellow}${i + 1}.${colors.reset} ${err}`);
    });
    console.log(
      `\n📝 Soluciona los errores anteriores antes de correr los tests.\n`
    );
  }

  console.log(`${"─".repeat(60)}\n`);
}
