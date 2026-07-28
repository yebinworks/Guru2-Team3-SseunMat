package com.guru2.team3.sseunmat.ui.archive

// [예빈] 7. 가치 아카이브 (당월 그래프 & 통계)

import android.content.Intent
import android.os.Bundle
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ArchiveFragment : Fragment() {

    private lateinit var tvArchiveDate: TextView
    private lateinit var layoutArchiveEmpty: LinearLayout
    private lateinit var layoutArchiveContent: NestedScrollView
    private lateinit var rvValueGraph: RecyclerView

    private lateinit var cardTopValue: FrameLayout
    private lateinit var tvDateRangeChip: TextView

    // 만족도 점 5개 ImageView
    private val ratingDots = mutableListOf<ImageView>()

    private val firebaseService = FirebaseService()
    private val allValues = listOf("위로", "휴식", "관계", "추억", "배움", "영감", "마음 표현")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        loadArchiveData()
    }

    private fun initViews(view: View) {
        tvArchiveDate = view.findViewById(R.id.tv_archive_date)
        layoutArchiveEmpty = view.findViewById(R.id.layout_archive_empty)
        layoutArchiveContent = view.findViewById(R.id.layout_archive_content)
        rvValueGraph = view.findViewById(R.id.rv_value_graph)

        cardTopValue = view.findViewById(R.id.card_top_value)
        tvDateRangeChip = view.findViewById(R.id.tv_date_range_chip)

        // 5개 만족도 도트 ImageView 리스트 초기화
        ratingDots.clear()
        ratingDots.add(view.findViewById(R.id.dot_1))
        ratingDots.add(view.findViewById(R.id.dot_2))
        ratingDots.add(view.findViewById(R.id.dot_3))
        ratingDots.add(view.findViewById(R.id.dot_4))
        ratingDots.add(view.findViewById(R.id.dot_5))

        // 현재 연도와 월 (2026. 07) 고정 표시
        val sdf = SimpleDateFormat("yyyy. MM", Locale.KOREA)
        tvArchiveDate.text = sdf.format(Date())

        rvValueGraph.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadArchiveData() {
        firebaseService.getThisMonthReceipts { receipts ->
            if (!isAdded) return@getThisMonthReceipts

            if (receipts.isEmpty()) {
                layoutArchiveEmpty.visibility = View.VISIBLE
                layoutArchiveContent.visibility = View.GONE
            } else {
                layoutArchiveEmpty.visibility = View.GONE
                layoutArchiveContent.visibility = View.VISIBLE

                calculateStatistics(receipts)
            }
        }
    }

    private fun calculateStatistics(receipts: List<Receipt>) {
        val countMap = receipts.groupingBy { it.value }.eachCount()

        val valueCountList = allValues.map { valName ->
            ValueCount(valName, countMap[valName] ?: 0)
        }.sortedByDescending { it.count }

        // 게이지 바 100% 기준이 될 최대 횟수
        val maxCount = valueCountList.maxOfOrNull { it.count } ?: 0
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
        val avgRating = receipts.map { it.rating }.average()
        val roundedRating = if (avgRating.isNaN()) 0 else Math.round(avgRating).toInt()
        updateRatingDots(roundedRating)

        // 3. 날짜 범위 칩 세팅 (예: 07.01 ~ 07.28)
        val cal = Calendar.getInstance()
        val currentMonth = String.format(Locale.KOREA, "%02d", cal.get(Calendar.MONTH) + 1)
        val currentDay = String.format(Locale.KOREA, "%02d", cal.get(Calendar.DAY_OF_MONTH))
        tvDateRangeChip.text = "$currentMonth.01 ~ $currentMonth.$currentDay"

        // 4. 가치 그래프 리사이클러뷰
        rvValueGraph.adapter = ValueGraphAdapter(valueCountList) { selectedValue ->
            val intent = Intent(requireContext(), ValueDetailActivity::class.java).apply {
                putExtra("SELECTED_VALUE", selectedValue)
            }
            startActivity(intent)
        }
    }

    // 만족도 점수에 따른 ON / OFF 이미지 교체
    private fun updateRatingDots(rating: Int) {
        for (i in 0 until 5) {
            if (i < rating) {
                ratingDots[i].setImageResource(R.drawable.ic_rating_dot_on)
            } else {
                ratingDots[i].setImageResource(R.drawable.ic_rating_dot_off)
            }
        }
    }

    companion object {
        fun newInstance() = ArchiveFragment()
    }
}