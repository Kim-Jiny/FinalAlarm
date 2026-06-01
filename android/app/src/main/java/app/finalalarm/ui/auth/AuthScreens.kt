package app.finalalarm.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import app.finalalarm.data.AuthRepository
import app.finalalarm.ui.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

@HiltViewModel
class AuthVm @Inject constructor(private val repo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    fun onEmail(v: String) { _state.value = _state.value.copy(email = v) }
    fun onPassword(v: String) { _state.value = _state.value.copy(password = v) }
    fun onName(v: String) { _state.value = _state.value.copy(displayName = v) }

    fun signup() = viewModelScope.launch {
        val s = _state.value
        _state.value = s.copy(loading = true, error = null)
        runCatching { repo.signup(s.email, s.password, s.displayName, TimeZone.getDefault().id) }
            .onSuccess { _state.value = _state.value.copy(loading = false, success = true) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
    }

    fun login() = viewModelScope.launch {
        val s = _state.value
        _state.value = s.copy(loading = true, error = null)
        runCatching { repo.login(s.email, s.password) }
            .onSuccess { _state.value = _state.value.copy(loading = false, success = true) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
    }
}

@Composable
fun LoginScreen(nav: NavController, vm: AuthVm = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state.success) {
        if (state.success) {
            nav.navigate(Routes.ONBOARDING) { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("FinalAlarm", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(state.email, vm::onEmail, label = { Text("이메일") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(state.password, vm::onPassword, label = { Text("비밀번호") },
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = vm::login, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.loading) "로그인 중…" else "로그인")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { nav.navigate(Routes.SIGNUP) }, modifier = Modifier.fillMaxWidth()) {
            Text("계정 없으세요? 가입하기")
        }
        state.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun SignupScreen(nav: NavController, vm: AuthVm = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state.success) {
        if (state.success) {
            nav.navigate(Routes.ONBOARDING) { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("가입", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(state.displayName, vm::onName, label = { Text("이름") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(state.email, vm::onEmail, label = { Text("이메일") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(state.password, vm::onPassword, label = { Text("비밀번호 (8자 이상)") },
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = vm::signup, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.loading) "가입 중…" else "가입하기")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { nav.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
            Text("이미 계정이 있어요")
        }
        state.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
