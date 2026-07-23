package com.guru2.team3.sseunmat.util

//[공통] Toast, Date 포맷팅, 천단위 콤마 확장함수 등

import android.widget.Toast
import android.content.Context
import java.text.NumberFormat
import java.util.Locale

// 금액 포맷팅 확장 함수
// Int 숫자(89000) -> "₩ 89,000" 형태의 문자열로 변환
fun Int.toCurrencyFormat(): String {
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
    return "₩ ${formatter.format(this)}"
}

// 날짜 포맷팅 유틸
// "2026-07-20" 이나 "20260720"으로 오더라도 "2026. 07. 20" 형태로 맞춤
fun String.toStandardDateFormat(): String {
    val cleanDate = this.replace("-", ".").replace("/", ".").trim()

    // "20260720" 형태인 경우
    if (cleanDate.length == 8 && cleanDate.all { it.isDigit() }) {
        val year = cleanDate.substring(0, 4)
        val month = cleanDate.substring(4, 6)
        val day = cleanDate.substring(6, 8)
        return "$year. $month. $day"
    }

    return cleanDate
}

// 토스트 메시지 출력 간소화 확장 함수
// context.showToast("메시지") 형태로 사용
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

// 짧은 글(일기) 100자 제한 검사 확장 함수, 100자 초과 시 true 반환
fun String.isExceedingMemoLimit(): Boolean {
    return this.length > 100
}