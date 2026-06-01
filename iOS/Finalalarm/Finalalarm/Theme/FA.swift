import SwiftUI

/// 따뜻한 코랄 + 라벤더 톤. Android와 동일한 팔레트.
enum FA {
    // 배경
    static let bgTop = Color(red: 1.0, green: 0.984, blue: 0.961)
    static let bgBottom = Color(red: 1.0, green: 0.941, blue: 0.898)
    static let surface = Color(red: 1.0, green: 0.988, blue: 0.969)

    // 텍스트
    static let label = Color(red: 0.122, green: 0.102, blue: 0.078)
    static let labelSecondary = Color(red: 0.420, green: 0.369, blue: 0.329)
    static let labelTertiary = Color(red: 0.659, green: 0.608, blue: 0.557)

    // 프라이머리 코랄
    static let primary = Color(red: 1.0, green: 0.482, blue: 0.420)
    static let primaryLight = Color(red: 1.0, green: 0.635, blue: 0.541)
    static let primaryDark = Color(red: 0.922, green: 0.365, blue: 0.302)
    static let onPrimary = Color.white

    // 라벤더 액센트
    static let accent = Color(red: 0.482, green: 0.357, blue: 0.878)
    static let accentLight = Color(red: 0.914, green: 0.875, blue: 0.988)

    // 성공/위험
    static let success = Color(red: 0.482, green: 0.725, blue: 0.624)
    static let destructive = Color(red: 0.910, green: 0.365, blue: 0.365)

    // 라인·필
    static let separator = Color(red: 0.941, green: 0.898, blue: 0.847)
    static let fill = Color(red: 1.0, green: 0.945, blue: 0.910)

    static let bgGradient = LinearGradient(
        colors: [bgTop, bgBottom],
        startPoint: .top,
        endPoint: .bottom
    )

    static let primaryGradient = LinearGradient(
        colors: [primaryLight, primary],
        startPoint: .top,
        endPoint: .bottom
    )
}

enum FASpacing {
    static let xxs: CGFloat = 4
    static let xs: CGFloat = 6
    static let sm: CGFloat = 10
    static let md: CGFloat = 14
    static let lg: CGFloat = 20
    static let xl: CGFloat = 28
    static let xxl: CGFloat = 40
    static let xxxl: CGFloat = 56
}

enum FACorner {
    static let small: CGFloat = 10
    static let medium: CGFloat = 14
    static let large: CGFloat = 20
    static let xl: CGFloat = 28
}
