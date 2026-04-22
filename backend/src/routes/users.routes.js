const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const multer = require("multer");
const User = require("../models/user.model");
const env = require("../config/env");
const { calculateDailyCalorieGoal } = require("../services/openai.service");

const router = express.Router();

// Configurar multer para imágenes
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024 }, // 5MB
  fileFilter: (req, file, cb) => {
    if (file.mimetype.startsWith('image/')) {
      cb(null, true);
    } else {
      cb(new Error('Solo se permiten archivos de imagen'));
    }
  }
});

// Middleware para manejar errores de multer
const handleMulterError = (err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    if (err.code === 'LIMIT_FILE_SIZE') {
      return res.status(400).json({ message: "Archivo muy grande (máximo 5MB)" });
    }
    return res.status(400).json({ message: "Error al procesar archivo: " + err.message });
  } else if (err) {
    return res.status(400).json({ message: err.message });
  }
  next();
};

// Middleware para verificar token JWT
const verifyToken = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ message: "Token requerido" });
  }

  const token = authHeader.slice(7);
  try {
    const decoded = jwt.verify(token, env.jwtSecret || "tu-secreto-super-seguro");
    req.userId = decoded.userId;
    next();
  } catch (error) {
    return res.status(401).json({ message: "Token inválido o expirado" });
  }
};

// POST /api/users/register - Registrar nuevo usuario
router.post("/register", async (req, res, next) => {
  try {
    const {
      fullName, email, password, age, heightCm,
      currentWeightLb, gender, physicalActivity, personalGoal
    } = req.body;

    if (!fullName || !email || !password) {
      return res.status(400).json({ message: "Datos incompletos" });
    }

    const existingUser = await User.findOne({ email: email.toLowerCase() });
    if (existingUser) {
      return res.status(409).json({ message: "El email ya está registrado" });
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    // Intentar calcular metas con IA al registrar
    let aiGoals = {
      dailyCalorieGoalKcal: 2000,
      dailyProteinGoalGrams: 150,
      dailyCarbsGoalGrams: 200,
      dailyFatGoalGrams: 80
    };

    try {
      console.log("Calculando metas iniciales con IA para nuevo registro...");
      const result = await calculateDailyCalorieGoal({
        age: age || 25,
        heightCm: heightCm || 170,
        currentWeightLb: currentWeightLb || 150,
        gender: gender || "masculino",
        physicalActivity: physicalActivity || "moderado",
        personalGoal: personalGoal || "mantener peso",
      });
      aiGoals = result;
    } catch (aiError) {
      console.error("Error calculando metas en registro (usando valores por defecto):", aiError.message);
    }

    const newUser = new User({
      fullName,
      email: email.toLowerCase(),
      password: hashedPassword,
      age: age || 25,
      heightCm: heightCm || 170,
      currentWeightLb: currentWeightLb || 150,
      gender: gender || "masculino",
      physicalActivity: physicalActivity || "moderado",
      personalGoal: personalGoal || "mantener peso",
      dailyCalorieGoalKcal: aiGoals.dailyCalorieGoalKcal,
      dailyProteinGoalGrams: aiGoals.dailyProteinGoalGrams,
      dailyCarbsGoalGrams: aiGoals.dailyCarbsGoalGrams,
      dailyFatGoalGrams: aiGoals.dailyFatGoalGrams
    });

    await newUser.save();

    const token = jwt.sign({ userId: newUser._id }, env.jwtSecret, { expiresIn: "30d" });

    res.status(201).json({
      message: "Usuario registrado",
      token,
      user: { id: newUser._id, fullName: newUser.fullName, email: newUser.email }
    });
  } catch (error) {
    next(error);
  }
});

// POST /api/users/login
router.post("/login", async (req, res, next) => {
  try {
    const { email, password } = req.body;
    const user = await User.findOne({ email: email.toLowerCase() });
    if (!user || !(await bcrypt.compare(password, user.password))) {
      return res.status(401).json({ message: "Credenciales inválidas" });
    }

    const token = jwt.sign({ userId: user._id }, env.jwtSecret, { expiresIn: "30d" });
    res.json({
      token,
      user: { id: user._id, fullName: user.fullName, email: user.email }
    });
  } catch (error) {
    next(error);
  }
});

// GET /api/users/profile
router.get("/profile", verifyToken, async (req, res, next) => {
  try {
    const user = await User.findById(req.userId).select("-password");
    res.json(user);
  } catch (error) {
    next(error);
  }
});

// PUT /api/users/profile - Actualizar perfil
router.put("/profile", verifyToken, async (req, res, next) => {
  try {
    const fields = ["age", "heightCm", "currentWeightLb", "gender", "physicalActivity", "personalGoal", "dailyCalorieGoalKcal", "dailyProteinGoalGrams", "dailyCarbsGoalGrams", "dailyFatGoalGrams"];
    const user = await User.findById(req.userId);

    fields.forEach(field => {
      if (req.body[field] !== undefined) user[field] = req.body[field];
    });

    await user.save();
    res.json({ message: "Perfil actualizado", user });
  } catch (error) {
    next(error);
  }
});

// POST /api/users/calculate-daily-goal - IA para calorías y macros
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

    // Guardar TODO en la base de datos
    user.dailyCalorieGoalKcal = result.dailyCalorieGoalKcal;
    user.dailyProteinGoalGrams = result.dailyProteinGoalGrams;
    user.dailyCarbsGoalGrams = result.dailyCarbsGoalGrams;
    user.dailyFatGoalGrams = result.dailyFatGoalGrams;

    await user.save();

    res.json({
      message: "Metas calculadas por IA exitosamente",
      dailyCalorieGoalKcal: user.dailyCalorieGoalKcal,
      dailyProteinGoalGrams: user.dailyProteinGoalGrams,
      dailyCarbsGoalGrams: user.dailyCarbsGoalGrams,
      dailyFatGoalGrams: user.dailyFatGoalGrams,
      rationale: result.rationale
    });
  } catch (error) {
    next(error);
  }
});

