const express = require("express");
const multer = require("multer");
const Analysis = require("../models/analysis.model");
const User = require("../models/user.model");
const ConsumedDish = require("../models/consumed-dish.model");
const { analyzeFoodImageBuffer, calculateDailyCalorieGoal } = require("../services/openai.service");

const router = express.Router();
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 },
});

router.get("/", async (_req, res, next) => {
  try {
    const analyses = await Analysis.find().sort({ createdAt: -1 }).limit(50);
    res.json(analyses);
  } catch (error) {
    next(error);
  }
});

router.post("/daily-goal/:userId", async (req, res, next) => {
  try {
    const user = await User.findById(req.params.userId);
    if (!user) {
      return res.status(404).json({ message: "Usuario no encontrado." });
    }

    const profile = {
      age: user.age,
      heightCm: user.heightCm,
      currentWeightLb: user.currentWeightLb,
      gender: user.gender,
      physicalActivity: user.physicalActivity,
      personalGoal: user.personalGoal,
    };

    const result = await calculateDailyCalorieGoal(profile);
    user.dailyCalorieGoalKcal = result.dailyCalorieGoalKcal;
    await user.save();

    res.json({
      userId: user._id,
      dailyCalorieGoalKcal: user.dailyCalorieGoalKcal,
      rationale: result.rationale,
    });
  } catch (error) {
    next(error);
  }
});

// 🔓 ENDPOINT SIN AUTENTICACIÓN - Analiza imagen sin requerir usuario
router.post("/image-quick", upload.single("image"), async (req, res, next) => {
  try {
    if (!req.file) {
      return res.status(400).json({ message: "Debes enviar un archivo en 'image'." });
    }

    const now = new Date();
    const photoTakenTime = typeof req.body.photoTakenTime === "string" && req.body.photoTakenTime.trim()
      ? req.body.photoTakenTime.trim()
      : `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;

    // Perfil por defecto para análisis sin usuario
    const defaultProfile = {
      age: 30,
      heightCm: 170,
      currentWeightLb: 150,
      gender: "other",
      physicalActivity: "moderate",
      personalGoal: "maintain",
    };

    const result = await analyzeFoodImageBuffer(req.file.buffer, req.file.mimetype, {
      photoTakenTime,
      userProfile: defaultProfile,
    });

    if (!result.isFood) {
      return res.status(422).json({ isFood: false, message: result.message });
    }

    const { parsed } = result;

    res.status(200).json({
      success: true,
      isFood: true,
      mealType: parsed.mealType,
      dishes: parsed.dishes,
      plateAnalysis: parsed.plateAnalysis,
      nutrition: parsed.nutrition,
      message: "Análisis completado sin requerir cuenta de usuario",
    });
  } catch (error) {
    next(error);
  }
});

router.post("/image", upload.single("image"), async (req, res, next) => {
  try {
    if (!req.file) {
      return res.status(400).json({ message: "Debes enviar un archivo en 'image'." });
    }
    if (!req.body.userId) {
      return res.status(400).json({ message: "Debes enviar userId." });
    }

    const user = await User.findById(req.body.userId);
    if (!user) {
      return res.status(404).json({ message: "Usuario no encontrado." });
    }

    const now = new Date();
    const photoTakenTime = typeof req.body.photoTakenTime === "string" && req.body.photoTakenTime.trim()
      ? req.body.photoTakenTime.trim()
      : `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
    const dishDate = typeof req.body.dishDate === "string" && req.body.dishDate.trim()
      ? req.body.dishDate.trim()
      : now.toISOString().slice(0, 10);

    const profile = {
      age: user.age,
      heightCm: user.heightCm,
      currentWeightLb: user.currentWeightLb,
      gender: user.gender,
      physicalActivity: user.physicalActivity,
      personalGoal: user.personalGoal,
    };

    const result = await analyzeFoodImageBuffer(req.file.buffer, req.file.mimetype, {
      photoTakenTime,
      userProfile: profile,
    });

    if (!result.isFood) {
      return res.status(422).json({ isFood: false, message: result.message });
    }

    const { parsed, rawResponse } = result;

    const foodsDetected = parsed.dishes.map((dish) => ({
      name: dish.name,
      estimatedPortion: dish.estimatedPortion || "",
      confidence: undefined,
    }));

    const analysis = await Analysis.create({
      userId: user._id,
      imageName: req.file.originalname || "unknown",
      foodsDetected,
      nutrition: {
        calories: parsed.nutrition.calories,
        proteinGrams: parsed.nutrition.proteinGrams,
        carbsGrams: parsed.nutrition.carbsGrams,
        fatGrams: parsed.nutrition.fatGrams,
      },
      notes: parsed.plateAnalysis || "",
      rawModelResponse: rawResponse,
    });

    const consumedDish = await ConsumedDish.create({
      userId: user._id,
      dishDate,
      dishTime: photoTakenTime,
      calories: parsed.nutrition.calories,
      mealType: parsed.mealType,
      plateAnalysis: parsed.plateAnalysis || "",
      carbsGrams: parsed.nutrition.carbsGrams,
      proteinGrams: parsed.nutrition.proteinGrams,
      fatGrams: parsed.nutrition.fatGrams,
    });

    const summaryDate = dishDate;
    if (user.todayNutritionSummary?.date !== summaryDate) {
      user.todayNutritionSummary = {
        date: summaryDate,
        proteinGramsConsumed: 0,
        carbsGramsConsumed: 0,
        fatGramsConsumed: 0,
      };
    }

    user.todayNutritionSummary.proteinGramsConsumed += parsed.nutrition.proteinGrams;
    user.todayNutritionSummary.carbsGramsConsumed += parsed.nutrition.carbsGrams;
    user.todayNutritionSummary.fatGramsConsumed += parsed.nutrition.fatGrams;
    await user.save();

    res.status(201).json({
      analysis,
      consumedDish,
      mealType: parsed.mealType,
      dishes: parsed.dishes,
      plateAnalysis: parsed.plateAnalysis,
      nutrition: parsed.nutrition,
      todayNutritionSummary: user.todayNutritionSummary,
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
