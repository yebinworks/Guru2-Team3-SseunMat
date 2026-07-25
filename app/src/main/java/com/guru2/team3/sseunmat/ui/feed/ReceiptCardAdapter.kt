package com.guru2.team3.sseunmat.ui.feed

// [예빈] 홈 피드 & 가치별 목록 공통 사용

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
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

    fun updateData(newList: List<Receipt>) {
        this.receiptList = newList.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptCardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_receipt_card, parent, false)
        return ReceiptCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReceiptCardViewHolder, position: Int) {
        holder.bind(receiptList[position], position)
    }

    override fun getItemCount(): Int = receiptList.size

    inner class ReceiptCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardContainer: CardView = itemView.findViewById(R.id.card_container)
        private val layoutFront: View = itemView.findViewById(R.id.layout_card_front)
        private val layoutBack: View = itemView.findViewById(R.id.layout_card_back)

        // 앞면 뷰
        private val tvFrontDate: TextView = itemView.findViewById(R.id.tv_front_date)
        private val tvFrontStore: TextView = itemView.findViewById(R.id.tv_front_store)
        private val tvFrontAmount: TextView = itemView.findViewById(R.id.tv_front_amount)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete_receipt)

        // 뒷면 뷰
        private val tvBackValueTag: TextView = itemView.findViewById(R.id.tv_back_value_tag)
        private val tvBackMemo: TextView = itemView.findViewById(R.id.tv_back_memo)

        private var isBackVisible = false

        fun bind(receipt: Receipt, position: Int) {
            val sdf = SimpleDateFormat("yyyy. MM. dd", Locale.KOREA)
            tvFrontDate.text = receipt.paymentDate?.let { sdf.format(it) } ?: ""
            tvFrontStore.text = receipt.store

            val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
            tvFrontAmount.text = "₩ ${formatter.format(receipt.amount)}"

            tvBackValueTag.text = receipt.value
            tvBackMemo.text = if (receipt.memo.isNotBlank()) receipt.memo else "남겨진 메모가 없어요."

            // 8.3 단일 기록 삭제 (별도 확인 팝업 없이 즉시 삭제)
            btnDelete.setOnClickListener {
                // 이벤트 파급 방지 (카드 뒤집히는 것 방지)
                it.isClickable = false

                firebaseService.deleteReceipt(receipt.receiptId) { isSuccess ->
                    if (isSuccess) {
                        Toast.makeText(itemView.context, "영수증이 삭제되었어요", Toast.LENGTH_SHORT).show()
                        if (adapterPosition in 0 until receiptList.size) {
                            receiptList.removeAt(adapterPosition)
                            notifyItemRemoved(adapterPosition)
                            notifyItemRangeChanged(adapterPosition, receiptList.size)
                        }
                        onDeleteCompleted?.invoke()
                    } else {
                        Toast.makeText(itemView.context, "ValueGraphAdapter\uD83D\uDCBB 3. ValueGraphAdapter.kt 수정 코드삭제 실패했습니다.", Toast.LENGTH_SHORT).show()
                        it.isClickable = true
                    }
                }
            }

            // 초기 상태 & 회전 클릭
            layoutFront.visibility = View.VISIBLE
            layoutBack.visibility = View.GONE
            isBackVisible = false

            cardContainer.setOnClickListener {
                flipCard()
            }
        }

        private fun flipCard() {
            if (!isBackVisible) {
                cardContainer.animate().rotationY(90f).setDuration(150).withEndAction {
                    layoutFront.visibility = View.GONE
                    layoutBack.visibility = View.VISIBLE
                    cardContainer.rotationY = -90f
                    cardContainer.animate().rotationY(0f).setDuration(150).start()
                }.start()
                isBackVisible = true
            } else {
                cardContainer.animate().rotationY(-90f).setDuration(150).withEndAction {
                    layoutBack.visibility = View.GONE
                    layoutFront.visibility = View.VISIBLE
                    cardContainer.rotationY = 90f
                    cardContainer.animate().rotationY(0f).setDuration(150).start()
                }.start()
                isBackVisible = false
            }
        }
    }
}