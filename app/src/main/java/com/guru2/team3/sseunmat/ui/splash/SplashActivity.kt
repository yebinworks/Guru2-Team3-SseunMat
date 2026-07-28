package com.guru2.team3.sseunmat.ui.splash

// [주은] 0. 스플래시

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
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
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    companion object {
        private const val TAG = "SplashActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_splash)

            val mainView = findViewById<View>(R.id.main) ?: findViewById(android.R.id.content)
            mainView?.let { v ->
                ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }

            runnable = Runnable {
                if (!isFinishing && !isDestroyed) {
                    navigateToNextScreen()
                }
            }
            runnable?.let { handler.postDelayed(it, 1000) }

        } catch (e: Exception) {
            Log.e(TAG, "onCreate 처리 중 오류 발생", e)
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        try {
            if (isFinishing || isDestroyed) return

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
            Log.e(TAG, "화면 전환 실패: ${e.message}", e)
            try {
                if (!isFinishing && !isDestroyed) {
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            } catch (ex: Exception) {
                Log.e(TAG, "최종 예외 복구 실패", ex)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            runnable?.let { handler.removeCallbacks(it) }
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy 핸들러 콜백 제거 실패", e)
        }
    }
}