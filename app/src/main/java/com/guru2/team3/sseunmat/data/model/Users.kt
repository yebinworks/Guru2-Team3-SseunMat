package com.guru2.team3.sseunmat.data.model

// 유저 데이터 모델

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Users(
    val usersId: String = "",           // 유저아이디 (Firebase Auth UID, 자동생성 스트링)
    val email: String = "",             // 이메일
    val nickname: String = "",          // 닉네임
    // '비밀번호'는 DB에 직접 저장하지 않음

    @ServerTimestamp
    val createdAt: Date? = null         // 가입 날짜 (연월일 시간)
)