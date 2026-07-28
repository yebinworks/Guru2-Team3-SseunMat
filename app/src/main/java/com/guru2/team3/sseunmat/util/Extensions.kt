package com.guru2.team3.sseunmat.util

// [공통] Toast, Date 포맷팅, 천단위 콤마 확장함수 등

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.text.NumberFormat
import java.util.Locale

private const val TAG = "UtilExtensions"

// 금액 포맷팅 확장 함수
// Int 숫자(89000) -> "₩ 89,000" 형태의 문자열로 변환
fun Int.toCurrencyFormat(): String {
    return try {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        "₩ ${formatter.format(this)}"
    } catch (e: Exception) {
        Log.e(TAG, "Int.toCurrencyFormat 변환 중 오류 발생", e)
        "₩ $this"
    }
}

// Long 숫자 금액 포맷팅 확장 함수
fun Long.toCurrencyFormat(): String {
    return try {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        "₩ ${formatter.format(this)}"
    } catch (e: Exception) {
        Log.e(TAG, "Long.toCurrencyFormat 변환 중 오류 발생", e)
        "₩ $this"
    }
}

// 날짜 포맷팅 유틸
// "2026-07-20" 이나 "20260720"으로 오더라도 "2026. 07. 20" 형태로 맞춤
fun String?.toStandardDateFormat(): String {
    if (this.isNullOrBlank()) return ""

    return try {
        val cleanDate = this.replace("-", ".").replace("/", ".").trim()

        // "20260720" 형태인 경우
        if (cleanDate.length == 8 && cleanDate.all { it.isDigit() }) {
            val year = cleanDate.substring(0, 4)
            val month = cleanDate.substring(4, 6)
            val day = cleanDate.substring(6, 8)
            "$year. $month. $day"
        } else {
            cleanDate
        }
    } catch (e: Exception) {
        Log.e(TAG, "toStandardDateFormat 변환 중 오류 발생", e)
        this ?: ""
    }
}

// 토스트 메시지 출력 간소화 확장 함수
// context.showToast("메시지") 형태로 사용
fun Context?.showToast(message: String) {
    if (this == null || message.isBlank()) return

    try {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e(TAG, "showToast 출력 중 오류 발생", e)
    }
}

// 짧은 글(일기) 100자 제한 검사 확장 함수, 100자 초과 시 true 반환
fun String?.isExceedingMemoLimit(): Boolean {
    if (this == null) return false
    return this.length > 100
}