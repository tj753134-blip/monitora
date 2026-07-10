package com.universidade.monitor

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.Properties
import java.util.concurrent.TimeUnit

object CloudinaryManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val properties: Properties by lazy {
        Properties().apply {
            val file = File("local.properties")
            if (file.exists()) {
                load(FileInputStream(file))
            }
        }
    }

    private val cloudName: String by lazy {
        properties.getProperty("CLOUDINARY_CLOUD_NAME", "fbfngkkv")
    }

    private val apiKey: String by lazy {
        properties.getProperty("CLOUDINARY_API_KEY", "982337771855171")
    }

    private val apiSecret: String by lazy {
        properties.getProperty("CLOUDINARY_API_SECRET", "")
    }

    suspend fun uploadImageBase64(base64Image: String, clientId: String): String? {
        return try {
            val url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "data:image/jpeg;base64,$base64Image")
                .addFormDataPart("upload_preset", "ml_default")
                .addFormDataPart("public_id", "screenshot_${clientId}_${System.currentTimeMillis()}")
                .addFormDataPart("folder", "monitorapp")
                .build()

            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)

            if (apiKey.isNotBlank() && apiSecret.isNotBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(apiKey, apiSecret))
            }

            val request = requestBuilder.build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            val json = JSONObject(responseBody)
            json.optString("secure_url")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
