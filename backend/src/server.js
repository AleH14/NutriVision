const app = require("./app");
const env = require("./config/env");
const { connectDb } = require("./config/db");

async function start() {
  await connectDb();
  // Cambiamos a "0.0.0.0" para permitir conexiones desde dispositivos externos (como tu movil)
  app.listen(env.port, "0.0.0.0", () => {
    console.log(`NutriVision backend corriendo en http://0.0.0.0:${env.port}`);
    console.log(`Accesible en tu red local en http://192.168.1.37:${env.port}`);
  });
}

start().catch((error) => {
  console.error("Error al iniciar backend:", error.message);
  process.exit(1);
});
