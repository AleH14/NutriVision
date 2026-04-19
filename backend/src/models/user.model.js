const mongoose = require("mongoose");

const todayNutritionSummarySchema = new mongoose.Schema(
  {
    date: { type: String, required: true },
    proteinGramsConsumed: { type: Number, required: true, default: 0, min: 0 },
    carbsGramsConsumed: { type: Number, required: true, default: 0, min: 0 },
    fatGramsConsumed: { type: Number, required: true, default: 0, min: 0 },
  },
  { _id: false }
);

const userSchema = new mongoose.Schema(
  {
    fullName: {
      type: String,
      required: true,
      trim: true,
      minlength: 3,
      maxlength: 120,
    },
    email: {
      type: String,
      required: true,
      unique: true,
      lowercase: true,
      trim: true,
      index: true,
      match: [/^\S+@\S+\.\S+$/, "Correo electronico invalido."],
    },
    password: {
      type: String,
      required: true,
      minlength: 6,
    },
    age: {
      type: Number,
      required: true,
      min: 10,
      max: 120,
    },
    heightCm: {
      type: Number,
      required: true,
      min: 80,
      max: 260,
    },
    currentWeightLb: {
      type: Number,
      required: true,
      min: 50,
      max: 700,
    },
    gender: {
      type: String,
      required: true,
      enum: ["masculino", "femenino"],
      lowercase: true,
      trim: true,
    },
    physicalActivity: {
      type: String,
      required: true,
      enum: ["sedentario", "ligero", "moderado", "intenso"],
      lowercase: true,
      trim: true,
    },
    personalGoal: {
      type: String,
      required: true,
      enum: ["mantener peso", "aumentar musculo", "subir peso", "bajar peso"],
      lowercase: true,
      trim: true,
    },
    dailyCalorieGoalKcal: {
      type: Number,
      required: true,
      min: 0,
    },
    todayNutritionSummary: {
      type: todayNutritionSummarySchema,
      required: true,
      default: () => ({
        date: new Date().toISOString().slice(0, 10),
        proteinGramsConsumed: 0,
        carbsGramsConsumed: 0,
        fatGramsConsumed: 0,
      }),
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model("User", userSchema);
