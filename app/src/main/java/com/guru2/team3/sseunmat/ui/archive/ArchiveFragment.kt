package com.guru2.team3.sseunmat.ui.archive

// [예빈] 7. 가치 아카이브 (당월 그래프 & 통계)

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.model.Receipt
import com.guru2.team3.sseunmat.data.remote.FirebaseService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArchiveFragment : Fragment() {

    private lateinit var tvArchiveDate: TextView
    private lateinit var layoutArchiveEmpty: LinearLayout
    private lateinit var layoutArchiveContent: LinearLayout
    private lateinit var tvTopValueSummary: TextView
    private lateinit var tvAverageRating: TextView
    private lateinit var rvValueGraph: RecyclerView

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
        tvTopValueSummary = view.findViewById(R.id.tv_top_value_summary)
        tvAverageRating = view.findViewById(R.id.tv_average_rating)
        rvValueGraph = view.findViewById(R.id.rv_value_graph)

        // 7.1.1 현재 연도와 월 (2026.07) 고정 표시
        val sdf = SimpleDateFormat("yyyy. MM", Locale.KOREA)
        tvArchiveDate.text = sdf.format(Date())

        rvValueGraph.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadArchiveData() {
        // 기존 작성된 이번 달 데이터 조회 로직 100% 재사용!
        firebaseService.getThisMonthReceipts { receipts ->
            if (!isAdded) return@getThisMonthReceipts

            if (receipts.isEmpty()) {
                // 7.1.2 당월 빈 상태 노출
                layoutArchiveEmpty.visibility = View.VISIBLE
                layoutArchiveContent.visibility = View.GONE
            } else {
                layoutArchiveEmpty.visibility = View.GONE
                layoutArchiveContent.visibility = View.VISIBLE

                calculateStatistics(receipts)
            }
        }
    }

    // 7.2 가치 요약 계산 (대표 가치, 평균 만족도, 가치별 횟수)
    private fun calculateStatistics(receipts: List<Receipt>) {
        // 가치별 횟수 집계
        val countMap = receipts.groupingBy { it.value }.eachCount()

        // 7가지 지정된 가치 항목 세팅
        val valueCountList = allValues.map { valName ->
            ValueCount(valName, countMap[valName] ?: 0)
        }.sortedByDescending { it.count }

        // 2. 7.2.2 대표 가치 찾기 (가장 많이 기록된 가치)
        val topValue = valueCountList.maxByOrNull { it.count }
        if (topValue != null && topValue.count > 0) {
            tvTopValueSummary.text = "이번 달 가장 많이 남은 가치는 '${topValue.valueName}'(이)에요."
        } else {
            tvTopValueSummary.text = "이번 달 기록된 가치를 모아보고 있어요."
        }

        // 3. 7.2.3 당월 평균 만족도 계산
        val avgRating = receipts.map { it.rating }.average()
        val formattedAvg = String.format(Locale.KOREA, "%.1f", if (avgRating.isNaN()) 0.0 else avgRating)
        tvAverageRating.text = "이번 달 평균 만족도 : ${formattedAvg}점"

        // 4. 리사이클러뷰 그래프 데이터 설정 및 클릭 이벤트
        rvValueGraph.adapter = ValueGraphAdapter(valueCountList) { selectedValue ->
            // 7.2.4 선택한 가치 모아보기 화면으로 이동
            val intent = Intent(requireContext(), ValueDetailActivity::class.java).apply {
                putExtra("SELECTED_VALUE", selectedValue)
            }
            startActivity(intent)
        }
    }

    companion object {
        fun newInstance() = ArchiveFragment()
    }
}