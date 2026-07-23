package com.guru2.team3.sseunmat.ui.main

// [공통] 바텀 네비게이션 메인 컨테이너 (홈/아카이브/마이페이지 탭)

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.guru2.team3.sseunmat.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
    }
}