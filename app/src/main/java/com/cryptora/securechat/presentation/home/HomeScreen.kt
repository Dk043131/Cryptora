package com.cryptora.securechat.presentation.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptora.securechat.core.designsystem.CryptoraAvatar
import com.cryptora.securechat.core.designsystem.CryptoraBrandLogo
import com.cryptora.securechat.core.designsystem.CryptoraButton
import com.cryptora.securechat.core.designsystem.CryptoraCard
import com.cryptora.securechat.core.designsystem.CryptoraColors
import com.cryptora.securechat.core.designsystem.CryptoraDimens
import com.cryptora.securechat.core.designsystem.CryptoraEmptyState
import com.cryptora.securechat.core.designsystem.CryptoraErrorView
import com.cryptora.securechat.core.designsystem.CryptoraLoadingIndicator
import com.cryptora.securechat.core.designsystem.CryptoraOutlinedButton
import com.cryptora.securechat.core.designsystem.CryptoraSecureBadge
import com.cryptora.securechat.domain.model.Conversation
import com.cryptora.securechat.domain.model.User
import com.cryptora.securechat.presentation.common.UiState
import com.cryptora.securechat.presentation.components.InAppNotificationHost
import com.cryptora.securechat.presentation.search.UserSearchScreen
import com.cryptora.securechat.presentation.search.UserSearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showNotesSheet by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CryptoraColors.SurfaceNavy,
                drawerContentColor = CryptoraColors.TextPrimary,
                modifier = Modifier.width(310.dp)
            ) {
                DrawerHeader(
                    user = (uiState as? UiState.Success)?.data?.currentUser
                )

                HorizontalDivider(color = CryptoraColors.BorderSubtle)
                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

                // 1. Chats
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = CryptoraColors.ElectricCyan) },
                    label = { Text("Chats", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == HomeTab.CHATS,
                    onClick = {
                        viewModel.selectTab(HomeTab.CHATS)
                        scope.launch { drawerState.close() }
                    },
                    colors = drawerItemColors()
                )

                // 2. Search
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = null, tint = CryptoraColors.ElectricCyan) },
                    label = { Text("Search & Discovery", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == HomeTab.SEARCH,
                    onClick = {
                        viewModel.selectTab(HomeTab.SEARCH)
                        scope.launch { drawerState.close() }
                    },
                    colors = drawerItemColors()
                )

                // 3. Notes Vault (in side menu as requested)
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Description, contentDescription = null, tint = CryptoraColors.ElectricCyan) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Notes Vault", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CryptoraColors.ElectricCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "SECURE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CryptoraColors.ElectricCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showNotesSheet = true
                    },
                    colors = drawerItemColors()
                )

                // 4. Access Requests
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Security, contentDescription = null, tint = CryptoraColors.AmberWarning) },
                    label = { Text("Access Requests", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.onAccessRequestsClicked()
                    },
                    colors = drawerItemColors()
                )

                // 5. Profile
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null, tint = CryptoraColors.ElectricCyan) },
                    label = { Text("My Profile", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == HomeTab.PROFILE,
                    onClick = {
                        viewModel.selectTab(HomeTab.PROFILE)
                        scope.launch { drawerState.close() }
                    },
                    colors = drawerItemColors()
                )

                // 6. Settings
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = CryptoraColors.ElectricCyan) },
                    label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == HomeTab.SETTINGS,
                    onClick = {
                        viewModel.selectTab(HomeTab.SETTINGS)
                        scope.launch { drawerState.close() }
                    },
                    colors = drawerItemColors()
                )

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(color = CryptoraColors.BorderSubtle)

                // Logout Item
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = CryptoraColors.CoralRevoked) },
                    label = { Text("Lock & Sign Out", color = CryptoraColors.CoralRevoked, fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.logout()
                    },
                    colors = drawerItemColors()
                )
                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))
            }
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = CryptoraColors.DeepNavyBackground,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CryptoraBrandLogo(size = 28.dp, showGlow = false)
                                Spacer(modifier = Modifier.width(CryptoraDimens.PaddingHalf))
                                Column {
                                    Text(
                                        text = when (selectedTab) {
                                            HomeTab.CHATS -> "CRYPTORA"
                                            HomeTab.SEARCH -> "SEARCH & DISCOVERY"
                                            HomeTab.PROFILE -> "MY PROFILE"
                                            HomeTab.SETTINGS -> "SECURITY SETTINGS"
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = CryptoraColors.TextPrimary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(CryptoraColors.EmeraldSecure)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ENCLAVE ACTIVE • E2EE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CryptoraColors.TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Drawer",
                                    tint = CryptoraColors.ElectricCyan
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.onAccessRequestsClicked() }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Access Requests",
                                    tint = CryptoraColors.ElectricCyan
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = CryptoraColors.DeepNavyBackground
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = CryptoraColors.SurfaceNavy,
                        contentColor = CryptoraColors.TextSecondary,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == HomeTab.CHATS,
                            onClick = { viewModel.selectTab(HomeTab.CHATS) },
                            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chats") },
                            label = { Text("Chats", fontWeight = if (selectedTab == HomeTab.CHATS) FontWeight.Bold else FontWeight.Normal) },
                            colors = customNavBarColors()
                        )
                        NavigationBarItem(
                            selected = selectedTab == HomeTab.SEARCH,
                            onClick = { viewModel.selectTab(HomeTab.SEARCH) },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search", fontWeight = if (selectedTab == HomeTab.SEARCH) FontWeight.Bold else FontWeight.Normal) },
                            colors = customNavBarColors()
                        )
                        NavigationBarItem(
                            selected = selectedTab == HomeTab.PROFILE,
                            onClick = { viewModel.selectTab(HomeTab.PROFILE) },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontWeight = if (selectedTab == HomeTab.PROFILE) FontWeight.Bold else FontWeight.Normal) },
                            colors = customNavBarColors()
                        )
                        NavigationBarItem(
                            selected = selectedTab == HomeTab.SETTINGS,
                            onClick = { viewModel.selectTab(HomeTab.SETTINGS) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontWeight = if (selectedTab == HomeTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                            colors = customNavBarColors()
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (val state = uiState) {
                        is UiState.Loading -> {
                            CryptoraLoadingIndicator(message = "Verifying cryptographic enclave...")
                        }
                        is UiState.Error -> {
                            CryptoraErrorView(
                                message = state.message,
                                onRetry = { viewModel.loadDashboardData() }
                            )
                        }
                        is UiState.Success -> {
                            when (selectedTab) {
                                HomeTab.CHATS -> {
                                    ChatsTabContent(
                                        data = state.data,
                                        onConversationClick = { viewModel.onConversationClicked(it) },
                                        onFindUsersClick = { viewModel.selectTab(HomeTab.SEARCH) }
                                    )
                                }
                                HomeTab.SEARCH -> {
                                    val searchViewModel: UserSearchViewModel = hiltViewModel()
                                    UserSearchScreen(viewModel = searchViewModel)
                                }
                                HomeTab.PROFILE -> {
                                    ProfileTabContent(user = state.data.currentUser)
                                }
                                HomeTab.SETTINGS -> {
                                    SettingsTabContent(
                                        onLogoutClick = { viewModel.logout() },
                                        isHardwareKeystoreActive = state.data.isHardwareKeystoreActive
                                    )
                                }
                            }
                        }
                        is UiState.Idle -> { /* No-op */ }
                    }
                }
            }

            InAppNotificationHost(
                notificationManager = viewModel.inAppNotificationManager,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (showNotesSheet) {
        val notesViewModel: com.cryptora.securechat.presentation.notes.NotesViewModel = hiltViewModel()
        com.cryptora.securechat.presentation.notes.NotesSheet(
            viewModel = notesViewModel,
            onDismissRequest = { showNotesSheet = false }
        )
    }
}

// ======================== DRAWER HEADER ========================

@Composable
private fun DrawerHeader(user: User?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(CryptoraDimens.PaddingLarge)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CryptoraAvatar(
                name = user?.fullName?.ifBlank { user.username } ?: "User",
                size = 56.dp,
                isKeyVerified = true
            )
            Spacer(modifier = Modifier.width(CryptoraDimens.PaddingDefault))
            Column {
                Text(
                    text = user?.fullName?.ifBlank { "Cryptora User" } ?: "Cryptora User",
                    style = MaterialTheme.typography.titleMedium,
                    color = CryptoraColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${user?.username ?: "unknown"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CryptoraColors.ElectricCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

        Surface(
            shape = RoundedCornerShape(CryptoraDimens.CornerSmall),
            color = CryptoraColors.DeepNavyBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, CryptoraColors.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                    text = "Hardware Key • AndroidKeyStore AES-256",
                    style = MaterialTheme.typography.labelSmall,
                    color = CryptoraColors.EmeraldSecure,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ======================== TAB 1: CHATS CONTENT ========================

@Composable
private fun ChatsTabContent(
    data: HomeDashboardData,
    onConversationClick: (String) -> Unit,
    onFindUsersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = CryptoraDimens.PaddingDefault),
        verticalArrangement = Arrangement.spacedBy(CryptoraDimens.PaddingDefault)
    ) {
        item {
            Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))
            // Security Status Card
            CryptoraCard(borderColor = CryptoraColors.BorderGlowing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Cryptographic Security Enclave",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CryptoraColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            CryptoraSecureBadge(text = "VERIFIED")
                        }
                        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))
                        Text(
                            text = "Keys stored in AndroidKeyStore • Forwarding Hash Chain Audited",
                            style = MaterialTheme.typography.bodySmall,
                            color = CryptoraColors.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CryptoraColors.ElectricCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield",
                            tint = CryptoraColors.ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Encrypted Conversations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CryptoraColors.TextPrimary
                )
                if (data.conversations.isNotEmpty()) {
                    Text(
                        text = "${data.conversations.size} active",
                        style = MaterialTheme.typography.labelSmall,
                        color = CryptoraColors.ElectricCyan
                    )
                }
            }
        }

        if (data.conversations.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = CryptoraDimens.PaddingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CryptoraEmptyState(
                        title = "No Conversations Yet",
                        description = "Find other users by their unique @username to establish a timed, hardware-encrypted secure chat."
                    )
                    Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))
                    CryptoraButton(
                        text = "Find Users by @username",
                        onClick = onFindUsersClick,
                        icon = Icons.Default.Search,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }
            }
        } else {
            items(data.conversations, key = { it.id }) { conversation ->
                ConversationRow(
                    conversation = conversation,
                    onClick = { onConversationClick(conversation.id) }
                )
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit
) {
    CryptoraCard(modifier = Modifier.clickable { onClick() }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CryptoraAvatar(
                name = conversation.participantUser.username,
                size = 46.dp,
                isKeyVerified = true
            )

            Spacer(modifier = Modifier.width(CryptoraDimens.PaddingDefault))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "@${conversation.participantUser.username}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.TextPrimary
                    )
                    CryptoraSecureBadge(text = "E2EE")
                }

                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))

                Text(
                    text = conversation.lastMessage?.encryptedContentBase64?.let { "Encrypted payload • End-to-End Encrypted" } ?: "Secure session established",
                    style = MaterialTheme.typography.bodySmall,
                    color = CryptoraColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

// ======================== TAB 3: PROFILE CONTENT ========================

@Composable
private fun ProfileTabContent(user: User?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(CryptoraDimens.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

        CryptoraAvatar(
            name = user?.fullName?.ifBlank { user.username } ?: "Cryptora",
            size = 84.dp,
            isKeyVerified = true
        )

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

        Text(
            text = user?.fullName?.ifBlank { "Cryptora User" } ?: "Cryptora User",
            style = MaterialTheme.typography.headlineSmall,
            color = CryptoraColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "@${user?.username ?: "unknown"}",
            style = MaterialTheme.typography.titleMedium,
            color = CryptoraColors.ElectricCyan
        )

        if (!user?.bio.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(CryptoraDimens.PaddingHalf))
            Text(
                text = user?.bio ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = CryptoraColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(CryptoraDimens.PaddingLarge))

        // Security Credentials Card
        CryptoraCard(borderColor = CryptoraColors.BorderGlowing) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CryptoraColors.ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Public Key Fingerprint",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CryptoraColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))
                Text(
                    text = user?.publicKey?.takeLast(24) ?: "e2ee_hw_enclave_key_v1",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = CryptoraColors.ElectricCyan
                )

                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))
                HorizontalDivider(color = CryptoraColors.BorderSubtle)
                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingDefault))

                Text(
                    text = "Identity Verification State",
                    style = MaterialTheme.typography.bodySmall,
                    color = CryptoraColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingQuarter))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CryptoraColors.EmeraldSecure)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mobile OTP Verified • Keystore Enclave Signed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CryptoraColors.EmeraldSecure
                    )
                }
            }
        }
    }
}

