package com.guru2.team3.sseunmat.ui.main

// [공통] 바텀 네비게이션 메인 컨테이너 (홈/아카이브/마이페이지 탭)

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.ui.write.ReceiptValueWriteActivity
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // [기능 테스트시 사용] 방금 개발한 Activity로 변경
        val intent = Intent(this, ReceiptValueWriteActivity::class.java).apply {
            putExtra("STORE", "더벤티 노원점")
            putExtra("DATE", "2026. 07. 24")
            putExtra("AMOUNT", 12500L)
        }

        startActivity(intent)
    }
}