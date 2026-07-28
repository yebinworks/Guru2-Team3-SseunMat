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

    companion object {
        private const val TAG = "ValueDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_value_detail)

            selectedValue = intent?.getStringExtra("SELECTED_VALUE") ?: ""
            Log.d(TAG, "전달받은 SELECTED_VALUE: '$selectedValue'")

            initViews()
            setupViewPager()
            loadFilteredReceipts()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate 처리 중 오류 발생", e)
        }
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
            try {
                val intent = Intent(this, MyPageActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "마이페이지 이동 중 오류 발생", e)
            }
        }
    }

    private fun setupViewPager() {
        try {
            cardAdapter = ReceiptCardAdapter(mutableListOf()) {
                if (cardAdapter.itemCount == 0) {
                    showEmptyState(true)
                } else {
                    showEmptyState(false)
                    val currentPos = vpReceiptCards.currentItem.coerceIn(0, cardAdapter.itemCount - 1)
                    tvCardIndicator.text = "${currentPos + 1} / ${cardAdapter.itemCount}"
                }
            }

            vpReceiptCards.adapter = cardAdapter
            vpReceiptCards.orientation = ViewPager2.ORIENTATION_VERTICAL
            vpReceiptCards.offscreenPageLimit = 3

            vpReceiptCards.post {
                try {
                    (vpReceiptCards.getChildAt(0) as? RecyclerView)?.apply {
                        clipChildren = false
                        clipToPadding = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "ViewPager2 내 RecyclerView 설정 중 오류 발생", e)
                }
            }

            vpReceiptCards.setPageTransformer(StackCardPageTransformer())

            vpReceiptCards.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val total = cardAdapter.itemCount
                    if (total > 0) {
                        val safePosition = position.coerceIn(0, total - 1)
                        tvCardIndicator.text = "${safePosition + 1} / $total"
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "setupViewPager 초기화 중 오류 발생", e)
        }
    }

    private fun loadFilteredReceipts() {
        firebaseService.getThisMonthReceipts { list ->
            // Activity가 종료 중이거나 파괴된 상태인 경우 처리하지 않음
            if (isFinishing || isDestroyed) return@getThisMonthReceipts

            runOnUiThread {
                try {
                    val safeList = list ?: emptyList()
                    val filteredList = if (selectedValue.isBlank()) {
                        safeList
                    } else {
                        safeList.filter { it.value.trim() == selectedValue.trim() }
                    }

                    Log.d(TAG, "전체 개수: ${safeList.size}, 필터링 개수: ${filteredList.size}")

                    if (filteredList.isEmpty()) {
                        showEmptyState(true)
                    } else {
                        showEmptyState(false)
                        cardAdapter.updateData(filteredList)
                        val currentPos = vpReceiptCards.currentItem.coerceIn(0, filteredList.size - 1)
                        tvCardIndicator.text = "${currentPos + 1} / ${filteredList.size}"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "필터링 데이터 UI 반영 중 오류 발생", e)
                    showEmptyState(true)
                }
            }
        }
    }

    private fun showEmptyState(isEmpty: Boolean) {
        try {
            if (isEmpty) {
                layoutEmpty.visibility = View.VISIBLE
                vpReceiptCards.visibility = View.GONE
                tvCardIndicator.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                vpReceiptCards.visibility = View.VISIBLE
                tvCardIndicator.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e(TAG, "EmptyState 전환 중 오류 발생", e)
        }
    }
}