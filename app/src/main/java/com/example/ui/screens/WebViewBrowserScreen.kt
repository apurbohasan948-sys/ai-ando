package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.South
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.AuraViewModel
import com.example.ui.ChatMessage
import com.example.webview.AuraWebViewContainer

@Composable
fun WebViewBrowserScreen(viewModel: AuraViewModel) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val pageTitle by viewModel.pageTitle.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    val isAutoAgentActive by viewModel.isAutoAgentActive.collectAsState()

    var urlInputText by remember(currentUrl) { mutableStateOf(currentUrl) }
    var promptInputText by remember { mutableStateOf("") }
    var isChatLogsExpanded by remember { mutableStateOf(false) }

    var showPostDialog by remember { mutableStateOf(false) }
    var postContentInput by remember { mutableStateOf("") }

    if (showPostDialog) {
        AlertDialog(
            onDismissRequest = { showPostDialog = false },
            title = { Text("Facebook Group Auto-Post (গ্রুপ পোস্ট)") },
            text = {
                Column {
                    Text(
                        text = "খোলা ফেসবুক গ্রুপে স্বয়ংক্রিয়ভাবে লেখার জন্য নিচের বক্সে আপনার পোস্টটি টাইপ করুন:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = postContentInput,
                        onValueChange = { postContentInput = it },
                        label = { Text("Post Content / Capton") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (postContentInput.isNotBlank()) {
                            viewModel.triggerFbGroupPost(postContentInput)
                            showPostDialog = false
                            postContentInput = ""
                        }
                    }
                ) {
                    Text("পোস্ট করুন (Post)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPostDialog = false }) {
                    Text("বাতিল (Cancel)")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // --- Top Address Bar ---
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { viewModel.evaluateJavaScript("window.history.back();") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    IconButton(
                        onClick = { viewModel.evaluateJavaScript("window.history.forward();") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                    }

                    OutlinedTextField(
                        value = urlInputText,
                        onValueChange = { urlInputText = it },
                        singleLine = true,
                        placeholder = { Text("Search or enter web URL") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.navigateTo(urlInputText) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Go", modifier = Modifier.size(18.dp))
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { viewModel.navigateTo(urlInputText) }),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("url_input_field")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Language Mode Toggle
                    AssistChip(
                        onClick = {
                            val newLang = if (selectedLanguage == "en") "bn" else "en"
                            viewModel.setLanguage(newLang)
                        },
                        label = {
                            Text(if (selectedLanguage == "bn") "বাংলা" else "ENG")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = "Language",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("language_chip")
                    )
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }

        // --- WebView Main Canvas ---
        Box(modifier = Modifier.weight(1f)) {
            AuraWebViewContainer(
                currentUrl = currentUrl,
                bridge = viewModel.webViewBridge,
                onUrlChanged = { viewModel.updateUrl(it) },
                onTitleChanged = { viewModel.updateTitle(it) },
                onLoadingStateChanged = { loading, progress -> viewModel.updateLoadingState(loading, progress) },
                onWebViewCreated = { webView -> viewModel.setWebViewInstance(webView) },
                modifier = Modifier.fillMaxSize()
            )

            // Chat & Log Drawer Overlay Toggle
            Card(
                onClick = { isChatLogsExpanded = !isChatLogsExpanded },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("chat_overlay_card")
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) Color.Red else Color.Green)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = chatHistory.lastOrNull()?.text?.take(40) ?: "AuraAssistant Ready",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = if (isChatLogsExpanded) "Hide Logs ▼" else "Show Logs ▲",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = isChatLogsExpanded) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(top = 8.dp)
                        ) {
                            items(chatHistory.takeLast(10)) { chat ->
                                ChatBubbleItem(chat)
                            }
                        }
                    }
                }
            }
        }

        // --- Quick AI & Facebook Automation Action Toolbar ---
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = isAutoAgentActive,
                    onClick = { viewModel.toggleAutoAgent() },
                    label = {
                        Text(
                            text = if (isAutoAgentActive) "Auto Agent: ON" else "Auto Agent: OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isAutoAgentActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                )
            }

            item {
                AssistChip(
                    onClick = { viewModel.processUserInput("login with google") },
                    label = { Text("🔑 Google Login (গুগল লগইন)", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            item {
                AssistChip(
                    onClick = { viewModel.processUserInput("extract video links from current page") },
                    label = { Text("🎬 Extract Video Link (ভিডিও লিঙ্ক নিন)", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            item {
                AssistChip(
                    onClick = { viewModel.processUserInput("auto post to admin panel") },
                    label = { Text("🚀 Post to Admin (এডমিন পোস্টে টাইপ)", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            item {
                AssistChip(
                    onClick = { viewModel.navigateTo("https://ramimhasan820.netlify.app/") },
                    label = { Text("🌐 Movie Admin Portal", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            item {
                AssistChip(
                    onClick = { viewModel.readCurrentPageContent() },
                    label = { Text("📖 Read Page (পেজ পড়ুন)", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            item {
                AssistChip(
                    onClick = { viewModel.navigateTo("https://m.facebook.com") },
                    label = { Text("Open Facebook", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            item {
                AssistChip(
                    onClick = { viewModel.triggerFbAutoReply("ধন্যবাদ! আমি অরা এআই অটোমেটেড মেসেজ।") },
                    label = { Text("💬 FB Auto-Reply (টাইপ রিপ্লাই)", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            item {
                AssistChip(
                    onClick = { showPostDialog = true },
                    label = { Text("📢 Group Post (টাইপ পোস্ট)", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            item {
                AssistChip(
                    onClick = { viewModel.triggerFbAutoInvite() },
                    label = { Text("👥 Auto Invite", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            item {
                AssistChip(
                    onClick = {
                        viewModel.processUserInput("Extract all main headings and content from this page")
                    },
                    label = { Text("Extract Data", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            item {
                AssistChip(
                    onClick = {
                        viewModel.processUserInput("Scroll down the page smoothly")
                    },
                    label = { Text("Scroll Down", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.South, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            item {
                AssistChip(
                    onClick = {
                        viewModel.processUserInput("Check saved credentials and auto fill login form")
                    },
                    label = { Text("Auto Login", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        // --- Bottom Voice Command & Prompt Input Bar ---
        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (isListening) viewModel.stopVoiceListening() else viewModel.startVoiceListening()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                        .testTag("voice_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Command",
                        tint = if (isListening) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = promptInputText,
                    onValueChange = { promptInputText = it },
                    placeholder = {
                        Text(if (selectedLanguage == "bn") "বাংলা বা ইংরেজিতে নির্দেশ দিন..." else "Ask AI to navigate, search, or fill forms...")
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (promptInputText.isNotBlank()) {
                                    viewModel.processUserInput(promptInputText)
                                    promptInputText = ""
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (promptInputText.isNotBlank()) {
                            viewModel.processUserInput(promptInputText)
                            promptInputText = ""
                        }
                    }),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("prompt_input_field")
                )
            }
        }
    }
}

@Composable
fun ChatBubbleItem(chat: ChatMessage) {
    val isUser = chat.sender == "USER"
    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(
                    text = "${chat.sender}: ${chat.text}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (chat.jsExecuted != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "JS: ${chat.jsExecuted.take(40)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
