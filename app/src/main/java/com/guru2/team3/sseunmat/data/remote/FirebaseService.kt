package com.guru2.team3.sseunmat.data.remote

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.guru2.team3.sseunmat.data.model.Receipt
import com.guru2.team3.sseunmat.data.model.Users
import java.util.Calendar

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

    // 로그인 처리
    fun login(email: String, pass: String, onResult: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            onResult(task.isSuccessful)
        }
    }

    // 영수증 기록 저장
    fun addReceipt(receipt: Receipt, onResult: (Boolean) -> Unit) {
         val currentUserId = getCurrentUserId()
         if (currentUserId.isEmpty()) {
             onResult(false)
             return
         }

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

    // 이번 달에 등록된 영수증 조회 (3.1.1 & 3.1.2)
    fun getThisMonthReceipts(onResult: (List<Receipt>) -> Unit) {
        val currentUserId = getCurrentUserId()

        if (currentUserId.isEmpty()) {
            android.util.Log.w("FirebaseService", "조회 취소: 로그인된 유저가 없습니다.")
            onResult(emptyList())
            return
        }

        // 이번 달 1일 00:00:00 계산
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.time

        db.collection("receipts")
            .whereEqualTo("usersId", currentUserId)
            .whereGreaterThanOrEqualTo("paymentDate", startOfMonth)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val list = querySnapshot.toObjects(Receipt::class.java)

                // 3.1.2 정렬: 결제 날짜 내림차순 -> createdAt 내림차순
                val sortedList = list.sortedWith(
                    compareByDescending<Receipt> { it.paymentDate }
                        .thenByDescending { it.createdAt }
                )
                onResult(sortedList)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // 영수증 즉시 삭제 (8.3.2)
    fun deleteReceipt(receiptId: String, onComplete: (Boolean) -> Unit) {
        if (receiptId.isBlank()) {
            onComplete(false)
            return
        }

        db.collection("receipts").document(receiptId)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}