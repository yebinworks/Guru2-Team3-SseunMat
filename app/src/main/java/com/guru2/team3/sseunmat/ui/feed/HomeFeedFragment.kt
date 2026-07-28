package com.guru2.team3.sseunmat.ui.feed

// [예빈] 3. 홈 화면 (당월 영수증 피드)

import StackCardPageTransformer
import android.content.Intent
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
import androidx.recyclerview.widget.RecyclerView
import com.guru2.team3.sseunmat.ui.mypage.MyPageActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFeedFragment : Fragment() {

    private lateinit var tvCurrentMonth: TextView
    private lateinit var btnProfile: ImageView
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
        btnProfile = view.findViewById(R.id.btn_profile)

        btnProfile.setOnClickListener {
            val intent = Intent(requireContext(), MyPageActivity::class.java)
            startActivity(intent)
        }

        val sdf = SimpleDateFormat("yyyy. MM", Locale.KOREA)
        tvCurrentMonth.text = sdf.format(Date())
    }

    private fun setupViewPager() {
        cardAdapter = ReceiptCardAdapter(mutableListOf()) {
            if (cardAdapter.itemCount == 0) {
                layoutEmptyState.visibility = View.VISIBLE
                vpReceiptCards.visibility = View.GONE
                tvCardIndicator.visibility = View.GONE
            } else {
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

    private fun loadThisMonthData() {
        firebaseService.getThisMonthReceipts { receiptList ->
            if (!isAdded) return@getThisMonthReceipts

            // UI 갱신은 메인 스레드에서 처리
            activity?.runOnUiThread {
                if (receiptList.isEmpty()) {
                    layoutEmptyState.visibility = View.VISIBLE
                    vpReceiptCards.visibility = View.GONE
                    tvCardIndicator.visibility = View.GONE
                } else {
                    layoutEmptyState.visibility = View.GONE
                    vpReceiptCards.visibility = View.VISIBLE
                    tvCardIndicator.visibility = View.VISIBLE

                    cardAdapter.updateData(receiptList)
                    val currentPos = vpReceiptCards.currentItem
                    tvCardIndicator.text = "${currentPos + 1} / ${receiptList.size}"
                }
            }
        }
    }

    companion object {
        fun newInstance() = HomeFeedFragment()
    }
}