package com.guru2.team3.sseunmat.ui.main

// 메인 화면

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.ui.feed.HomeFeedFragment
import com.guru2.team3.sseunmat.ui.write.ReceiptModeBottomSheet

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        // 초기 화면 설정 (홈 피드)
        if (savedInstanceState == null) {
            replaceFragment(HomeFeedFragment.newInstance())
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFeedFragment.newInstance())
                    true
                }
                R.id.nav_add -> {
                    // TODO: ReceiptModeBottomSheet 구현 완료 시 주석 해제
                    // val bottomSheet = ReceiptModeBottomSheet()
                    // bottomSheet.show(supportFragmentManager, "ReceiptModeBottomSheet")
                    false
                }
                R.id.nav_archive -> {
                    // TODO: ArchiveFragment 완성 시 교체
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}