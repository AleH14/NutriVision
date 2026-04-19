const express = require("express");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const User = require("../models/user.model");
const env = require("../config/env");
const { calculateDailyCalorieGoal } = require("../services/openai.service");

const router = express.Router();

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

module.exports = router;
