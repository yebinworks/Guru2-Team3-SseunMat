package com.guru2.team3.sseunmat.ui.main

// [예빈] 메인 화면

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.ui.archive.ArchiveFragment
import com.guru2.team3.sseunmat.ui.feed.HomeFeedFragment
import com.guru2.team3.sseunmat.ui.write.ReceiptModeBottomSheet

class MainActivity : AppCompatActivity() {

    private lateinit var btnNavHome: ImageView
    private lateinit var btnNavAdd: ImageView
    private lateinit var btnNavArchive: ImageView

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            btnNavHome = findViewById(R.id.btn_nav_home)
            btnNavAdd = findViewById(R.id.btn_nav_add)
            btnNavArchive = findViewById(R.id.btn_nav_archive)

            if (savedInstanceState == null) {
                replaceFragment(HomeFeedFragment.newInstance())
                updateNavSelection(btnNavHome)
            }

            // 홈
            btnNavHome.setOnClickListener {
                try {
                    replaceFragment(HomeFeedFragment.newInstance())
                    updateNavSelection(btnNavHome)
                } catch (e: Exception) {
                    Log.e(TAG, "홈 탭 클릭 처리 중 오류 발생", e)
                }
            }

            // 추가(+)
            btnNavAdd.setOnClickListener {
                try {
                    val existingFragment = supportFragmentManager.findFragmentByTag("ReceiptModeBottomSheet")
                    if (existingFragment == null && !supportFragmentManager.isStateSaved) {
                        val bottomSheet = ReceiptModeBottomSheet()
                        bottomSheet.show(supportFragmentManager, "ReceiptModeBottomSheet")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "작성 모드 바텀시트 표시 중 오류 발생", e)
                }
            }

            // 아카이브
            btnNavArchive.setOnClickListener {
                try {
                    replaceFragment(ArchiveFragment.newInstance())
                    updateNavSelection(btnNavArchive)
                } catch (e: Exception) {
                    Log.e(TAG, "아카이브 탭 클릭 처리 중 오류 발생", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate 처리 중 오류 발생", e)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        try {
            if (isFinishing || isDestroyed || supportFragmentManager.isStateSaved) return

            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e(TAG, "replaceFragment 실행 중 오류 발생", e)
        }
    }

    private fun updateNavSelection(selectedView: ImageView) {
        try {
            btnNavHome.isSelected = (selectedView == btnNavHome)
            btnNavArchive.isSelected = (selectedView == btnNavArchive)
        } catch (e: Exception) {
            Log.e(TAG, "updateNavSelection 상태 변경 중 오류 발생", e)
        }
    }
}