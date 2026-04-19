const express = require("express");
const cors = require("cors");
const analysisRoutes = require("./routes/analysis.routes");

const app = express();

app.use(cors());
app.use(express.json({ limit: "2mb" }));

app.get("/health", (_req, res) => {
  res.json({ ok: true });
});

app.use("/api/analysis", analysisRoutes);

app.use((err, _req, res, _next) => {
  const message = err?.message || "Error interno del servidor.";
  const status = err?.status || 500;
  res.status(status).json({ message });
});

module.exports = app;
