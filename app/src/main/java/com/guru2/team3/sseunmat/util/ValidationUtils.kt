package com.guru2.team3.sseunmat.util

// [공통] 이메일/비밀번호/닉네임 검증 유틸

import android.util.Log
import android.util.Patterns

object ValidationUtils {

    private const val TAG = "ValidationUtils"

    // 이메일 형식 검증
    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return try {
            Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
        } catch (e: Exception) {
            Log.e(TAG, "isValidEmail 검증 중 오류 발생", e)
            false
        }
    }

    // 비밀번호 검증: 영문, 숫자, 특수문자 조합 8자 이상
    fun isValidPassword(password: String?): Boolean {
        if (password.isNullOrEmpty() || password.length < 8) return false
        return try {
            val hasLetter = password.any { it.isLetter() }
            val hasDigit = password.any { it.isDigit() }
            val hasSpecial = password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
            hasLetter && hasDigit && hasSpecial
        } catch (e: Exception) {
            Log.e(TAG, "isValidPassword 검증 중 오류 발생", e)
            false
        }
    }

    // 닉네임 검증: 공백만 입력 불가, 2자 이상 10자 이하
    fun isValidNickname(nickname: String?): Boolean {
        if (nickname.isNullOrBlank()) return false
        return try {
            val trimmed = nickname.trim()
            trimmed.length in 2..10
        } catch (e: Exception) {
            Log.e(TAG, "isValidNickname 검증 중 오류 발생", e)
            false
        }
    }
}