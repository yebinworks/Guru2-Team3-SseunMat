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

// 5. 영수증 스캔 및 AI 분석 화면 (촬영 가이드 화면)
class ReceiptScanActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnReceiptCapture: Button
    private lateinit var layoutCameraGuide: LinearLayout
    private lateinit var layoutAnalysisLoading: LinearLayout

    private val openAIService = OpenAIService()
    private var isGalleryMode = false
    private var photoUri: Uri? = null
    private var photoFile: File? = null

    // 원본 카메라 촬영 결과 받아오기
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess && photoUri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val bitmap = getBitmapFromUri(photoUri!!)
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        analyzeImageWithOpenAI(bitmap)
                    } else {
                        showToast("이미지를 불러오지 못했습니다. 직접 입력 화면으로 이동합니다.")
                        navigateToManualWrite()
                    }
                }
            }
        } else {
            Log.d("ReceiptScan", "카메라 촬영이 취소되었습니다.")
        }
    }

    // 갤러리 이미지 선택 결과 받아오기
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                val bitmap = getBitmapFromUri(it)
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        analyzeImageWithOpenAI(bitmap)
                    } else {
                        showToast("이미지를 불러오지 못했습니다. 직접 입력 화면으로 이동합니다.")
                        navigateToManualWrite()
                    }
                }
            }
        }
    }

    // 권한 요청 처리
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startImagePickOrCapture()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_receipt_scan)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        isGalleryMode = intent.getBooleanExtra("IS_GALLERY_MODE", true)

        initViews()
        setupListeners()
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
            if (layoutAnalysisLoading.visibility == View.VISIBLE) return@setOnClickListener
            checkPermissionAndProceed()
        }
    }

    private fun checkPermissionAndProceed() {
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
    }

    private fun startImagePickOrCapture() {
        if (isGalleryMode) {
            galleryLauncher.launch("image/*")
        } else {
            try {
                // 원본 사진을 저장할 임시 파일 생성
                photoFile = File.createTempFile("receipt_", ".jpg", cacheDir).apply {
                    createNewFile()
                }
                photoUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    photoFile!!
                )

                // Intent 생성 없이 photoUri를 직접 넘겨서 카메라 실행
                cameraLauncher.launch(photoUri!!)
            } catch (e: Exception) {
                Log.e("ReceiptScan", "카메라 준비 중 오류 발생", e)
                showToast("카메라를 실행할 수 없습니다.")
            }
        }
    }

    // OpenAI API로 이미지 전달 후 분석
    private fun analyzeImageWithOpenAI(bitmap: Bitmap) {
        layoutCameraGuide.visibility = View.GONE
        layoutAnalysisLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = openAIService.analyzeReceiptImage(bitmap)
            layoutAnalysisLoading.visibility = View.GONE

            result.onSuccess { (store, date, amount) ->
                val intent = Intent(this@ReceiptScanActivity, ReceiptWriteActivity::class.java).apply {
                    putExtra("IS_GALLERY_MODE", isGalleryMode)
                    putExtra("STORE", store)
                    putExtra("DATE", date)
                    putExtra("AMOUNT", amount) // amount가 이미 Long이므로 중복 toLong() 제거
                }
                startActivity(intent)
                finish()
            }.onFailure { error ->
                Log.e("OpenAIError", "OpenAI 호출 실패 원인: ${error.localizedMessage}", error)
                showToast("영수증 정보를 정확히 읽지 못했어요. 직접 입력해 주세요.")
                navigateToManualWrite()
            }
        }
    }

    private fun navigateToManualWrite() {
        val intent = Intent(this, ReceiptWriteActivity::class.java).apply {
            putExtra("IS_GALLERY_MODE", isGalleryMode)
        }
        startActivity(intent)
        finish()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한 필요 안내")
            .setMessage("영수증을 촬영하려면 카메라 접근 권한이 필요합니다.")
            .setPositiveButton("확인", null)
            .show()
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
            Log.e("ReceiptScan", "Bitmap 생성 오류", e)
            null
        }
    }
}