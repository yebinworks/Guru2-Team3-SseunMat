package com.guru2.team3.sseunmat.ui.write

// [공통] 5,6. AI 스캔 결과 수정 & 수동 작성

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.util.showToast
import com.guru2.team3.sseunmat.util.toStandardDateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReceiptWriteActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvHeaderSubtitle: TextView
    private lateinit var etStore: EditText
    private lateinit var etDate: EditText
    private lateinit var etAmount: EditText
    private lateinit var tvErrorStore: TextView
    private lateinit var tvErrorDate: TextView
    private lateinit var tvErrorAmount: TextView
    private lateinit var btnRetryScan: Button
    private lateinit var btnNext: Button

    private var isFormattingDate = false
    private var isFormattingAmount = false

    // 내부 저장/전달용 원시 금액 데이터 (Long)
    private var rawAmount: Long = 0L
    private var isGalleryMode: Boolean = false

    companion object {
        private const val TAG = "ReceiptWriteActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_receipt_write)

            val mainView = findViewById<View>(R.id.main) ?: findViewById(android.R.id.content)
            mainView?.let { v ->
                ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }

            initViews()
            setupListeners()
            loadIntentDataDataAndSetupUI()
            setupDateAutoFormatter()
            setupAmountAutoFormatter()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate 처리 중 오류 발생", e)
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        tvHeaderTitle = findViewById(R.id.tv_header_title)
        tvHeaderSubtitle = findViewById(R.id.tv_header_subtitle)
        etStore = findViewById(R.id.et_store)
        etDate = findViewById(R.id.et_date)
        etAmount = findViewById(R.id.et_amount)
        tvErrorStore = findViewById(R.id.tv_error_store)
        tvErrorDate = findViewById(R.id.tv_error_date)
        tvErrorAmount = findViewById(R.id.tv_error_amount)
        btnRetryScan = findViewById(R.id.btn_retry_scan)
        btnNext = findViewById(R.id.btn_next)
    }

    // AI 스캔(ReceiptScanActivity)에서 넘겨받은 JSON 데이터 자동 채우기 또는 수동 입력
    private fun loadIntentDataDataAndSetupUI() {
        try {
            val isManualMode = intent?.getBooleanExtra("IS_MANUAL_MODE", false) ?: false
            isGalleryMode = intent?.getBooleanExtra("IS_GALLERY_MODE", false) ?: false

            val extractedStore = intent?.getStringExtra("STORE") ?: ""
            val extractedDate = intent?.getStringExtra("DATE") ?: ""
            val extractedAmount = intent?.getLongExtra("AMOUNT", 0L) ?: 0L

            val isFullyExtracted = extractedStore.isNotBlank() && extractedDate.isNotBlank() && extractedAmount > 0L

            // 3가지 상황에 따른 타이틀 분기 처리
            when {
                // 1. 처음부터 수동 입력 옵션을 선택하고 들어온 경우
                isManualMode -> {
                    tvHeaderTitle.text = "직접 등록"
                    tvHeaderSubtitle.text = "영수증 정보를 직접 입력해 주세요"
                    btnRetryScan.visibility = View.GONE
                }
                // 2. AI 분석 결과 모든 항목이 인식된 경우
                isFullyExtracted -> {
                    tvHeaderTitle.text = "영수증 정보를\n확인해 주세요"
                    tvHeaderSubtitle.text = "AI가 분석한 영수증 정보가 맞는지 확인해 주세요"

                    btnRetryScan.visibility = View.VISIBLE
                    btnRetryScan.text = if (isGalleryMode) "다른 이미지 선택하기" else "다시 촬영하기"
                }
                // 3. AI 분석에 실패했거나 일부 항목이 빈 경우
                else -> {
                    tvHeaderTitle.text = "영수증 정보를\n정확히 읽지 못했어요"
                    tvHeaderSubtitle.text = "인식되지 않거나 잘못된 항목을 직접 입력해 주세요"

                    btnRetryScan.visibility = View.VISIBLE
                    btnRetryScan.text = if (isGalleryMode) "다른 이미지 선택하기" else "다시 촬영하기"
                }
            }

            // 받아온 데이터 채우기
            if (extractedStore.isNotBlank()) {
                etStore.setText(extractedStore)
            }

            if (extractedDate.isNotBlank()) {
                etDate.setText(extractedDate.toStandardDateFormat())
            }

            if (extractedAmount > 0L) {
                rawAmount = extractedAmount
                try {
                    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
                    etAmount.setText(formatter.format(rawAmount))
                } catch (e: Exception) {
                    etAmount.setText(rawAmount.toString())
                }
            }

            validateInputs()
        } catch (e: Exception) {
            Log.e(TAG, "loadIntentDataDataAndSetupUI 처리 중 오류 발생", e)
        }
    }

    private fun setupAmountAutoFormatter() {
        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormattingAmount) return

                try {
                    // 콤마 제거 후 숫자만 추출
                    val cleanDigits = s?.toString()?.replace("[^0-9]".toRegex(), "") ?: ""

                    // 순수 Long 변수에 보관
                    rawAmount = cleanDigits.toLongOrNull() ?: 0L

                    if (cleanDigits.isNotEmpty()) {
                        isFormattingAmount = true
                        try {
                            val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
                            val formatted = formatter.format(rawAmount)

                            etAmount.setText(formatted)
                            etAmount.setSelection(formatted.length) // 커서 위치 유지
                        } catch (e: Exception) {
                            Log.e(TAG, "금액 포맷팅 중 오류 발생", e)
                        }
                        isFormattingAmount = false
                    }
                    validateInputs()
                } catch (e: Exception) {
                    Log.e(TAG, "Amount TextWatcher 처리 중 오류 발생", e)
                    isFormattingAmount = false
                }
            }
        })
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnRetryScan.setOnClickListener {
            try {
                val intent = Intent(this, ReceiptScanActivity::class.java).apply {
                    putExtra("IS_GALLERY_MODE", isGalleryMode)
                }
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "재스캔 버튼 클릭 처리 중 오류 발생", e)
            }
        }

        // 상호명 변경 감지
        etStore.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        })

        // 6.3.2 가치 기록 이동
        btnNext.setOnClickListener {
            try {
                val store = etStore.text?.toString()?.trim() ?: ""
                val date = etDate.text?.toString()?.trim() ?: ""
                val amount = rawAmount
                showToast("영수증 정보 입력이 완료되었습니다.")

                val intent = Intent(this, ReceiptValueWriteActivity::class.java).apply {
                    putExtra("STORE", store)
                    putExtra("DATE", date)
                    putExtra("AMOUNT", amount)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "다음 버튼 클릭 처리 중 오류 발생", e)
            }
        }
    }

    // 결제 날짜 정수(YYYYMMDD) 8자리 입력 시 자동 "YYYY. MM. DD" 포맷팅
    private fun setupDateAutoFormatter() {
        etDate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormattingDate) return

                try {
                    val rawText = s?.toString()?.replace("[^0-9]".toRegex(), "") ?: ""
                    if (rawText.length == 8) {
                        isFormattingDate = true
                        val formatted = rawText.toStandardDateFormat()
                        etDate.setText(formatted)
                        etDate.setSelection(formatted.length)
                        isFormattingDate = false
                    }
                    validateInputs()
                } catch (e: Exception) {
                    Log.e(TAG, "Date TextWatcher 처리 중 오류 발생", e)
                    isFormattingDate = false
                }
            }
        })
    }

    // 입력값 유효성 실시간 검사
    private fun validateInputs() {
        try {
            val store = etStore.text?.toString()?.trim() ?: ""
            val date = etDate.text?.toString()?.trim() ?: ""

            var isValidStore = false
            var isValidDate = false
            var isValidAmount = false

            // 6.2.1 상호명 검사 (공백 제외 1자 이상)
            if (store.isNotEmpty()) {
                isValidStore = true
                tvErrorStore.visibility = View.GONE
            } else {
                tvErrorStore.text = "상호명을 입력해 주세요."
                tvErrorStore.visibility = if (etStore.hasFocus()) View.VISIBLE else View.GONE
            }

            // 6.2.2 결제 날짜 검사 (형식 검증 및 미래 날짜 제한)
            val dateRegex = "^\\d{4}\\.\\s\\d{2}\\.\\s\\d{2}$".toRegex()
            if (date.matches(dateRegex)) {
                val sdf = SimpleDateFormat("yyyy. MM. dd", Locale.KOREA)
                try {
                    val parsedDate = sdf.parse(date)
                    val currentDate = Date()

                    if (parsedDate != null && parsedDate.after(currentDate)) {
                        tvErrorDate.text = "미래 날짜는 입력할 수 없습니다."
                        tvErrorDate.visibility = View.VISIBLE
                    } else {
                        isValidDate = true
                        tvErrorDate.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    tvErrorDate.text = "올바른 날짜 형식이 아닙니다."
                    tvErrorDate.visibility = View.VISIBLE
                }
            } else if (date.isNotEmpty()) {
                tvErrorDate.text = "날짜 8자리를 입력해 주세요 (예: 20260724)"
                tvErrorDate.visibility = View.VISIBLE
            } else {
                tvErrorDate.visibility = View.GONE
            }

            // 6.2.3 최종 금액 검사 (0원보다 큰 금액, rawAmount 직접 검사)
            if (rawAmount > 0L) {
                isValidAmount = true
                tvErrorAmount.visibility = View.GONE
            } else if (etAmount.text?.toString()?.isNotEmpty() == true) {
                tvErrorAmount.text = "0원보다 큰 금액을 입력해 주세요."
                tvErrorAmount.visibility = View.VISIBLE
            } else {
                tvErrorAmount.visibility = View.GONE
            }

            // 6.3.1 정보 검증 및 버튼 활성화 처리
            val isAllValid = isValidStore && isValidDate && isValidAmount
            btnNext.isEnabled = isAllValid
            btnNext.backgroundTintList = getColorStateList(
                if (isAllValid) R.color.btn_active else R.color.btn_disabled
            )
        } catch (e: Exception) {
            Log.e(TAG, "validateInputs 실행 중 오류 발생", e)
        }
    }
}