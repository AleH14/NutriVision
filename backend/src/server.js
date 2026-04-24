const os = require("os");
const app = require("./app");
const env = require("./config/env");
const { connectDb } = require("./config/db");

function getLocalIPs() {
  const interfaces = os.networkInterfaces();
  const ips = [];
  for (const iface of Object.values(interfaces)) {
    for (const config of iface) {
      if (config.family === "IPv4" && !config.internal) {
        ips.push(config.address);
      }
    }
  }
  return ips;
}

async function start() {
  await connectDb();
  app.listen(env.port, "0.0.0.0", () => {
    console.log(`NutriVision backend corriendo en http://0.0.0.0:${env.port}`);
    console.log(`Accesible en tu red local`);
  });
}

start().catch((error) => {
  console.error("Error al iniciar backend:", error.message);
  process.exit(1);
});
