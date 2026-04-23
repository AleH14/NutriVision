package com.example.nutrivision.data.model

data class UpdateUserRequest(
    val age: Int,
    val heightCm: Int,
    val currentWeightLb: Int,
    val gender: String,
    val physicalActivity: String,
    val personalGoal: String
)