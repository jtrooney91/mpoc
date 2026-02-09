package com.wizzitdigital.emv.flutter

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * HTTP client for API communication
 */
class HttpClient(
    private var baseUrl: String,
    private var authCredentials: String?
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun updateBaseUrl(url: String) {
        baseUrl = url
    }

    fun updateAuthCredentials(credentials: String?) {
        authCredentials = credentials
    }

    fun get(endpoint: String): HttpResponse {
        return try {
            val requestBuilder = Request.Builder()
                .url("$baseUrl$endpoint")
                .get()

            authCredentials?.let {
                requestBuilder.addHeader("Authorization", "Basic $it")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            HttpResponse(
                isSuccessful = response.isSuccessful,
                statusCode = response.code,
                body = response.body?.string(),
                error = if (!response.isSuccessful) "HTTP ${response.code}" else null
            )
        } catch (e: Exception) {
            HttpResponse(
                isSuccessful = false,
                statusCode = -1,
                body = null,
                error = e.message ?: "Unknown error"
            )
        }
    }

    fun post(endpoint: String, params: Map<String, Any>): HttpResponse {
        return try {
            val json = JSONObject(params)
            val requestBody = json.toString().toRequestBody(jsonMediaType)

            val requestBuilder = Request.Builder()
                .url("$baseUrl$endpoint")
                .post(requestBody)

            authCredentials?.let {
                requestBuilder.addHeader("Authorization", "Basic $it")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            HttpResponse(
                isSuccessful = response.isSuccessful,
                statusCode = response.code,
                body = response.body?.string(),
                error = if (!response.isSuccessful) "HTTP ${response.code}" else null
            )
        } catch (e: Exception) {
            HttpResponse(
                isSuccessful = false,
                statusCode = -1,
                body = null,
                error = e.message ?: "Unknown error"
            )
        }
    }
}

/**
 * HTTP response data class
 */
data class HttpResponse(
    val isSuccessful: Boolean,
    val statusCode: Int,
    val body: String?,
    val error: String?
)
