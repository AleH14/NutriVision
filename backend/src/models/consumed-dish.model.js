const mongoose = require("mongoose");

const consumedDishSchema = new mongoose.Schema(
  {
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      index: true,
    },
    dishDate: {
      type: String,
      required: true,
      index: true,
    },
    dishTime: {
      type: String,
      required: true,
    },
    calories: {
      type: Number,
      required: true,
      min: 0,
    },
    mealType: {
      type: String,
      required: true,
      enum: ["desayuno", "almuerzo", "cena", "merienda"],
      lowercase: true,
      trim: true,
    },
    plateAnalysis: {
      type: String,
      required: true,
      trim: true,
    },
    carbsGrams: {
      type: Number,
      required: true,
      min: 0,
    },
    proteinGrams: {
      type: Number,
      required: true,
      min: 0,
    },
    fatGrams: {
      type: Number,
      required: true,
      min: 0,
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model("ConsumedDish", consumedDishSchema);
