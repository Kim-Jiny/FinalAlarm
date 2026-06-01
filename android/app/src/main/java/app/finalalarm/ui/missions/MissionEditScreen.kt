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
    val type: MissionType = MissionType.MATH,
    val name: String = "",
    val difficulty: String = "medium",
    val questionCount: Int = 3,
    val photoMode: String = "QR",
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

    fun onType(t: MissionType) { _state.value = _state.value.copy(type = t) }
    fun onName(v: String) { _state.value = _state.value.copy(name = v) }
    fun onDifficulty(v: String) { _state.value = _state.value.copy(difficulty = v) }
    fun onQuestionCount(v: Int) { _state.value = _state.value.copy(questionCount = v) }
    fun onPhotoMode(v: String) { _state.value = _state.value.copy(photoMode = v) }
    fun onShakeCount(v: Int) { _state.value = _state.value.copy(shakeCount = v) }
    fun toggleDefault() { _state.value = _state.value.copy(isDefault = !_state.value.isDefault) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        val config: Map<String, JsonElement> = when (s.type) {
            MissionType.MATH -> mapOf(
                "difficulty" to JsonPrimitive(s.difficulty),
                "questionCount" to JsonPrimitive(s.questionCount),
            )
            MissionType.PHOTO -> mapOf("mode" to JsonPrimitive(s.photoMode))
            MissionType.SHAKE -> mapOf("shakeCount" to JsonPrimitive(s.shakeCount))
        }
        _state.value = s.copy(saving = true, error = null)
        runCatching {
            api.createMission(CreateMissionReq(s.type, s.name.ifBlank { "${s.type}" }, config, s.isDefault))
        }.onSuccess { _state.value = _state.value.copy(saving = false, saved = true) }
            .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
    }
}

@Composable
fun MissionEditScreen(nav: NavController, missionId: String?, vm: MissionEditVm = hiltViewModel()) {
    val s by vm.state.collectAsState()
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
