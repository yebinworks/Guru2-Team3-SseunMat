package com.guru2.team3.sseunmat.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.JsonParser
import com.guru2.team3.sseunmat.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class OpenAIService {

    private val client = OkHttpClient()
    private val apiKey = BuildConfig.OPENAI_API_KEY

    suspend fun analyzeReceiptImage(bitmap: Bitmap): Result<Triple<String, String, Long>> = withContext(Dispatchers.IO) {
        try {
            // 이미지 압축 및 Base64 인코딩
            val resizedBitmap = bitmap.resizeAndCompressBitmap(1024)
            val base64Image = bitmapToBase64(resizedBitmap)

            // OpenAI API 요청 JSON 생성 (gpt-4o-mini 지정)
            val requestJson = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("response_format", JSONObject().put("type", "json_object")) // JSON 전용 응답 강제

                val messages = JSONArray().apply {
                    val userMessage = JSONObject().apply {
                        put("role", "user")
                        val contentList = JSONArray().apply {
                            // 프롬프트 텍스트
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", """
                                    이 영수증 사진을 분석해서 아래 3개 필드를 가진 JSON 객체로 반환해줘.
                                    - store: 상호명/가게 이름 (없으면 "")
                                    - date: 결제 날짜 (형식: YYYYMMDD, 없으면 "")
                                    - amount: 총 지출 금액 (숫자만, 없으면 0)
                                """.trimIndent())
                            })
                            // 이미지 (Base64)
                            put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().apply {
                                    put("url", "data:image/jpeg;base64,$base64Image")
                                })
                            })
                        }
                        put("content", contentList)
                    }
                    put(userMessage)
                }
                put("messages", messages)
                put("max_tokens", 300)
            }

            // HTTP 요청 보내기
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(RuntimeException("OpenAI API 에러: ${response.code} - $responseBody"))
            }

            // 응답 JSON 파싱
            val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
            val contentText = jsonResponse.getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString

            val parsedData = JsonParser.parseString(contentText).asJsonObject
            val store = parsedData.get("store")?.asString ?: ""
            val date = parsedData.get("date")?.asString ?: ""
            val amount = parsedData.get("amount")?.asLong ?: 0L

            Result.success(Triple(store, date, amount))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun Bitmap.resizeAndCompressBitmap(maxDimension: Int = 1024): Bitmap {
        val width = this.width
        val height = this.height
        if (width <= maxDimension && height <= maxDimension) return this
        val ratio = width.toFloat() / height.toFloat()
        val (targetWidth, targetHeight) = if (ratio > 1) {
            maxDimension to (maxDimension / ratio).toInt()
        } else {
            (maxDimension * ratio).toInt() to maxDimension
        }
        return Bitmap.createScaledBitmap(this, Math.max(1, targetWidth), Math.max(1, targetHeight), true)
    }
}