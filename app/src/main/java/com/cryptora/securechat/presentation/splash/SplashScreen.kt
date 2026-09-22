package com.cryptora.securechat.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptora.securechat.core.designsystem.CryptoraBrandLogo
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.core.designsystem.CryptoraDimens

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    statusMessage: String = "Verifying cryptographic enclave..."
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CryptoraColors.DeepNavyBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(CryptoraDimens.PaddingLarge)
        ) {
            // Animated Brand Logo
            CryptoraBrandLogo(
                size = 80.dp,
                showGlow = true
            )

            Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

            // App Name
            Text(
                text = "CRYPTORA",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                ),
                color = CryptoraColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))

            // Tagline
            Text(
                text = "SECURE END-TO-END CHAT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = CryptoraColors.ElectricCyan
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Status Spinner & Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = CryptoraColors.ElectricCyan,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(CryptoraDimens.PaddingDefault))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CryptoraColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Enclave Trust Badge
            Surface(
                shape = RoundedCornerShape(CryptoraDimens.CornerFull),
                color = CryptoraColors.SurfaceNavy,
                border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CryptoraColors.EmeraldSecure,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AndroidKeyStore • AES-256-GCM • Zero-Knowledge Plaintext",
                        style = MaterialTheme.typography.labelSmall,
                        color = CryptoraColors.TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
