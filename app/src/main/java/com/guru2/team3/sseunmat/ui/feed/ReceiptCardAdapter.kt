package com.guru2.team3.sseunmat.ui.feed

// [예빈] 홈 피드 & 가치별 목록 공통 사용

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.model.Receipt
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptCardAdapter(
    private var receiptList: List<Receipt>
) : RecyclerView.Adapter<ReceiptCardAdapter.ReceiptCardViewHolder>() {

    fun updateData(newList: List<Receipt>) {
        this.receiptList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptCardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_receipt_card, parent, false)
        return ReceiptCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReceiptCardViewHolder, position: Int) {
        holder.bind(receiptList[position])
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
        private val ivFrontDonationBadge: ImageView = itemView.findViewById(R.id.iv_front_donation_badge)

        // 뒷면 뷰
        private val tvBackValueTag: TextView = itemView.findViewById(R.id.tv_back_value_tag)
        private val tvBackMemo: TextView = itemView.findViewById(R.id.tv_back_memo)
        private val ratingDots = listOf<ImageView>(
            itemView.findViewById(R.id.dot_1),
            itemView.findViewById(R.id.dot_2),
            itemView.findViewById(R.id.dot_3),
            itemView.findViewById(R.id.dot_4),
            itemView.findViewById(R.id.dot_5)
        )

        private var isBackVisible = false

        fun bind(receipt: Receipt) {
            // 앞면 데이터 채우기
            val sdf = SimpleDateFormat("yyyy. MM. dd", Locale.KOREA)
            tvFrontDate.text = receipt.paymentDate?.let { sdf.format(it) } ?: ""
            tvFrontStore.text = receipt.store

            val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
            tvFrontAmount.text = "₩ ${formatter.format(receipt.amount)}"

            ivFrontDonationBadge.visibility = if (receipt.isDonation) View.VISIBLE else View.GONE

            // 뒷면 데이터 채우기
            tvBackValueTag.text = receipt.value
            tvBackMemo.text = if (receipt.memo.isNotBlank()) receipt.memo else "남겨진 메모가 없어요."

            // 만족도 점 업데이트
            ratingDots.forEachIndexed { index, imageView ->
                if (index < receipt.rating) {
                    imageView.setImageResource(R.drawable.ic_rating_dot_on)
                } else {
                    imageView.setImageResource(R.drawable.ic_rating_dot_off)
                }
            }

            // 초기 상태: 앞면
            layoutFront.visibility = View.VISIBLE
            layoutBack.visibility = View.GONE
            isBackVisible = false

            // 3. 3.4 카드 뒤집기 (180도 플립 애니메이션)
            cardContainer.setOnClickListener {
                flipCard()
            }
        }

        private fun flipCard() {
            val scale = itemView.context.resources.displayMetrics.density
            cardContainer.cameraDistance = 8000 * scale

            if (!isBackVisible) {
                // 앞 -> 뒤 회전
                cardContainer.animate().rotationY(90f).setDuration(150).withEndAction {
                    layoutFront.visibility = View.GONE
                    layoutBack.visibility = View.VISIBLE
                    cardContainer.rotationY = -90f
                    cardContainer.animate().rotationY(0f).setDuration(150).start()
                }.start()
                isBackVisible = true
            } else {
                // 뒤 -> 앞 회전
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