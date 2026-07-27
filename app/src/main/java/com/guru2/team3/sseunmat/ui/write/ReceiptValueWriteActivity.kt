package com.guru2.team3.sseunmat.ui.write

// [예빈] 6.4~6.7 가치 선택 및 등록 처리 액티비티

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_receipt_value_write)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 이전 화면 데이터 복원
        store = intent.getStringExtra("STORE") ?: ""
        paymentDateStr = intent.getStringExtra("DATE") ?: ""
        amount = when (val extra = intent.extras?.get("AMOUNT")) {
            is Long -> extra
            is Int -> extra.toLong()
            is String -> extra.toLongOrNull() ?: 0L
            else -> 0L
        }

        initViews()
        setupListeners()
        setupBackPressHandler()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        etMemo = findViewById(R.id.et_memo)
        cbDonation = findViewById(R.id.cb_donation)
        btnSave = findViewById(R.id.btn_save)

        // 가치 버튼과 이미지 자원을 1:1 매핑
        valueItems = listOf(
            ValueButtonItem(findViewById(R.id.btn_val_consolation), "위로", R.drawable.btn_val_consolation_on, R.drawable.btn_val_consolation_off),
            ValueButtonItem(findViewById(R.id.btn_val_rest), "휴식", R.drawable.btn_val_rest_off, R.drawable.btn_val_rest_on),
            ValueButtonItem(findViewById(R.id.btn_val_relation), "관계", R.drawable.btn_val_relation_off, R.drawable.btn_val_relation_on),
            ValueButtonItem(findViewById(R.id.btn_val_memory), "추억", R.drawable.btn_val_memory_off, R.drawable.btn_val_memory_on),
            ValueButtonItem(findViewById(R.id.btn_val_learning), "배움", R.drawable.btn_val_learning_off, R.drawable.btn_val_learning_on),
            ValueButtonItem(findViewById(R.id.btn_val_inspiration), "영감", R.drawable.btn_val_inspiration_off, R.drawable.btn_val_inspiration_on),
            ValueButtonItem(findViewById(R.id.btn_val_expression), "마음 표현", R.drawable.btn_val_expression_off, R.drawable.btn_val_expression_on)
        )

        ratingDots = listOf(
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
                // 1. 모든 버튼을 off 이미지로 초기화
                valueItems.forEach { item ->
                    item.button.setImageResource(item.offRes)
                }

                // 2. 현재 선택된 버튼만 on 이미지로 변경
                targetItem.button.setImageResource(targetItem.onRes)

                // 3. 가치 데이터 저장 및 유효성 검사
                selectedValue = targetItem.valueName
                validateInputs()
            }
        }

        // 6.5.2 만족도 5중 척도 선택
        ratingDots.forEachIndexed { index, dot ->
            dot.setOnClickListener {
                selectedRating = index + 1
                updateRatingDotsUI(selectedRating)
                validateInputs()
            }
        }

        // 저장 버튼 클릭
        btnSave.setOnClickListener {
            val memoText = etMemo.text.toString().trim()

            // 6.6.2 글자 수 초과 검증 (100자 초과 제한)
            if (memoText.length > 100) {
                showToast("100자 이내로 작성해 주세요")
                return@setOnClickListener
            }

            saveReceiptToFirestore(memoText)
        }
    }

    private fun updateRatingDotsUI(rating: Int) {
        ratingDots.forEachIndexed { index, imageView ->
            if (index < rating) {
                imageView.setImageResource(R.drawable.ic_rating_dot_on)
            } else {
                imageView.setImageResource(R.drawable.ic_rating_dot_off)
            }
        }
    }

    private fun validateInputs() {
        // 가치와 만족도가 모두 선택되어야 등록 버튼 활성화
        val isValid = selectedValue.isNotEmpty() && selectedRating > 0
        btnSave.isEnabled = isValid
        btnSave.backgroundTintList = ContextCompat.getColorStateList(
            this,
            if (isValid) R.color.main_purple else R.color.btn_disabled
        )
    }

    private fun saveReceiptToFirestore(memo: String) {
        val parsedDate: Date = try {
            val cleanDate = paymentDateStr.replace(".", "").replace(" ", "").trim()
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
            if (isSuccess) {
                showToast("영수증이 저장되었습니다.")
                setResult(RESULT_OK)
                finish()
            } else {
                showToast("영수증 저장에 실패했습니다. 네트워크 연결을 확인해 주세요.")
                btnSave.isEnabled = true
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
        AlertDialog.Builder(this)
            .setTitle("작성 중인 내용을 삭제하고 나갈까요?")
            .setMessage("지금까지 입력한 내용은 저장되지 않아요.")
            .setPositiveButton("계속 작성하기", null)
            .setNegativeButton("나가기") { _, _ -> finish() }
            .show()
    }
}