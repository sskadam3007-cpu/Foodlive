package com.foodlive.app.data.models

import com.google.gson.annotations.SerializedName

data class PresignedUrlResponse(
    val url: String,
    val fields: Map<String, String>
)

data class GeoPointPayload(
    val type: String = "Point",
    val coordinates: List<Double> // [Longitude, Latitude]
)

data class CreateMealPostRequest(
    @SerializedName("meal_type") val mealType: String,
    @SerializedName("spot_name") val spotName: String,
    @SerializedName("image_url") val imageUrl: String,
    val location: GeoPointPayload
)

data class MealPostResponse(
    val id: Long,
    val username: String,
    @SerializedName("meal_type") val mealType: String,
    @SerializedName("spot_name") val spotName: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("average_rating") val averageRating: Double,
    @SerializedName("ratings_count") val ratingsCount: Int
)