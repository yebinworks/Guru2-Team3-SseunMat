package com.guru2.team3.sseunmat.ui.archive

// [예빈] 가치 그래프 어댑터

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.guru2.team3.sseunmat.R

data class ValueCount(val valueName: String, val count: Int)

class ValueGraphAdapter(
    private var itemList: List<ValueCount>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ValueGraphAdapter.ValueGraphViewHolder>() {

    // 전체 가치 중 최다 기록 횟수 (게이지 100% 기준)
    private var maxCount: Int = 1

    init {
        updateMaxCount()
    }

    fun updateData(newList: List<ValueCount>) {
        this.itemList = newList
        updateMaxCount()
        notifyDataSetChanged()
    }

    private fun updateMaxCount() {
        val highest = itemList.maxOfOrNull { it.count } ?: 0
        maxCount = if (highest > 0) highest else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValueGraphViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_value_graph, parent, false)
        return ValueGraphViewHolder(view)
    }

    override fun onBindViewHolder(holder: ValueGraphViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = itemList.size

    inner class ValueGraphViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvValueName: TextView = itemView.findViewById(R.id.tv_value_name)
        private val tvValueCount: TextView = itemView.findViewById(R.id.tv_value_count)
        private val progressValue: ProgressBar = itemView.findViewById(R.id.progress_value)

        fun bind(item: ValueCount) {
            tvValueName.text = item.valueName
            tvValueCount.text = "기록 ${item.count}개"

            // 가장 높은 기록 대비 비율(%) 계산해서 게이지 채우기
            val progressPercent = ((item.count.toFloat() / maxCount.toFloat()) * 100).toInt()
            progressValue.progress = progressPercent

            // 7.2.4 가치 항목 클릭 시 필터링 모아보기 화면으로 이동
            itemView.setOnClickListener {
                onItemClick(item.valueName)
            }
        }
    }
}