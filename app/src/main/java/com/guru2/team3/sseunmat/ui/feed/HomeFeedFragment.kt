package com.guru2.team3.sseunmat.ui.feed

// [예빈] 3. 홈 화면 (당월 영수증 피드)

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.remote.FirebaseService
import java.util.Calendar

class HomeFeedFragment : Fragment() {

    private lateinit var tvCurrentMonth: TextView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var vpReceiptCards: ViewPager2
    private lateinit var tvCardIndicator: TextView
    private lateinit var btnAddReceipt: ImageView

    private lateinit var cardAdapter: ReceiptCardAdapter
    private val firebaseService = FirebaseService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupViewPager()
        loadThisMonthData()
    }

    override fun onResume() {
        super.onResume()
        loadThisMonthData()
    }

    private fun initViews(view: View) {
        tvCurrentMonth = view.findViewById(R.id.tv_current_month)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        vpReceiptCards = view.findViewById(R.id.vp_receipt_cards)
        tvCardIndicator = view.findViewById(R.id.tv_card_indicator)

        // 현재 월 표시 (예: 7월)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        tvCurrentMonth.text = "${currentMonth}월"
    }

    private fun setupViewPager() {
        cardAdapter = ReceiptCardAdapter(emptyList())
        vpReceiptCards.adapter = cardAdapter
        vpReceiptCards.orientation = ViewPager2.ORIENTATION_VERTICAL // 3.2.1 세로 스와이프

        // 3.2.2 카드 순서 표시
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

    private fun loadThisMonthData() {
        firebaseService.getThisMonthReceipts { receiptList ->
            if (!isAdded) return@getThisMonthReceipts

            if (receiptList.isEmpty()) {
                // 3.1.3 홈 빈 상태 표시
                layoutEmptyState.visibility = View.VISIBLE
                vpReceiptCards.visibility = View.GONE
                tvCardIndicator.visibility = View.GONE
            } else {
                layoutEmptyState.visibility = View.GONE
                vpReceiptCards.visibility = View.VISIBLE
                tvCardIndicator.visibility = View.VISIBLE

                cardAdapter.updateData(receiptList)
                tvCardIndicator.text = "1 / ${receiptList.size}"
            }
        }
    }

    companion object {
        fun newInstance() = HomeFeedFragment()
    }
}