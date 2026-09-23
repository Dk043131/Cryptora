package com.cryptora.securechat.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cryptora.securechat.R

// =========================================================================
// 1. BRAND LOGO COMPONENT
// =========================================================================

/**
 * Official Cryptora Brand Logo.
 * Displays the security lock emblem with ambient pulsing glow ring.
 */
@Composable
fun CryptoraBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showGlow: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "brandGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (showGlow) {
            // Ambient outer glow ring
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CryptoraColors.ElectricCyan.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Inner Shield Emblem Container
        Box(
            modifier = Modifier
                .size(size * 0.85f)
                .clip(RoundedCornerShape(CryptoraDimens.CornerMedium))
                .background(CryptoraColors.SurfaceElevated)
                .border(
                    BorderStroke(1.5.dp, CryptoraColors.ElectricCyan.copy(alpha = 0.8f)),
                    RoundedCornerShape(CryptoraDimens.CornerMedium)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Cryptora Security Emblem",
                tint = CryptoraColors.ElectricCyan,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

// =========================================================================
// 2. BUTTONS: PRIMARY & OUTLINED
// =========================================================================

@Composable
fun CryptoraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    containerColor: Color = CryptoraColors.ElectricCyan,
    contentColor: Color = CryptoraColors.DeepNavyBackground,
    shape: Shape = RoundedCornerShape(CryptoraDimens.CornerMedium),
    height: Dp = 50.dp
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.3f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        shape = shape,
        contentPadding = PaddingValues(horizontal = CryptoraDimens.PaddingLarge, vertical = CryptoraDimens.PaddingQuarter)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))
            Text("Please wait...", color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        } else {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))
            }
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun CryptoraOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    borderColor: Color = CryptoraColors.ElectricCyan,
    contentColor: Color = CryptoraColors.ElectricCyan,
    shape: Shape = RoundedCornerShape(CryptoraDimens.CornerMedium),
    height: Dp = 50.dp
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, if (enabled) borderColor.copy(alpha = 0.7f) else borderColor.copy(alpha = 0.25f)),
        shape = shape,
        contentPadding = PaddingValues(horizontal = CryptoraDimens.PaddingLarge)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))
            }
            Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

// =========================================================================
// 3. INPUT FIELDS & SEARCH BAR
// =========================================================================

@Composable
fun CryptoraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            label = { Text(label) },
            placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder, color = CryptoraColors.TextMuted) } } else null,
            leadingIcon = if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (isError) CryptoraColors.CoralRevoked else CryptoraColors.ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else null,
            trailingIcon = trailingIcon,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(CryptoraDimens.CornerMedium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CryptoraColors.SurfaceNavy,
                unfocusedContainerColor = CryptoraColors.SurfaceNavy,
                disabledContainerColor = CryptoraColors.SurfaceNavy.copy(alpha = 0.5f),
                focusedBorderColor = CryptoraColors.ElectricCyan,
                unfocusedBorderColor = CryptoraColors.BorderSubtle,
                errorBorderColor = CryptoraColors.CoralRevoked,
                focusedLabelColor = CryptoraColors.ElectricCyan,
                unfocusedLabelColor = CryptoraColors.TextSecondary,
                focusedTextColor = CryptoraColors.TextPrimary,
                unfocusedTextColor = CryptoraColors.TextPrimary
            )
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = CryptoraColors.CoralRevoked,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun CryptoraSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search users, messages, notes...",
    onClear: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CryptoraDimens.CornerMedium))
            .background(CryptoraColors.SurfaceNavy)
            .border(1.dp, CryptoraColors.BorderSubtle, RoundedCornerShape(CryptoraDimens.CornerMedium))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CryptoraDimens.PaddingDefault, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = CryptoraColors.ElectricCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(placeholder, color = CryptoraColors.TextMuted, fontSize = 14.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = CryptoraColors.TextPrimary,
                    unfocusedTextColor = CryptoraColors.TextPrimary
                )
            )
            if (query.isNotEmpty() && onClear != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = CryptoraColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 4. CARDS & SURFACES
// =========================================================================

@Composable
fun CryptoraCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CryptoraColors.BorderSubtle,
    backgroundColor: Color = CryptoraColors.SurfaceNavy,
    shape: Shape = RoundedCornerShape(CryptoraDimens.CornerMedium),
    contentPadding: PaddingValues = PaddingValues(CryptoraDimens.PaddingDefault),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(clickableModifier)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .padding(contentPadding)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

// =========================================================================
// 5. SECURITY BADGES & EXPIRY INDICATORS
// =========================================================================

@Composable
fun CryptoraSecureBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Lock,
    color: Color = CryptoraColors.ElectricCyan
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(CryptoraDimens.CornerExtraSmall))
            .background(color.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.35f)), RoundedCornerShape(CryptoraDimens.CornerExtraSmall))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
