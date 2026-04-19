const app = require("./app");
const env = require("./config/env");
const { connectDb } = require("./config/db");

async function start() {
  await connectDb();
  app.listen(env.port, () => {
    console.log(`NutriVision backend corriendo en puerto ${env.port}`);
  });
}

start().catch((error) => {
  console.error("Error al iniciar backend:", error.message);
  process.exit(1);
});
