package com.foodlive.app.data.repository

import com.foodlive.app.data.models.CreateMealPostRequest
import com.foodlive.app.data.models.GeoPointPayload
import com.foodlive.app.data.models.MealPostResponse
import com.foodlive.app.data.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class MealRepository {

    suspend fun uploadAndCreateMeal(
        authToken: String,
        imageFile: File,
        mealType: String,
        spotName: String,
        latitude: Double,
        longitude: Double
    ): Result<MealPostResponse> = withContext(Dispatchers.IO) {
        try {
            // 1. Request presigned S3 URL
            val presignedRes = NetworkClient.api.getPresignedUrl("Bearer $authToken")
            if (!presignedRes.isSuccessful || presignedRes.body() == null) {
                return@withContext Result.failure(Exception("Failed to get S3 upload signature"))
            }

            val presigned = presignedRes.body()!!
            val uploadUrl = presigned.url
            val key = presigned.fields["key"] ?: return@withContext Result.failure(Exception("Missing key in S3 fields"))

            // 2. Prepare multipart body and upload to S3
            val fieldsMap = presigned.fields.mapValues { (_, value) ->
                value.toRequestBody("text/plain".toMediaTypeOrNull())
            }

            val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

            val s3Response = NetworkClient.s3Api.uploadToS3(uploadUrl, fieldsMap, filePart)
            if (!s3Response.isSuccessful && s3Response.code() != 204) {
                return@withContext Result.failure(Exception("S3 Direct Upload failed: ${s3Response.code()}"))
            }

            val publicImageUrl = "$uploadUrl$key"

            // 3. Register meal post in Django backend with spatial GeoPoint
            val postRequest = CreateMealPostRequest(
                mealType = mealType,
                spotName = spotName,
                imageUrl = publicImageUrl,
                location = GeoPointPayload(coordinates = listOf(longitude, latitude)) // GeoJSON expects [lng, lat]
            )

            val createPostRes = NetworkClient.api.createMealPost("Bearer $authToken", postRequest)
            if (createPostRes.isSuccessful && createPostRes.body() != null) {
                Result.success(createPostRes.body()!!)
            } else {
                Result.failure(Exception("Failed to register meal in backend: ${createPostRes.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}