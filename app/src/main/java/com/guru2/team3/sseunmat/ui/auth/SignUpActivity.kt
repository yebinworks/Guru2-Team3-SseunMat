package com.guru2.team3.sseunmat.ui.auth

// [주은] 2. 회원가입

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.remote.FirebaseService
import com.guru2.team3.sseunmat.ui.main.MainActivity
import com.guru2.team3.sseunmat.util.ValidationUtils
import com.guru2.team3.sseunmat.util.showToast

class SignUpActivity : AppCompatActivity() {

    private val firebaseService = FirebaseService()

    private lateinit var etUserEmail: EditText
    private lateinit var etUserPassword: EditText
    private lateinit var etPasswordConfirm: EditText
    private lateinit var etNickname: EditText
    private lateinit var tvEmailError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var tvPasswordConfirmError: TextView
    private lateinit var tvNicknameError: TextView
    private lateinit var tvSignUpError: TextView
    private lateinit var cbTermsService: CheckBox
    private lateinit var cbTermsPrivacy: CheckBox
    private lateinit var btnSignUp: Button

    companion object {
        private const val TAG = "SignUpActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_sign_up)

            val mainView = findViewById<View>(R.id.main) ?: findViewById(android.R.id.content)
            mainView?.let { v ->
                ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }

            // 뷰 참조
            etUserEmail = findViewById(R.id.et_user_email)
            etUserPassword = findViewById(R.id.et_user_password)
            etPasswordConfirm = findViewById(R.id.et_password_confirm)
            etNickname = findViewById(R.id.et_nickname)
            tvEmailError = findViewById(R.id.tv_email_error)
            tvPasswordError = findViewById(R.id.tv_password_error)
            tvPasswordConfirmError = findViewById(R.id.tv_password_confirm_error)
            tvNicknameError = findViewById(R.id.tv_nickname_error)
            tvSignUpError = findViewById(R.id.tv_sign_up_error)
            cbTermsService = findViewById(R.id.cb_terms_service)
            cbTermsPrivacy = findViewById(R.id.cb_terms_privacy)
            btnSignUp = findViewById(R.id.btn_sign_up)

            // [2.3.1] 모든 입력 + 필수 약관 충족 시 가입 버튼 활성화
            // [2.3.3] 입력을 수정하면 해당 항목의 에러 표시 해제
            watchInput(etUserEmail, tvEmailError)
            watchInput(etUserPassword, tvPasswordError)
            watchInput(etPasswordConfirm, tvPasswordConfirmError)
            watchInput(etNickname, tvNicknameError)
            cbTermsService.setOnCheckedChangeListener { _, _ -> updateButtonState() }
            cbTermsPrivacy.setOnCheckedChangeListener { _, _ -> updateButtonState() }

            // [2.3.2 / 2.3.3] 가입 처리
            btnSignUp.setOnClickListener {
                try {
                    if (!validateInputs()) return@setOnClickListener

                    btnSignUp.isEnabled = false  // 중복 클릭 방지
                    tvSignUpError.visibility = View.GONE

                    val email = etUserEmail.text?.toString()?.trim() ?: ""
                    val password = etUserPassword.text?.toString() ?: ""
                    val nickname = etNickname.text?.toString()?.trim() ?: ""

                    firebaseService.signUp(email, password, nickname) { isSuccess ->
                        if (isFinishing || isDestroyed) return@signUp

                        runOnUiThread {
                            try {
                                if (isSuccess) {
                                    showToast("가입이 완료되었어요!")
                                    // 가입 완료 후 자동 로그인 상태로 홈 이동, 이전 화면 스택 제거
                                    val intent = Intent(this, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    startActivity(intent)
                                    finish()
                                } else {
                                    tvSignUpError.text = "회원가입에 실패했어요. 잠시 후 다시 시도해 주세요."
                                    tvSignUpError.visibility = View.VISIBLE
                                    btnSignUp.isEnabled = true
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "회원가입 결과 UI 반영 중 오류 발생", e)
                                btnSignUp.isEnabled = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "가입 버튼 클릭 처리 중 오류 발생", e)
                    btnSignUp.isEnabled = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate 처리 중 오류 발생", e)
        }
    }

    // 입력창에 리스너 부착: 에러 해제 + 버튼 상태 갱신
    private fun watchInput(editText: EditText, errorView: TextView) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    errorView.visibility = View.GONE
                    updateButtonState()
                } catch (e: Exception) {
                    Log.e(TAG, "watchInput 처리 중 오류 발생", e)
                }
            }
        })
    }

    // [2.3.1] 버튼 활성 조건: 4개 입력 존재 + 필수 약관 2개 동의
    private fun updateButtonState() {
        try {
            val email = etUserEmail.text?.toString()?.trim() ?: ""
            val password = etUserPassword.text?.toString() ?: ""
            val passwordConfirm = etPasswordConfirm.text?.toString() ?: ""
            val nickname = etNickname.text?.toString()?.trim() ?: ""

            val allFilled = email.isNotEmpty() &&
                    password.isNotEmpty() &&
                    passwordConfirm.isNotEmpty() &&
                    nickname.isNotEmpty()
            val allAgreed = cbTermsService.isChecked && cbTermsPrivacy.isChecked
            btnSignUp.isEnabled = allFilled && allAgreed
        } catch (e: Exception) {
            Log.e(TAG, "updateButtonState 계산 중 오류 발생", e)
            btnSignUp.isEnabled = false
        }
    }

    // 가입 시도 시 형식 검증, 실패 항목에 에러 표시 [2.1.1 ~ 2.1.4]
    private fun validateInputs(): Boolean {
        return try {
            var isValid = true

            val email = etUserEmail.text?.toString()?.trim() ?: ""
            val password = etUserPassword.text?.toString() ?: ""
            val passwordConfirm = etPasswordConfirm.text?.toString() ?: ""
            val nickname = etNickname.text?.toString() ?: ""

            if (!ValidationUtils.isValidEmail(email)) {
                tvEmailError.text = "이메일 형식을 확인해 주세요"
                tvEmailError.visibility = View.VISIBLE
                isValid = false
            }
            if (!ValidationUtils.isValidPassword(password)) {
                tvPasswordError.text = "영문, 숫자, 특수문자 조합 8자 이상으로 입력해주세요"
                tvPasswordError.visibility = View.VISIBLE
                isValid = false
            }
            if (password != passwordConfirm) {
                tvPasswordConfirmError.text = "비밀번호가 맞지 않습니다"
                tvPasswordConfirmError.visibility = View.VISIBLE
                isValid = false
            }
            if (!ValidationUtils.isValidNickname(nickname)) {
                tvNicknameError.text = "닉네임은 2자 이상 10자 이하로 입력해주세요"
                tvNicknameError.visibility = View.VISIBLE
                isValid = false
            }
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "validateInputs 실행 중 오류 발생", e)
            false
        }
    }
}