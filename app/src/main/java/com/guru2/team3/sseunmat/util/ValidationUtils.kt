package com.guru2.team3.sseunmat.util

// [공통] 이메일/비밀번호/닉네임 검증 유틸

import android.util.Patterns

object ValidationUtils {

    // 이메일 형식 검증
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // 비밀번호 검증: 영문, 숫자, 특수문자 조합 8자 이상
    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasLetter && hasDigit && hasSpecial
    }

    // 닉네임 검증: 공백만 입력 불가, 2자 이상 10자 이하
    fun isValidNickname(nickname: String): Boolean {
        val trimmed = nickname.trim()
        return trimmed.length in 2..10
    }
}