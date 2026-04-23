const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const multer = require("multer");
const rateLimit = require("express-rate-limit");
const User = require("../models/user.model");
const env = require("../config/env");
const { calculateDailyCalorieGoal } = require("../services/openai.service");

const router = express.Router();

// Rate limiters
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  message: { message: "Demasiados intentos. Espera 15 minutos." }
});

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 },
});

const verifyToken = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ message: "Token requerido" });
  }
  const token = authHeader.slice(7);
  try {
    const decoded = jwt.verify(token, env.jwtSecret || "secreto");
    req.userId = decoded.userId;
    next();
  } catch (error) {
    return res.status(401).json({ message: "Token inválido" });
  }
};

// ==================== REGISTER ====================
router.post("/register", async (req, res, next) => {
  try {
    const { fullName, email, password, age, heightCm, currentWeightLb, gender, physicalActivity, personalGoal } = req.body;
    if (!fullName || !email || !password) return res.status(400).json({ message: "Datos incompletos" });

    const normalizedEmail = email.toLowerCase().trim();
    const existingUser = await User.findOne({ email: normalizedEmail });
    if (existingUser) return res.status(409).json({ message: "Email ya registrado" });

    const hashedPassword = await bcrypt.hash(password, 10);
    const newUser = new User({
      fullName,
      email: normalizedEmail,
      password: hashedPassword,
      age: age || 25,
      heightCm: heightCm || 170,
      currentWeightLb: currentWeightLb || 150,
      gender: gender || "masculino",
      physicalActivity: physicalActivity || "moderado",
      personalGoal: personalGoal || "mantener peso",
    });

    // Calcular metas con IA
    try {
      const result = await calculateDailyCalorieGoal({
        age: newUser.age,
        heightCm: newUser.heightCm,
        currentWeightLb: newUser.currentWeightLb,
        gender: newUser.gender,
        physicalActivity: newUser.physicalActivity,
        personalGoal: newUser.personalGoal,
      });
      newUser.dailyCalorieGoalKcal = result.dailyCalorieGoalKcal;
      newUser.dailyProteinGoalGrams = result.dailyProteinGoalGrams;
      newUser.dailyCarbsGoalGrams = result.dailyCarbsGoalGrams;
      newUser.dailyFatGoalGrams = result.dailyFatGoalGrams;
    } catch (err) {
      console.error("Error calculando metas, usando defaults", err);
    }

    await newUser.save();
    const token = jwt.sign({ userId: newUser._id }, env.jwtSecret, { expiresIn: "30d" });
    res.status(201).json({ token, user: { id: newUser._id, fullName: newUser.fullName, email: newUser.email } });
  } catch (error) { next(error); }
});

// ==================== LOGIN ====================
router.post("/login", authLimiter, async (req, res, next) => {
  try {
    const { email, password } = req.body;
    const normalizedEmail = email?.toLowerCase().trim();
    if (!normalizedEmail) return res.status(400).json({ message: "Email requerido" });

    const user = await User.findOne({ email: normalizedEmail });
    if (!user || !(await bcrypt.compare(password, user.password))) {
      return res.status(401).json({ message: "Credenciales inválidas" });
    }

    const token = jwt.sign({ userId: user._id }, env.jwtSecret, { expiresIn: "30d" });
    res.json({ token, user: { id: user._id, fullName: user.fullName, email: user.email } });
  } catch (error) { next(error); }
});

// ==================== PROFILE (con reinicio diario según fecha local) ====================
router.get("/profile", verifyToken, async (req, res, next) => {
  try {
    const user = await User.findById(req.userId).select("-password");
    if (!user) return res.status(404).json({ message: "Usuario no encontrado" });

    const clientDate = req.headers['x-client-date']; // formato YYYY-MM-DD
    if (clientDate) {
      if (!user.todayNutritionSummary || user.todayNutritionSummary.date !== clientDate) {
        console.log(`🔄 Reiniciando resumen diario para fecha local: ${clientDate}`);
        user.todayNutritionSummary = {
          date: clientDate,
          proteinGramsConsumed: 0,
          carbsGramsConsumed: 0,
          fatGramsConsumed: 0,
        };
        await user.save();
      }
    }
    res.json(user);
  } catch (error) { next(error); }
});

// ==================== UPDATE PROFILE ====================
router.put("/profile", verifyToken, async (req, res, next) => {
  try {
    const fields = ["age", "heightCm", "currentWeightLb", "gender", "physicalActivity", "personalGoal", "dailyCalorieGoalKcal", "dailyProteinGoalGrams", "dailyCarbsGoalGrams", "dailyFatGoalGrams"];
    const user = await User.findById(req.userId);
    if (!user) return res.status(404).json({ message: "Usuario no encontrado" });
    fields.forEach(field => { if (req.body[field] !== undefined) user[field] = req.body[field]; });
    await user.save();
    res.json({ message: "Perfil actualizado", user });
  } catch (error) { next(error); }
});

// ==================== CALCULATE DAILY GOAL (IA) ====================
router.post("/calculate-daily-goal", verifyToken, async (req, res, next) => {
  try {
    const user = await User.findById(req.userId);
    if (!user) return res.status(404).json({ message: "Usuario no encontrado" });
    const result = await calculateDailyCalorieGoal({
      age: user.age,
      heightCm: user.heightCm,
      currentWeightLb: user.currentWeightLb,
      gender: user.gender,
      physicalActivity: user.physicalActivity,
      personalGoal: user.personalGoal,
    });
    user.dailyCalorieGoalKcal = result.dailyCalorieGoalKcal;
    user.dailyProteinGoalGrams = result.dailyProteinGoalGrams;
    user.dailyCarbsGoalGrams = result.dailyCarbsGoalGrams;
    user.dailyFatGoalGrams = result.dailyFatGoalGrams;
    await user.save();
    res.json(result);
  } catch (error) { next(error); }
});

