package com.guru2.team3.sseunmat.ui.archive

// [예빈] 가치 그래프 어댑터

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.guru2.team3.sseunmat.R

data class ValueCount(val valueName: String, val count: Int)

class ValueGraphAdapter(
    private var itemList: List<ValueCount>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ValueGraphAdapter.ValueGraphViewHolder>() {

    private var maxCount: Int = 1

    companion object {
        private const val TAG = "ValueGraphAdapter"
    }

    init {
        updateMaxCount()
    }

    fun updateData(newList: List<ValueCount>?) {
        try {
            this.itemList = newList ?: emptyList()
            updateMaxCount()
            notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(TAG, "updateData 처리 중 오류 발생", e)
        }
    }

    private fun updateMaxCount() {
        try {
            val highest = itemList.maxOfOrNull { it.count } ?: 0
            maxCount = if (highest > 0) highest else 1
        } catch (e: Exception) {
            Log.e(TAG, "updateMaxCount 실행 중 오류 발생", e)
            maxCount = 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValueGraphViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_value_graph, parent, false)
        return ValueGraphViewHolder(view)
    }

    override fun onBindViewHolder(holder: ValueGraphViewHolder, position: Int) {
        try {
            if (position in itemList.indices) {
                val item = itemList[position]
                holder.bind(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onBindViewHolder 처리 중 오류 발생 (position: $position)", e)
        }
    }

    override fun getItemCount(): Int = itemList.size

    inner class ValueGraphViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivValueSymbol: ImageView = itemView.findViewById(R.id.iv_value_symbol)
        private val tvValueName: TextView = itemView.findViewById(R.id.tv_value_name)
        private val tvValueCount: TextView = itemView.findViewById(R.id.tv_value_count)
        private val progressValue: ProgressBar = itemView.findViewById(R.id.progress_value)

        fun bind(item: ValueCount) {
            try {
                tvValueName.text = item.valueName
                tvValueCount.text = "기록 ${item.count}개"

                val safeMaxCount = if (maxCount > 0) maxCount else 1
                val progressPercent = ((item.count.toFloat() / safeMaxCount.toFloat()) * 100).toInt().coerceIn(0, 100)
                progressValue.progress = progressPercent

                val symbolRes = when (item.valueName) {
                    "위로" -> R.drawable.ic_symbol_comfort
                    "휴식" -> R.drawable.ic_symbol_rest
                    "관계" -> R.drawable.ic_symbol_relation
                    "추억" -> R.drawable.ic_symbol_memory
                    "배움" -> R.drawable.ic_symbol_learn
                    "영감" -> R.drawable.ic_symbol_inspire
                    "마음 표현" -> R.drawable.ic_symbol_heart
                    else -> R.drawable.ic_symbol_comfort
                }
                ivValueSymbol.setImageResource(symbolRes)

                itemView.setOnClickListener {
                    try {
                        onItemClick(item.valueName)
                    } catch (e: Exception) {
                        Log.e(TAG, "항목 클릭 이벤트 처리 중 오류 발생", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ViewHolder 바인딩 중 오류 발생", e)
            }
        }
    }
}