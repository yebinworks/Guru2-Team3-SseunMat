package com.guru2.team3.sseunmat.ui.write

// [주은] 4. 촬영/갤러리/수동입력 선택 (바텀시트)

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.guru2.team3.sseunmat.R

class ReceiptModeBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_receipt_mode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardCamera = view.findViewById<LinearLayout>(R.id.card_camera)
        val cardGallery = view.findViewById<LinearLayout>(R.id.card_gallery)
        val cardManual = view.findViewById<LinearLayout>(R.id.card_manual)

        // [4.1.2] 영수증 촬영하기 → 스캔 화면(카메라 모드). 권한 확인은 스캔 화면이 처리
        cardCamera.setOnClickListener {
            dismiss()
            val intent = Intent(requireContext(), ReceiptScanActivity::class.java).apply {
                putExtra("IS_GALLERY_MODE", false)
            }
            startActivity(intent)
        }

        // [4.1.3] 갤러리에서 불러오기 → 스캔 화면(갤러리 모드). 권한 확인은 스캔 화면이 처리
        cardGallery.setOnClickListener {
            dismiss()
            val intent = Intent(requireContext(), ReceiptScanActivity::class.java).apply {
                putExtra("IS_GALLERY_MODE", true)
            }
            startActivity(intent)
        }

        // [4.1.4] 직접 입력하기 → 빈 영수증 정보 입력 화면 (예빈님 메모대로 IS_MANUAL_MODE 전달)
        cardManual.setOnClickListener {
            dismiss()
            val intent = Intent(requireContext(), ReceiptWriteActivity::class.java).apply {
                putExtra("IS_MANUAL_MODE", true)
            }
            startActivity(intent)
        }
    }
}