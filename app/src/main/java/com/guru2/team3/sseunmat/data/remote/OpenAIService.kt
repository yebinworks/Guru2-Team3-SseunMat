package com.guru2.team3.sseunmat.data.remote

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
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
import java.util.concurrent.TimeUnit

class OpenAIService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeReceiptImage(bitmap: Bitmap?): Result<Triple<String, String, Long>> = withContext(Dispatchers.IO) {
        try {
            // 비트맵 유효성 검증 (Null 및 Recycle 여부 방어)
            if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
                Log.e("OpenAIService", "유효하지 않은 비트맵입니다.")
                return@withContext Result.failure(IllegalArgumentException("유효하지 않은 이미지 파일입니다."))
            }

            Log.d("OpenAIService", "1. AI 분석 시작 (원본 비트맵 크기: ${bitmap.width}x${bitmap.height})")

            val apiKey = BuildConfig.OPENAI_API_KEY
            if (apiKey.isNullOrEmpty()) {
                Log.e("OpenAIService", "API Key가 설정되지 않았습니다. local.properties를 확인해주세요.")
                return@withContext Result.failure(IllegalStateException("OpenAI API Key가 누락되었습니다."))
            }

            // 이미지 리사이징 & Base64 변환
            val resizedBitmap = bitmap.resizeAndCompressBitmap(1024)
            val base64Image = bitmapToBase64(resizedBitmap)
            if (base64Image.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("이미지 변환에 실패했습니다."))
            }
            Log.d("OpenAIService", "2. 이미지 리사이징 & Base64 변환 완료 (Base64 길이: ${base64Image.length})")

            // OpenAI API 요청 JSON 생성
            val requestJson = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("response_format", JSONObject().put("type", "json_object"))

                val messages = JSONArray().apply {
                    val userMessage = JSONObject().apply {
                        put("role", "user")
                        val contentList = JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", """
                                    이 영수증 사진을 분석해서 아래 3개 필드를 가진 JSON 객체로 반환해줘.
                                    - store: 상호명/가게 이름 (없으면 "")
                                    - date: 결제 날짜 (형식: YYYYMMDD, 없으면 "")
                                    - amount: 총 지출 금액 (숫자만, 없으면 0)
                                """.trimIndent())
                            })
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

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            Log.d("OpenAIService", "3. OpenAI API HTTP 요청 전송 중...")

            // Response 자원 해제(use) 및 HTTP 요청 처리
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                Log.d("OpenAIService", "4. 응답 도착! Response Code: ${response.code}")

                if (!response.isSuccessful) {
                    Log.e("OpenAIService", "OpenAI API 에러 응답 Body: $responseBody")
                    return@withContext Result.failure(RuntimeException("OpenAI API 에러 (${response.code}): $responseBody"))
                }

                // JSON 파싱 예외 안전 처리
                return@withContext try {
                    val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                    val choices = jsonResponse.getAsJsonArray("choices")

                    if (choices == null || choices.size() == 0) {
                        return@withContext Result.failure(IllegalStateException("OpenAI 응답 데이터가 비어있습니다."))
                    }

                    val contentText = choices.get(0).asJsonObject
                        .getAsJsonObject("message")
                        .get("content")?.asString ?: "{}"

                    Log.d("OpenAIService", "5. AI 분석 텍스트 결과: $contentText")

                    val parsedData = JsonParser.parseString(contentText).asJsonObject
                    val store = parsedData.get("store")?.asString ?: ""
                    val date = parsedData.get("date")?.asString ?: ""

                    // amount 가 문자열("1000")이나 소수점 형태로 올 경우 예외 방어
                    val amountRaw = parsedData.get("amount")
                    val amount = when {
                        amountRaw == null -> 0L
                        amountRaw.isJsonPrimitive && amountRaw.asJsonPrimitive.isNumber -> amountRaw.asLong
                        else -> amountRaw.asString.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                    }

                    Result.success(Triple(store, date, amount))
                } catch (e: Exception) {
                    Log.e("OpenAIService", "JSON 파싱 중 오류 발생: ${e.message}", e)
                    Result.failure(e)
                }
            }

        } catch (e: Exception) {
            Log.e("OpenAIService", "OpenAI 서비스 예외 발생", e)
            Result.failure(e)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("OpenAIService", "Base64 인코딩 실패", e)
            ""
        }
    }

    private fun Bitmap.resizeAndCompressBitmap(maxDimension: Int = 1024): Bitmap {
        return try {
            val width = this.width
            val height = this.height
            if (width <= maxDimension && height <= maxDimension) return this
            val ratio = width.toFloat() / height.toFloat()
            val (targetWidth, targetHeight) = if (ratio > 1) {
                maxDimension to (maxDimension / ratio).toInt()
            } else {
                (maxDimension * ratio).toInt() to maxDimension
            }
            Bitmap.createScaledBitmap(this, Math.max(1, targetWidth), Math.max(1, targetHeight), true)
        } catch (e: Exception) {
            Log.e("OpenAIService", "비트맵 리사이징 실패, 원본 유지", e)
            this
        }
    }
}