fun CryptoraExpiryBadge(
    remainingMillis: Long,
    isExpired: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (badgeColor, text) = when {
        isExpired -> CryptoraColors.CrimsonDanger to "EXPIRED"
        remainingMillis <= 60_000L -> CryptoraColors.CrimsonDanger to formatCountdown(remainingMillis)
        remainingMillis <= 5 * 60_000L -> CryptoraColors.AmberWarning to formatCountdown(remainingMillis)
        else -> CryptoraColors.ElectricCyan to formatCountdown(remainingMillis)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(CryptoraDimens.CornerExtraSmall))
            .background(badgeColor.copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)), RoundedCornerShape(CryptoraDimens.CornerExtraSmall))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = badgeColor,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remMin = minutes % 60
        "${hours}h ${remMin}m"
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

// =========================================================================
// 6. AVATAR COMPONENT
// =========================================================================

@Composable
fun CryptoraAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isKeyVerified: Boolean = true
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(CryptoraColors.SurfaceElevated)
                .border(BorderStroke(1.5.dp, CryptoraColors.ElectricCyan.copy(alpha = 0.6f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.trim().take(1).uppercase().ifEmpty { "?" },
                color = CryptoraColors.ElectricCyan,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }

        if (isKeyVerified) {
            Box(
                modifier = Modifier
                    .size(size * 0.32f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(CryptoraColors.EmeraldSecure)
                    .border(BorderStroke(1.5.dp, CryptoraColors.DeepNavyBackground), CircleShape)
            )
        }
    }
}

// =========================================================================
// 7. ATTACHMENT CARD COMPONENT
// =========================================================================

@Composable
fun CryptoraAttachmentCard(
    fileName: String,
    fileSizeFormatted: String,
    isImage: Boolean,
    modifier: Modifier = Modifier,
    isDecrypted: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CryptoraDimens.CornerSmall))
            .clickable(onClick = onClick)
            .background(CryptoraColors.SurfaceElevated)
            .border(BorderStroke(1.dp, CryptoraColors.BorderSubtle), RoundedCornerShape(CryptoraDimens.CornerSmall))
            .padding(CryptoraDimens.PaddingHalf),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(CryptoraDimens.CornerExtraSmall))
                .background(CryptoraColors.ElectricCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isImage) Icons.Default.Image else Icons.Default.Description,
                contentDescription = null,
                tint = CryptoraColors.ElectricCyan,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = CryptoraColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fileSizeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = CryptoraColors.TextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isDecrypted) "• AES Decrypted" else "• Encrypted",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDecrypted) CryptoraColors.EmeraldSecure else CryptoraColors.AmberWarning
                )
            }
        }
    }
}

// =========================================================================
// 8. DIALOG & BOTTOM SHEET WRAPPERS
// =========================================================================

@Composable
fun CryptoraDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Security,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(CryptoraDimens.CornerLarge))
                .background(CryptoraColors.SurfaceNavy)
                .border(BorderStroke(1.5.dp, CryptoraColors.ElectricCyan.copy(alpha = 0.4f)), RoundedCornerShape(CryptoraDimens.CornerLarge)),
            color = CryptoraColors.SurfaceNavy
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(CryptoraDimens.PaddingLarge)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CryptoraColors.ElectricCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = CryptoraColors.ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = CryptoraColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

                content()

                if (confirmButton != null || dismissButton != null) {
                    Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButton != null) {
                            dismissButton()
                            Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))
                        }
                        if (confirmButton != null) {
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoraBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = CryptoraColors.DeepNavyBackground,
        contentColor = CryptoraColors.TextPrimary,
        shape = RoundedCornerShape(topStart = CryptoraDimens.CornerLarge, topEnd = CryptoraDimens.CornerLarge),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CryptoraDimens.PaddingLarge, vertical = CryptoraDimens.PaddingHalf)
        ) {
            content()
        }
    }
}

// =========================================================================
// 9. STATES: EMPTY, LOADING, ERROR
// =========================================================================

@Composable
fun CryptoraEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Lock,
    actionButton: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(CryptoraDimens.PaddingLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CryptoraColors.SurfaceElevated)
                    .border(BorderStroke(1.5.dp, CryptoraColors.BorderSubtle), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CryptoraColors.ElectricCyan,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = CryptoraColors.TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(CryptoraDimens.PaddingHalf))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = CryptoraColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            if (actionButton != null) {
                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))
                actionButton()
            }
        }
    }
}

@Composable
fun CryptoraLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String = "Decrypting secure channel..."
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = CryptoraColors.ElectricCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = CryptoraColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CryptoraErrorView(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(CryptoraDimens.PaddingLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = CryptoraColors.CoralRevoked,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))
                CryptoraOutlinedButton(
                    text = "Retry Connection",
                    onClick = onRetry,
                    borderColor = CryptoraColors.ElectricCyan,
                    modifier = Modifier.width(180.dp)
                )
            }
        }
    }
}
