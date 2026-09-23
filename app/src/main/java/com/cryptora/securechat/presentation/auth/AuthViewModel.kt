package com.cryptora.securechat.presentation.auth

import androidx.lifecycle.viewModelScope
import com.cryptora.securechat.core.common.DispatcherProvider
import com.cryptora.securechat.core.navigation.AppNavigator
import com.cryptora.securechat.core.navigation.Screen
import com.cryptora.securechat.domain.model.CountryCode
import com.cryptora.securechat.domain.model.SupportedCountryCodes
import com.cryptora.securechat.domain.usecase.auth.CheckUsernameAvailabilityUseCase
import com.cryptora.securechat.domain.usecase.auth.LoginUserUseCase
import com.cryptora.securechat.domain.usecase.auth.RegisterUserUseCase
import com.cryptora.securechat.domain.usecase.auth.SendOtpUseCase
import com.cryptora.securechat.domain.usecase.auth.VerifyOtpUseCase
import com.cryptora.securechat.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode {
    LOGIN,
    REGISTER
}

enum class RegisterStep {
    NAME_USERNAME,
    MOBILE_NUMBER,
    OTP_VERIFICATION,
    PASSWORD_CREATION,
    PROFILE_SETUP
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val registerStep: RegisterStep = RegisterStep.NAME_USERNAME,

    // Inputs
    val fullNameInput: String = "",
    val usernameInput: String = "",
    val isCheckingUsername: Boolean = false,
    val isUsernameAvailable: Boolean? = null,

    val selectedCountryCode: CountryCode = SupportedCountryCodes.default,
    val mobileNumberInput: String = "",

    val otpSessionId: String = "",
    val otpInput: String = "",
    val otpCountdown: Int = 0,
    val canResendOtp: Boolean = false,

    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val isPasswordVisible: Boolean = false,

