package com.example.nutrivision.data.model

import com.google.gson.annotations.SerializedName

data class Analysis(
    @SerializedName("_id") val id: String? = null,
    val userId: String? = null,
    val imageName: String,
    val foodsDetected: List<FoodItem>,
    val nutrition: Nutrition,
    val notes: String? = null,
    val createdAt: String? = null
)

data class FoodItem(
    val name: String,
    val estimatedPortion: String? = null,
    val confidence: Double? = null
)

data class Nutrition(
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double? = null,
    val sugarGrams: Double? = null,
    val sodiumMg: Double? = null
)

// Modelo para la respuesta del análisis de OpenAI
data class FoodAnalysisResponse(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("imageData")
    val imageData: ImageData? = null,
    @SerializedName("analysis")
    val analysis: FoodAnalysis? = null,
    @SerializedName("isFood")
    val isFood: Boolean = true
)

data class ImageData(
    @SerializedName("filename")
    val filename: String,
    @SerializedName("size")
    val size: Int
)

data class FoodAnalysis(
    @SerializedName("dishes")
    val dishes: List<Dish>,
    @SerializedName("plateAnalysis")
    val plateAnalysis: String,
    @SerializedName("nutrition")
    val nutrition: Nutrition,
    @SerializedName("mealType")
    val mealType: String
)

data class Dish(
    @SerializedName("name")
    val name: String,
    @SerializedName("estimatedPortion")
    val estimatedPortion: String
)

// Modelos para guardar análisis
data class SaveAnalysisRequest(
    @SerializedName("imageFilename")
    val imageFilename: String,
    @SerializedName("dishes")
    val dishes: List<Dish>,
    @SerializedName("nutrition")
    val nutrition: Nutrition,
    @SerializedName("plateAnalysis")
    val plateAnalysis: String,
    @SerializedName("mealType")
    val mealType: String,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("date")
    val date: String
)

data class SaveAnalysisResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("analysisId")
    val analysisId: String
)

data class AnalysesResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("data")
    val data: List<AnalysisItem>
)

data class AnalysisItem(
    @SerializedName("_id")
    val id: String,
    @SerializedName("imageName")
    val imageName: String,
    @SerializedName("foodsDetected")
    val foodsDetected: List<Dish>,
    @SerializedName("nutrition")
    val nutrition: Nutrition,
    @SerializedName("rawModelResponse")
    val rawModelResponse: RawModelResponse? = null,
    @SerializedName("createdAt")
    val createdAt: String
)

data class RawModelResponse(
    @SerializedName("plateAnalysis")
    val plateAnalysis: String? = null,
    @SerializedName("mealType")
    val mealType: String? = null
)


