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
