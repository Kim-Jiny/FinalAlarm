package com.jiny.finalalarm.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jiny.finalalarm.core.network.userMessage
import com.jiny.finalalarm.data.AuthRepository
import com.jiny.finalalarm.ui.Routes
import com.jiny.finalalarm.ui.components.ErrorText
import com.jiny.finalalarm.ui.components.FaTextField
import com.jiny.finalalarm.ui.components.PrimaryButton
import com.jiny.finalalarm.ui.components.SecondaryButton
import com.jiny.finalalarm.ui.theme.FaSpacing
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
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage()) }
    }

    fun login() = viewModelScope.launch {
        val s = _state.value
        _state.value = s.copy(loading = true, error = null)
        runCatching { repo.login(s.email, s.password) }
            .onSuccess { _state.value = _state.value.copy(loading = false, success = true) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.userMessage()) }
    }
}

@Composable
fun LoginScreen(nav: NavController, vm: AuthVm = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.success) {
        if (s.success) {
            nav.navigate(Routes.ONBOARDING) { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = FaSpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text("FinalAlarm", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(FaSpacing.sm))
        Text(
            "함께 일어나기.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(FaSpacing.xxl))

        FaTextField(s.email, vm::onEmail, "이메일")
        Spacer(Modifier.height(FaSpacing.sm))
        FaTextField(
            s.password, vm::onPassword, "비밀번호",
            visualTransformation = PasswordVisualTransformation(),
        )

        if (s.error != null) ErrorText(s.error!!)

        Spacer(Modifier.height(FaSpacing.lg))
        PrimaryButton(
            text = if (s.loading) "로그인 중…" else "로그인",
            onClick = vm::login,
            enabled = !s.loading && s.email.isNotBlank() && s.password.isNotBlank(),
        )
        Spacer(Modifier.height(FaSpacing.sm))
        SecondaryButton(
            text = "계정 만들기",
            onClick = { nav.navigate(Routes.SIGNUP) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SignupScreen(nav: NavController, vm: AuthVm = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.success) {
        if (s.success) {
            nav.navigate(Routes.ONBOARDING) { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = FaSpacing.lg),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("계정 만들기", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(FaSpacing.xxl))

        FaTextField(s.displayName, vm::onName, "이름")
        Spacer(Modifier.height(FaSpacing.sm))
        FaTextField(s.email, vm::onEmail, "이메일")
        Spacer(Modifier.height(FaSpacing.sm))
        FaTextField(
            s.password, vm::onPassword, "비밀번호 (8자 이상)",
            visualTransformation = PasswordVisualTransformation(),
        )

        if (s.error != null) ErrorText(s.error!!)

        Spacer(Modifier.height(FaSpacing.lg))
        PrimaryButton(
            text = if (s.loading) "가입 중…" else "가입",
            onClick = vm::signup,
            enabled = !s.loading && s.email.isNotBlank() && s.password.length >= 8 && s.displayName.isNotBlank(),
        )
        Spacer(Modifier.height(FaSpacing.sm))
        SecondaryButton(
            text = "뒤로",
            onClick = { nav.popBackStack() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
