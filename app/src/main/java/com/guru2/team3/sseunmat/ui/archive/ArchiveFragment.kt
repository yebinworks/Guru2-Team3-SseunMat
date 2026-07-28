package com.guru2.team3.sseunmat.ui.archive

// [예빈] 7. 가치 아카이브 (당월 그래프 & 통계)

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.model.Receipt
import com.guru2.team3.sseunmat.data.remote.FirebaseService
import com.guru2.team3.sseunmat.ui.mypage.MyPageActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ArchiveFragment : Fragment() {

    private lateinit var tvArchiveDate: TextView
    private lateinit var btnProfile: ImageView
    private lateinit var layoutArchiveEmpty: LinearLayout
    private lateinit var layoutArchiveContent: NestedScrollView
    private lateinit var rvValueGraph: RecyclerView

    private lateinit var cardTopValue: FrameLayout
    private lateinit var tvDateRangeChip: TextView

    // 만족도 점 5개 ImageView
    private val ratingDots = mutableListOf<ImageView>()

    private val firebaseService = FirebaseService()
    private val allValues = listOf("위로", "휴식", "관계", "추억", "배움", "영감", "마음 표현")

    companion object {
        private const val TAG = "ArchiveFragment"
        fun newInstance() = ArchiveFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            inflater.inflate(R.layout.fragment_archive, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "onCreateView 인플레이트 중 오류 발생", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            initViews(view)
            loadArchiveData()
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated 처리 중 오류 발생", e)
        }
    }

    private fun initViews(view: View) {
        tvArchiveDate = view.findViewById(R.id.tv_archive_date)
        layoutArchiveEmpty = view.findViewById(R.id.layout_archive_empty)
        layoutArchiveContent = view.findViewById(R.id.layout_archive_content)
        rvValueGraph = view.findViewById(R.id.rv_value_graph)
        btnProfile = view.findViewById(R.id.btn_profile)

        cardTopValue = view.findViewById(R.id.card_top_value)
        tvDateRangeChip = view.findViewById(R.id.tv_date_range_chip)

        btnProfile.setOnClickListener {
            try {
                val currentContext = context ?: return@setOnClickListener
                val intent = Intent(currentContext, MyPageActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "마이페이지 이동 중 오류 발생", e)
            }
        }

        // 5개 만족도 도트 ImageView 리스트 초기화
        ratingDots.clear()
        view.findViewById<ImageView>(R.id.dot_1)?.let { ratingDots.add(it) }
        view.findViewById<ImageView>(R.id.dot_2)?.let { ratingDots.add(it) }
        view.findViewById<ImageView>(R.id.dot_3)?.let { ratingDots.add(it) }
        view.findViewById<ImageView>(R.id.dot_4)?.let { ratingDots.add(it) }
        view.findViewById<ImageView>(R.id.dot_5)?.let { ratingDots.add(it) }

        // 현재 연도와 월 (예: 2026. 07) 고정 표시
        try {
            val sdf = SimpleDateFormat("yyyy. MM", Locale.KOREA)
            tvArchiveDate.text = sdf.format(Date())
        } catch (e: Exception) {
            tvArchiveDate.text = ""
        }

        context?.let { ctx ->
            rvValueGraph.layoutManager = LinearLayoutManager(ctx)
        }
    }

    private fun loadArchiveData() {
        firebaseService.getThisMonthReceipts { receipts ->
            // 생명주기 방어 - 콜백 시점에 Fragment가 detach 상태면 안전 종료
            if (!isAdded || activity == null || isDetached) return@getThisMonthReceipts

            activity?.runOnUiThread {
                try {
                    if (receipts.isEmpty()) {
                        layoutArchiveEmpty.visibility = View.VISIBLE
                        layoutArchiveContent.visibility = View.GONE
                    } else {
                        layoutArchiveEmpty.visibility = View.GONE
                        layoutArchiveContent.visibility = View.VISIBLE

                        calculateStatistics(receipts)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "loadArchiveData UI 반영 중 오류 발생", e)
                }
            }
        }
    }

    private fun calculateStatistics(receipts: List<Receipt>) {
        try {
            val countMap = receipts.groupingBy { it.value }.eachCount()

            val valueCountList = allValues.map { valName ->
                ValueCount(valName, countMap[valName] ?: 0)
            }.sortedByDescending { it.count }

            val topValue = valueCountList.firstOrNull { it.count > 0 }

            // 1. 대표 가치 통에셋 배경 세팅
            if (topValue != null) {
                val bgResId = when (topValue.valueName) {
                    "위로" -> R.drawable.card_bg_comfort
                    "휴식" -> R.drawable.card_bg_rest
                    "관계" -> R.drawable.card_bg_relation
                    "추억" -> R.drawable.card_bg_memory
                    "배움" -> R.drawable.card_bg_learning
                    "영감" -> R.drawable.card_bg_inspiration
                    "마음 표현" -> R.drawable.card_bg_heart
                    else -> R.drawable.card_bg_comfort
                }
                cardTopValue.setBackgroundResource(bgResId)
            }

            // 2. 당월 평균 만족도 계산 및 5개 도트 이미지 스위칭
            val validRatings = receipts.map { it.rating }
            val avgRating = if (validRatings.isNotEmpty()) validRatings.average() else 0.0
            val roundedRating = if (avgRating.isNaN() || avgRating.isInfinite()) 0 else Math.round(avgRating).toInt()
            updateRatingDots(roundedRating)

            // 3. 날짜 범위 칩 세팅 (예: 07.01 ~ 07.28)
            val cal = Calendar.getInstance()
            val currentMonth = String.format(Locale.KOREA, "%02d", cal.get(Calendar.MONTH) + 1)
            val currentDay = String.format(Locale.KOREA, "%02d", cal.get(Calendar.DAY_OF_MONTH))
            tvDateRangeChip.text = "$currentMonth.01 ~ $currentMonth.$currentDay"

            // 4. 가치 그래프 리사이클러뷰
            rvValueGraph.adapter = ValueGraphAdapter(valueCountList) { selectedValue ->
                try {
                    val currentContext = context ?: return@ValueGraphAdapter
                    val intent = Intent(currentContext, ValueDetailActivity::class.java).apply {
                        putExtra("SELECTED_VALUE", selectedValue)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "가치 상세 화면 이동 중 오류 발생", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "calculateStatistics 실행 중 오류 발생", e)
        }
    }

    // 만족도 점수에 따른 ON / OFF 이미지 교체
    private fun updateRatingDots(rating: Int) {
        val safeRating = rating.coerceIn(0, 5)
        for (i in 0 until ratingDots.size) {
            try {
                if (i < safeRating) {
                    ratingDots[i].setImageResource(R.drawable.ic_rating_dot_on)
                } else {
                    ratingDots[i].setImageResource(R.drawable.ic_rating_dot_off)
                }
            } catch (e: Exception) {
                Log.e(TAG, "도트 이미지 변경 중 오류 발생 (index: $i)", e)
            }
        }
    }
}