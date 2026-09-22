package com.cryptora.securechat.presentation.access

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.domain.model.AccessRequest

enum class GrantDurationOption(val label: String, val durationMillis: Long) {
    MINUTES_5("5m", 5L * 60L * 1000L),
    MINUTES_15("15m", 15L * 60L * 1000L),
    MINUTES_30("30m", 30L * 60L * 1000L),
    HOUR_1("1h", 60L * 60L * 1000L),
    HOURS_24("24h", 24L * 60L * 60L * 1000L),
    CUSTOM("Custom", -1L)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApproveAccessRequestDialog(
    request: AccessRequest,
    onApprove: (finalDurationMillis: Long) -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit
) {
    // Determine closest preset or custom matching the requested duration
    val initialOption = when (request.requestedDuration) {
        5L * 60L * 1000L -> GrantDurationOption.MINUTES_5
        15L * 60L * 1000L -> GrantDurationOption.MINUTES_15
        30L * 60L * 1000L -> GrantDurationOption.MINUTES_30
        60L * 60L * 1000L -> GrantDurationOption.HOUR_1
        24L * 60L * 60L * 1000L -> GrantDurationOption.HOURS_24
        else -> GrantDurationOption.MINUTES_30
    }

    var selectedOption by remember { mutableStateOf(initialOption) }
    var customMinutes by remember {
        mutableStateOf(((request.requestedDuration ?: (30L * 60L * 1000L)) / 60000L).toString())
    }

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
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = CryptoraColors.ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Access Request",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CryptoraColors.TextPrimary
                            )
                            Text(
                                text = "Grant time-limited cryptographic key",
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

                // Requester & Content details
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CryptoraColors.DeepNavyBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "@${request.requesterUsername} wants access to:",
                            style = MaterialTheme.typography.bodySmall,
                            color = CryptoraColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = request.contentTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CryptoraColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val reqMinutes = (request.requestedDuration ?: (30L * 60L * 1000L)) / 60000L
                        Text(
                            text = "Requested duration: $reqMinutes minutes",
                            style = MaterialTheme.typography.labelSmall,
                            color = CryptoraColors.ElectricCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sender sets final access duration
                Text(
                    text = "FINAL ACCESS DURATION TO GRANT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CryptoraColors.ElectricCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GrantDurationOption.entries.forEach { option ->
                        val isSelected = selectedOption == option
                        Surface(
                            onClick = { selectedOption = option },
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
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isSelected) CryptoraColors.ElectricCyan else CryptoraColors.TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = option.label,
                                    color = if (isSelected) CryptoraColors.ElectricCyan else CryptoraColors.TextPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                if (selectedOption == GrantDurationOption.CUSTOM) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it },
                        label = { Text("Duration to grant (minutes)", color = CryptoraColors.TextSecondary) },
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

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "As owner, your chosen duration defines the authoritative key lifetime. Receiver access will automatically revoke upon expiry.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CryptoraColors.TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: [Reject] [Approve]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.CrimsonDanger.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = CryptoraColors.CrimsonDanger,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", color = CryptoraColors.CrimsonDanger, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val duration = if (selectedOption == GrantDurationOption.CUSTOM) {
                                val mins = customMinutes.toLongOrNull() ?: 30L
                                mins * 60L * 1000L
                            } else {
                                selectedOption.durationMillis
                            }
                            onApprove(duration)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CryptoraColors.EmeraldSafe,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
