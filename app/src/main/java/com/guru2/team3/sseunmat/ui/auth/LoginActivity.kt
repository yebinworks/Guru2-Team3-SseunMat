package com.guru2.team3.sseunmat.ui.auth

// [주은] 1. 로그인

import com.guru2.team3.sseunmat.data.remote.FirebaseService
import com.guru2.team3.sseunmat.ui.main.MainActivity
import com.guru2.team3.sseunmat.util.showToast
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guru2.team3.sseunmat.R

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_login)

            val mainView = findViewById<View>(R.id.main) ?: findViewById(android.R.id.content)
            mainView?.let { v ->
                ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }

            // 뷰 참조
            val etUserEmail = findViewById<EditText>(R.id.et_user_email)
            val etUserPassword = findViewById<EditText>(R.id.et_user_password)
            val btnLogin = findViewById<Button>(R.id.btn_login)
            val btnTogglePassword = findViewById<ImageButton>(R.id.btn_toggle_password)
            val tvGoSignUp = findViewById<TextView>(R.id.tv_go_sign_up)

            // [1.2.1] 이메일/비밀번호 모두 입력 시 로그인 버튼 활성화
            val inputWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    try {
                        val email = etUserEmail.text?.toString()?.trim() ?: ""
                        val password = etUserPassword.text?.toString() ?: ""
                        btnLogin.isEnabled = email.isNotEmpty() && password.isNotEmpty()
                    } catch (e: Exception) {
                        Log.e(TAG, "입력값 변경 감지 중 오류 발생", e)
                    }
                }
            }
            etUserEmail.addTextChangedListener(inputWatcher)
            etUserPassword.addTextChangedListener(inputWatcher)

            // [1.1.2] 비밀번호 보기/숨기기 토글
            var isPasswordVisible = false
            btnTogglePassword.setOnClickListener {
                try {
                    isPasswordVisible = !isPasswordVisible
                    etUserPassword.inputType = if (isPasswordVisible) {
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    } else {
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }
                    val textLength = etUserPassword.text?.length ?: 0
                    etUserPassword.setSelection(textLength)
                } catch (e: Exception) {
                    Log.e(TAG, "비밀번호 토글 처리 중 오류 발생", e)
                }
            }

            // [1.2.2 / 1.2.3] 로그인 처리
            val firebaseService = FirebaseService()
            btnLogin.setOnClickListener {
                try {
                    val email = etUserEmail.text?.toString()?.trim() ?: ""
                    val password = etUserPassword.text?.toString() ?: ""

                    if (email.isEmpty() || password.isEmpty()) {
                        showToast("이메일과 비밀번호를 모두 입력해 주세요")
                        return@setOnClickListener
                    }

                    btnLogin.isEnabled = false  // 중복 클릭 방지
                    firebaseService.login(email, password) { isSuccess ->
                        if (isFinishing || isDestroyed) return@login

                        runOnUiThread {
                            try {
                                if (isSuccess) {
                                    // [1.2.2] 로그인 성공 -> 홈으로, 이전 화면 스택 제거
                                    val intent = Intent(this, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // [1.2.3] 로그인 실패 -> 안내 문구
                                    showToast("이메일 또는 비밀번호를 확인해 주세요")
                                    btnLogin.isEnabled = true
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "로그인 결과 UI 반영 중 오류 발생", e)
                                btnLogin.isEnabled = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "로그인 버튼 클릭 이벤트 처리 중 오류 발생", e)
                    btnLogin.isEnabled = true
                }
            }

            // [1.3.1] 회원가입 화면 이동
            tvGoSignUp.setOnClickListener {
                try {
                    startActivity(Intent(this, SignUpActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "회원가입 화면 이동 중 오류 발생", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate 처리 중 오류 발생", e)
        }
    }
}