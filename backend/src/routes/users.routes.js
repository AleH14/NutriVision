const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const multer = require("multer");
const rateLimit = require("express-rate-limit");
const User = require("../models/user.model");
const env = require("../config/env");
const { calculateDailyCalorieGoal } = require("../services/openai.service");

const router = express.Router();

// Rate limiters para endpoints sensibles
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutos
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
  message: { message: "Demasiados intentos. Espera 15 minutos antes de intentarlo de nuevo." }
});

const analyzeImageLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minuto
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
  message: { message: "Demasiadas solicitudes de análisis. Intenta de nuevo en un minuto." }
});

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
router.post("/login", authLimiter, async (req, res, next) => {
  try {
    const { email, password } = req.body;

    const normalizedEmail = typeof email === "string" ? email.trim().toLowerCase() : "";
    if (!normalizedEmail) {
      return res.status(400).json({ message: "El correo electrónico es requerido" });
    }
    if (!password || typeof password !== "string" || !password.trim()) {
      return res.status(400).json({ message: "La contraseña es requerida" });
    }

    const user = await User.findOne({ email: normalizedEmail });
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

// GET /api/users/profile - UN SOLO ENDPOINT
router.get("/profile", verifyToken, async (req, res, next) => {
  try {
    let user = await User.findById(req.userId).select("-password");
    if (!user) return res.status(404).json({ message: "Usuario no encontrado" });

    // El reinicio solo debe ocurrir en:
    // 1. reset-daily-summary (cuando el usuario cambia fecha manualmente)
    // 2. save-analysis (cuando se guarda una comida y la fecha es diferente)

    res.json(user);
  } catch (error) {
    next(error);
  }
});

// PUT /api/users/profile - Actualizar perfil
router.put("/profile", verifyToken, async (req, res, next) => {
  try {
    const user = await User.findById(req.userId);
    if (!user) {
      return res.status(404).json({ message: "Usuario no encontrado" });
    }

    // Campos permitidos para actualizar
    const allowedFields = ["age", "heightCm", "currentWeightLb", "gender", "physicalActivity", "personalGoal"];

    allowedFields.forEach(field => {
      if (req.body[field] !== undefined) {
        user[field] = req.body[field];
      }
    });

    await user.save();

    // Devolver usuario sin password
    const userResponse = user.toObject();
    delete userResponse.password;

    res.json(userResponse);
  } catch (error) {
    next(error);
  }
});

// Función auxiliar
function getLocalDateString() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

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
router.post("/analyze-food-image", verifyToken, analyzeImageLimiter, upload.single('image'), handleMulterError, async (req, res, next) => {
  try {
    if (!req.file) return res.status(400).json({ message: "Imagen requerida" });

    const user = await User.findById(req.userId);
    if (!user) return res.status(404).json({ message: "Usuario no encontrado" });

    const { analyzeFoodImageBuffer } = require("../services/openai.service");

    // Formatear hora a HH:mm para el prompt
    const now = new Date();
    const rawTime = req.body.photoTakenTime;
    const photoTakenTime = (typeof rawTime === "string" && /^\d{2}:\d{2}/.test(rawTime.trim()))
      ? rawTime.trim().slice(0, 5)
      : `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;

    const result = await analyzeFoodImageBuffer(req.file.buffer, req.file.mimetype, {
      photoTakenTime,
      userProfile: user
    });

    if (!result.isFood) {
      return res.status(422).json({ isFood: false, message: result.message });
    }

    res.json({ isFood: true, analysis: result.parsed });
  } catch (error) {
    next(error);
  }
});

// POST /api/users/save-analysis
router.post("/save-analysis", verifyToken, async (req, res, next) => {
  try {
    const { imageFilename, dishes, nutrition, plateAnalysis, mealType, createdAt, date, time } = req.body;
    const userId = req.userId;

    const Analysis = require("../models/analysis.model");

    // 🔥 IMPORTANTE: Usar la fecha enviada por el cliente o la actual
    const clientDate = date || getLocalDateString();
    const clientTime = time || "";

    const analysisDate = createdAt ? new Date(parseInt(createdAt)) : new Date();

    // 1. Guardar el análisis
    const analysis = new Analysis({
      userId,
      imageName: imageFilename,
      foodsDetected: dishes,
      nutrition: nutrition,
      notes: plateAnalysis,
      rawModelResponse: { mealType, plateAnalysis },
      createdAt: analysisDate,
      localDate: clientDate,
      localTime: clientTime
    });

    await analysis.save();
    console.log(`✅ Análisis guardado para fecha: ${clientDate}`);

    // 2. Actualizar el resumen del usuario
    const user = await User.findById(userId);
    if (!user) return res.status(404).json({ message: "Usuario no encontrado" });

    console.log(`📊 Resumen actual en BD:`, user.todayNutritionSummary);

    // 🔥 Verificar si es un nuevo día
    const fechaActual = getLocalDateString();
    const fechaResumen = user.todayNutritionSummary?.date;

    if (!user.todayNutritionSummary || fechaResumen !== clientDate) {
      console.log(`🔄 Reiniciando resumen diario`);
      console.log(`   Fecha BD: ${fechaResumen}`);
      console.log(`   Fecha cliente: ${clientDate}`);
      console.log(`   Fecha servidor: ${fechaActual}`);

      user.todayNutritionSummary = {
        date: clientDate,  // ← Usar la fecha del cliente
        proteinGramsConsumed: 0,
        carbsGramsConsumed: 0,
        fatGramsConsumed: 0
      };
    }

    // 3. Sumar los nuevos valores
    const proteinasAAgregar = nutrition.proteinGrams || 0;
    const carbsAAgregar = nutrition.carbsGrams || 0;
    const grasasAAgregar = nutrition.fatGrams || 0;

    console.log(`➕ Sumando: P=${proteinasAAgregar}g, C=${carbsAAgregar}g, G=${grasasAAgregar}g`);

    user.todayNutritionSummary.proteinGramsConsumed += proteinasAAgregar;
    user.todayNutritionSummary.carbsGramsConsumed += carbsAAgregar;
    user.todayNutritionSummary.fatGramsConsumed += grasasAAgregar;

    console.log(`📊 Nuevo resumen:`, user.todayNutritionSummary);

    await user.save();

    res.json({
      message: "Progreso guardado",
      summary: user.todayNutritionSummary,
      analysisId: analysis._id
    });
  } catch (error) {
    console.error("❌ Error en save-analysis:", error);
    next(error);
  }
});

// Asegúrate de que la función esté disponible
function getLocalDateString() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

// GET /api/users/analyses - Obtener análisis por fecha
router.get("/analyses", verifyToken, async (req, res, next) => {
  try {
    const { date } = req.query;
    const userId = req.userId;

    const Analysis = require("../models/analysis.model");

    let query = { userId };

    if (date) {
      // Validar formato de fecha YYYY-MM-DD
      if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
        return res.status(400).json({ message: "Formato de fecha inválido, se espera YYYY-MM-DD" });
      }

      // Buscar por fecha local exacta (string matching)
      query.localDate = date;

      console.log(`Buscando análisis para fecha: ${date}`);
    }

    const analyses = await Analysis.find(query).sort({ createdAt: -1 });

    const formattedAnalyses = analyses.map(analysis => ({
      _id: analysis._id,
      imageName: analysis.imageName,
      foodsDetected: analysis.foodsDetected,
      nutrition: analysis.nutrition,
      rawModelResponse: analysis.rawModelResponse,
      createdAt: analysis.createdAt,
      localTime: analysis.localTime // Incluir la hora local en la respuesta
    }));

    res.json({
      count: formattedAnalyses.length,
      data: formattedAnalyses
    });
  } catch (error) {
    next(error);
  }
});

// POST /api/users/change-password - Cambiar contraseña (requiere sesión activa)
router.post("/change-password", authLimiter, verifyToken, async (req, res, next) => {
  try {
    const { currentPassword, newPassword } = req.body;

    // Validaciones
    if (!currentPassword) {
      return res.status(400).json({ message: "La contraseña actual es requerida" });
    }

    if (!newPassword) {
      return res.status(400).json({ message: "La nueva contraseña es requerida" });
    }

    if (newPassword.length < 6) {
      return res.status(400).json({ message: "La contraseña debe tener al menos 6 caracteres" });
    }

    // Buscar usuario autenticado
    const user = await User.findById(req.userId);
    if (!user) {
      return res.status(404).json({ message: "Usuario no encontrado" });
    }

    // Verificar contraseña actual
    const isCurrentPasswordValid = await bcrypt.compare(currentPassword, user.password);
    if (!isCurrentPasswordValid) {
      return res.status(401).json({ message: "La contraseña actual es incorrecta" });
    }

    // Hashear y guardar nueva contraseña
    user.password = await bcrypt.hash(newPassword, 10);
    await user.save();

    res.json({ message: "Contraseña actualizada exitosamente" });
  } catch (error) {
    next(error);
  }
});


// POST /api/users/reset-daily-summary - Reiniciar resumen manualmente
router.post("/reset-daily-summary", verifyToken, async (req, res, next) => {
  try {
    const user = await User.findById(req.userId);
    if (!user) return res.status(404).json({ message: "Usuario no encontrado" });

    const hoy = getLocalDateString();

    user.todayNutritionSummary = {
      date: hoy,
      proteinGramsConsumed: 0,
      carbsGramsConsumed: 0,
      fatGramsConsumed: 0
    };

    await user.save();

    console.log(`🔄 Resumen reiniciado manualmente para usuario: ${user.email} (fecha: ${hoy})`);

    res.json({
      message: "Resumen diario reiniciado",
      date: hoy,
      summary: user.todayNutritionSummary
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