// POST /api/users/analyze-food-image
router.post("/analyze-food-image", upload.single('image'), handleMulterError, verifyToken, async (req, res, next) => {
  try {
    if (!req.file) return res.status(400).json({ message: "Imagen requerida" });

    const user = await User.findById(req.userId);
    const { analyzeFoodImageBuffer } = require("../services/openai.service");

    const analysisResult = await analyzeFoodImageBuffer(req.file.buffer, req.file.mimetype, {
      photoTakenTime: req.body.photoTakenTime || new Date().toLocaleTimeString(),
      userProfile: user
    });

    res.json({ analysis: analysisResult.parsed });
  } catch (error) {
    next(error);
  }
});

// POST /api/users/save-analysis
router.post("/save-analysis", verifyToken, async (req, res, next) => {
  try {
    const { imageFilename, dishes, nutrition, plateAnalysis, mealType, createdAt, date } = req.body;
    const userId = req.userId;

    const Analysis = require("../models/analysis.model");

    // 🔥 IMPORTANTE: Usar la fecha LOCAL que envía el cliente
    // El cliente envía: "2026-04-21T19:16:55" (hora local)
    // MongoDB lo guardará como Date, pero queremos mantener la hora local
    let analysisDate;
    if (createdAt) {
      // Crear fecha manteniendo la hora local (sin convertir a UTC)
      // Parsear la fecha local y crear un Date object
      const [datePart, timePart] = createdAt.split('T');
      const [year, month, day] = datePart.split('-');
      const [hour, minute, second] = timePart.split(':');

      // Crear fecha en UTC pero con los valores locales
      // Esto hace que MongoDB guarde 2026-04-21T19:16:55.000Z en lugar de convertirlo
      analysisDate = new Date(Date.UTC(
        parseInt(year),
        parseInt(month) - 1,
        parseInt(day),
        parseInt(hour),
        parseInt(minute),
        parseInt(second || 0)
      ));
    } else {
      analysisDate = new Date();
    }

    const analysis = new Analysis({
      userId,
      imageName: imageFilename,
      foodsDetected: dishes,
      nutrition: nutrition,
      notes: plateAnalysis,
      rawModelResponse: { mealType, plateAnalysis },
      createdAt: analysisDate
    });

    await analysis.save();

    // Actualizar resumen diario usando la fecha LOCAL
    const hoy = date || new Date().toISOString().slice(0, 10);
    const user = await User.findById(userId);

    if (user.todayNutritionSummary.date !== hoy) {
      user.todayNutritionSummary = {
        date: hoy,
        proteinGramsConsumed: 0,
        carbsGramsConsumed: 0,
        fatGramsConsumed: 0
      };
    }

    user.todayNutritionSummary.proteinGramsConsumed += nutrition.proteinGrams;
    user.todayNutritionSummary.carbsGramsConsumed += nutrition.carbsGrams;
    user.todayNutritionSummary.fatGramsConsumed += nutrition.fatGrams;

    await user.save();

    res.json({
      message: "Progreso guardado",
      summary: user.todayNutritionSummary,
      analysisId: analysis._id
    });
  } catch (error) {
    next(error);
  }
});

