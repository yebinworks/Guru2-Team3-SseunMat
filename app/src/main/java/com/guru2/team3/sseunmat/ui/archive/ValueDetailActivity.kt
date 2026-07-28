package com.guru2.team3.sseunmat.ui.archive

// [예빈] 8. 특정 가치별 영수증 목록

import StackCardPageTransformer
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.remote.FirebaseService
import com.guru2.team3.sseunmat.ui.feed.ReceiptCardAdapter
import com.guru2.team3.sseunmat.ui.mypage.MyPageActivity

class ValueDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var btnProfile: ImageView
    private lateinit var vpReceiptCards: ViewPager2
    private lateinit var tvCardIndicator: TextView
    private lateinit var layoutEmpty: LinearLayout

    private val firebaseService = FirebaseService()
    private lateinit var cardAdapter: ReceiptCardAdapter
    private var selectedValue: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_value_detail)

        selectedValue = intent.getStringExtra("SELECTED_VALUE") ?: ""
        Log.d("ValueDetailActivity", "전달받은 SELECTED_VALUE: '$selectedValue'")

        initViews()
        setupViewPager()
        loadFilteredReceipts()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_header_title)
        btnProfile = findViewById(R.id.btn_profile)
        vpReceiptCards = findViewById(R.id.vp_receipt_cards)
        tvCardIndicator = findViewById(R.id.tv_card_indicator)
        layoutEmpty = findViewById(R.id.layout_empty_state)

        tvTitle.text = if (selectedValue.isNotBlank()) selectedValue else "전체 가치"

        btnBack.setOnClickListener { finish() }

        btnProfile.setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupViewPager() {
        cardAdapter = ReceiptCardAdapter(mutableListOf()) {
            if (cardAdapter.itemCount == 0) {
                showEmptyState(true)
            } else {
                showEmptyState(false)
                val currentPos = vpReceiptCards.currentItem
                tvCardIndicator.text = "${currentPos + 1} / ${cardAdapter.itemCount}"
            }
        }

        vpReceiptCards.adapter = cardAdapter
        vpReceiptCards.orientation = ViewPager2.ORIENTATION_VERTICAL
        vpReceiptCards.offscreenPageLimit = 3

        vpReceiptCards.post {
            (vpReceiptCards.getChildAt(0) as? RecyclerView)?.apply {
                clipChildren = false
                clipToPadding = false
            }
        }

        vpReceiptCards.setPageTransformer(StackCardPageTransformer())

        vpReceiptCards.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val total = cardAdapter.itemCount
                if (total > 0) {
                    tvCardIndicator.text = "${position + 1} / $total"
                }
            }
        })
    }

    private fun loadFilteredReceipts() {
        firebaseService.getThisMonthReceipts { list ->
            val filteredList = if (selectedValue.isBlank()) {
                list
            } else {
                list.filter { it.value.trim() == selectedValue.trim() }
            }

            Log.d("ValueDetailActivity", "전체 개수: ${list.size}, 필터링 개수: ${filteredList.size}")

            runOnUiThread {
                if (filteredList.isEmpty()) {
                    showEmptyState(true)
                } else {
                    showEmptyState(false)
                    cardAdapter.updateData(filteredList)
                    val currentPos = vpReceiptCards.currentItem
                    tvCardIndicator.text = "${currentPos + 1} / ${filteredList.size}"
                }
            }
        }
    }

    private fun showEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            layoutEmpty.visibility = View.VISIBLE
            vpReceiptCards.visibility = View.GONE
            tvCardIndicator.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            vpReceiptCards.visibility = View.VISIBLE
            tvCardIndicator.visibility = View.VISIBLE
        }
    }
}