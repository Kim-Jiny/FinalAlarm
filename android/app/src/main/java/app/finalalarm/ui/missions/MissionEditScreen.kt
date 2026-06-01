package app.finalalarm.ui.missions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import app.finalalarm.core.network.userMessage
import app.finalalarm.data.api.CreateMissionReq
import app.finalalarm.data.api.FinalAlarmApi
import app.finalalarm.data.api.MissionType
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
                    // QR/BARCODE: 기대 raw value. REFERENCE_IMAGE: aHash hex.
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
                    app.finalalarm.data.api.UpdateMissionReq(
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

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (missionId == null) "미션 추가" else "미션 편집") }) },
        bottomBar = {
            Button(onClick = vm::save, enabled = !s.saving, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(if (s.saving) "저장 중…" else "저장")
            }
        },
    ) { inner ->
        Column(modifier = Modifier.padding(inner).padding(16.dp)) {
            OutlinedTextField(s.name, vm::onName, label = { Text("미션 이름") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text("미션 종류", style = MaterialTheme.typography.titleSmall)
            Row {
                MissionType.entries.forEach { t ->
                    FilterChip(
                        selected = s.type == t,
                        onClick = { vm.onType(t) },
                        label = { Text(t.name) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            when (s.type) {
                MissionType.MATH -> {
                    Text("난이도")
                    Row {
                        listOf("easy", "medium", "hard").forEach {
                            FilterChip(
                                selected = s.difficulty == it,
                                onClick = { vm.onDifficulty(it) },
                                label = { Text(it) },
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("문제 수: ${s.questionCount}")
                    Slider(
                        value = s.questionCount.toFloat(),
                        onValueChange = { vm.onQuestionCount(it.toInt()) },
                        valueRange = 1f..10f, steps = 8,
                    )
                }
                MissionType.PHOTO -> {
                    Text("모드")
                    Row {
                        listOf("QR", "BARCODE", "REFERENCE_IMAGE").forEach {
                            FilterChip(
                                selected = s.photoMode == it,
                                onClick = { vm.onPhotoMode(it) },
                                label = { Text(it) },
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                    }
                    if (s.photoMode == "QR" || s.photoMode == "BARCODE") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = s.expectedCode,
                            onValueChange = vm::onExpectedCode,
                            label = { Text("기대 코드 (비우면 어떤 코드든 통과)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (s.photoMode == "REFERENCE_IMAGE") {
                        Spacer(Modifier.height(8.dp))
                        var capturing by remember { mutableStateOf(false) }
                        if (s.expectedCode.isNotBlank()) {
                            Text("기준 사진 등록됨 (해시: ${s.expectedCode.take(8)}…)")
                        } else {
                            Text("기준 사진이 등록되지 않았습니다.", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = { capturing = true }) {
                            Text(if (s.expectedCode.isBlank()) "기준 사진 촬영" else "기준 사진 다시 촬영")
                        }
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
                MissionType.SHAKE -> {
                    Text("흔들기 횟수: ${s.shakeCount}")
                    Slider(
                        value = s.shakeCount.toFloat(),
                        onValueChange = { vm.onShakeCount(it.toInt()) },
                        valueRange = 5f..100f, steps = 18,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Switch(checked = s.isDefault, onCheckedChange = { vm.toggleDefault() })
                Spacer(Modifier.width(8.dp))
                Text("기본 미션으로 설정")
            }
            s.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
