package com.jiny.finalalarm.ui.missions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.core.network.userMessage
import com.jiny.finalalarm.data.api.CreateMissionReq
import com.jiny.finalalarm.data.api.FinalAlarmApi
import com.jiny.finalalarm.data.api.MissionType
import com.jiny.finalalarm.ui.components.ErrorText
import com.jiny.finalalarm.ui.components.FaTextField
import com.jiny.finalalarm.ui.components.HelloHeader
import com.jiny.finalalarm.ui.components.ListRow
import com.jiny.finalalarm.ui.components.PrimaryButton
import com.jiny.finalalarm.ui.components.SecondaryButton
import com.jiny.finalalarm.ui.components.Section
import com.jiny.finalalarm.ui.components.WarmBackground
import com.jiny.finalalarm.ui.theme.FA
import com.jiny.finalalarm.ui.theme.FaSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

data class MissionEditUi(
    val editingId: String? = null,
    val type: MissionType = MissionType.MATH,
    val name: String = "",
    val difficulty: String = "medium",
    val questionCount: Int = 3,
    val photoMode: String = "QR",
    val expectedCode: String = "",
    val shakeCount: Int = 30,
    val isDefault: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class MissionEditVm @Inject constructor(private val api: FinalAlarmApi) : ViewModel() {
    private val _state = MutableStateFlow(MissionEditUi())
    val state = _state.asStateFlow()

    fun loadExisting(id: String) = viewModelScope.launch {
        val m = runCatching { api.getMission(id) }.getOrNull() ?: return@launch
        val cfg = m.config
        _state.value = _state.value.copy(
            editingId = m.id,
            type = m.type,
            name = m.name,
            isDefault = m.isDefault,
            difficulty = cfg["difficulty"]?.toString()?.trim('"') ?: _state.value.difficulty,
            questionCount = cfg["questionCount"]?.toString()?.toIntOrNull() ?: _state.value.questionCount,
            photoMode = cfg["mode"]?.toString()?.trim('"') ?: _state.value.photoMode,
            expectedCode = cfg["expectedCode"]?.toString()?.trim('"') ?: "",
            shakeCount = cfg["shakeCount"]?.toString()?.toIntOrNull() ?: _state.value.shakeCount,
        )
    }

    fun onType(t: MissionType) { _state.value = _state.value.copy(type = t) }
    fun onName(v: String) { _state.value = _state.value.copy(name = v) }
    fun onDifficulty(v: String) { _state.value = _state.value.copy(difficulty = v) }
    fun onQuestionCount(v: Int) { _state.value = _state.value.copy(questionCount = v) }
    fun onPhotoMode(v: String) { _state.value = _state.value.copy(photoMode = v) }
    fun onExpectedCode(v: String) { _state.value = _state.value.copy(expectedCode = v) }
    fun onShakeCount(v: Int) { _state.value = _state.value.copy(shakeCount = v) }
    fun toggleDefault() { _state.value = _state.value.copy(isDefault = !_state.value.isDefault) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        val config: Map<String, JsonElement> = when (s.type) {
            MissionType.MATH -> mapOf(
                "difficulty" to JsonPrimitive(s.difficulty),
                "questionCount" to JsonPrimitive(s.questionCount),
            )
            MissionType.PHOTO -> buildMap {
                put("mode", JsonPrimitive(s.photoMode))
                if (s.expectedCode.isNotBlank()) {
                    put("expectedCode", JsonPrimitive(s.expectedCode.trim()))
                }
            }
            MissionType.SHAKE -> mapOf("shakeCount" to JsonPrimitive(s.shakeCount))
        }
        _state.value = s.copy(saving = true, error = null)
        runCatching {
            if (s.editingId == null) {
                api.createMission(CreateMissionReq(s.type, s.name.ifBlank { "${s.type}" }, config, s.isDefault))
            } else {
                api.updateMission(
                    s.editingId,
                    com.jiny.finalalarm.data.api.UpdateMissionReq(
                        name = s.name.takeIf { it.isNotBlank() },
                        config = config,
                        isDefault = s.isDefault,
                    ),
                )
            }
        }.onSuccess { _state.value = _state.value.copy(saving = false, saved = true) }
            .onFailure { _state.value = _state.value.copy(saving = false, error = it.userMessage()) }
    }
}

@Composable
fun MissionEditScreen(nav: NavController, missionId: String?, vm: MissionEditVm = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(missionId) { if (missionId != null) vm.loadExisting(missionId) }
    LaunchedEffect(s.saved) { if (s.saved) nav.popBackStack() }

    WarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FaSpacing.sm, vertical = FaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { nav.popBackStack() }) { Text("뒤로") }
                Spacer(Modifier.weight(1f))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FaSpacing.lg),
            ) {
                HelloHeader(
                    title = if (missionId == null) "새 미션" else "미션 수정",
                    subtitle = "알람을 끄기 전에 풀어야 해요",
                )

                Section("이름") {
                    FaTextField(s.name, vm::onName, "미션 이름 (예: 아침 수학)")
                }

                Section("종류") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FaSpacing.sm),
                    ) {
                        MissionType.entries.forEach { t ->
                            ChoicePill(
                                text = when (t) {
                                    MissionType.MATH -> "수학"
                                    MissionType.PHOTO -> "사진"
                                    MissionType.SHAKE -> "흔들기"
                                },
                                selected = s.type == t,
                                onClick = { vm.onType(t) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                when (s.type) {
                    MissionType.MATH -> {
                        Section("난이도") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FaSpacing.sm),
                            ) {
                                listOf("easy" to "쉬움", "medium" to "보통", "hard" to "어려움").forEach { (v, l) ->
                                    ChoicePill(
                                        text = l,
                                        selected = s.difficulty == v,
                                        onClick = { vm.onDifficulty(v) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        Section("문제 수") {
                            Text(
                                "${s.questionCount}문제",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Slider(
                                value = s.questionCount.toFloat(),
                                onValueChange = { vm.onQuestionCount(it.toInt()) },
                                valueRange = 1f..10f, steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                    MissionType.PHOTO -> {
                        Section("모드") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FaSpacing.sm),
                            ) {
                                listOf("QR" to "QR", "BARCODE" to "바코드", "REFERENCE_IMAGE" to "기준 사진").forEach { (v, l) ->
                                    ChoicePill(
                                        text = l,
                                        selected = s.photoMode == v,
                                        onClick = { vm.onPhotoMode(v) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        if (s.photoMode == "QR" || s.photoMode == "BARCODE") {
                            Section("기대 코드 (선택)") {
                                FaTextField(s.expectedCode, vm::onExpectedCode, "비우면 어떤 코드든 통과")
                            }
                        }
                        if (s.photoMode == "REFERENCE_IMAGE") {
                            Section("기준 사진") {
                                var capturing by remember { mutableStateOf(false) }
                                if (s.expectedCode.isNotBlank()) {
                                    Text(
                                        "등록됨 (해시 ${s.expectedCode.take(8)}…)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    Text(
                                        "기준 사진이 등록되지 않았어요",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                Spacer(Modifier.height(FaSpacing.sm))
                                SecondaryButton(
                                    text = if (s.expectedCode.isBlank()) "기준 사진 촬영" else "다시 촬영",
                                    onClick = { capturing = true },
                                )
                                if (capturing) {
                                    androidx.compose.ui.window.Dialog(
                                        onDismissRequest = { capturing = false },
                                        properties = androidx.compose.ui.window.DialogProperties(
                                            usePlatformDefaultWidth = false,
                                        ),
                                    ) {
                                        Surface(modifier = Modifier.fillMaxSize()) {
                                            CameraCapture(
                                                onCancel = { capturing = false },
                                                onCapture = { bmp ->
                                                    val hex = ImageHash.toHex(ImageHash.aHash(bmp))
                                                    vm.onExpectedCode(hex)
                                                    capturing = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    MissionType.SHAKE -> {
                        Section("흔들기 횟수") {
                            Text(
                                "${s.shakeCount}회",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Slider(
                                value = s.shakeCount.toFloat(),
                                onValueChange = { vm.onShakeCount(it.toInt()) },
                                valueRange = 5f..100f, steps = 18,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }

                Section("옵션") {
                    ListRow(
                        headline = "기본 미션으로 설정",
                        supporting = "새 알람 만들 때 자동으로 선택돼요",
                        trailing = {
                            Switch(
                                checked = s.isDefault,
                                onCheckedChange = { vm.toggleDefault() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        },
                    )
                }

                s.error?.let { ErrorText(it) }
                Spacer(Modifier.height(FaSpacing.xxl))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FaSpacing.lg, vertical = FaSpacing.sm),
            ) {
                PrimaryButton(
                    text = if (s.saving) "저장 중…" else "저장",
                    onClick = vm::save,
                    enabled = !s.saving,
                )
            }
        }
    }
}

@Composable
private fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) FA.OnPrimary else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
