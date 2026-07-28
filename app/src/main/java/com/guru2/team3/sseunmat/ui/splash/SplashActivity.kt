package com.guru2.team3.sseunmat.ui.splash

// [주은] 0. 스플래시

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, 1000)
    }

    private fun navigateToNextScreen() {
        try {
            val userId = firebaseService.getCurrentUserId()
            val isLoggedIn = !userId.isNullOrEmpty()

            val nextActivity = if (isLoggedIn) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }

            val intent = Intent(this, nextActivity).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("SplashActivity", "화면 전환 실패: ${e.message}", e)
            // 예외 발생 시 기본적으로 로그인 화면으로 안전 이동
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}