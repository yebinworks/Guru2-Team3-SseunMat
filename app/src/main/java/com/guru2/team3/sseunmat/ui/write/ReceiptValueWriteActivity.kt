package com.guru2.team3.sseunmat.ui.write

// [예빈] 6.4~6.7 가치 선택 및 등록 처리 액티비티

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.model.Receipt
import com.guru2.team3.sseunmat.data.remote.FirebaseService
import com.guru2.team3.sseunmat.ui.main.MainActivity
import com.guru2.team3.sseunmat.util.showToast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 가치 버튼 아이템 래퍼 데이터 클래스
private data class ValueButtonItem(
    val button: ImageButton,
    val valueName: String,
    val offRes: Int,
    val onRes: Int
)

class ReceiptValueWriteActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etMemo: EditText
    private lateinit var cbDonation: CheckBox
    private lateinit var btnSave: Button

    private lateinit var valueItems: List<ValueButtonItem>
    private lateinit var ratingDots: List<ImageView>

    private val firebaseService = FirebaseService()

    // 이전 단계 전달 데이터
    private var store: String = ""
    private var paymentDateStr: String = ""
    private var amount: Long = 0L

    // 가치 및 만족도 입력 데이터
    private var selectedValue: String = ""
    private var selectedRating: Int = 0 // 1~5 (0은 미선택)

    companion object {
        private const val TAG = "ReceiptValueWriteActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_receipt_value_write)

            val mainView = findViewById<android.view.View>(R.id.main) ?: findViewById(android.R.id.content)
            mainView?.let { v ->
                ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }

            // 이전 화면 데이터 복원
            store = intent?.getStringExtra("STORE") ?: ""
            paymentDateStr = intent?.getStringExtra("DATE") ?: ""
            amount = when (val extra = intent?.extras?.get("AMOUNT")) {
                is Long -> extra
                is Int -> extra.toLong()
                is String -> extra.toLongOrNull() ?: 0L
                else -> 0L
            }

            initViews()
            setupListeners()
            setupBackPressHandler()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate 처리 중 오류 발생", e)
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        etMemo = findViewById(R.id.et_memo)
        cbDonation = findViewById(R.id.cb_donation)
        btnSave = findViewById(R.id.btn_save)

        // 가치 버튼과 이미지 자원을 1:1 매핑
        val valueButtonsList = mutableListOf<ValueButtonItem>()
        findViewById<ImageButton>(R.id.btn_val_consolation)?.let {
            valueButtonsList.add(ValueButtonItem(it, "위로", R.drawable.btn_val_consolation_off, R.drawable.btn_val_consolation_on))
        }
        findViewById<ImageButton>(R.id.btn_val_rest)?.let {
            valueButtonsList.add(ValueButtonItem(it, "휴식", R.drawable.btn_val_rest_off, R.drawable.btn_val_rest_on))
        }
        findViewById<ImageButton>(R.id.btn_val_relation)?.let {
            valueButtonsList.add(ValueButtonItem(it, "관계", R.drawable.btn_val_relation_off, R.drawable.btn_val_relation_on))
        }
        findViewById<ImageButton>(R.id.btn_val_memory)?.let {
            valueButtonsList.add(ValueButtonItem(it, "추억", R.drawable.btn_val_memory_off, R.drawable.btn_val_memory_on))
        }
        findViewById<ImageButton>(R.id.btn_val_learning)?.let {
            valueButtonsList.add(ValueButtonItem(it, "배움", R.drawable.btn_val_learning_off, R.drawable.btn_val_learning_on))
        }
        findViewById<ImageButton>(R.id.btn_val_inspiration)?.let {
            valueButtonsList.add(ValueButtonItem(it, "영감", R.drawable.btn_val_inspiration_off, R.drawable.btn_val_inspiration_on))
        }
        findViewById<ImageButton>(R.id.btn_val_expression)?.let {
            valueButtonsList.add(ValueButtonItem(it, "마음 표현", R.drawable.btn_val_expression_off, R.drawable.btn_val_expression_on))
        }
        valueItems = valueButtonsList

        ratingDots = listOfNotNull(
            findViewById(R.id.dot_rating_1),
            findViewById(R.id.dot_rating_2),
            findViewById(R.id.dot_rating_3),
            findViewById(R.id.dot_rating_4),
            findViewById(R.id.dot_rating_5)
        )
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { showExitDialog() }

        // 6.4.2 가치 단일 선택 (이미지 스위칭 방식)
        valueItems.forEach { targetItem ->
            targetItem.button.setOnClickListener {
                try {
                    // 1. 모든 버튼을 off 이미지로 초기화
                    valueItems.forEach { item ->
                        item.button.setImageResource(item.offRes)
                    }

                    // 2. 현재 선택된 버튼만 on 이미지로 변경
                    targetItem.button.setImageResource(targetItem.onRes)

                    // 3. 가치 데이터 저장 및 유효성 검사
                    selectedValue = targetItem.valueName
                    validateInputs()
                } catch (e: Exception) {
                    Log.e(TAG, "가치 버튼 클릭 처리 중 오류 발생", e)
                }
            }
        }

        // 6.5.2 만족도 5중 척도 선택
        ratingDots.forEachIndexed { index, dot ->
            dot.setOnClickListener {
                try {
                    selectedRating = index + 1
                    updateRatingDotsUI(selectedRating)
                    validateInputs()
                } catch (e: Exception) {
                    Log.e(TAG, "만족도 선택 처리 중 오류 발생", e)
                }
            }
        }

        // 저장 버튼 클릭
        btnSave.setOnClickListener {
            try {
                val memoText = etMemo.text?.toString()?.trim() ?: ""

                // 6.6.2 글자 수 초과 검증 (100자 초과 제한)
                if (memoText.length > 100) {
                    showToast("100자 이내로 작성해 주세요")
                    return@setOnClickListener
                }

                saveReceiptToFirestore(memoText)
            } catch (e: Exception) {
                Log.e(TAG, "저장 버튼 클릭 처리 중 오류 발생", e)
            }
        }
    }

    private fun updateRatingDotsUI(rating: Int) {
        val safeRating = rating.coerceIn(0, 5)
        ratingDots.forEachIndexed { index, imageView ->
            try {
                if (index < safeRating) {
                    imageView.setImageResource(R.drawable.ic_rating_dot_on)
                } else {
                    imageView.setImageResource(R.drawable.ic_rating_dot_off)
                }
            } catch (e: Exception) {
                Log.e(TAG, "만족도 도트 이미지 스위칭 중 오류 발생", e)
            }
        }
    }

    private fun validateInputs() {
        try {
            // 가치와 만족도가 모두 선택되어야 등록 버튼 활성화
            val isValid = selectedValue.isNotEmpty() && selectedRating > 0
            btnSave.isEnabled = isValid
            btnSave.backgroundTintList = ContextCompat.getColorStateList(
                this,
                if (isValid) R.color.main_purple else R.color.btn_disabled
            )
        } catch (e: Exception) {
            Log.e(TAG, "validateInputs 실행 중 오류 발생", e)
        }
    }

    private fun saveReceiptToFirestore(memo: String) {
        val parsedDate: Date = try {
            val cleanDate = paymentDateStr.replace(".", "").replace("-", "").replace(" ", "").trim()
            if (cleanDate.length == 8) {
                SimpleDateFormat("yyyyMMdd", Locale.KOREA).parse(cleanDate) ?: Date()
            } else {
                SimpleDateFormat("yyyy. MM. dd", Locale.KOREA).parse(paymentDateStr) ?: Date()
            }
        } catch (e: Exception) {
            Date()
        }

        val newReceipt = Receipt(
            store = store,
            paymentDate = parsedDate,
            amount = amount,
            value = selectedValue,
            rating = selectedRating,
            memo = memo,
            isDonation = cbDonation.isChecked
        )

        btnSave.isEnabled = false // 중복 클릭 방지

        firebaseService.addReceipt(newReceipt) { isSuccess ->
            if (isFinishing || isDestroyed) return@addReceipt

            runOnUiThread {
                try {
                    if (isSuccess) {
                        showToast("영수증이 저장되었습니다.")

                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }

                        startActivity(intent)
                        finish()
                    } else {
                        showToast("영수증 저장에 실패했습니다. 네트워크 연결을 확인해 주세요.")
                        btnSave.isEnabled = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "저장 결과 UI 반영 중 오류 발생", e)
                    btnSave.isEnabled = true
                }
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })
    }

    private fun showExitDialog() {
        try {
            if (isFinishing || isDestroyed) return
            AlertDialog.Builder(this)
                .setTitle("작성 중인 내용을 삭제하고 나갈까요?")
                .setMessage("지금까지 입력한 내용은 저장되지 않아요.")
                .setPositiveButton("계속 작성하기", null)
                .setNegativeButton("나가기") { _, _ -> finish() }
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "이탈 다이얼로그 표시 중 오류 발생", e)
            finish()
        }
    }
}