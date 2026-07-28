package com.guru2.team3.sseunmat.data.remote

import android.util.Log
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

    companion object {
        private const val TAG = "FirebaseService"
    }

    // 현재 로그인된 유저 ID 가져오기
    fun getCurrentUserId(): String {
        return try {
            auth.currentUser?.uid ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentUserId 중 오류 발생: ${e.message}", e)
            ""
        }
    }

    // 회원가입 처리
    fun signUp(email: String, pass: String, nickname: String, onResult: (Boolean) -> Unit) {
        try {
            // 유효성 검증: 공백이나 이메일 형식 미달 사전 차단
            if (email.isBlank() || pass.isBlank() || nickname.isBlank() || pass.length < 6) {
                Log.w(TAG, "signUp 실패: 유효하지 않은 입력값 (이메일/비밀번호/닉네임 확인 필요)")
                onResult(false)
                return
            }

            auth.createUserWithEmailAndPassword(email.trim(), pass.trim())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = getCurrentUserId()
                        if (userId.isEmpty()) {
                            onResult(false)
                            return@addOnCompleteListener
                        }

                        val newUser = Users(usersId = userId, email = email.trim(), nickname = nickname.trim())

                        db.collection("users").document(userId).set(newUser)
                            .addOnSuccessListener { onResult(true) }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Firestore 유저 정보 저장 실패: ${e.message}", e)
                                onResult(false)
                            }
                    } else {
                        Log.e(TAG, "Firebase 회원가입 실패: ${task.exception?.message}")
                        onResult(false)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "signUp 작업 중 예기치 못한 예외 발생", e)
            onResult(false)
        }
    }

    // 로그인 처리
    fun login(email: String, pass: String, onResult: (Boolean) -> Unit) {
        try {
            // 유효성 검증: 빈 값 전송 시 Firebase Crash 방지
            if (email.isBlank() || pass.isBlank()) {
                Log.w(TAG, "login 실패: 이메일 또는 비밀번호가 비어있음")
                onResult(false)
                return
            }

            auth.signInWithEmailAndPassword(email.trim(), pass.trim())
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e(TAG, "Firebase 로그인 실패: ${task.exception?.message}")
                    }
                    onResult(task.isSuccessful)
                }
        } catch (e: Exception) {
            Log.e(TAG, "login 작업 중 예기치 못한 예외 발생", e)
            onResult(false)
        }
    }

    // 영수증 기록 저장
    fun addReceipt(receipt: Receipt?, onResult: (Boolean) -> Unit) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId.isEmpty()) {
                Log.w(TAG, "addReceipt 취소: 로그인된 유저가 없습니다.")
                onResult(false)
                return
            }

            // 유효성 검증: null 영수증 객체 방어
            if (receipt == null) {
                Log.w(TAG, "addReceipt 취소: receipt 객체가 null입니다.")
                onResult(false)
                return
            }

            val docRef = db.collection("receipts").document()

            val finalReceipt = receipt.copy(
                receiptId = docRef.id,
                usersId = currentUserId
            )

            docRef.set(finalReceipt)
                .addOnSuccessListener { onResult(true) }
                .addOnFailureListener { e ->
                    Log.e(TAG, "addReceipt Firestore 저장 실패: ${e.message}", e)
                    onResult(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "addReceipt 작업 중 예기치 못한 예외 발생", e)
            onResult(false)
        }
    }

    // 이번 달에 등록된 영수증 조회 (3.1.1 & 3.1.2)
    fun getThisMonthReceipts(onResult: (List<Receipt>) -> Unit) {
        try {
            val currentUserId = getCurrentUserId()

            if (currentUserId.isEmpty()) {
                Log.w(TAG, "조회 취소: 로그인된 유저가 없습니다.")
                onResult(emptyList())
                return
            }

            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfMonth = calendar.time

            db.collection("receipts")
                .whereEqualTo("usersId", currentUserId)
                .whereGreaterThanOrEqualTo("paymentDate", startOfMonth)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    try {
                        // Firestore 객체 변환 중 파싱 에러 안전 처리
                        val list = querySnapshot.toObjects(Receipt::class.java)

                        // paymentDate나 createdAt이 null인 경우에도 NPE 없이 안전 정렬
                        val sortedList = list.sortedWith(
                            compareByDescending<Receipt, java.util.Date?>(nullsLast()) { it.paymentDate }
                                .thenByDescending(nullsLast()) { it.createdAt }
                        )
                        Log.d(TAG, "조회 성공: ${sortedList.size}개 검색됨")
                        onResult(sortedList)
                    } catch (e: Exception) {
                        Log.e(TAG, "Firestore 데이터 객체 변환(toObjects) 중 오류 발생", e)
                        onResult(emptyList())
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "영수증 불러오기 실패: ${exception.message}", exception)
                    onResult(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "getThisMonthReceipts 실행 중 예외 발생", e)
            onResult(emptyList())
        }
    }

    // 영수증 즉시 삭제 (8.3.2)
    fun deleteReceipt(receiptId: String?, onComplete: (Boolean) -> Unit) {
        try {
            if (receiptId.isNullOrBlank()) {
                Log.w(TAG, "deleteReceipt 취소: receiptId가 비어있습니다.")
                onComplete(false)
                return
            }

            db.collection("receipts").document(receiptId)
                .delete()
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { e ->
                    Log.e(TAG, "deleteReceipt 삭제 실패: ${e.message}", e)
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "deleteReceipt 실행 중 예외 발생", e)
            onComplete(false)
        }
    }

    // 유저 프로필 정보 가져오기 (마이페이지용)
    fun getUserProfile(onResult: (Users?) -> Unit) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId.isEmpty()) {
                onResult(null)
                return
            }

            db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        val user = document.toObject(Users::class.java)
                        onResult(user)
                    } catch (e: Exception) {
                        Log.e(TAG, "유저 데이터 파싱 중 오류 발생", e)
                        onResult(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "getUserProfile 실패: ${e.message}", e)
                    onResult(null)
                }
        } catch (e: Exception) {
            Log.e(TAG, "getUserProfile 실행 중 예외 발생", e)
            onResult(null)
        }
    }

    // 전체 작성 영수증 개수 가져오기
    fun getTotalReceiptCount(onResult: (Int) -> Unit) {
        try {
            val currentUserId = getCurrentUserId()
            if (currentUserId.isEmpty()) {
                onResult(0)
                return
            }

            db.collection("receipts")
                .whereEqualTo("usersId", currentUserId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    onResult(querySnapshot?.size() ?: 0)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "getTotalReceiptCount 실패: ${e.message}", e)
                    onResult(0)
                }
        } catch (e: Exception) {
            Log.e(TAG, "getTotalReceiptCount 실행 중 예외 발생", e)
            onResult(0)
        }
    }

    // 로그아웃
    fun logout() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "logout 중 예외 발생: ${e.message}", e)
        }
    }
}