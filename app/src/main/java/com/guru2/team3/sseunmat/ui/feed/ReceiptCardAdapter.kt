package com.guru2.team3.sseunmat.ui.feed

// [예빈] 홈 피드 & 가치별 목록 공통 사용

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.model.Receipt
import com.guru2.team3.sseunmat.data.remote.FirebaseService
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptCardAdapter(
    private var receiptList: MutableList<Receipt>,
    private val onDeleteCompleted: (() -> Unit)? = null // 삭제 후 목록 최신화/빈상태 체크용
) : RecyclerView.Adapter<ReceiptCardAdapter.ReceiptCardViewHolder>() {

    private val firebaseService = FirebaseService()

    companion object {
        private const val TAG = "ReceiptCardAdapter"
    }

    fun updateData(newList: List<Receipt>?) {
        try {
            this.receiptList = newList?.toMutableList() ?: mutableListOf()
            notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(TAG, "updateData 처리 중 오류 발생", e)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptCardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_receipt_card, parent, false)
        return ReceiptCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReceiptCardViewHolder, position: Int) {
        try {
            if (position in receiptList.indices) {
                holder.bind(receiptList[position], position)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onBindViewHolder 처리 중 오류 발생 (position: $position)", e)
        }
    }

    override fun getItemCount(): Int = receiptList.size

    data class ValueTheme(
        val bgResId: Int,
        val iconResId: Int,
        val tagResId: Int
    )

    // 가치에 따라 배경, 심볼 아이콘, 뒷면 태그 반환
    private fun getValueTheme(valueName: String?): ValueTheme {
        return try {
            when (valueName?.trim()) {
                "위로" -> ValueTheme(R.drawable.bg_receipt_comfort, R.drawable.ic_symbol_comfort, R.drawable.ic_tag_comfort)
                "휴식" -> ValueTheme(R.drawable.bg_receipt_rest, R.drawable.ic_symbol_rest, R.drawable.ic_tag_rest)
                "관계" -> ValueTheme(R.drawable.bg_receipt_relation, R.drawable.ic_symbol_relation, R.drawable.ic_tag_relation)
                "추억" -> ValueTheme(R.drawable.bg_receipt_memory, R.drawable.ic_symbol_memory, R.drawable.ic_tag_memory)
                "배움" -> ValueTheme(R.drawable.bg_receipt_learn, R.drawable.ic_symbol_learn, R.drawable.ic_tag_learn)
                "영감" -> ValueTheme(R.drawable.bg_receipt_inspire, R.drawable.ic_symbol_inspire, R.drawable.ic_tag_inspire)
                "마음 표현", "마음표현" -> ValueTheme(R.drawable.bg_receipt_heart, R.drawable.ic_symbol_heart, R.drawable.ic_tag_heart)
                else -> ValueTheme(R.drawable.bg_receipt_comfort, R.drawable.ic_symbol_comfort, R.drawable.ic_tag_comfort)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getValueTheme 매핑 중 오류 발생", e)
            ValueTheme(R.drawable.bg_receipt_comfort, R.drawable.ic_symbol_comfort, R.drawable.ic_tag_comfort)
        }
    }

    inner class ReceiptCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardContainer: View = itemView.findViewById(R.id.card_container)
        private val layoutFront: View = itemView.findViewById(R.id.layout_card_front)
        private val layoutBack: View = itemView.findViewById(R.id.layout_card_back)

        // 앞면 뷰
        private val ivFrontCategoryIcon: ImageView = itemView.findViewById(R.id.iv_front_category_icon)
        private val tvFrontDate: TextView = itemView.findViewById(R.id.tv_front_date)
        private val tvFrontStore: TextView = itemView.findViewById(R.id.tv_front_store)
        private val tvFrontAmount: TextView = itemView.findViewById(R.id.tv_front_amount)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete_receipt)
        private val ivDonation: ImageView = itemView.findViewById(R.id.iv_front_value_dot)

        // 뒷면 뷰
        private val ivBackValueTag: ImageView = itemView.findViewById(R.id.iv_back_value_tag)
        private val tvBackMemo: TextView = itemView.findViewById(R.id.tv_back_memo)
        private val ratingDots: List<ImageView> = listOfNotNull(
            itemView.findViewById(R.id.dot_1),
            itemView.findViewById(R.id.dot_2),
            itemView.findViewById(R.id.dot_3),
            itemView.findViewById(R.id.dot_4),
            itemView.findViewById(R.id.dot_5)
        )

        private var isBackVisible = false
        private var isAnimating = false

        fun bind(receipt: Receipt, position: Int) {
            try {
                val theme = getValueTheme(receipt.value)

                layoutFront.setBackgroundResource(theme.bgResId)
                layoutBack.setBackgroundResource(theme.bgResId)

                ivFrontCategoryIcon.setImageResource(theme.iconResId)
                ivBackValueTag.setImageResource(theme.tagResId)

                try {
                    val sdf = SimpleDateFormat("yyyy. MM. dd", Locale.KOREA)
                    tvFrontDate.text = receipt.paymentDate?.let { sdf.format(it) } ?: ""
                } catch (e: Exception) {
                    tvFrontDate.text = ""
                }

                tvFrontStore.text = receipt.store ?: ""

                try {
                    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
                    tvFrontAmount.text = "₩ ${formatter.format(receipt.amount)}"
                } catch (e: Exception) {
                    tvFrontAmount.text = "₩ ${receipt.amount}"
                }

                tvBackMemo.text = if (!receipt.memo.isNullOrBlank()) receipt.memo else "남겨진 메모가 없어요."

                if (receipt.isDonation == true) {
                    ivDonation.visibility = View.VISIBLE
                } else {
                    ivDonation.visibility = View.GONE
                }

                val score = receipt.rating ?: 0

                ratingDots.forEachIndexed { index, imageView ->
                    if (index < score) {
                        imageView.setImageResource(R.drawable.ic_rating_dot_on)
                    } else {
                        imageView.setImageResource(R.drawable.ic_rating_dot_off)
                    }
                }

                // 8.3 단일 기록 삭제 (별도 확인 팝업 없이 즉시 삭제)
                btnDelete.setOnClickListener { v ->
                    try {
                        v.isClickable = false

                        val targetReceiptId = receipt.receiptId
                        if (targetReceiptId.isNullOrBlank()) {
                            Toast.makeText(itemView.context, "삭제에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                            v.isClickable = true
                            return@setOnClickListener
                        }

                        firebaseService.deleteReceipt(targetReceiptId) { isSuccess ->
                            itemView.post {
                                try {
                                    if (isSuccess) {
                                        Toast.makeText(itemView.context, "영수증이 삭제되었어요", Toast.LENGTH_SHORT).show()
                                        val pos = bindingAdapterPosition
                                        if (pos != RecyclerView.NO_POSITION && pos in receiptList.indices) {
                                            receiptList.removeAt(pos)
                                            notifyItemRemoved(pos)
                                            notifyItemRangeChanged(pos, receiptList.size)
                                        }
                                        onDeleteCompleted?.invoke()
                                    } else {
                                        Toast.makeText(itemView.context, "삭제에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                                        v.isClickable = true
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "삭제 후 UI 반영 중 오류 발생", e)
                                    v.isClickable = true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "삭제 버튼 클릭 처리 중 오류 발생", e)
                        v.isClickable = true
                    }
                }

                // 초기 상태 & 회전 클릭
                layoutFront.visibility = View.VISIBLE
                layoutBack.visibility = View.GONE
                cardContainer.rotationY = 0f
                isBackVisible = false
                isAnimating = false

                cardContainer.setOnClickListener {
                    flipCard()
                }
            } catch (e: Exception) {
                Log.e(TAG, "ViewHolder 바인딩 중 오류 발생", e)
            }
        }

        private fun flipCard() {
            if (isAnimating) return
            isAnimating = true

            try {
                if (!isBackVisible) {
                    cardContainer.animate().rotationY(90f).setDuration(150).withEndAction {
                        try {
                            layoutFront.visibility = View.GONE
                            layoutBack.visibility = View.VISIBLE
                            cardContainer.rotationY = -90f
                            cardContainer.animate().rotationY(0f).setDuration(150).withEndAction {
                                isAnimating = false
                            }.start()
                            isBackVisible = true
                        } catch (e: Exception) {
                            Log.e(TAG, "카드 회전 애니메이션 실행 중 오류 발생", e)
                            isAnimating = false
                        }
                    }.start()
                } else {
                    cardContainer.animate().rotationY(-90f).setDuration(150).withEndAction {
                        try {
                            layoutBack.visibility = View.GONE
                            layoutFront.visibility = View.VISIBLE
                            cardContainer.rotationY = 90f
                            cardContainer.animate().rotationY(0f).setDuration(150).withEndAction {
                                isAnimating = false
                            }.start()
                            isBackVisible = false
                        } catch (e: Exception) {
                            Log.e(TAG, "카드 회전 애니메이션 실행 중 오류 발생", e)
                            isAnimating = false
                        }
                    }.start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "flipCard 애니메이션 시도 중 오류 발생", e)
                isAnimating = false
            }
        }
    }
}