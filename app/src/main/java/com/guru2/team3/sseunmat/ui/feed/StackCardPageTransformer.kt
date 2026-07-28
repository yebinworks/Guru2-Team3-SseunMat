import android.util.Log
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

// [예빈]

class StackCardPageTransformer : ViewPager2.PageTransformer {

    companion object {
        private const val TAG = "StackCardPageTransformer"
    }

    override fun transformPage(page: View, position: Float) {
        try {
            // position이 Float.NaN이나 Infinity인 경우 기본 상태 유지
            if (position.isNaN() || position.isInfinite()) {
                page.alpha = 1f
                page.scaleX = 1f
                page.scaleY = 1f
                page.translationY = 0f
                return
            }

            page.apply {
                val pageHeight = height.toFloat()
                if (pageHeight <= 0f) return

                when {
                    // 완전히 위/아래 범위를 벗어난 카드
                    position < -3f || position > 3f -> {
                        alpha = 0f
                    }

                    // 지나간 카드 (위로 넘긴 카드: -3 <= position < 0)
                    position < 0f -> {
                        alpha = (1f + (position * 0.2f)).coerceIn(0f, 1f)
                        translationZ = position

                        val scale = (1f + (position * 0.10f)).coerceIn(0.1f, 1f)
                        scaleX = scale
                        scaleY = scale
                        translationY = -position * pageHeight + (position * 120f)
                    }

                    // 현재 카드 & 아래 대기 중인 카드들 (0 <= position <= 3)
                    position <= 3f -> {
                        alpha = (1f - (position * 0.15f)).coerceIn(0f, 1f)
                        translationZ = -position

                        val scale = (1f - (position * 0.08f)).coerceIn(0.1f, 1f)
                        scaleX = scale
                        scaleY = scale
                        translationY = -position * pageHeight + (position * 80f)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "transformPage 실행 중 오류 발생", e)
        }
    }
}