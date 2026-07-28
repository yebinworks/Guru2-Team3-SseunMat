package com.guru2.team3.sseunmat.ui.main

// 메인 화면

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            replaceFragment(HomeFeedFragment.newInstance())
            updateNavSelection(btnNavHome)
        }

        // 추가(+)
        btnNavAdd.setOnClickListener {
            btnNavAdd.setOnClickListener {
                val bottomSheet = ReceiptModeBottomSheet()
                bottomSheet.show(supportFragmentManager, "ReceiptModeBottomSheet")
            }
        }

        // 아카이브
        btnNavArchive.setOnClickListener {
             replaceFragment(ArchiveFragment.newInstance())
            updateNavSelection(btnNavArchive)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .commit()
    }

    private fun updateNavSelection(selectedView: ImageView) {
        btnNavHome.isSelected = (selectedView == btnNavHome)
        btnNavArchive.isSelected = (selectedView == btnNavArchive)
    }
}