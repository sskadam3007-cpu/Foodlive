package com.foodlive.app.data.network

import com.foodlive.app.data.models.CreateMealPostRequest
import com.foodlive.app.data.models.MealPostResponse
import com.foodlive.app.data.models.PresignedUrlResponse
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface FoodliveApi {

    @POST("api/meals/posts/generate_upload_url/")
    suspend fun getPresignedUrl(
        @Header("Authorization") token: String,
        @Body body: Map<String, String> = mapOf("extension" to "jpg")
    ): Response<PresignedUrlResponse>

    @POST("api/meals/posts/")
    suspend fun createMealPost(
        @Header("Authorization") token: String,
        @Body request: CreateMealPostRequest
    ): Response<MealPostResponse>

    @GET("api/meals/posts/nearby/")
    suspend fun getNearbyMeals(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius_km") radiusKm: Double = 5.0
    ): Response<List<MealPostResponse>>
}

interface S3DirectApi {
    @Multipart
    @POST
    suspend fun uploadToS3(
        @Url url: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part
    ): Response<Unit>
}

object NetworkClient {
    private const val BASE_URL = "https://foodlive.app/" // Replace with dev or prod backend IP/domain

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val api: FoodliveApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FoodliveApi::class.java)
    }

    val s3Api: S3DirectApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://s3.amazonaws.com/")
            .client(okHttpClient)
            .build()
            .create(S3DirectApi::class.java)
    }
}