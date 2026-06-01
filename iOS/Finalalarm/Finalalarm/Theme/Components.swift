import SwiftUI

// MARK: - WarmBackground

struct WarmBackground<Content: View>: View {
    @ViewBuilder let content: Content
    var body: some View {
        ZStack {
            FA.bgGradient.ignoresSafeArea()
            content
        }
    }
}

// MARK: - HelloHeader

struct HelloHeader: View {
    let title: String
    var subtitle: String? = nil
    var body: some View {
        VStack(alignment: .leading, spacing: FASpacing.xxs) {
            Text(title)
                .font(.system(size: 32, weight: .heavy))
                .foregroundStyle(FA.label)
                .kerning(-0.6)
            if let subtitle {
                Text(subtitle)
                    .font(.system(size: 16))
                    .foregroundStyle(FA.labelSecondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, FASpacing.xl)
    }
}

// MARK: - Section

struct FASection<Content: View>: View {
    var title: String? = nil
    var topGap: CGFloat = FASpacing.xl
    @ViewBuilder let content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: FASpacing.sm) {
            Spacer().frame(height: topGap)
            if let title {
                Text(title)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(FA.accent)
                    .padding(.leading, FASpacing.sm)
            }
            content
        }
    }
}

// MARK: - PrimaryButton

struct PrimaryButton: View {
    let text: String
    var enabled: Bool = true
    let action: () -> Void
    @State private var pressed = false

    var body: some View {
        Button(action: { if enabled { action() } }) {
            Text(text)
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(FA.onPrimary)
                .frame(maxWidth: .infinity, minHeight: 56)
                .background(
                    Group {
                        if enabled {
                            FA.primaryGradient
                        } else {
                            FA.labelTertiary
                        }
                    }
                )
                .clipShape(RoundedRectangle(cornerRadius: FACorner.large))
                .scaleEffect(pressed && enabled ? 0.97 : 1)
                .animation(.spring(response: 0.25, dampingFraction: 0.7), value: pressed)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .simultaneousGesture(DragGesture(minimumDistance: 0)
            .onChanged { _ in pressed = true }
            .onEnded { _ in pressed = false }
        )
    }
}

// MARK: - SecondaryButton

struct SecondaryButton: View {
    let text: String
    var destructive: Bool = false
    var enabled: Bool = true
    let action: () -> Void
    @State private var pressed = false

    var body: some View {
        Button(action: { if enabled { action() } }) {
            Text(text)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(destructive ? FA.destructive : FA.primary)
                .frame(minHeight: 48)
                .padding(.horizontal, FASpacing.md)
                .scaleEffect(pressed && enabled ? 0.97 : 1)
                .animation(.spring(response: 0.25, dampingFraction: 0.7), value: pressed)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .simultaneousGesture(DragGesture(minimumDistance: 0)
            .onChanged { _ in pressed = true }
            .onEnded { _ in pressed = false }
        )
    }
}

// MARK: - FaTextField

struct FaTextField: View {
    let placeholder: String
    @Binding var text: String
    var isSecure: Bool = false
    var keyboardType: UIKeyboardType = .default
    var autocapitalization: TextInputAutocapitalization = .never

    var body: some View {
        Group {
            if isSecure {
                SecureField(placeholder, text: $text)
            } else {
                TextField(placeholder, text: $text)
                    .keyboardType(keyboardType)
                    .textInputAutocapitalization(autocapitalization)
                    .autocorrectionDisabled()
            }
        }
        .font(.system(size: 16))
        .foregroundStyle(FA.label)
        .padding(.horizontal, FASpacing.md)
        .padding(.vertical, FASpacing.md)
        .background(FA.fill)
        .clipShape(RoundedRectangle(cornerRadius: FACorner.medium))
    }
}

// MARK: - ListRow

struct ListRow<Trailing: View>: View {
    let headline: String
    var supporting: String? = nil
    var destructive: Bool = false
    var onTap: (() -> Void)? = nil
    @ViewBuilder let trailing: Trailing
    @State private var pressed = false

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: FASpacing.sm) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(headline)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(destructive ? FA.destructive : FA.label)
                    if let supporting {
                        Text(supporting)
                            .font(.system(size: 14))
                            .foregroundStyle(FA.labelSecondary)
                    }
                }
                Spacer()
                trailing
            }
            .frame(minHeight: 52)
            .padding(.vertical, FASpacing.sm)
            .contentShape(Rectangle())
            .scaleEffect(pressed && onTap != nil ? 0.98 : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.7), value: pressed)
            .onTapGesture {
                onTap?()
            }
            .simultaneousGesture(DragGesture(minimumDistance: 0)
                .onChanged { _ in if onTap != nil { pressed = true } }
                .onEnded { _ in pressed = false }
            )

            Rectangle()
                .fill(FA.separator)
                .frame(height: 0.5)
        }
    }
}

extension ListRow where Trailing == EmptyView {
    init(headline: String, supporting: String? = nil, destructive: Bool = false, onTap: (() -> Void)? = nil) {
        self.init(headline: headline, supporting: supporting, destructive: destructive, onTap: onTap, trailing: { EmptyView() })
    }
}

// MARK: - EmptyState

struct EmptyState: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 16))
            .foregroundStyle(FA.labelSecondary)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .padding(.vertical, FASpacing.xxxl)
    }
}

// MARK: - ErrorText

struct ErrorText: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 14))
            .foregroundStyle(FA.destructive)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, FASpacing.md)
            .padding(.vertical, FASpacing.sm)
            .background(FA.destructive.opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: FACorner.medium))
            .padding(.vertical, FASpacing.sm)
    }
}

// MARK: - ChoicePill

struct ChoicePill: View {
    let text: String
    let selected: Bool
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 15, weight: selected ? .bold : .semibold))
                .foregroundStyle(selected ? FA.onPrimary : FA.label)
                .frame(maxWidth: .infinity, minHeight: 48)
                .background(selected ? FA.primary : FA.fill)
                .clipShape(RoundedRectangle(cornerRadius: FACorner.medium))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - DayPill (요일)

struct DayPill: View {
    let day: String
    let selected: Bool
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(day)
                .font(.system(size: 14, weight: selected ? .bold : .medium))
                .foregroundStyle(selected ? FA.onPrimary : FA.label)
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(selected ? FA.primary : FA.fill)
                .clipShape(RoundedRectangle(cornerRadius: FACorner.medium))
        }
        .buttonStyle(.plain)
    }
}