// ==================== ANALYZE FOOD IMAGE ====================
router.post("/analyze-food-image", verifyToken, upload.single('image'), async (req, res, next) => {
  try {
    if (!req.file) return res.status(400).json({ message: "Imagen requerida" });
    const user = await User.findById(req.userId);
    if (!user) return res.status(404).json({ message: "Usuario no encontrado" });

    const { analyzeFoodImageBuffer } = require("../services/openai.service");
    const photoTakenTime = req.body.photoTakenTime || new Date().toLocaleTimeString();
    const result = await analyzeFoodImageBuffer(req.file.buffer, req.file.mimetype, {
      photoTakenTime,
      userProfile: user,
    });
    if (!result.isFood) return res.status(422).json({ isFood: false, message: result.message });
    res.json({ isFood: true, analysis: result.parsed });
  } catch (error) { next(error); }
});

// ==================== SAVE ANALYSIS (con fecha y hora local) ====================
router.post("/save-analysis", verifyToken, async (req, res, next) => {
  try {
    const { imageFilename, dishes, nutrition, plateAnalysis, mealType, createdAt, date } = req.body;
    const userId = req.userId;

    const Analysis = require("../models/analysis.model");
    const ConsumedDish = require("../models/consumed-dish.model");

    const localDateTime = createdAt; // ej: "2026-04-22T23:34:37.000-03:00"
    const hoy = date;                // ej: "2026-04-22"

    // Extraer hora para ConsumedDish
    let dishTime = "12:00";
    if (localDateTime) {
      const match = localDateTime.match(/T(\d{2}:\d{2})/);
      if (match) dishTime = match[1];
    }

    // Guardar en Analysis
    const analysis = new Analysis({
      userId,
      imageName: imageFilename,
      foodsDetected: dishes,
      nutrition,
      notes: plateAnalysis,
      rawModelResponse: { mealType, plateAnalysis },
      date: hoy,
      localCreatedAt: localDateTime,
    });
    await analysis.save();

    // Guardar en ConsumedDish (opcional)
    const consumedDish = new ConsumedDish({
      userId,
      dishDate: hoy,
      dishTime,
      calories: nutrition.calories,
      mealType: mealType || "comida",
      plateAnalysis: plateAnalysis || "",
      carbsGrams: nutrition.carbsGrams,
      proteinGrams: nutrition.proteinGrams,
      fatGrams: nutrition.fatGrams,
    });
    await consumedDish.save();

    // Actualizar resumen diario del usuario
    const user = await User.findById(userId);
    if (!user) return res.status(404).json({ message: "Usuario no encontrado" });

    if (!user.todayNutritionSummary || user.todayNutritionSummary.date !== hoy) {
      user.todayNutritionSummary = {
        date: hoy,
        proteinGramsConsumed: 0,
        carbsGramsConsumed: 0,
        fatGramsConsumed: 0,
      };
    }
    user.todayNutritionSummary.proteinGramsConsumed += nutrition.proteinGrams;
    user.todayNutritionSummary.carbsGramsConsumed += nutrition.carbsGrams;
    user.todayNutritionSummary.fatGramsConsumed += nutrition.fatGrams;
    await user.save();

    res.json({
      message: "Guardado con fecha local",
      summary: user.todayNutritionSummary,
      analysisId: analysis._id,
    });
  } catch (error) { next(error); }
});

// ==================== GET ANALYSES BY DATE (devuelve localCreatedAt como createdAt) ====================
router.get("/analyses", verifyToken, async (req, res, next) => {
  try {
    const { date } = req.query;
    const userId = req.userId;
    const Analysis = require("../models/analysis.model");

    let query = { userId };
    if (date) {
      if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
        return res.status(400).json({ message: "Formato de fecha inválido, use YYYY-MM-DD" });
      }
      query.date = date;
      console.log(`🔍 Buscando análisis para fecha local: ${date}`);
    }

    const analyses = await Analysis.find(query).sort({ localCreatedAt: -1 });
    // Mapear para que el campo createdAt sea el local (con offset)
    const data = analyses.map(a => ({
      ...a.toObject(),
      createdAt: a.localCreatedAt,
    }));

    res.json({ count: data.length, data });
  } catch (error) { next(error); }
});

// ==================== CHANGE PASSWORD ====================
router.post("/change-password", authLimiter, verifyToken, async (req, res, next) => {
  try {
    const { currentPassword, newPassword } = req.body;
    if (!currentPassword || !newPassword) return res.status(400).json({ message: "Ambas contraseñas son requeridas" });
    if (newPassword.length < 6) return res.status(400).json({ message: "La nueva contraseña debe tener al menos 6 caracteres" });

    const user = await User.findById(req.userId);
    if (!user) return res.status(404).json({ message: "Usuario no encontrado" });

    const isValid = await bcrypt.compare(currentPassword, user.password);
    if (!isValid) return res.status(401).json({ message: "Contraseña actual incorrecta" });

    user.password = await bcrypt.hash(newPassword, 10);
    await user.save();
    res.json({ message: "Contraseña actualizada" });
  } catch (error) { next(error); }
});

module.exports = router;