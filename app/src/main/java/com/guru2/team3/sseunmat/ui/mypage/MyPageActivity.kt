package com.guru2.team3.sseunmat.ui.mypage

// [예빈] 마이페이지

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.remote.FirebaseService
import com.guru2.team3.sseunmat.ui.auth.LoginActivity

class MyPageActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvNickname: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvTotalReceiptCount: TextView
    private lateinit var btnLogout: Button

    private val firebaseService = FirebaseService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        initViews()
        loadUserData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvNickname = findViewById(R.id.tv_user_nickname)
        tvEmail = findViewById(R.id.tv_user_email)
        tvTotalReceiptCount = findViewById(R.id.tv_total_receipt_count)
        btnLogout = findViewById(R.id.btn_logout)

        btnBack.setOnClickListener { finish() }

        // 로그아웃 버튼 클릭 -> 팝업 다이얼로그 출력
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadUserData() {
        // 1. 유저 프로필 조회 (닉네임, 이메일)
        firebaseService.getUserProfile { user ->
            user?.let {
                tvNickname.text = it.nickname.ifEmpty { "사용자" }
                tvEmail.text = it.email
            }
        }

        // 2. 전체 영수증 개수 조회
        firebaseService.getTotalReceiptCount { count ->
            val formattedCount = String.format("%02d", count)
            tvTotalReceiptCount.text = "전체 영수증 ${formattedCount}건"
        }
    }

    // 팝업 다이얼로그
    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnConfirmLogout = dialogView.findViewById<Button>(R.id.btn_confirm_logout)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirmLogout.setOnClickListener {
            dialog.dismiss()
            firebaseService.logout()

            // 로그인 화면으로 이동 & 기존 액티비티 스택 모두 삭제
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        dialog.show()
    }
}