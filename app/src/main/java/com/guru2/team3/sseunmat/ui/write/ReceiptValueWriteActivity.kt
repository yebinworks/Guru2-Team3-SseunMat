package com.guru2.team3.sseunmat.ui.write

//[예빈]

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.model.Receipt
import com.guru2.team3.sseunmat.data.remote.FirebaseService
import com.guru2.team3.sseunmat.util.showToast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReceiptValueWriteActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etMemo: EditText
    private lateinit var cbDonation: CheckBox
    private lateinit var btnSave: Button

    private lateinit var valueButtons: List<Button>
    private lateinit var ratingDots: List<ImageView>

    private val firebaseService = FirebaseService()

    // 이전 단계 전달 데이터
    private var store: String = ""
    private var paymentDateStr: String = ""
    private var amount: Long = 0L

    // 가치 입력 데이터
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
        amount = intent.getLongExtra("AMOUNT", 0L)

        initViews()
        setupListeners()
        setupBackPressHandler()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        etMemo = findViewById(R.id.et_memo)
        cbDonation = findViewById(R.id.cb_donation)
        btnSave = findViewById(R.id.btn_save)

        valueButtons = listOf(
            findViewById(R.id.btn_val_consolation),
            findViewById(R.id.btn_val_rest),
            findViewById(R.id.btn_val_relation),
            findViewById(R.id.btn_val_memory),
            findViewById(R.id.btn_val_learning),
            findViewById(R.id.btn_val_inspiration),
            findViewById(R.id.btn_val_expression)
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

        // 6.4.2 가치 단일 선택
        valueButtons.forEach { button ->
            button.setOnClickListener { clickedBtn ->
                valueButtons.forEach { it.isSelected = false }
                clickedBtn.isSelected = true
                selectedValue = (clickedBtn as Button).text.toString().replace("\n", " ")
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
        // 가치와 만족도가 모두 선택되어야 저장 버튼 활성화
        val isValid = selectedValue.isNotEmpty() && selectedRating > 0
        btnSave.isEnabled = isValid
        btnSave.backgroundTintList = getColorStateList(
            if (isValid) R.color.btn_active else R.color.btn_disabled
        )
    }

    private fun saveReceiptToFirestore(memo: String) {
        val parsedDate = try {
            SimpleDateFormat("yyyy. MM. dd", Locale.KOREA).parse(paymentDateStr)
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
                finish()
            } else {
                showToast("영수증 저장에 실패했습니다. 다시 시도해 주세요.")
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