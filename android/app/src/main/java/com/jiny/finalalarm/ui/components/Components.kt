package com.jiny.finalalarm.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jiny.finalalarm.ui.theme.FA
import com.jiny.finalalarm.ui.theme.FaSpacing

/**
 * 화면이 ON_RESUME 될 때마다 콜백 실행 (뒤로가기로 돌아왔을 때 list 갱신용).
 */
@Composable
fun OnResume(callback: () -> Unit) {
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) callback()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

/**
 * 따뜻한 그라데이션 배경. 거의 모든 화면에서 사용.
 */
@Composable
fun WarmBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FA.BgGradient),
        content = content,
    )
}

/**
 * 큰 인사 헤더 — 이모지 + 타이틀 + 보조 문구.
 */
@Composable
fun HelloHeader(
    title: String,
    subtitle: String? = null,
    emoji: String? = null,
) {
    Column(modifier = Modifier.padding(top = FaSpacing.xl)) {
        if (emoji != null) {
            Text(emoji, style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(FaSpacing.sm))
        }
        Text(title, style = MaterialTheme.typography.displayMedium)
        if (subtitle != null) {
            Spacer(Modifier.height(FaSpacing.xs))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 섹션 — 부드러운 라벤더 헤더.
 */
@Composable
fun Section(
    title: String? = null,
    topGap: androidx.compose.ui.unit.Dp = FaSpacing.xl,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column {
        Spacer(Modifier.height(topGap))
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(start = FaSpacing.sm, bottom = FaSpacing.sm),
            )
        }
        content()
    }
}

/**
 * iOS 스타일 리스트 행. 카드는 아니지만 hover/press 미세 반응.
 */
@Composable
fun ListRow(
    headline: String,
    supporting: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    destructive: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, spring(), label = "row")

    val rowMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
    } else Modifier

    Column(modifier = rowMod.scale(scale)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(vertical = FaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (destructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground,
                )
                if (supporting != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(FaSpacing.sm))
                trailing()
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            thickness = 0.5.dp,
        )
    }
}

/**
 * Primary CTA — 코랄 그라데이션 + 누르면 살짝 작아짐.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, spring(), label = "btn")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .scale(scale)
            .clip(MaterialTheme.shapes.large)
            .background(if (enabled) FA.PrimaryGradient else SolidColor(FA.LabelTertiary))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = FA.OnPrimary,
        )
    }
}

/**
 * 보조 액션 — 텍스트 버튼, 친근한 톤.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, spring(), label = "sec")

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = FaSpacing.md, vertical = FaSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * 인풋 — 부드러운 따뜻한 배경, 테두리 없음.
 */
@Composable
fun FaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions =
        androidx.compose.foundation.text.KeyboardOptions.Default,
    singleLine: Boolean = true,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        ),
        shape = MaterialTheme.shapes.medium,
    )
}

/**
 * 빈 상태 — 이모지 + 다정한 안내.
 */
@Composable
fun EmptyState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FaSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 인라인 에러 — 약간의 살구 배경 + 둥근 모서리.
 */
@Composable
fun ErrorText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FaSpacing.sm)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
            .padding(horizontal = FaSpacing.md, vertical = FaSpacing.sm),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/**
 * 시각 입력 — 텍스트 필드 대신 시계 다이얼로그를 띄움.
 * value: "HH:mm" 포맷. 키보드 안 띄우니 화면 가림 문제 없음.
 */
@Composable
fun TimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val (initialHour, initialMinute) = remember(value) { parseHourMinute(value) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { open = true }
            .padding(horizontal = FaSpacing.md, vertical = FaSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }

    if (open) {
        val state = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { open = false },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(FaSpacing.md))
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = state.hour.toString().padStart(2, '0')
                    val m = state.minute.toString().padStart(2, '0')
                    onValueChange("$h:$m")
                    open = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("취소") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

private fun parseHourMinute(s: String): Pair<Int, Int> {
    val parts = s.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 7
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
}

/**
 * 작은 컬러 뱃지 — 상태 표시 (해제됨/만료/스누즈 등).
 */
@Composable
fun Pill(text: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(tint.copy(alpha = 0.15f))
            .padding(horizontal = FaSpacing.sm, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

