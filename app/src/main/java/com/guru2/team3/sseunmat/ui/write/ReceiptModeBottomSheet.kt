package com.guru2.team3.sseunmat.ui.write

// [주은]  4. 촬영/갤러리/수동입력 선택

class ReceiptModeBottomSheet {
}

// 예빈 메모: 주은님 촬영/갤러리/수동입력 선택 작업하실 때,
// 아래와 같이 수동입력 선택 시 플래그 전달해주시면 됩니다!

//// ReceiptModeBottomSheet.kt
//btnModeManual.setOnClickListener {
//    dismiss()
//    val intent = Intent(requireContext(), ReceiptWriteActivity::class.java).apply {
//        putExtra("IS_MANUAL_MODE", true) // 직접 입력 플래그 전달
//    }
//    startActivity(intent)
//}