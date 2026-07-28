package com.guru2.team3.sseunmat.ui.write

// [주은] 4. 촬영/갤러리/수동입력 선택 (바텀시트)

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.guru2.team3.sseunmat.R

class ReceiptModeBottomSheet : BottomSheetDialogFragment() {

    private lateinit var cardCamera: CardView
    private lateinit var cardGallery: CardView
    private lateinit var cardManual: CardView

    companion object {
        private const val TAG = "ReceiptModeBottomSheet"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            inflater.inflate(R.layout.bottom_sheet_receipt_mode, container, false)
        } catch (e: Exception) {
            Log.e(TAG, "onCreateView 인플레이트 중 오류 발생", e)
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            cardCamera = view.findViewById(R.id.card_camera)
            cardGallery = view.findViewById(R.id.card_gallery)
            cardManual = view.findViewById(R.id.card_manual)

            // [4.1.2] 영수증 촬영하기 -> 스캔 화면(카메라 모드). 권한 확인은 스캔 화면이 처리
            cardCamera.setOnClickListener {
                safeNavigate {
                    Intent(it, ReceiptScanActivity::class.java).apply {
                        putExtra("IS_GALLERY_MODE", false)
                    }
                }
            }

            // [4.1.3] 갤러리에서 불러오기 -> 스캔 화면(갤러리 모드). 권한 확인은 스캔 화면이 처리
            cardGallery.setOnClickListener {
                safeNavigate {
                    Intent(it, ReceiptScanActivity::class.java).apply {
                        putExtra("IS_GALLERY_MODE", true)
                    }
                }
            }

            // [4.1.4] 직접 입력하기 -> 빈 영수증 정보 입력 화면
            cardManual.setOnClickListener {
                safeNavigate {
                    Intent(it, ReceiptWriteActivity::class.java).apply {
                        putExtra("IS_MANUAL_MODE", true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onViewCreated 처리 중 오류 발생", e)
        }
    }

    private inline fun safeNavigate(intentBuilder: (android.content.Context) -> Intent) {
        try {
            val currentContext = context ?: return
            if (isRemoving || isDetached) return

            dismissAllowingStateLoss()

            val intent = intentBuilder(currentContext)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "화면 이동 중 오류 발생", e)
        }
    }
}