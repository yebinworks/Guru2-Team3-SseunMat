package com.guru2.team3.sseunmat.data.remote

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.guru2.team3.sseunmat.data.model.Receipt
import com.guru2.team3.sseunmat.data.model.Users

// [예빈] Firebase Auth & Firestore CRUD

class FirebaseService {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    // 현재 로그인된 유저 ID 가져오기
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    // 회원가입 처리
    fun signUp(email: String, pass: String, nickname: String, onResult: (Boolean) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: ""
                val newUser = Users(usersId = userId, email = email, nickname = nickname)

                // Firestore에 유저 정보 저장
                db.collection("users").document(userId).set(newUser)
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            } else {
                onResult(false)
            }
        }
    }

    // 영수증 기록 저장
    fun addReceipt(receipt: Receipt, onResult: (Boolean) -> Unit) {
        // TODO: 실제 연동 시 아래 주석 해제하여 현재 로그인 유저 ID 사용
        // val currentUserId = getCurrentUserId()
        // if (currentUserId.isEmpty()) {
        //     onResult(false)
        //     return
        // }

        // [테스트용] 고정 유저 ID "1" 사용
        val currentUserId = "1"

        // 문서 ID 미리 생성
        val docRef = db.collection("receipts").document()

        // receiptId 및 usersId 자동 채우기
        val finalReceipt = receipt.copy(
            receiptId = docRef.id,
            usersId = currentUserId
        )

        docRef.set(finalReceipt)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}