    val selectedAvatar: String = "🛡️",
    val bioInput: String = "",

    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sendOtpUseCase: SendOtpUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val checkUsernameAvailabilityUseCase: CheckUsernameAvailabilityUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val otpProvider: com.cryptora.securechat.core.network.otp.OtpProvider,
    navigator: AppNavigator,
    dispatchers: DispatcherProvider
) : BaseViewModel(dispatchers, navigator) {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    val avatarOptions = listOf("🛡️", "🔒", "⚡", "🚀", "👤", "💼", "🌐", "🦊")

    init {
        viewModelScope.launch {
            if (otpProvider is com.cryptora.securechat.core.network.otp.FirebasePhoneOtpProvider) {
                otpProvider.autoDetectedSmsCode.collect { autoCode ->
                    if (!autoCode.isNullOrBlank() && _uiState.value.registerStep == RegisterStep.OTP_VERIFICATION) {
                        onOtpChanged(autoCode)
                        otpProvider.clearAutoDetectedCode()
                    }
                }
            }
        }
    }

    fun switchMode(mode: AuthMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                registerStep = RegisterStep.NAME_USERNAME,
                errorMessage = null,
                isLoading = false
            )
        }
    }

    // ==================== LOGIN ACTIONS ====================

    fun onLoginUsernameChanged(username: String) {
        _uiState.update { it.copy(usernameInput = username, errorMessage = null) }
    }

    fun onLoginPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun submitLogin() {
        val username = _uiState.value.usernameInput.trim()
        val password = _uiState.value.passwordInput

        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter both username and password") }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = loginUserUseCase(username, password)
                result.onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    navigator.popUpTo(Screen.Auth.route, inclusive = true)
                    navigator.navigateTo(Screen.Home.route)
                }.onError { error, _ ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Sign in failed. Please retry.") }
            }
        }
    }

    // ==================== REGISTRATION STEP 1: NAME & USERNAME ====================

    fun onFullNameChanged(name: String) {
        _uiState.update { it.copy(fullNameInput = name, errorMessage = null) }
    }

    fun onRegisterUsernameChanged(username: String) {
        val clean = username.lowercase().replace(" ", "")
        _uiState.update {
            it.copy(
                usernameInput = clean,
                errorMessage = null,
                isUsernameAvailable = null
            )
        }
        checkUsernameDebounced(clean)
    }

    private var usernameCheckJob: Job? = null

    private fun checkUsernameDebounced(username: String) {
        usernameCheckJob?.cancel()
        if (username.length < 3) return

        usernameCheckJob = viewModelScope.launch(dispatchers.io) {
            delay(400)
            _uiState.update { it.copy(isCheckingUsername = true) }
            val result = checkUsernameAvailabilityUseCase(username)
            result.onSuccess { available ->
                _uiState.update { it.copy(isCheckingUsername = false, isUsernameAvailable = available) }
            }.onError { _, _ ->
                _uiState.update { it.copy(isCheckingUsername = false, isUsernameAvailable = null) }
            }
        }
    }

    fun submitStep1() {
        val name = _uiState.value.fullNameInput.trim()
        val username = _uiState.value.usernameInput.trim()
        val isAvailable = _uiState.value.isUsernameAvailable

        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your full name") }
            return
        }
        if (username.length < 3) {
            _uiState.update { it.copy(errorMessage = "Username must be at least 3 characters") }
            return
        }
        if (isAvailable == false) {
            _uiState.update { it.copy(errorMessage = "Username is already taken. Please choose another.") }
            return
        }

        _uiState.update {
            it.copy(
                registerStep = RegisterStep.MOBILE_NUMBER,
                errorMessage = null
            )
        }
    }

    // ==================== REGISTRATION STEP 2: MOBILE & COUNTRY CODE ====================

    fun onCountryCodeSelected(country: CountryCode) {
        _uiState.update { it.copy(selectedCountryCode = country) }
    }

    fun onMobileNumberChanged(number: String) {
        val clean = number.filter { it.isDigit() }
        _uiState.update { it.copy(mobileNumberInput = clean, errorMessage = null) }
    }

    fun submitSendOtp() {
        val mobile = _uiState.value.mobileNumberInput
        val country = _uiState.value.selectedCountryCode.dialCode

        if (mobile.length < 7) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid mobile number") }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = sendOtpUseCase(mobile, country)
                result.onSuccess { sessionId ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            otpSessionId = sessionId,
                            registerStep = RegisterStep.OTP_VERIFICATION,
                            otpInput = ""
                        )
                    }
                    startOtpCountdown()
                }.onError { error, _ ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to dispatch verification code.") }
            }
        }
    }

    // ==================== REGISTRATION STEP 3: OTP VERIFICATION ====================

    fun onOtpChanged(otp: String) {
        val clean = otp.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(otpInput = clean, errorMessage = null) }
        if (clean.length == 6) {
            submitVerifyOtp()
        }
    }

    fun resendOtp() {
        if (!_uiState.value.canResendOtp) return
        submitSendOtp()
    }

    fun submitVerifyOtp() {
        val otp = _uiState.value.otpInput
        val sessionId = _uiState.value.otpSessionId

        if (otp.length != 6) {
            _uiState.update { it.copy(errorMessage = "Enter the 6-digit verification code") }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = verifyOtpUseCase(sessionId, otp)
                result.onSuccess {
                    countdownJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            registerStep = RegisterStep.PASSWORD_CREATION,
                            errorMessage = null
                        )
                    }
                }.onError { error, _ ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Verification failed.") }
            }
        }
    }

    private fun startOtpCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch(dispatchers.default) {
            _uiState.update { it.copy(otpCountdown = 60, canResendOtp = false) }
            for (sec in 59 downTo 0) {
                delay(1000)
                _uiState.update { it.copy(otpCountdown = sec) }
            }
            _uiState.update { it.copy(canResendOtp = true) }
        }
    }

    // ==================== REGISTRATION STEP 4: PASSWORD ====================

    fun onRegisterPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(confirmPasswordInput = password, errorMessage = null) }
    }

    fun submitPasswordStep() {
        val p1 = _uiState.value.passwordInput
        val p2 = _uiState.value.confirmPasswordInput

        if (p1.length < 8) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 8 characters") }
            return
        }
        if (!p1.any { it.isUpperCase() } || !p1.any { it.isDigit() }) {
            _uiState.update { it.copy(errorMessage = "Password must contain at least one uppercase letter and one number") }
            return
        }
        if (p1 != p2) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        _uiState.update {
            it.copy(
                registerStep = RegisterStep.PROFILE_SETUP,
                errorMessage = null
            )
        }
    }

    // ==================== REGISTRATION STEP 5: PROFILE & FINALIZE ====================

    fun onAvatarSelected(avatar: String) {
        _uiState.update { it.copy(selectedAvatar = avatar) }
    }

    fun onBioChanged(bio: String) {
        _uiState.update { it.copy(bioInput = bio) }
    }

    fun submitCompleteRegistration() {
        val state = _uiState.value
        val fullMobile = "${state.selectedCountryCode.dialCode} ${state.mobileNumberInput}"

        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = registerUserUseCase(
                    fullName = state.fullNameInput,
                    username = state.usernameInput,
                    mobileNumber = fullMobile,
                    password = state.passwordInput,
                    avatarUrl = state.selectedAvatar,
                    bio = state.bioInput
                )

                result.onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    navigator.popUpTo(Screen.Auth.route, inclusive = true)
                    navigator.navigateTo(Screen.Home.route)
                }.onError { error, _ ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Registration failed.") }
            }
        }
    }

    fun previousRegisterStep() {
        val current = _uiState.value.registerStep
        val prev = when (current) {
            RegisterStep.NAME_USERNAME -> null
            RegisterStep.MOBILE_NUMBER -> RegisterStep.NAME_USERNAME
            RegisterStep.OTP_VERIFICATION -> RegisterStep.MOBILE_NUMBER
            RegisterStep.PASSWORD_CREATION -> RegisterStep.OTP_VERIFICATION
            RegisterStep.PROFILE_SETUP -> RegisterStep.PASSWORD_CREATION
        }
        if (prev != null) {
            _uiState.update { it.copy(registerStep = prev, errorMessage = null) }
        } else {
            switchMode(AuthMode.LOGIN)
        }
    }
}
