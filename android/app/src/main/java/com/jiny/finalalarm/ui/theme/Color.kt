package com.jiny.finalalarm.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * "아침" 무드 — 따뜻한 코랄 + 라벤더 포인트.
 * Forest / Finch / Gentler Streak 같은 인디 앱 톤.
 */
object FA {
    // 배경: 크림 → 살구. 그라데이션으로 사용.
    val BgTop = Color(0xFFFFFBF5)
    val BgBottom = Color(0xFFFFF0E5)
    val Surface = Color(0xFFFFFCF7)

    // 텍스트: 순흑 대신 따뜻한 다크 (대비 강화)
    val Label = Color(0xFF1F1A14)
    val LabelSecondary = Color(0xFF6B5E54)
    val LabelTertiary = Color(0xFFA89B8E)

    // 프라이머리: 따뜻한 코랄
    val Primary = Color(0xFFFF7B6B)
    val PrimaryLight = Color(0xFFFFA28A)      // 그라데이션 상단
    val PrimaryDark = Color(0xFFEB5D4D)       // 그라데이션 하단 / pressed
    val OnPrimary = Color(0xFFFFFFFF)

    // 보조 액센트: 라벤더 (대비 위해 살짝 진하게)
    val Accent = Color(0xFF7B5BE0)
    val AccentLight = Color(0xFFE9DFFC)

    // 성공: 세이지 그린 (해제됨, 완료)
    val Success = Color(0xFF7BB99F)
    val SuccessLight = Color(0xFFD7EBE0)

    // 위험: 따뜻한 빨강
    val Destructive = Color(0xFFE85D5D)

    // 라인·필
    val Separator = Color(0xFFF0E5D8)
    val Fill = Color(0xFFFFF1E8)              // 인풋 배경

    // 다크 모드 (미세 보정)
    val BgTopDark = Color(0xFF1A1714)
    val BgBottomDark = Color(0xFF231D18)
    val SurfaceDark = Color(0xFF2A2420)
    val LabelDark = Color(0xFFFAF6F0)
    val LabelSecondaryDark = Color(0xFFBFB1A3)
    val SeparatorDark = Color(0xFF3D342C)
    val FillDark = Color(0xFF3A302A)

    val BgGradient = Brush.verticalGradient(listOf(BgTop, BgBottom))
    val PrimaryGradient = Brush.verticalGradient(listOf(PrimaryLight, Primary))
}
