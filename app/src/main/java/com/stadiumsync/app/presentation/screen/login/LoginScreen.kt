package com.stadiumsync.app.presentation.screen.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stadiumsync.app.domain.model.*
import com.stadiumsync.app.domain.usecase.*
import com.stadiumsync.app.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.sin

enum class AuthScreen { LOGIN, REGISTER, FORGOT }
enum class AuthStatus { IDLE, LOADING, SUCCESS, ERROR, LOCKED }

data class LoginUiState(
    val screen: AuthScreen = AuthScreen.LOGIN,
    val email: String = "", val password: String = "",
    val name: String = "", val confirmPassword: String = "",
    val badgeId: String = "", val department: String = "",
    val selectedRole: UserRole = UserRole.OPERATOR,
    val forgotEmail: String = "",
    val status: AuthStatus = AuthStatus.IDLE,
    val error: String = "",
    val lockoutSeconds: Int = 0,
    val user: User? = null,
    val forgotSent: Boolean = false,
    val passwordStrength: Int = 0   // 0-4
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val forgotUseCase: ForgotPasswordUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun setScreen(s: AuthScreen) = _state.update { it.copy(screen = s, error = "", status = AuthStatus.IDLE) }
    fun setEmail(v: String) = _state.update { it.copy(email = v) }
    fun setPassword(v: String) = _state.update { it.copy(password = v, passwordStrength = calcStrength(v)) }
    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setConfirmPwd(v: String) = _state.update { it.copy(confirmPassword = v) }
    fun setBadgeId(v: String) = _state.update { it.copy(badgeId = v) }
    fun setDepartment(v: String) = _state.update { it.copy(department = v) }
    fun setRole(r: UserRole) = _state.update { it.copy(selectedRole = r) }
    fun setForgotEmail(v: String) = _state.update { it.copy(forgotEmail = v) }

    private fun calcStrength(pwd: String): Int {
        var score = 0
        if (pwd.length >= 8) score++
        if (pwd.any { it.isUpperCase() }) score++
        if (pwd.any { it.isDigit() }) score++
        if (pwd.any { !it.isLetterOrDigit() }) score++
        return score
    }

    fun login() = viewModelScope.launch {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) { _state.update { it.copy(error = "Please fill all fields") }; return@launch }
        _state.update { it.copy(status = AuthStatus.LOADING, error = "") }
        loginUseCase(s.email.trim(), s.password)
            .onSuccess { user -> _state.update { it.copy(status = AuthStatus.SUCCESS, user = user) } }
            .onFailure { e ->
                val msg = e.message ?: ""
                when {
                    msg.startsWith("LOCKED:") -> {
                        val sec = msg.substringAfter(":").toIntOrNull() ?: 30
                        _state.update { it.copy(status = AuthStatus.LOCKED, lockoutSeconds = sec, error = "Account locked. Try again in ${sec}s") }
                        startLockoutCountdown()
                    }
                    msg.startsWith("INVALID_CREDENTIALS:") -> {
                        val attempts = msg.substringAfter(":").toIntOrNull() ?: 1
                        val remaining = 3 - attempts
                        _state.update { it.copy(status = AuthStatus.ERROR, error = "Invalid credentials. $remaining attempt${if(remaining!=1)"s" else ""} remaining.") }
                    }
                    else -> _state.update { it.copy(status = AuthStatus.ERROR, error = "Invalid email or password.") }
                }
            }
    }

    fun register() = viewModelScope.launch {
        val s = _state.value
        if (s.name.isBlank() || s.email.isBlank() || s.password.isBlank()) { _state.update { it.copy(error = "Please fill all required fields") }; return@launch }
        if (s.password != s.confirmPassword) { _state.update { it.copy(error = "Passwords do not match") }; return@launch }
        _state.update { it.copy(status = AuthStatus.LOADING, error = "") }
        registerUseCase(s.name.trim(), s.email.trim(), s.password, s.selectedRole, s.badgeId.trim(), s.department.trim())
            .onSuccess { user -> _state.update { it.copy(status = AuthStatus.SUCCESS, user = user) } }
            .onFailure { e ->
                val msg = e.message ?: ""
                val errorMsg = when {
                    msg.contains("EMAIL_ALREADY_EXISTS") -> "This email is already registered."
                    msg.contains("VALIDATION_ERROR:") -> msg.substringAfter("VALIDATION_ERROR:")
                    else -> "Registration failed. Please try again."
                }
                _state.update { it.copy(status = AuthStatus.ERROR, error = errorMsg) }
            }
    }

    fun forgotPassword() = viewModelScope.launch {
        val s = _state.value
        if (s.forgotEmail.isBlank()) { _state.update { it.copy(error = "Enter your email address") }; return@launch }
        _state.update { it.copy(status = AuthStatus.LOADING, error = "") }
        forgotUseCase(s.forgotEmail.trim())
            .onSuccess { _state.update { it.copy(status = AuthStatus.IDLE, forgotSent = true) } }
            .onFailure { _state.update { it.copy(status = AuthStatus.ERROR, error = "Failed to send reset link.") } }
    }

    private fun startLockoutCountdown() = viewModelScope.launch {
        while (_state.value.lockoutSeconds > 0) {
            delay(1000)
            _state.update { it.copy(lockoutSeconds = it.lockoutSeconds - 1) }
        }
        _state.update { it.copy(status = AuthStatus.IDLE, error = "") }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, vm: LoginViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state.user) { if (state.user != null) onLoginSuccess() }

    val inf = rememberInfiniteTransition(label = "bg")
    val phase by inf.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "p")

    Box(Modifier.fillMaxSize().background(Obsidian)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            for (layer in 0..2) {
                val amp = 30f + layer * 15f; val yBase = h * 0.25f + layer * h * 0.15f
                val color = when(layer) { 0 -> VividBlue; 1 -> ElectricCyan; else -> DeepViolet }
                val path = Path(); path.moveTo(0f, yBase)
                for (x in 0..w.toInt() step 4) { val xf = x.toFloat(); path.lineTo(xf, yBase + sin((xf/w*4+phase+layer).toDouble()).toFloat()*amp) }
                path.lineTo(w,h); path.lineTo(0f,h); path.close()
                drawPath(path, color.copy(alpha = 0.04f + layer * 0.02f))
                val lp = Path(); lp.moveTo(0f, yBase)
                for (x in 0..w.toInt() step 4) { val xf = x.toFloat(); lp.lineTo(xf, yBase + sin((xf/w*4+phase+layer).toDouble()).toFloat()*amp) }
                drawPath(lp, color.copy(alpha = 0.15f), style = Stroke(1.5f))
            }
        }
        when (state.screen) {
            AuthScreen.LOGIN -> LoginForm(state, vm, onLoginSuccess)
            AuthScreen.REGISTER -> RegisterForm(state, vm)
            AuthScreen.FORGOT -> ForgotForm(state, vm)
        }
    }
}

