package com.example.socially.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.socially.api.RetrofitClient
import com.example.socially.api.UploadPostResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Worker to upload media files (posts/messages/stories) in background using multipart/form-data.
 * Expects inputData with keys:
 * - filePath: String (absolute path to local file)
 * - userId: Int
 * - caption: String (optional)
 * - actionType: String ("post" | "message" | "story")
 */
class UploadMediaWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    private val TAG = "UploadMediaWorker"
    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val filePath = inputData.getString("filePath") ?: return@withContext Result.failure()
            val userId = inputData.getInt("userId", -1)
            val caption = inputData.getString("caption") ?: ""

            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "File not found: $filePath")
                return@withContext Result.failure()
            }

            // Prepare multipart parts
            val mediaType = guessMimeType(filePath) ?: "application/octet-stream"
            val requestFile = file.asRequestBody(mediaType.toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("media", file.name, requestFile)

            val userIdBody = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val captionBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())

            val call = RetrofitClient.apiService.uploadPostMultipart(userIdBody, captionBody, body)
            val response = call.execute()

            return@withContext if (response.isSuccessful && response.body() != null) {
                val resp: UploadPostResponse? = response.body()
                Log.d(TAG, "Upload success: ${gson.toJson(resp)}")
                Result.success()
            } else {
                val err = response.errorBody()?.string()
                Log.e(TAG, "Upload failed: $err")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload error: ${e.message}", e)
            return@withContext Result.retry()
        }
    }

    private fun guessMimeType(path: String): String? {
        return when (path.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "pdf" -> "application/pdf"
            else -> null
        }
    }
}
