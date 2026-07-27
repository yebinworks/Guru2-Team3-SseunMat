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
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.guru2.team3.sseunmat.R
import com.guru2.team3.sseunmat.data.remote.OpenAIService
import com.guru2.team3.sseunmat.util.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 5. 영수증 스캔 및 AI 분석 화면
class ReceiptScanActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnReceiptCapture: Button
    private lateinit var btnCancelAnalysis: Button
    private lateinit var layoutCameraGuide: LinearLayout
    private lateinit var layoutAnalysisLoading: LinearLayout

    private val openAIService = OpenAIService()
    private var isGalleryMode = false

    // 카메라 촬영 결과 받아오기
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                analyzeImageWithOpenAI(bitmap)
            } else {
                showToast("이미지를 불러오지 못했습니다. 직접 입력 화면으로 이동합니다.")
                navigateToManualWrite()
            }
        }
    }

    // 갤러리 이미지 선택 결과 받아오기
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // 갤러리 이미지 로딩 시 메인 스레드가 멈추지 않도록 IO 스레드에서 백그라운드로 처리
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

        if (isGalleryMode) {
            btnReceiptCapture.text = "갤러리에서 사진 선택하기"
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnReceiptCapture.setOnClickListener {
            if (layoutAnalysisLoading.visibility == View.VISIBLE) return@setOnClickListener
            checkPermissionAndProceed()
        }

        btnCancelAnalysis.setOnClickListener {
            layoutAnalysisLoading.visibility = View.GONE
            layoutCameraGuide.visibility = View.VISIBLE
        }
    }

    private fun checkPermissionAndProceed() {
        val permission = if (isGalleryMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        } else {
            Manifest.permission.CAMERA
        }

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
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                cameraLauncher.launch(takePictureIntent)
            } catch (e: Exception) {
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
                android.util.Log.d("OpenAISuccess", "상호명(Store) : $store")
                android.util.Log.d("OpenAISuccess", "결제 날짜(Date)  : $date")
                android.util.Log.d("OpenAISuccess", "지출 금액(Amount): $amount")

                val intent = Intent(this@ReceiptScanActivity, ReceiptWriteActivity::class.java).apply {
                    putExtra("IS_GALLERY_MODE", isGalleryMode)
                    putExtra("STORE", store)
                    putExtra("DATE", date)
                    putExtra("AMOUNT", amount.toLong())
                }
                startActivity(intent)
                finish()
            }.onFailure { error ->
                // 디버깅용 로그 출력
                android.util.Log.e("OpenAIFailure", "분석 실패 이유: ${error.message}", error)
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
            .setMessage("영수증 스캔 기능을 사용하려면 접근 권한 허용이 필요합니다.")
            .setPositiveButton("확인", null)
            .show()
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }
}