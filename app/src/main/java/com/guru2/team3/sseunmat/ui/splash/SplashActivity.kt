package com.guru2.team3.sseunmat.ui.splash

// [주은] 0. 스플래시

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.remote.FirebaseService
import com.guru2.team3.sseunmat.ui.auth.LoginActivity
import com.guru2.team3.sseunmat.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    private val firebaseService = FirebaseService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // [0.1.1] 저장된 인증 정보 확인 후 분기
        // 로고/로딩 표시를 위해 1초 뒤 이동한다
        Handler(Looper.getMainLooper()).postDelayed({
            val isLoggedIn = firebaseService.getCurrentUserId().isNotEmpty()

            val nextActivity = if (isLoggedIn) {
                MainActivity::class.java   // 유효한 인증 정보 있음 → 홈
            } else {
                LoginActivity::class.java  // 없음/만료 → 로그인
            }

            val intent = Intent(this, nextActivity)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }, 1000)
    }
}