// GET /api/users/analyses - Obtener análisis por fecha
router.get("/analyses", verifyToken, async (req, res, next) => {
  try {
    const { date } = req.query;
    const userId = req.userId;

    const Analysis = require("../models/analysis.model");

    let query = { userId };

    if (date) {
      // Crear rango de fechas en UTC para buscar
      // La fecha que viene del frontend es LOCAL (ej: "2026-04-21")
      // Necesitamos buscar desde las 00:00:00 UTC hasta las 23:59:59 UTC de ese día
      // Pero como El Salvador es UTC-6, el rango debe ajustarse

      const localDate = new Date(date);

      // Inicio del día en UTC (00:00:00 UTC)
      const startDate = new Date(Date.UTC(
        localDate.getUTCFullYear(),
        localDate.getUTCMonth(),
        localDate.getUTCDate(),
        0, 0, 0, 0
      ));

      // Fin del día en UTC (23:59:59 UTC)
      const endDate = new Date(Date.UTC(
        localDate.getUTCFullYear(),
        localDate.getUTCMonth(),
        localDate.getUTCDate(),
        23, 59, 59, 999
      ));

      query.createdAt = { $gte: startDate, $lte: endDate };

      console.log(`Buscando análisis para fecha local: ${date}`);
      console.log(`Rango UTC: ${startDate.toISOString()} - ${endDate.toISOString()}`);
    }

    const analyses = await Analysis.find(query).sort({ createdAt: -1 });

    const formattedAnalyses = analyses.map(analysis => ({
      _id: analysis._id,
      imageName: analysis.imageName,
      foodsDetected: analysis.foodsDetected,
      nutrition: analysis.nutrition,
      rawModelResponse: analysis.rawModelResponse,
      createdAt: analysis.createdAt
    }));

    res.json({
      count: formattedAnalyses.length,
      data: formattedAnalyses
    });
  } catch (error) {
    next(error);
  }
});

// POST /api/users/change-password - Cambiar contraseña por email
router.post("/change-password", async (req, res, next) => {
  try {
    const { email, newPassword } = req.body;

    console.log("Change password request for email:", email);

    // Validaciones
    if (!email) {
      return res.status(400).json({ message: "El correo electrónico es requerido" });
    }

    if (!newPassword) {
      return res.status(400).json({ message: "La nueva contraseña es requerida" });
    }

    if (newPassword.length < 6) {
      return res.status(400).json({ message: "La contraseña debe tener al menos 6 caracteres" });
    }

    // Buscar usuario por email
    const user = await User.findOne({ email: email.toLowerCase() });
    if (!user) {
      return res.status(404).json({ message: "No existe una cuenta con este correo electrónico" });
    }

    // Hashear nueva contraseña
    const hashedPassword = await bcrypt.hash(newPassword, 10);
    user.password = hashedPassword;
    await user.save();

    console.log("Password changed successfully for user:", user.email);

    res.json({ message: "Contraseña actualizada exitosamente" });
  } catch (error) {
    console.error("Error changing password:", error);
    next(error);
  }
});

module.exports = router;
