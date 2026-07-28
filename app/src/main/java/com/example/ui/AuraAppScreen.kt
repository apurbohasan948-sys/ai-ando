package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.CredentialAuthModal
import com.example.ui.screens.GeminiSetupScreen
import com.example.ui.screens.MemoryHubScreen
import com.example.ui.screens.SecureVaultScreen
import com.example.ui.screens.SystemArchitectureScreen
import com.example.ui.screens.WebViewBrowserScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraAppScreen(viewModel: AuraViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val pendingCredential by viewModel.pendingAuthCredential.collectAsState()
    val isAuthModalVisible by viewModel.isAuthModalVisible.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AuraAssistant AI",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Language, contentDescription = "Browser") },
                    label = { Text("Browser") },
                    modifier = Modifier.testTag("tab_browser")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = "Architecture") },
                    label = { Text("System") },
                    modifier = Modifier.testTag("tab_architecture")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Vault") },
                    label = { Text("Vault") },
                    modifier = Modifier.testTag("tab_vault")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Memory, contentDescription = "Memory") },
                    label = { Text("Memory") },
                    modifier = Modifier.testTag("tab_memory")
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Setup") },
                    label = { Text("Setup") },
                    modifier = Modifier.testTag("tab_setup")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> WebViewBrowserScreen(viewModel = viewModel)
                1 -> SystemArchitectureScreen(viewModel = viewModel)
                2 -> SecureVaultScreen(viewModel = viewModel)
                3 -> MemoryHubScreen(viewModel = viewModel)
                4 -> GeminiSetupScreen(viewModel = viewModel)
            }

            // Credential Injection Authorization Guard Dialog
            if (isAuthModalVisible && pendingCredential != null) {
                CredentialAuthModal(
                    credential = pendingCredential!!,
                    onAuthorize = { viewModel.authorizeCredentialInjection() },
                    onCancel = { viewModel.cancelCredentialInjection() }
                )
            }
        }
    }
}
