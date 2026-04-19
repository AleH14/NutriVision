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
    console.log("Multer fileFilter - archivo:", file.originalname, "mime:", file.mimetype);
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
    console.error("MulterError:", err.message);
    if (err.code === 'LIMIT_FILE_SIZE') {
      return res.status(400).json({ message: "Archivo muy grande (máximo 5MB)" });
    }
    return res.status(400).json({ message: "Error al procesar archivo: " + err.message });
  } else if (err) {
    console.error("Multer error:", err.message);
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
      fullName, 
      email, 
      password,
      age,
      heightCm,
      currentWeightLb,
      gender,
      physicalActivity,
      personalGoal,
      dailyCalorieGoalKcal
    } = req.body;

    // Validaciones básicas
    if (!fullName || !email || !password) {
      return res.status(400).json({ 
        message: "Nombre, email y contraseña son requeridos" 
      });
    }

    if (password.length < 6) {
      return res.status(400).json({ 
        message: "La contraseña debe tener al menos 6 caracteres" 
      });
    }

    // Verificar si el usuario ya existe
    const existingUser = await User.findOne({ email: email.toLowerCase() });
    if (existingUser) {
      return res.status(409).json({ message: "El email ya está registrado" });
    }

    // Hashear contraseña
    const hashedPassword = await bcrypt.hash(password, 10);

    // Crear nuevo usuario
    const newUser = new User({
      fullName,
      email: email.toLowerCase(),
      password: hashedPassword,
      age: age || 30,
      heightCm: heightCm || 170,
      currentWeightLb: currentWeightLb || 150,
      gender: gender || "masculino",
      physicalActivity: physicalActivity || "moderado",
      personalGoal: personalGoal || "mantener peso",
      dailyCalorieGoalKcal: dailyCalorieGoalKcal || 2000,
    });

    await newUser.save();

    // Generar JWT
    const token = jwt.sign(
      { userId: newUser._id },
      env.jwtSecret || "tu-secreto-super-seguro",
      { expiresIn: "30d" }
    );

    res.status(201).json({
      message: "Usuario registrado exitosamente",
      token,
      user: {
        id: newUser._id,
        fullName: newUser.fullName,
        email: newUser.email,
      },
    });
  } catch (error) {
    next(error);
  }
});

// POST /api/users/login - Iniciar sesión
router.post("/login", async (req, res, next) => {
  try {
    const { email, password } = req.body;

    // Validaciones
    if (!email || !password) {
      return res.status(400).json({ 
        message: "Email y contraseña son requeridos" 
      });
    }

    // Buscar usuario por email
    const user = await User.findOne({ email: email.toLowerCase() });
    if (!user) {
      return res.status(401).json({ message: "Credenciales inválidas" });
    }

    // Verificar contraseña
    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) {
      return res.status(401).json({ message: "Credenciales inválidas" });
    }

    // Generar JWT
    const token = jwt.sign(
      { userId: user._id },
      env.jwtSecret || "tu-secreto-super-seguro",
      { expiresIn: "30d" }
    );

    res.json({
      message: "Sesión iniciada exitosamente",
      token,
      user: {
        id: user._id,
        fullName: user.fullName,
        email: user.email,
      },
    });
  } catch (error) {
    next(error);
  }
});

// GET /api/users/profile - Obtener perfil del usuario actual
router.get("/profile", verifyToken, async (req, res, next) => {
  try {
    const user = await User.findById(req.userId).select("-password");
    if (!user) {
      return res.status(404).json({ message: "Usuario no encontrado" });
    }

    res.json(user);
  } catch (error) {
    next(error);
  }
});

// PUT /api/users/profile - Actualizar perfil del usuario
router.put("/profile", verifyToken, async (req, res, next) => {
  try {
    const { age, heightCm, currentWeightLb, gender, physicalActivity, personalGoal } = req.body;

    const user = await User.findById(req.userId);
    if (!user) {
      return res.status(404).json({ message: "Usuario no encontrado" });
    }

    // Actualizar solo los campos proporcionados
    if (age !== undefined && age >= 10 && age <= 120) user.age = age;
    if (heightCm !== undefined && heightCm >= 80 && heightCm <= 260) user.heightCm = heightCm;
    if (currentWeightLb !== undefined && currentWeightLb >= 50 && currentWeightLb <= 700) user.currentWeightLb = currentWeightLb;
    if (gender !== undefined && ["masculino", "femenino"].includes(gender)) user.gender = gender;
    if (physicalActivity !== undefined && ["sedentario", "ligero", "moderado", "intenso"].includes(physicalActivity)) user.physicalActivity = physicalActivity;
    if (personalGoal !== undefined && ["mantener peso", "aumentar musculo", "subir peso", "bajar peso"].includes(personalGoal)) user.personalGoal = personalGoal;

    await user.save();

    res.json({
      message: "Perfil actualizado exitosamente",
      user
    });
  } catch (error) {
    next(error);
  }
});

// POST /api/users/calculate-daily-goal - Calcular meta diaria de calorías
router.post("/calculate-daily-goal", verifyToken, async (req, res, next) => {
  try {
    const user = await User.findById(req.userId);
    if (!user) {
      return res.status(404).json({ message: "Usuario no encontrado" });
    }

    const userProfile = {
      age: user.age,
      heightCm: user.heightCm,
      currentWeightLb: user.currentWeightLb,
      gender: user.gender,
      physicalActivity: user.physicalActivity,
      personalGoal: user.personalGoal,
    };

    const result = await calculateDailyCalorieGoal(userProfile);
    user.dailyCalorieGoalKcal = result.dailyCalorieGoalKcal;
    await user.save();

    res.json({
      message: "Meta diaria de calorías calculada exitosamente",
      userId: user._id,
      dailyCalorieGoalKcal: user.dailyCalorieGoalKcal,
      rationale: result.rationale,
    });
  } catch (error) {
    next(error);
  }
});