@Composable
private fun LoginForm(state: LoginUiState, vm: LoginViewModel, onSkip: () -> Unit) {
    var showPwd by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("Stadium", style = MaterialTheme.typography.displayLarge.copy(color = Color.White, lineHeight = 48.sp))
        Text("Sync", style = MaterialTheme.typography.displayLarge.copy(color = ElectricCyan, lineHeight = 48.sp))
        Text("MOBILITY CONTROL PLATFORM", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp), color = MutedText)
        Spacer(Modifier.height(48.dp))

        AuthFieldLabel("EMAIL")
        AuthTextField(state.email, vm::setEmail, "operator@stadiumsync.in", Icons.Filled.AlternateEmail, KeyboardType.Email)
        Spacer(Modifier.height(18.dp))

        AuthFieldLabel("PASSWORD")
        AuthTextField(state.password, vm::setPassword, "••••••••", Icons.Filled.Lock, KeyboardType.Password,
            showToggle = true, showValue = showPwd, onToggle = { showPwd = !showPwd })

        AnimatedVisibility(state.error.isNotEmpty()) {
            Column {
                Spacer(Modifier.height(8.dp))
                Text(state.error, color = HotCoral, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (state.status == AuthStatus.LOCKED && state.lockoutSeconds > 0) {
            Spacer(Modifier.height(4.dp))
            Text("Unlocks in ${state.lockoutSeconds}s", color = SolarAmber, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { vm.setScreen(AuthScreen.FORGOT) }, contentPadding = PaddingValues(0.dp)) {
            Text("Forgot password?", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
        }
        Spacer(Modifier.height(20.dp))

        Button(onClick = vm::login, modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = state.email.isNotBlank() && state.password.isNotBlank() && state.status != AuthStatus.LOADING && state.status != AuthStatus.LOCKED,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent)) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(if (state.email.isNotBlank() && state.password.isNotBlank()) CyanGlow else Brush.linearGradient(listOf(Zinc, Zinc))), contentAlignment = Alignment.Center) {
                if (state.status == AuthStatus.LOADING) CircularProgressIndicator(Modifier.size(20.dp), color = Obsidian, strokeWidth = 2.dp)
                else Text("Sign In", style = MaterialTheme.typography.labelLarge, color = Obsidian)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { vm.setScreen(AuthScreen.REGISTER) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, VividBlue.copy(0.4f))) {
            Text("Create Account", color = VividBlue, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.CloudOff, null, tint = MutedText, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Continue Offline", style = MaterialTheme.typography.labelMedium, color = MutedText)
        }
        Spacer(Modifier.height(24.dp))
        // Demo credentials hint
        Surface(shape = RoundedCornerShape(10.dp), color = Zinc.copy(0.15f)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Demo Credentials", style = MaterialTheme.typography.labelSmall, color = MutedText)
                Text("admin@stadiumsync.in  /  Admin@123", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                Text("operator@stadiumsync.in  /  Op3r@t0r", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                Text("transit@stadiumsync.in  /  Tr4ns!t", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
            }
        }
    }
}

@Composable
private fun RegisterForm(state: LoginUiState, vm: LoginViewModel) {
    val roles = listOf(UserRole.OPERATOR, UserRole.TRANSIT_OFFICER, UserRole.VIEWER)
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(32.dp)) {
        Spacer(Modifier.height(24.dp))
        IconButton(onClick = { vm.setScreen(AuthScreen.LOGIN) }) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
        Spacer(Modifier.height(8.dp))
        Text("Create", style = MaterialTheme.typography.displaySmall.copy(color = Color.White))
        Text("Account", style = MaterialTheme.typography.displaySmall.copy(color = ElectricCyan))
        Spacer(Modifier.height(32.dp))

        AuthFieldLabel("FULL NAME *")
        AuthTextField(state.name, vm::setName, "e.g. Rahul Sharma", Icons.Filled.Person)
        Spacer(Modifier.height(14.dp))

        AuthFieldLabel("EMAIL *")
        AuthTextField(state.email, vm::setEmail, "you@stadiumsync.in", Icons.Filled.AlternateEmail, KeyboardType.Email)
        Spacer(Modifier.height(14.dp))

        AuthFieldLabel("PASSWORD *")
        var showPwd by remember { mutableStateOf(false) }
        AuthTextField(state.password, vm::setPassword, "Min 8 chars, uppercase, digit, symbol", Icons.Filled.Lock, KeyboardType.Password,
            showToggle = true, showValue = showPwd, onToggle = { showPwd = !showPwd })
        // Strength indicator
        if (state.password.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val labels = listOf("Weak","Fair","Good","Strong")
                val colors = listOf(HotCoral, SolarAmber, VividBlue, ElectricCyan)
                repeat(4) { i ->
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i < state.passwordStrength) colors[state.passwordStrength - 1] else Zinc))
                }
                Spacer(Modifier.width(8.dp))
                if (state.passwordStrength > 0) Text(labels[state.passwordStrength - 1], style = MaterialTheme.typography.labelSmall, color = colors[state.passwordStrength - 1])
            }
        }
        Spacer(Modifier.height(14.dp))

        AuthFieldLabel("CONFIRM PASSWORD *")
        var showConfirm by remember { mutableStateOf(false) }
        AuthTextField(state.confirmPassword, vm::setConfirmPwd, "Re-enter password", Icons.Filled.Lock, KeyboardType.Password,
            showToggle = true, showValue = showConfirm, onToggle = { showConfirm = !showConfirm })
        if (state.confirmPassword.isNotEmpty() && state.password != state.confirmPassword) {
            Text("Passwords do not match", color = HotCoral, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(14.dp))

        AuthFieldLabel("BADGE ID")
        AuthTextField(state.badgeId, vm::setBadgeId, "e.g. OPS-042", Icons.Filled.Badge)
        Spacer(Modifier.height(14.dp))

        AuthFieldLabel("DEPARTMENT")
        AuthTextField(state.department, vm::setDepartment, "e.g. Operations", Icons.Filled.Business)
        Spacer(Modifier.height(14.dp))

        AuthFieldLabel("ROLE")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            roles.forEach { role ->
                val sel = state.selectedRole == role
                FilterChip(selected = sel, onClick = { vm.setRole(role) },
                    label = { Text(role.name.replace("_"," "), style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VividBlue.copy(0.2f), selectedLabelColor = VividBlue))
            }
        }
        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(state.error.isNotEmpty()) {
            Text(state.error, color = HotCoral, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(20.dp))

        Button(onClick = vm::register, modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = state.status != AuthStatus.LOADING,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent)) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(CyanGlow), contentAlignment = Alignment.Center) {
                if (state.status == AuthStatus.LOADING) CircularProgressIndicator(Modifier.size(20.dp), color = Obsidian, strokeWidth = 2.dp)
                else Text("Create Account", style = MaterialTheme.typography.labelLarge, color = Obsidian)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ForgotForm(state: LoginUiState, vm: LoginViewModel) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(32.dp), verticalArrangement = Arrangement.Center) {
        IconButton(onClick = { vm.setScreen(AuthScreen.LOGIN) }) { Icon(Icons.Filled.ArrowBack, null, tint = Color.White) }
        Spacer(Modifier.height(16.dp))
        Text("Reset", style = MaterialTheme.typography.displaySmall.copy(color = Color.White))
        Text("Password", style = MaterialTheme.typography.displaySmall.copy(color = ElectricCyan))
        Spacer(Modifier.height(8.dp))
        Text("Enter your registered email and we'll send you a reset link.", style = MaterialTheme.typography.bodyMedium, color = MutedText)
        Spacer(Modifier.height(32.dp))

        if (state.forgotSent) {
            Surface(shape = RoundedCornerShape(12.dp), color = ElectricCyan.copy(0.1f)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = ElectricCyan)
                    Column {
                        Text("Reset link sent!", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                        Text("Check your inbox at ${state.forgotEmail}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { vm.setScreen(AuthScreen.LOGIN) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Back to Login")
            }
        } else {
            AuthFieldLabel("EMAIL ADDRESS")
            AuthTextField(state.forgotEmail, vm::setForgotEmail, "your@email.com", Icons.Filled.AlternateEmail, KeyboardType.Email)
            AnimatedVisibility(state.error.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(state.error, color = HotCoral, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = vm::forgotPassword, modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = state.forgotEmail.isNotBlank() && state.status != AuthStatus.LOADING,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(CyanGlow), contentAlignment = Alignment.Center) {
                    if (state.status == AuthStatus.LOADING) CircularProgressIndicator(Modifier.size(20.dp), color = Obsidian, strokeWidth = 2.dp)
                    else Text("Send Reset Link", style = MaterialTheme.typography.labelLarge, color = Obsidian)
                }
            }
        }
    }
}

// ─── Shared UI helpers ────────────────────────────────────
@Composable
private fun AuthFieldLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp), color = MutedText)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun AuthTextField(
    value: String, onValue: (String) -> Unit, placeholder: String,
    leadIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    showToggle: Boolean = false, showValue: Boolean = true, onToggle: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValue, singleLine = true,
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        placeholder = { Text(placeholder, color = Zinc) },
        leadingIcon = { Icon(leadIcon, null, tint = MutedText, modifier = Modifier.size(18.dp)) },
        trailingIcon = if (showToggle) ({
            IconButton(onClick = { onToggle?.invoke() }) {
                Icon(if (showValue) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = MutedText)
            }
        }) else null,
        visualTransformation = if (!showValue && keyboardType == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Zinc, focusedBorderColor = ElectricCyan,
            unfocusedTextColor = Color.White, focusedTextColor = Color.White, cursorColor = ElectricCyan
        )
    )
}
