package com.guru2.team3.sseunmat.data.remote

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.guru2.team3.sseunmat.data.model.Users

// [예빈] Firebase Auth & Firestore CRUD

class FirebaseService {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

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
}