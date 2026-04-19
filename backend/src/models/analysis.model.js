const mongoose = require("mongoose");

const nutritionSchema = new mongoose.Schema(
  {
    calories: { type: Number, required: true },
    proteinGrams: { type: Number, required: true },
    carbsGrams: { type: Number, required: true },
    fatGrams: { type: Number, required: true },
    fiberGrams: { type: Number, required: false },
    sugarGrams: { type: Number, required: false },
    sodiumMg: { type: Number, required: false },
  },
  { _id: false }
);

const foodItemSchema = new mongoose.Schema(
  {
    name: { type: String, required: true },
    estimatedPortion: { type: String, required: false },
    confidence: { type: Number, required: false },
  },
  { _id: false }
);

const analysisSchema = new mongoose.Schema(
  {
    userId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: false, index: true },
    imageName: { type: String, required: true },
    foodsDetected: { type: [foodItemSchema], required: true, default: [] },
    nutrition: { type: nutritionSchema, required: true },
    notes: { type: String, required: false },
    rawModelResponse: { type: Object, required: true },
  },
  { timestamps: true }
);

module.exports = mongoose.model("Analysis", analysisSchema);
