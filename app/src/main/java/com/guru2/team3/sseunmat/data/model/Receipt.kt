package com.guru2.team3.sseunmat.data.model

// 영수증 데이터 모델

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Receipt(
    // 식별자
    val receiptId: String = "",         // 영수증 아이디 (자동생성/문서ID)
    val usersId: String = "",           // 작성한 유저 아이디 (Firebase Auth UID)

    // 영수증 기본 정보
    val store: String = "",             // 상호명
    val date: String = "",              // 결제 날짜 (앱 표준 포맷: "2026. 07. 20", 저장은 시간까지)
    val amount: Int = 0,                // 최종 결제 금액 (원시 숫자값, 노출 시 "₩ 89,000" 포맷팅)

    // 가치 지출 일기 정보
    val value: String = "",             // 가치 (소비 카테고리)
    val rating: Int = 3,                // 만족도 (1~5, 기본값 3)
    val memo: String = "",              // 짧은 글 (일기, 최대 100자)
    @field:JvmField
    val isDonation: Boolean = false,    // 기부 소비 여부 (true/false)

    // 시스템 관리용
    @ServerTimestamp
    val createdAt: Date? = null         // 저장 날짜 (연월일 시간, Firestore 서버 시간)
)