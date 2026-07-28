package com.guru2.team3.sseunmat.ui.feed

// [예빈] 3. 홈 화면 (당월 영수증 피드)

import StackCardPageTransformer
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.remote.FirebaseService
import com.guru2.team3.sseunmat.ui.mypage.MyPageActivity
import java.text.SimpleDateFormat
import java.util.Calendar
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

    companion object {
        private const val TAG = "HomeFeedFragment"
        fun newInstance() = HomeFeedFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            inflater.inflate(R.layout.fragment_home_feed, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "onCreateView 인플레이트 중 오류 발생", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            initViews(view)
            setupViewPager()
            loadThisMonthData()
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated 처리 중 오류 발생", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            loadThisMonthData()
        } catch (e: Exception) {
            Log.e(TAG, "onResume 로딩 중 오류 발생", e)
        }
    }

    private fun initViews(view: View) {
        tvCurrentMonth = view.findViewById(R.id.tv_current_month)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        vpReceiptCards = view.findViewById(R.id.vp_receipt_cards)
        tvCardIndicator = view.findViewById(R.id.tv_card_indicator)
        btnProfile = view.findViewById(R.id.btn_profile)

        btnProfile.setOnClickListener {
            try {
                val currentContext = context ?: return@setOnClickListener
                val intent = Intent(currentContext, MyPageActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "마이페이지 이동 중 오류 발생", e)
            }
        }

        try {
            val sdf = SimpleDateFormat("yyyy. MM", Locale.KOREA)
            tvCurrentMonth.text = sdf.format(Date())
        } catch (e: Exception) {
            tvCurrentMonth.text = ""
        }
    }

    private fun setupViewPager() {
        try {
            cardAdapter = ReceiptCardAdapter(mutableListOf()) {
                if (cardAdapter.itemCount == 0) {
                    layoutEmptyState.visibility = View.VISIBLE
                    vpReceiptCards.visibility = View.GONE
                    tvCardIndicator.visibility = View.GONE
                } else {
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

    private fun loadThisMonthData() {
        firebaseService.getThisMonthReceipts { receiptList ->
            if (!isAdded || activity == null || isDetached) return@getThisMonthReceipts

            activity?.runOnUiThread {
                try {
                    val safeList = receiptList ?: emptyList()
                    if (safeList.isEmpty()) {
                        layoutEmptyState.visibility = View.VISIBLE
                        vpReceiptCards.visibility = View.GONE
                        tvCardIndicator.visibility = View.GONE
                    } else {
                        layoutEmptyState.visibility = View.GONE
                        vpReceiptCards.visibility = View.VISIBLE
                        tvCardIndicator.visibility = View.VISIBLE

                        cardAdapter.updateData(safeList)
                        val currentPos = vpReceiptCards.currentItem.coerceIn(0, safeList.size - 1)
                        tvCardIndicator.text = "${currentPos + 1} / ${safeList.size}"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "loadThisMonthData UI 반영 중 오류 발생", e)
                }
            }
        }
    }
}