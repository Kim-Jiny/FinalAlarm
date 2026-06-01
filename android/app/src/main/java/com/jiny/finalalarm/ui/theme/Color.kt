package com.jiny.finalalarm.ui.theme

import androidx.compose.ui.graphics.Color

// iOS HIG 기반 미니멀 팔레트. 하나의 primary, 나머지는 중성 그레이.
object FA {
    // 라이트 모드
    val Bg = Color(0xFFFFFFFF)
    val Surface = Color(0xFFFFFFFF)
    val Label = Color(0xFF1C1C1E)
    val LabelSecondary = Color(0xFF8E8E93)
    val LabelTertiary = Color(0xFFC7C7CC)
    val Separator = Color(0xFFE5E5EA)
    val Fill = Color(0xFFF2F2F7)        // 인풋 배경, 선택된 chip 등

    // 단 하나의 강조 색
    val Primary = Color(0xFF007AFF)

    // 위험 액션 (삭제·탈퇴)
    val Destructive = Color(0xFFFF3B30)

    // 다크 모드 (자동 적용)
    val BgDark = Color(0xFF000000)
    val SurfaceDark = Color(0xFF1C1C1E)
    val LabelDark = Color(0xFFFFFFFF)
    val LabelSecondaryDark = Color(0xFF8E8E93)
    val LabelTertiaryDark = Color(0xFF48484A)
    val SeparatorDark = Color(0xFF38383A)
    val FillDark = Color(0xFF2C2C2E)
}
