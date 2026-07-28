package com.guru2.team3.sseunmat.ui.write

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.remote.OpenAIService
import com.guru2.team3.sseunmat.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// [예빈] 5. 영수증 스캔 및 AI 분석 화면 (촬영 가이드 화면)
class ReceiptScanActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnReceiptCapture: Button
    private lateinit var layoutCameraGuide: LinearLayout
    private lateinit var layoutAnalysisLoading: LinearLayout

    private val openAIService = OpenAIService()
    private var isGalleryMode = false
    private var photoUri: Uri? = null
    private var photoFile: File? = null

    companion object {
        private const val TAG = "ReceiptScanActivity"
    }

    // 원본 카메라 촬영 결과 받아오기
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        try {
            if (isSuccess && photoUri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val bitmap = getBitmapFromUri(photoUri!!)
                    withContext(Dispatchers.Main) {
                        if (isFinishing || isDestroyed) return@withContext
                        if (bitmap != null) {
                            analyzeImageWithOpenAI(bitmap)
                        } else {
                            showToast("이미지를 불러오지 못했습니다. 직접 입력 화면으로 이동합니다.")
                            navigateToManualWrite()
                        }
                    }
                }
            } else {
                Log.d(TAG, "카메라 촬영이 취소되었습니다.")
                clearTempFile()
            }
        } catch (e: Exception) {
            Log.e(TAG, "cameraLauncher 처리 중 오류 발생", e)
            navigateToManualWrite()
        }
    }

    // 갤러리 이미지 선택 결과 받아오기
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        try {
            if (uri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val bitmap = getBitmapFromUri(uri)
                    withContext(Dispatchers.Main) {
                        if (isFinishing || isDestroyed) return@withContext
                        if (bitmap != null) {
                            analyzeImageWithOpenAI(bitmap)
                        } else {
                            showToast("이미지를 불러오지 못했습니다. 직접 입력 화면으로 이동합니다.")
                            navigateToManualWrite()
                        }
                    }
                }
            } else {
                Log.d(TAG, "갤러리 선택이 취소되었습니다.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "galleryLauncher 처리 중 오류 발생", e)
            navigateToManualWrite()
        }
    }

    // 권한 요청 처리
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        try {
            if (isGranted) {
                startImagePickOrCapture()
            } else {
                showPermissionDeniedDialog()
            }
        } catch (e: Exception) {
            Log.e(TAG, "permissionLauncher 처리 중 오류 발생", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_receipt_scan)

            val mainView = findViewById<View>(R.id.main) ?: findViewById(android.R.id.content)
            mainView?.let { v ->
                ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }

            isGalleryMode = intent?.getBooleanExtra("IS_GALLERY_MODE", true) ?: true

            initViews()
            setupListeners()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate 처리 중 오류 발생", e)
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        btnReceiptCapture = findViewById(R.id.btn_receipt_capture)
        layoutCameraGuide = findViewById(R.id.layout_camera_guide)
        layoutAnalysisLoading = findViewById(R.id.layout_analysis_loading)

        // 모드에 맞춰 버튼 텍스트 변경
        if (isGalleryMode) {
            btnReceiptCapture.text = "갤러리에서 사진 선택하기"
        } else {
            btnReceiptCapture.text = "영수증 촬영하기"
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // 하단 버튼 클릭 시 권한 확인 후 카메라/갤러리 실행
        btnReceiptCapture.setOnClickListener {
            try {
                if (layoutAnalysisLoading.visibility == View.VISIBLE) return@setOnClickListener
                checkPermissionAndProceed()
            } catch (e: Exception) {
                Log.e(TAG, "버튼 클릭 처리 중 오류 발생", e)
            }
        }
    }

    private fun checkPermissionAndProceed() {
        try {
            // 갤러리 모드는 Photo Picker 사용으로 별도 권한 없이 가능, 카메라 모드만 권한 체크
            if (isGalleryMode) {
                startImagePickOrCapture()
                return
            }

            val permission = Manifest.permission.CAMERA
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                startImagePickOrCapture()
            } else {
                permissionLauncher.launch(permission)
            }
        } catch (e: Exception) {
            Log.e(TAG, "권한 검사 중 오류 발생", e)
            startImagePickOrCapture()
        }
    }

    private fun startImagePickOrCapture() {
        try {
            if (isGalleryMode) {
                galleryLauncher.launch("image/*")
            } else {
                // 원본 사진을 저장할 임시 파일 생성
                clearTempFile()
                val tempFile = File.createTempFile("receipt_", ".jpg", cacheDir)
                photoFile = tempFile
                photoUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    tempFile
                )

                photoUri?.let { uri ->
                    cameraLauncher.launch(uri)
                } ?: run {
                    showToast("카메라를 실행할 수 없습니다.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "카메라/갤러리 실행 준비 중 오류 발생", e)
            showToast("카메라를 실행할 수 없습니다.")
        }
    }

    // OpenAI API로 이미지 전달 후 분석
    private fun analyzeImageWithOpenAI(bitmap: Bitmap) {
        try {
            layoutCameraGuide.visibility = View.GONE
            layoutAnalysisLoading.visibility = View.VISIBLE

            lifecycleScope.launch {
                val result = openAIService.analyzeReceiptImage(bitmap)

                if (isFinishing || isDestroyed) return@launch
                layoutAnalysisLoading.visibility = View.GONE

                result.onSuccess { (store, date, amount) ->
                    try {
                        val intent = Intent(this@ReceiptScanActivity, ReceiptWriteActivity::class.java).apply {
                            putExtra("IS_GALLERY_MODE", isGalleryMode)
                            putExtra("STORE", store)
                            putExtra("DATE", date)
                            putExtra("AMOUNT", amount)
                        }
                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        Log.e(TAG, "ReceiptWriteActivity 이동 중 오류 발생", e)
                        navigateToManualWrite()
                    }
                }.onFailure { error ->
                    Log.e(TAG, "OpenAI 호출 실패 원인: ${error.localizedMessage}", error)
                    showToast("영수증 정보를 정확히 읽지 못했어요. 직접 입력해 주세요.")
                    navigateToManualWrite()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "analyzeImageWithOpenAI 처리 중 오류 발생", e)
            layoutAnalysisLoading.visibility = View.GONE
            navigateToManualWrite()
        }
    }

    private fun navigateToManualWrite() {
        try {
            if (isFinishing || isDestroyed) return
            val intent = Intent(this, ReceiptWriteActivity::class.java).apply {
                putExtra("IS_GALLERY_MODE", isGalleryMode)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "navigateToManualWrite 처리 중 오류 발생", e)
        }
    }

    private fun showPermissionDeniedDialog() {
        try {
            if (isFinishing || isDestroyed) return
            AlertDialog.Builder(this)
                .setTitle("권한 필요 안내")
                .setMessage("영수증을 촬영하려면 카메라 접근 권한이 필요합니다.")
                .setPositiveButton("확인", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "권한 안내 다이얼로그 표시 중 오류 발생", e)
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap 생성 오류", e)
            null
        }
    }

    private fun clearTempFile() {
        try {
            photoFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
            photoFile = null
            photoUri = null
        } catch (e: Exception) {
            Log.e(TAG, "임시 파일 삭제 실패", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clearTempFile()
    }
}