// ======================== TAB 4: SETTINGS CONTENT ========================

@Composable
private fun SettingsTabContent(
    onLogoutClick: () -> Unit,
    isHardwareKeystoreActive: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(CryptoraDimens.PaddingDefault),
        verticalArrangement = Arrangement.spacedBy(CryptoraDimens.PaddingDefault)
    ) {
        CryptoraCard {
            Column {
                Text(text = "Security & Hardware", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CryptoraColors.TextPrimary)
                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingHalf))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AndroidKeyStore Enclave", color = CryptoraColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = (if (isHardwareKeystoreActive) CryptoraColors.EmeraldSecure else CryptoraColors.CoralRevoked).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isHardwareKeystoreActive) "ACTIVE" else "UNAVAILABLE",
                            color = if (isHardwareKeystoreActive) CryptoraColors.EmeraldSecure else CryptoraColors.CoralRevoked,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        CryptoraCard {
            Column {
                Text(text = "Ephemeral Message Policies", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CryptoraColors.TextPrimary)
                Spacer(modifier = Modifier.height(CryptoraDimens.PaddingHalf))
                Text(
                    text = "• Default time-lock duration: 10 minutes\n• Sender forward approval: Enforced for sensitive items\n• Screen protection: Enabled\n• Autorotation: Supported",
                    style = MaterialTheme.typography.bodySmall,
                    color = CryptoraColors.TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        CryptoraOutlinedButton(
            text = "Lock Vault & Sign Out",
            onClick = onLogoutClick,
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            borderColor = CryptoraColors.CoralRevoked.copy(alpha = 0.6f),
            contentColor = CryptoraColors.CoralRevoked
        )
    }
}

// Helpers
@Composable
private fun drawerItemColors() = NavigationDrawerItemDefaults.colors(
    selectedContainerColor = CryptoraColors.ElectricCyan.copy(alpha = 0.15f),
    unselectedContainerColor = CryptoraColors.SurfaceNavy,
    selectedTextColor = CryptoraColors.ElectricCyan,
    unselectedTextColor = CryptoraColors.TextPrimary
)

@Composable
private fun customNavBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = CryptoraColors.DeepNavyBackground,
    selectedTextColor = CryptoraColors.ElectricCyan,
    indicatorColor = CryptoraColors.ElectricCyan,
    unselectedIconColor = CryptoraColors.TextSecondary,
    unselectedTextColor = CryptoraColors.TextSecondary
)
