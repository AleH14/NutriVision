const mongoose = require("mongoose");

const nutritionSchema = new mongoose.Schema({
  calories: { type: Number, required: true },
  proteinGrams: { type: Number, required: true },
  carbsGrams: { type: Number, required: true },
  fatGrams: { type: Number, required: true },
}, { _id: false });

const foodItemSchema = new mongoose.Schema({
  name: { type: String, required: true },
  estimatedPortion: { type: String },
}, { _id: false });

const analysisSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true, index: true },
  imageName: { type: String, required: true },
  foodsDetected: { type: [foodItemSchema], default: [] },
  nutrition: { type: nutritionSchema, required: true },
  notes: { type: String },
  rawModelResponse: { type: Object },
  date: { type: String, required: true, index: true },            // YYYY-MM-DD local
  localCreatedAt: { type: String, required: true },              // ISO con offset local
}, { timestamps: true });  // createdAt/updatedAt en UTC (no se usan para mostrar)

module.exports = mongoose.model("Analysis", analysisSchema);