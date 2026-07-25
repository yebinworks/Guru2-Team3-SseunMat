package com.guru2.team3.sseunmat.ui.archive

// [예빈] 8. 특정 가치별 영수증 목록

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.remote.FirebaseService
import com.guru2.team3.sseunmat.ui.feed.ReceiptCardAdapter

class ValueDetailActivity : AppCompatActivity() {
    private lateinit var btnBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var vpReceiptCards: ViewPager2
    private lateinit var tvCardIndicator: TextView
    private lateinit var layoutEmpty: View

    private val firebaseService = FirebaseService()
    private lateinit var cardAdapter: ReceiptCardAdapter
    private var selectedValue: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_value_detail)

        selectedValue = intent.getStringExtra("SELECTED_VALUE") ?: ""

        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_header_title)
        vpReceiptCards = findViewById(R.id.vp_receipt_cards)
        tvCardIndicator = findViewById(R.id.tv_card_indicator)
        layoutEmpty = findViewById(R.id.layout_empty_state)

        tvTitle.text = selectedValue
        btnBack.setOnClickListener { finish() }

        // 기존 카드 어댑터 재사용
        cardAdapter = ReceiptCardAdapter(mutableListOf()) {
            if (cardAdapter.itemCount == 0) {
                layoutEmpty.visibility = View.VISIBLE
                vpReceiptCards.visibility = View.GONE
                tvCardIndicator.visibility = View.GONE
            } else {
                val currentPos = vpReceiptCards.currentItem
                tvCardIndicator.text = "${currentPos + 1} / ${cardAdapter.itemCount}"
            }
        }
        vpReceiptCards.adapter = cardAdapter
        vpReceiptCards.orientation = ViewPager2.ORIENTATION_VERTICAL

        vpReceiptCards.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val total = cardAdapter.itemCount
                if (total > 0) {
                    tvCardIndicator.text = "${position + 1} / $total"
                }
            }
        })

        loadFilteredReceipts()
    }

    private fun loadFilteredReceipts() {
        firebaseService.getThisMonthReceipts { list ->
            // 해당 가치로만 필터링
            val filteredList = list.filter { it.value == selectedValue }

            if (filteredList.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                vpReceiptCards.visibility = View.GONE
                tvCardIndicator.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                vpReceiptCards.visibility = View.VISIBLE
                tvCardIndicator.visibility = View.VISIBLE

                cardAdapter.updateData(filteredList)
                tvCardIndicator.text = "1 / ${filteredList.size}"
            }
        }
    }
}