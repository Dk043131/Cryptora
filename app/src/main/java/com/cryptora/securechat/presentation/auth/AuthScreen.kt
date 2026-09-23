package com.cryptora.securechat.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptora.securechat.core.designsystem.CryptoraBrandLogo
import com.cryptora.securechat.core.designsystem.CryptoraButton
import com.cryptora.securechat.core.designsystem.CryptoraCard
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.core.designsystem.CryptoraDimens
import com.cryptora.securechat.core.designsystem.CryptoraTextField
import com.cryptora.securechat.domain.model.SupportedCountryCodes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CryptoraColors.DeepNavyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CryptoraBrandLogo(size = 28.dp, showGlow = false)
                        Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))
                        Text(
                            text = "CRYPTORA",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = CryptoraColors.TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    if (state.mode == AuthMode.REGISTER) {
                        IconButton(onClick = { viewModel.previousRegisterStep() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = CryptoraColors.ElectricCyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CryptoraColors.DeepNavyBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = CryptoraDimens.PaddingLarge)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

            // Error Banner
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.errorMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(CryptoraDimens.CornerMedium),
                        color = CryptoraColors.CoralRevoked.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.CoralRevoked.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = CryptoraDimens.PaddingDefault)
                    ) {
                        Row(
                            modifier = Modifier.padding(CryptoraDimens.PaddingDefault),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = CryptoraColors.CoralRevoked,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))
                            Text(
                                text = msg,
                                color = CryptoraColors.CoralRevoked,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (state.mode == AuthMode.LOGIN) {
                LoginSection(viewModel = viewModel, state = state)
            } else {
                RegisterWizard(viewModel = viewModel, state = state)
            }

            Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))
        }
    }
}

// ======================== LOGIN VIEW ========================

@Composable
private fun LoginSection(
    viewModel: AuthViewModel,
    state: AuthUiState
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

        // Hero Brand Section
        CryptoraBrandLogo(size = 72.dp, showGlow = true)

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

        Text(
            text = "Welcome to Cryptora",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = CryptoraColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))
        Text(
            text = "Hardware-enforced zero-knowledge messaging",
            style = MaterialTheme.typography.bodyMedium,
            color = CryptoraColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        // Credential Card
        CryptoraCard(
            borderColor = CryptoraColors.BorderGlowing
        ) {
            Column {
                // Username
                CryptoraTextField(
                    value = state.usernameInput,
                    onValueChange = { viewModel.onLoginUsernameChanged(it) },
                    label = "Username",
                    placeholder = "e.g. alex_rivera",
                    leadingIcon = Icons.Default.Person
                )

                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

                // Password
                CryptoraTextField(
                    value = state.passwordInput,
                    onValueChange = { viewModel.onLoginPasswordChanged(it) },
                    label = "Password",
                    placeholder = "Master account passphrase",
                    leadingIcon = Icons.Default.Lock,
                    visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                            Icon(
                                imageVector = if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password",
                                tint = CryptoraColors.TextSecondary
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

                // Sign In Button
                CryptoraButton(
                    text = "Unlock Vault & Sign In",
                    onClick = { viewModel.submitLogin() },
                    isLoading = state.isLoading,
                    icon = Icons.Default.Security
                )
            }
        }

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "First time using Cryptora?",
                color = CryptoraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = { viewModel.switchMode(AuthMode.REGISTER) }) {
                Text(
                    text = "Create Account",
                    color = CryptoraColors.ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ======================== REGISTRATION WIZARD ========================

@Composable
private fun RegisterWizard(
    viewModel: AuthViewModel,
    state: AuthUiState
) {
    val currentStepNumber = when (state.registerStep) {
        RegisterStep.NAME_USERNAME -> 1
        RegisterStep.MOBILE_NUMBER -> 2
        RegisterStep.OTP_VERIFICATION -> 3
        RegisterStep.PASSWORD_CREATION -> 4
        RegisterStep.PROFILE_SETUP -> 5
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Step progress header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STEP $currentStepNumber OF 5",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = CryptoraColors.ElectricCyan
            )
            Text(
                text = when (state.registerStep) {
                    RegisterStep.NAME_USERNAME -> "Identity"
                    RegisterStep.MOBILE_NUMBER -> "Phone Verification"
                    RegisterStep.OTP_VERIFICATION -> "Security Code"
                    RegisterStep.PASSWORD_CREATION -> "PBKDF2 Encryption"
                    RegisterStep.PROFILE_SETUP -> "Profile Setup"
                },
                style = MaterialTheme.typography.labelSmall,
                color = CryptoraColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))

        // Progress line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(CryptoraColors.SurfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(currentStepNumber / 5f)
                    .height(4.dp)
                    .background(CryptoraColors.ElectricCyan)
            )
        }

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        when (state.registerStep) {
            RegisterStep.NAME_USERNAME -> StepNameUsername(viewModel, state)
            RegisterStep.MOBILE_NUMBER -> StepMobileNumber(viewModel, state)
            RegisterStep.OTP_VERIFICATION -> StepOtpVerification(viewModel, state)
            RegisterStep.PASSWORD_CREATION -> StepPasswordCreation(viewModel, state)
            RegisterStep.PROFILE_SETUP -> StepProfileSetup(viewModel, state)
        }
    }
}

