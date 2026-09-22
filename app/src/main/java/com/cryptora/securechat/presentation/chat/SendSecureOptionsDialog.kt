package com.cryptora.securechat.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cryptora.securechat.core.designsystem.CryptoraButton
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.core.designsystem.CryptoraOutlinedButton
import com.cryptora.securechat.domain.model.AccessMode
import com.cryptora.securechat.domain.model.ExpiryOption
import com.cryptora.securechat.domain.model.ForwardingPolicy
import com.cryptora.securechat.domain.model.SecureMessagePolicy

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SendSecureOptionsDialog(
    recipientName: String,
    recipientUsername: String,
    contentPreview: String,
    initialAccessMode: AccessMode = AccessMode.IMMEDIATE_ACCESS,
    initialExpiryOption: ExpiryOption = ExpiryOption.FIVE_MINUTES,
    initialForwardingPolicy: ForwardingPolicy = ForwardingPolicy.FORWARDING_DISABLED,
    onConfirm: (SecureMessagePolicy) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAccessMode by remember { mutableStateOf(initialAccessMode) }
    var selectedExpiryOption by remember { mutableStateOf(initialExpiryOption) }
    var customExpiryMinutes by remember { mutableStateOf("10") }
    var selectedForwardingPolicy by remember { mutableStateOf(initialForwardingPolicy) }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CryptoraColors.SurfaceNavy,
            border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.ElectricCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CryptoraColors.ElectricCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CryptoraColors.ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Send Securely",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CryptoraColors.TextPrimary
                            )
                            Text(
                                text = "Time-Based Cryptographic Access",
                                style = MaterialTheme.typography.labelSmall,
                                color = CryptoraColors.ElectricCyan
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CryptoraColors.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CryptoraColors.BorderSubtle)
                Spacer(modifier = Modifier.height(14.dp))

                // Recipient Section
                SectionLabel(title = "RECIPIENT")
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CryptoraColors.DeepNavyBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CryptoraColors.ElectricCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = recipientName.take(1).uppercase().ifBlank { "U" },
                                fontWeight = FontWeight.Bold,
                                color = CryptoraColors.ElectricCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = recipientName.ifBlank { "Cryptora Contact" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CryptoraColors.TextPrimary
                            )
                            Text(
                                text = if (recipientUsername.isNotBlank()) "@$recipientUsername" else "Verified Identity",
                                style = MaterialTheme.typography.labelSmall,
                                color = CryptoraColors.TextMuted
                            )
                        }
                    }
                }

                // Content Preview Section
                SectionLabel(title = "CONTENT PREVIEW")
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CryptoraColors.DeepNavyBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 14.dp)
                ) {
                    Text(
                        text = contentPreview.ifBlank { "[Secure Payload]" },
                        color = CryptoraColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Access Mode Section
                SectionLabel(title = "ACCESS MODE")
                AccessModeSelector(
                    selectedMode = selectedAccessMode,
                    onSelectMode = { selectedAccessMode = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Expiry Options Section
                SectionLabel(title = "EXPIRY / TIMELOCK")
                ExpiryOptionSelector(
                    selectedOption = selectedExpiryOption,
                    onSelectOption = { selectedExpiryOption = it },
                    customMinutes = customExpiryMinutes,
                    onCustomMinutesChange = { customExpiryMinutes = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Forwarding Policy Section
                SectionLabel(title = "FORWARDING POLICY")
                ForwardingPolicySelector(
                    selectedPolicy = selectedForwardingPolicy,
                    onSelectPolicy = { selectedForwardingPolicy = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CryptoraOutlinedButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    CryptoraButton(
                        text = "Confirm & Encrypt",
                        icon = Icons.Default.Lock,
                        onClick = {
                            val durationMillis = when (selectedExpiryOption) {
                                ExpiryOption.CUSTOM -> {
                                    val mins = customExpiryMinutes.toLongOrNull() ?: 10L
                                    mins * 60L * 1000L
                                }
                                else -> selectedExpiryOption.durationMillis
                            }

                            val authoritativeNow = System.currentTimeMillis()
                            val calculatedExpiresAt = durationMillis?.let { authoritativeNow + it }

                            val policy = SecureMessagePolicy(
                                accessMode = selectedAccessMode,
                                expiresAt = calculatedExpiresAt,
                                forwardingPolicy = selectedForwardingPolicy,
                                approvalRequired = (selectedAccessMode == AccessMode.REQUEST_ACCESS),
                                createdAt = authoritativeNow,
                                isAccessGranted = (selectedAccessMode == AccessMode.IMMEDIATE_ACCESS),
                                expiryDurationMillis = durationMillis
                            )
                            onConfirm(policy)
                        },
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = CryptoraColors.ElectricCyan,
        letterSpacing = 1.sp
    )
}

@Composable
private fun AccessModeSelector(
    selectedMode: AccessMode,
    onSelectMode: (AccessMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AccessModeOptionCard(
            title = "1. Immediate Access",
            description = "Recipient can decrypt immediately. Expiry countdown starts upon receipt.",
            isSelected = selectedMode == AccessMode.IMMEDIATE_ACCESS,
            onClick = { onSelectMode(AccessMode.IMMEDIATE_ACCESS) }
        )

        AccessModeOptionCard(
            title = "2. Request Access",
            description = "Message arrives locked. Recipient must request access; you must approve before decryption.",
            isSelected = selectedMode == AccessMode.REQUEST_ACCESS,
            onClick = { onSelectMode(AccessMode.REQUEST_ACCESS) }
        )
    }
}

@Composable
private fun AccessModeOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) CryptoraColors.ElectricCyan.copy(alpha = 0.1f) else CryptoraColors.DeepNavyBackground,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) CryptoraColors.ElectricCyan else CryptoraColors.BorderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = CryptoraColors.ElectricCyan,
                    unselectedColor = CryptoraColors.TextMuted
                ),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) CryptoraColors.ElectricCyan else CryptoraColors.TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = CryptoraColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpiryOptionSelector(
    selectedOption: ExpiryOption,
    onSelectOption: (ExpiryOption) -> Unit,
    customMinutes: String,
    onCustomMinutesChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExpiryOption.entries.forEach { option ->
                val isSelected = selectedOption == option
                Surface(
                    onClick = { onSelectOption(option) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) CryptoraColors.ElectricCyan.copy(alpha = 0.2f) else CryptoraColors.DeepNavyBackground,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) CryptoraColors.ElectricCyan else CryptoraColors.BorderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isSelected) CryptoraColors.ElectricCyan else CryptoraColors.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = option.displayName,
                            color = if (isSelected) CryptoraColors.ElectricCyan else CryptoraColors.TextPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (selectedOption == ExpiryOption.CUSTOM) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = customMinutes,
                onValueChange = onCustomMinutesChange,
                label = { Text("Duration (minutes)", color = CryptoraColors.TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CryptoraColors.ElectricCyan,
                    unfocusedBorderColor = CryptoraColors.BorderSubtle,
                    focusedTextColor = CryptoraColors.TextPrimary,
                    unfocusedTextColor = CryptoraColors.TextPrimary
                )
            )
        }
    }
}

@Composable
private fun ForwardingPolicySelector(
    selectedPolicy: ForwardingPolicy,
    onSelectPolicy: (ForwardingPolicy) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ForwardingPolicyOptionCard(
            title = "Forwarding Disabled",
            description = "Prevent recipient from forwarding to any third party.",
            isSelected = selectedPolicy == ForwardingPolicy.FORWARDING_DISABLED,
            onClick = { onSelectPolicy(ForwardingPolicy.FORWARDING_DISABLED) }
        )

        ForwardingPolicyOptionCard(
            title = "Forwarding Allowed",
            description = "Recipient can forward this content while keeping encryption intact.",
            isSelected = selectedPolicy == ForwardingPolicy.FORWARDING_ALLOWED,
            onClick = { onSelectPolicy(ForwardingPolicy.FORWARDING_ALLOWED) }
        )

        ForwardingPolicyOptionCard(
            title = "Original Sender Approval Required",
            description = "Attempting to forward triggers a cryptographic permission request to you.",
            isSelected = selectedPolicy == ForwardingPolicy.REQUIRES_ORIGINAL_SENDER_APPROVAL,
            onClick = { onSelectPolicy(ForwardingPolicy.REQUIRES_ORIGINAL_SENDER_APPROVAL) }
        )
    }
}

@Composable
private fun ForwardingPolicyOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) CryptoraColors.ElectricCyan.copy(alpha = 0.1f) else CryptoraColors.DeepNavyBackground,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) CryptoraColors.ElectricCyan else CryptoraColors.BorderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = CryptoraColors.ElectricCyan,
                    unselectedColor = CryptoraColors.TextMuted
                ),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) CryptoraColors.ElectricCyan else CryptoraColors.TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = CryptoraColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
