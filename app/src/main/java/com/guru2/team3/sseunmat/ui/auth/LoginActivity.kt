package com.guru2.team3.sseunmat.ui.auth

// [주은] 1. 로그인

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
                val email = etUserEmail.text.toString().trim()
                val password = etUserPassword.text.toString()
                btnLogin.isEnabled = email.isNotEmpty() && password.isNotEmpty()
            }
        }
        etUserEmail.addTextChangedListener(inputWatcher)
        etUserPassword.addTextChangedListener(inputWatcher)

        // [1.1.2] 비밀번호 보기/숨기기 토글
        var isPasswordVisible = false
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etUserPassword.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // inputType 변경 시 커서가 맨 앞으로 이동하므로 맨 뒤로 되돌린다
            etUserPassword.setSelection(etUserPassword.text.length)
        }

        // [1.3.1] 회원가입 화면 이동
        tvGoSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}