// STEP 1: Name and Username
@Composable
private fun StepNameUsername(viewModel: AuthViewModel, state: AuthUiState) {
    Column {
        Text("Create Your Identity", style = MaterialTheme.typography.headlineSmall, color = CryptoraColors.TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))
        Text("Your username is used for public-key encryption discovery.", style = MaterialTheme.typography.bodyMedium, color = CryptoraColors.TextSecondary)

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        CryptoraTextField(
            value = state.fullNameInput,
            onValueChange = { viewModel.onFullNameChanged(it) },
            label = "Full Name",
            placeholder = "e.g. Alex Rivera",
            leadingIcon = Icons.Default.Person
        )

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

        CryptoraTextField(
            value = state.usernameInput,
            onValueChange = { viewModel.onRegisterUsernameChanged(it) },
            label = "Unique Username",
            placeholder = "alex_rivera",
            leadingIcon = Icons.Default.Security,
            trailingIcon = {
                if (state.isCheckingUsername) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CryptoraColors.ElectricCyan, strokeWidth = 2.dp)
                } else if (state.isUsernameAvailable == true) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Available", tint = CryptoraColors.EmeraldSecure)
                } else if (state.isUsernameAvailable == false) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Taken", tint = CryptoraColors.CoralRevoked)
                }
            }
        )

        if (state.isUsernameAvailable == true) {
            Text("✓ Username is available for enclave registration", color = CryptoraColors.EmeraldSecure, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        } else if (state.isUsernameAvailable == false) {
            Text("✕ Username is already taken", color = CryptoraColors.CoralRevoked, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        CryptoraButton(
            text = "Continue",
            onClick = { viewModel.submitStep1() }
        )
    }
}

// STEP 2: Mobile Number & Country Code
@Composable
private fun StepMobileNumber(viewModel: AuthViewModel, state: AuthUiState) {
    var expandedDropdown by remember { mutableStateOf(false) }

    Column {
        Text("Mobile Verification", style = MaterialTheme.typography.headlineSmall, color = CryptoraColors.TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))
        Text("We'll send a 6-digit cryptographic authentication code.", style = MaterialTheme.typography.bodyMedium, color = CryptoraColors.TextSecondary)

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Country Code Picker Button
            Box {
                Surface(
                    shape = RoundedCornerShape(CryptoraDimens.CornerMedium),
                    color = CryptoraColors.SurfaceNavy,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle),
                    modifier = Modifier
                        .height(56.dp)
                        .clickable { expandedDropdown = true }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = CryptoraDimens.PaddingDefault),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${state.selectedCountryCode.flagEmoji} ${state.selectedCountryCode.dialCode}",
                            color = CryptoraColors.TextPrimary,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier.background(CryptoraColors.SurfaceElevated)
                ) {
                    SupportedCountryCodes.list.forEach { country ->
                        DropdownMenuItem(
                            text = { Text("${country.flagEmoji} ${country.name} (${country.dialCode})", color = CryptoraColors.TextPrimary) },
                            onClick = {
                                viewModel.onCountryCodeSelected(country)
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))

            // Phone Input
            CryptoraTextField(
                value = state.mobileNumberInput,
                onValueChange = { viewModel.onMobileNumberChanged(it) },
                label = "Mobile Number",
                placeholder = "9876543210",
                leadingIcon = Icons.Default.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        CryptoraButton(
            text = "Send Verification Code",
            isLoading = state.isLoading,
            onClick = { viewModel.submitSendOtp() }
        )
    }
}

// STEP 3: OTP Verification
@Composable
private fun StepOtpVerification(viewModel: AuthViewModel, state: AuthUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Enter Security Code", style = MaterialTheme.typography.headlineSmall, color = CryptoraColors.TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))
        Text(
            text = "Code sent to ${state.selectedCountryCode.dialCode} ${state.mobileNumberInput}",
            style = MaterialTheme.typography.bodyMedium,
            color = CryptoraColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        // OTP Input Field
        OutlinedTextField(
            value = state.otpInput,
            onValueChange = { viewModel.onOtpChanged(it) },
            label = { Text("6-Digit Code") },
            placeholder = { Text("••••••") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center,
                letterSpacing = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = CryptoraColors.ElectricCyan
            ),
            modifier = Modifier.fillMaxWidth(0.85f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CryptoraColors.SurfaceNavy,
                unfocusedContainerColor = CryptoraColors.SurfaceNavy,
                focusedBorderColor = CryptoraColors.ElectricCyan,
                unfocusedBorderColor = CryptoraColors.BorderSubtle,
                focusedLabelColor = CryptoraColors.ElectricCyan,
                unfocusedLabelColor = CryptoraColors.TextSecondary
            ),
            shape = RoundedCornerShape(CryptoraDimens.CornerMedium)
        )

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

        // Countdown & Resend
        if (state.otpCountdown > 0) {
            Text(
                text = "⏱️ Resend code in 00:${state.otpCountdown.toString().padStart(2, '0')}",
                color = CryptoraColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            TextButton(onClick = { viewModel.resendOtp() }) {
                Text("Resend Code", color = CryptoraColors.ElectricCyan, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

        // Quick Fill & Test OTP Banner
        Surface(
            shape = RoundedCornerShape(CryptoraDimens.CornerMedium),
            color = CryptoraColors.ElectricCyan.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.ElectricCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable { viewModel.onOtpChanged("123456") }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "🔐 Instant Verification Code",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.ElectricCyan
                    )
                    Text(
                        text = "Use code: 123456 (or check notification)",
                        style = MaterialTheme.typography.bodySmall,
                        color = CryptoraColors.TextSecondary
                    )
                }
                Text(
                    text = "Tap to Fill ⚡",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CryptoraColors.ElectricCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        CryptoraButton(
            text = "Verify Code",
            isLoading = state.isLoading,
            onClick = { viewModel.submitVerifyOtp() }
        )
    }
}

// STEP 4: Password Creation
@Composable
private fun StepPasswordCreation(viewModel: AuthViewModel, state: AuthUiState) {
    Column {
        Text("Create Master Password", style = MaterialTheme.typography.headlineSmall, color = CryptoraColors.TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))
        Text("Your passphrase derives local encryption keys via PBKDF2 with SHA-256.", style = MaterialTheme.typography.bodyMedium, color = CryptoraColors.TextSecondary)

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        CryptoraTextField(
            value = state.passwordInput,
            onValueChange = { viewModel.onRegisterPasswordChanged(it) },
            label = "Password",
            placeholder = "Choose a strong password",
            leadingIcon = Icons.Default.Lock,
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                    Icon(
                        imageVector = if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password",
                        tint = CryptoraColors.TextSecondary
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

        CryptoraTextField(
            value = state.confirmPasswordInput,
            onValueChange = { viewModel.onConfirmPasswordChanged(it) },
            label = "Confirm Password",
            placeholder = "Re-enter chosen password",
            leadingIcon = Icons.Default.Lock,
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

        // Strength Checklist
        val p = state.passwordInput
        ChecklistRow(isValid = p.length >= 8, text = "At least 8 characters")
        ChecklistRow(isValid = p.any { it.isUpperCase() }, text = "At least one uppercase letter")
        ChecklistRow(isValid = p.any { it.isDigit() }, text = "At least one number")

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        CryptoraButton(
            text = "Continue to Profile",
            onClick = { viewModel.submitPasswordStep() }
        )
    }
}

// STEP 5: Profile Customization
@Composable
private fun StepProfileSetup(viewModel: AuthViewModel, state: AuthUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Profile Customization", style = MaterialTheme.typography.headlineSmall, color = CryptoraColors.TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))
        Text("Choose your avatar icon and optional profile bio.", style = MaterialTheme.typography.bodyMedium, color = CryptoraColors.TextSecondary)

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        // Large Selected Avatar
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(CryptoraColors.SurfaceElevated)
                .border(2.dp, CryptoraColors.ElectricCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = state.selectedAvatar, fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

        // Avatar selector choices
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            viewModel.avatarOptions.take(4).forEach { avatar ->
                AvatarChoice(avatar = avatar, isSelected = state.selectedAvatar == avatar) {
                    viewModel.onAvatarSelected(avatar)
                }
            }
        }
        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingHalf))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            viewModel.avatarOptions.drop(4).forEach { avatar ->
                AvatarChoice(avatar = avatar, isSelected = state.selectedAvatar == avatar) {
                    viewModel.onAvatarSelected(avatar)
                }
            }
        }

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        CryptoraTextField(
            value = state.bioInput,
            onValueChange = { viewModel.onBioChanged(it) },
            label = "Bio / Status (Optional)",
            placeholder = "Secured with Cryptora",
            singleLine = false,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        CryptoraButton(
            text = "Generate Keys & Complete Vault Setup",
            isLoading = state.isLoading,
            onClick = { viewModel.submitCompleteRegistration() },
            icon = Icons.Default.Security
        )
    }
}

// Helpers
@Composable
private fun AvatarChoice(avatar: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (isSelected) CryptoraColors.ElectricCyan.copy(alpha = 0.25f) else CryptoraColors.SurfaceNavy)
            .border(
                1.5.dp,
                if (isSelected) CryptoraColors.ElectricCyan else CryptoraColors.BorderSubtle,
                CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = avatar, fontSize = 20.sp)
    }
}

@Composable
private fun ChecklistRow(isValid: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(
            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (isValid) CryptoraColors.EmeraldSecure else CryptoraColors.TextDisabled,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))
        Text(
            text = text,
            color = if (isValid) CryptoraColors.TextPrimary else CryptoraColors.TextDisabled,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