// POST /api/users/analyze-food-image - Analizar imagen de comida
router.post("/analyze-food-image", upload.single('image'), handleMulterError, verifyToken, async (req, res, next) => {
  try {
    console.log("=== ANALYZE FOOD IMAGE ===");
    console.log("Archivo recibido:", req.file ? { name: req.file.originalname, size: req.file.size, mime: req.file.mimetype } : "NO RECIBIDO");
    console.log("UserId:", req.userId);
    console.log("Token verificado:", req.userId ? "✓" : "✗");

    if (!req.file) {
      console.error("Error: Imagen requerida");
      return res.status(400).json({ message: "Imagen requerida", error: "No se recibió archivo" });
    }

    const user = await User.findById(req.userId);
    if (!user) {
      console.error("Error: Usuario no encontrado");
      return res.status(404).json({ message: "Usuario no encontrado" });
    }

    console.log("Usuario encontrado:", user.fullName);

    const { analyzeFoodImageBuffer } = require("../services/openai.service");
    const photoTakenTime = req.body.photoTakenTime || new Date().toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
    
    const userProfile = {
      age: user.age,
      heightCm: user.heightCm,
      currentWeightLb: user.currentWeightLb,
      gender: user.gender,
      physicalActivity: user.physicalActivity,
      personalGoal: user.personalGoal,
    };

    console.log("Enviando a OpenAI...");
    const analysisResult = await analyzeFoodImageBuffer(req.file.buffer, req.file.mimetype, {
      photoTakenTime,
      userProfile,
    });

    console.log("Análisis completado:", analysisResult.parsed);

    res.json({
      message: "Análisis de comida completado",
      imageData: {
        filename: req.file.originalname,
        size: req.file.size,
      },
      analysis: analysisResult.parsed,
    });
  } catch (error) {
    console.error("Error en analyze-food-image:", error.message);
    console.error("Stack:", error.stack);
    next(error);
  }
});

// POST /api/users/save-analysis - Guardar análisis completado
router.post("/save-analysis", verifyToken, async (req, res, next) => {
  try {
    console.log("=== SAVE ANALYSIS ===");
    console.log("UserId:", req.userId);
    
    const { imageFilename, dishes, nutrition, plateAnalysis, mealType } = req.body;
    
    if (!imageFilename || !dishes || !nutrition) {
      return res.status(400).json({ message: "Datos incompletos para guardar análisis" });
    }

    // Obtener usuario para actualizar su resumen nutricional
    const user = await User.findById(req.userId);
    if (!user) {
      return res.status(404).json({ message: "Usuario no encontrado" });
    }

    // Crear documento de análisis
    const Analysis = require("../models/analysis.model");
    const newAnalysis = new Analysis({
      userId: req.userId,
      imageName: imageFilename,
      foodsDetected: dishes.map(d => ({
        name: d.name,
        estimatedPortion: d.estimatedPortion
      })),
      nutrition: {
        calories: nutrition.calories,
        proteinGrams: nutrition.proteinGrams,
        carbsGrams: nutrition.carbsGrams,
        fatGrams: nutrition.fatGrams
      },
      rawModelResponse: {
        plateAnalysis,
        mealType,
        dishes
      }
    });

    const savedAnalysis = await newAnalysis.save();
    console.log("Análisis guardado:", savedAnalysis._id);

    // Actualizar resumen nutricional del usuario
    const hoy = new Date().toISOString().slice(0, 10);
    const resumenHoy = user.todayNutritionSummary;
    
    // Si el resumen es de un día anterior, reiniciar
    if (resumenHoy.date !== hoy) {
      console.log("Reiniciando resumen (fecha antigua):", resumenHoy.date, "->", hoy);
      user.todayNutritionSummary = {
        date: hoy,
        proteinGramsConsumed: 0,
        carbsGramsConsumed: 0,
        fatGramsConsumed: 0
      };
    }

    // Agregar los valores nutricionales del análisis
    user.todayNutritionSummary.proteinGramsConsumed += nutrition.proteinGrams;
    user.todayNutritionSummary.carbsGramsConsumed += nutrition.carbsGrams;
    user.todayNutritionSummary.fatGramsConsumed += nutrition.fatGrams;

    console.log("Resumen actualizado:", user.todayNutritionSummary);

    await user.save();

    res.json({
      message: "Análisis guardado exitosamente",
      analysisId: savedAnalysis._id,
      updatedSummary: user.todayNutritionSummary
    });
  } catch (error) {
    console.error("Error en save-analysis:", error.message);
    next(error);
  }
});

// GET /api/users/analyses - Obtener análisis del usuario (con filtro opcional por fecha)
router.get("/analyses", verifyToken, async (req, res, next) => {
  try {
    console.log("=== GET ANALYSES ===");
    console.log("UserId:", req.userId);
    
    const { date } = req.query; // Formato: yyyy-MM-dd
    
    const Analysis = require("../models/analysis.model");
    let query = { userId: req.userId };
    
    if (date) {
      // Filtrar por fecha específica
      const startDate = new Date(date);
      const endDate = new Date(date);
      endDate.setDate(endDate.getDate() + 1);
      
      query.createdAt = {
        $gte: startDate,
        $lt: endDate
      };
      
      console.log("Filtrando por fecha:", date);
    }
    
    const analyses = await Analysis.find(query).sort({ createdAt: -1 });
    
    console.log("Análisis encontrados:", analyses.length);
    
    res.json({
      count: analyses.length,
      data: analyses
    });
  } catch (error) {
    console.error("Error en GET analyses:", error.message);
    next(error);
  }
});

module.exports = router;
