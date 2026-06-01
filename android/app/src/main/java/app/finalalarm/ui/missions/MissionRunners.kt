package app.finalalarm.ui.missions

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import kotlin.random.Random

// ---- 수학 미션 ----
data class MathQuestion(val a: Int, val b: Int, val op: Char) {
    val answer: Int = when (op) { '+' -> a + b; '-' -> a - b; else -> a * b }
}

fun makeMathQuestions(difficulty: String, count: Int): List<MathQuestion> {
    val range = when (difficulty) {
        "easy" -> 1..10
        "medium" -> 5..50
        "hard" -> 10..200
        else -> 5..50
    }
    val ops = if (difficulty == "easy") listOf('+', '-') else listOf('+', '-', '×')
    return List(count) {
        MathQuestion(Random.nextInt(range.first, range.last), Random.nextInt(range.first, range.last), ops.random())
    }
}

@Composable
fun MathMissionRunner(
    difficulty: String,
    questionCount: Int,
    onComplete: (answers: List<Int>) -> Unit,
) {
    val questions = remember(difficulty, questionCount) { makeMathQuestions(difficulty, questionCount) }
    var index by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    val answers = remember { mutableListOf<Int>() }

    if (index >= questions.size) {
        LaunchedEffect(Unit) { onComplete(answers.toList()) }
        return
    }
    val q = questions[index]
    Column(modifier = Modifier.padding(24.dp)) {
        Text("${index + 1}/${questions.size}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Text("${q.a} ${q.op} ${q.b} = ?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.filter { c -> c.isDigit() || c == '-' } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = input.isNotEmpty(),
            onClick = {
                val ans = input.toIntOrNull() ?: return@Button
                if (ans == q.answer) {
                    answers += ans
                    input = ""
                    index += 1
                } else {
                    input = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("확인") }
    }
}

// ---- 흔들기 미션 ----
@Composable
fun ShakeMissionRunner(targetCount: Int, onComplete: (count: Int) -> Unit) {
    val ctx = LocalContext.current
    var count by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShake = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val x = e.values[0]; val y = e.values[1]; val z = e.values[2]
                val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                if (g > 2.5) {
                    val now = System.currentTimeMillis()
                    if (now - lastShake > 250) {
                        lastShake = now
                        count += 1
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
    LaunchedEffect(count) {
        if (count >= targetCount) onComplete(count)
    }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("흔들어 주세요", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = { (count.toFloat() / targetCount).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text("$count / $targetCount")
    }
}

// ---- 사진 미션 (QR/Barcode) ----
// CameraX 미리보기 + MLKit BarcodeScanner. expectedCode와 매칭되면 미션 완료.
@Composable
fun PhotoMissionRunner(
    mode: String,
    expectedCode: String?,
    onComplete: (imageUrlOrCode: String) -> Unit,
) {
    when (mode) {
        "QR", "BARCODE" -> BarcodeMissionRunner(mode, expectedCode, onComplete)
        else -> ReferenceImageMissionRunner(onComplete)
    }
}

@Composable
private fun ReferenceImageMissionRunner(onComplete: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("사진 인증 (사물)", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("이미지 매칭은 추후 구현 — 지금은 더미 통과")
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onComplete("dummy://mission-reference-image") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("통과 (임시)") }
    }
}
