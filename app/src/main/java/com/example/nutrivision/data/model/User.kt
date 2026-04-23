package com.example.nutrivision.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id") val id: String? = null,
    val fullName: String,
    val email: String,
    val age: Int,
    val heightCm: Int,
    val currentWeightLb: Int,
    val gender: String,
    val physicalActivity: String,
    val personalGoal: String,
    val dailyCalorieGoalKcal: Double,
    val dailyProteinGoalGrams: Int = 150,
    val dailyCarbsGoalGrams: Int = 200,
    val dailyFatGoalGrams: Int = 80,
    val todayNutritionSummary: NutritionSummary? = null
)

data class NutritionSummary(
    val date: String,
    val proteinGramsConsumed: Double,
    val carbsGramsConsumed: Double,
    val fatGramsConsumed: Double,
    val caloriesConsumed: Double = 0.0
)