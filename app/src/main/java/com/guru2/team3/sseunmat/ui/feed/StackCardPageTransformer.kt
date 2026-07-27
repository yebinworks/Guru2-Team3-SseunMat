import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class StackCardPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        page.apply {
            val pageHeight = height.toFloat()

            when {
                // 완전히 위/아래 범위를 벗어난 카드
                position < -3 || position > 3 -> {
                    alpha = 0f
                }

                // 지나간 카드 (위로 넘긴 카드: -3 <= position < 0)
                position < 0 -> {
                    alpha = 1f + (position * 0.2f)
                    translationZ = position

                    val scale = 1f + (position * 0.10f)
                    scaleX = scale
                    scaleY = scale
                    translationY = -position * pageHeight + (position * 120f)
                }

                // 현재 카드 & 아래 대기 중인 카드들 (0 <= position <= 3)
                position <= 3 -> {
                    alpha = 1f - (position * 0.15f)
                    translationZ = -position

                    val scale = 1f - (position * 0.08f)
                    scaleX = scale
                    scaleY = scale
                    translationY = -position * pageHeight + (position * 80f)
                }
            }
        }
    }
}