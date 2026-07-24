package com.guru2.team3.sseunmat.ui.main

// [공통] 바텀 네비게이션 메인 컨테이너 (홈/아카이브/마이페이지 탭)

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.ui.write.ReceiptValueWriteActivity
import com.guru2.team3.sseunmat.ui.write.ReceiptWriteActivity
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // [기능 테스트시 사용] 방금 개발한 Activity로 변경

        // AI 인식 입력 테스트 코드 (false: 카메라 모드 / true: 갤러리 모드)
        val intent = Intent(this, ReceiptWriteActivity::class.java).apply {
            putExtra("IS_GALLERY_MODE", true)
            putExtra("EXTRACTED_STORE", "더벤티 노원점")
            putExtra("EXTRACTED_DATE", "2026. 07. 24")
            putExtra("EXTRACTED_AMOUNT", 12500L)
        }
        startActivity(intent)

    //        수동 입력 테스트 코드
//        val intent = Intent(this, ReceiptWriteActivity::class.java).apply {
//            putExtra("IS_MANUAL_MODE", true)
//        }
//        startActivity(intent)
    }
}