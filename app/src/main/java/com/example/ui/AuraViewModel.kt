package com.example.ui

import android.app.Application
import android.content.Context
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AuraAiActionResponse
import com.example.data.api.GeminiAssistantService
import com.example.data.db.AppDatabase
import com.example.data.db.CredentialEntity
import com.example.data.db.MemoryRuleEntity
import com.example.data.db.SecurityManager
import com.example.data.memory.MemoryStore
import com.example.data.memory.RoomMemoryStore
import com.example.voice.SpeechNlpManager
import com.example.webview.WebViewBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AURA"
    val text: String,
    val actionType: String? = null,
    val jsExecuted: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class SystemLogEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val module: String, // "VOICE_NLP", "GEMINI_AI", "WEBVIEW_BRIDGE", "ROOM_VAULT", "SELF_MEMORY"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val credentialDao = db.credentialDao()
    private val memoryStore: MemoryStore = RoomMemoryStore(db.memoryRuleDao())

    val geminiService = GeminiAssistantService()
    val speechManager = SpeechNlpManager(application)
    val webViewBridge = WebViewBridge()

    private var activeWebView: WebView? = null

    // UI States
    private val prefs = application.getSharedPreferences("aura_settings", Context.MODE_PRIVATE)

    private val _customApiKey = MutableStateFlow(prefs.getString("custom_gemini_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _currentUrl = MutableStateFlow("https://www.google.com")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("Google")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    private val _domSummary = MutableStateFlow("No page inspected yet")
    val domSummary: StateFlow<String> = _domSummary.asStateFlow()

    private val _extractedContent = MutableStateFlow("")
    val extractedContent: StateFlow<String> = _extractedContent.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "AURA",
                text = "Hello! I am AuraAssistant, your autonomous web agent. I speak English and Bengali (বাংলা). How can I assist you today?"
            )
        )
    )
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _systemLogs = MutableStateFlow<List<SystemLogEvent>>(emptyList())
    val systemLogs: StateFlow<List<SystemLogEvent>> = _systemLogs.asStateFlow()

    val credentialsList: StateFlow<List<CredentialEntity>> = credentialDao.getAllCredentials()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memoryRulesList: StateFlow<List<MemoryRuleEntity>> = memoryStore.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Authorization Modal state
    private val _pendingAuthCredential = MutableStateFlow<CredentialEntity?>(null)
    val pendingAuthCredential: StateFlow<CredentialEntity?> = _pendingAuthCredential.asStateFlow()

    private val _isAuthModalVisible = MutableStateFlow(false)
    val isAuthModalVisible: StateFlow<Boolean> = _isAuthModalVisible.asStateFlow()

    val isListening: StateFlow<Boolean> = speechManager.isListening
    val selectedLanguage: StateFlow<String> = speechManager.selectedLanguage

    init {
        logEvent("SYSTEM_INIT", "AuraAssistant architecture initialized.")

        // Pre-populate initial memory rules if empty
        viewModelScope.launch {
            memoryStore.getAllRules().collect { rules ->
                if (rules.isEmpty()) {
                    memoryStore.saveTaskPattern(
                        domainFilter = "google.com",
                        title = "Google Search Auto-Fill",
                        description = "Locates textarea/input[name=q] and triggers submit.",
                        jsSnippet = "document.querySelector('textarea, input[name=q]').value = 'QUERY'; document.querySelector('form').submit();"
                    )
                    memoryStore.saveUserPreference(
                        domainFilter = "*",
                        title = "Bengali Voice Default",
                        description = "User prefers polite Bengali speech feedback for web operations."
                    )
                }
            }
        }

        // Listen for WebView bridge events
        viewModelScope.launch {
            webViewBridge.extractedDataFlow.collect { event ->
                _extractedContent.value = event.content
                logEvent("WEBVIEW_BRIDGE", "Extracted page data (${event.content.length} chars).")
                addChatMessage("AURA", "Data extracted: ${event.content.take(150)}...")
            }
        }

        viewModelScope.launch {
            webViewBridge.pageInspectionFlow.collect { event ->
                _domSummary.value = "Title: ${event.title}\nForms: ${event.formsJson}"
                logEvent("WEBVIEW_BRIDGE", "DOM Inspected: ${event.title}")
            }
        }

        viewModelScope.launch {
            webViewBridge.credentialRequestedFlow.collect { domain ->
                logEvent("ROOM_VAULT", "Credential authorization requested for domain: $domain")
                requestCredentialInjectionForDomain(domain)
            }
        }
    }

    fun setWebViewInstance(webView: WebView) {
        activeWebView = webView
    }

    fun setLanguage(langCode: String) {
        speechManager.setLanguage(langCode)
        logEvent("VOICE_NLP", "Language updated to: ${if (langCode == "bn") "Bengali (বাংলা)" else "English"}")
    }

    fun updateUrl(newUrl: String) {
        _currentUrl.value = newUrl
    }

    fun updateTitle(title: String) {
        _pageTitle.value = title
    }

    fun updateLoadingState(loading: Boolean, progress: Int) {
        _isLoading.value = loading
        _loadingProgress.value = progress
    }

    fun navigateTo(url: String) {
        var formatted = url.trim()
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            formatted = if (formatted.contains(".")) "https://$formatted" else "https://www.google.com/search?q=$formatted"
        }
        _currentUrl.value = formatted
        logEvent("WEBVIEW_BRIDGE", "Navigating to: $formatted")
    }

    fun processUserInput(input: String) {
        if (input.isBlank()) return

        val lang = speechManager.selectedLanguage.value
        addChatMessage("USER", input)
        logEvent("VOICE_NLP", "Processing input ($lang): \"$input\"")

        viewModelScope.launch {
            val credentials = credentialDao.getCredentialsForDomain(_currentUrl.value)
            val rules = memoryStore.getRulesForDomain(_currentUrl.value)

            logEvent("GEMINI_AI", "Calling Gemini 3.5 Flash for autonomous decision...")

            val actionResponse = geminiService.processUserCommand(
                userInput = input,
                currentUrl = _currentUrl.value,
                pageTitle = _pageTitle.value,
                domContentSummary = _domSummary.value,
                availableCredentials = credentials,
                learnedRules = rules,
                preferredLanguage = lang,
                customApiKey = _customApiKey.value
            )

            logEvent("GEMINI_AI", "Action determined: ${actionResponse.actionType}. Voice reply: ${actionResponse.spokenResponse}")

            // Speak response via TTS
            speechManager.speak(actionResponse.spokenResponse)

            // Add AI chat entry
            addChatMessage(
                sender = "AURA",
                text = actionResponse.spokenResponse,
                actionType = actionResponse.actionType,
                jsExecuted = actionResponse.jsCode
            )

            // Execute action
            executeAiAction(actionResponse)
        }
    }

    private fun executeAiAction(action: AuraAiActionResponse) {
        when (action.actionType) {
            "NAVIGATE" -> {
                action.targetUrl?.let { navigateTo(it) }
            }
            "EXECUTE_JS" -> {
                action.jsCode?.let { js ->
                    evaluateJavaScript(js)
                }
            }
            "REQUEST_CREDENTIAL_AUTH" -> {
                val domain = action.requestedCredentialDomain ?: _currentUrl.value
                requestCredentialInjectionForDomain(domain)
            }
            "UPDATE_MEMORY" -> {
                if (action.newMemoryTitle != null && action.newMemoryDescription != null) {
                    viewModelScope.launch {
                        memoryStore.saveRule(
                            MemoryRuleEntity(
                                category = action.newMemoryCategory ?: "TASK_PATTERN",
                                domainFilter = _currentUrl.value,
                                ruleTitle = action.newMemoryTitle,
                                ruleDescription = action.newMemoryDescription,
                                jsSnippet = action.jsCode
                            )
                        )
                        logEvent("SELF_MEMORY", "New task rule recorded: ${action.newMemoryTitle}")
                    }
                }
            }
        }
    }

    fun evaluateJavaScript(jsCode: String) {
        logEvent("WEBVIEW_BRIDGE", "Executing JS: $jsCode")
        activeWebView?.evaluateJavascript(jsCode) { result ->
            logEvent("WEBVIEW_BRIDGE", "JS Execution Result: $result")
        }
    }

    fun startVoiceListening() {
        speechManager.startListening { text ->
            processUserInput(text)
        }
    }

    fun stopVoiceListening() {
        speechManager.stopListening()
    }

    // Secure Vault Operations
    fun saveCredential(domain: String, label: String, username: String, pass: String) {
        viewModelScope.launch {
            val encryptedPass = SecurityManager.encrypt(pass)
            val credential = CredentialEntity(
                serviceDomain = domain,
                accountLabel = label,
                username = username,
                encryptedPassword = encryptedPass
            )
            credentialDao.insertCredential(credential)
            logEvent("ROOM_VAULT", "Credential saved for domain $domain (AES encrypted)")
        }
    }

    fun deleteCredential(credential: CredentialEntity) {
        viewModelScope.launch {
            credentialDao.deleteCredential(credential)
            logEvent("ROOM_VAULT", "Credential deleted for ${credential.serviceDomain}")
        }
    }

    private fun requestCredentialInjectionForDomain(domain: String) {
        viewModelScope.launch {
            val matches = credentialDao.getCredentialsForDomain(domain)
            if (matches.isNotEmpty()) {
                _pendingAuthCredential.value = matches.first()
                _isAuthModalVisible.value = true
            } else {
                addChatMessage("AURA", "No matching credentials found in vault for $domain.")
            }
        }
    }

    fun authorizeCredentialInjection() {
        val cred = _pendingAuthCredential.value ?: return
        val decryptedPass = SecurityManager.decrypt(cred.encryptedPassword)
        val fillJs = """
            (function() {
                var userInputs = document.querySelectorAll('input[type=email], input[type=text], input[name*=user], input[name*=login]');
                var passInputs = document.querySelectorAll('input[type=password]');
                if (userInputs.length > 0) userInputs[0].value = '${cred.username}';
                if (passInputs.length > 0) passInputs[0].value = '$decryptedPass';
                return 'Auto-filled username & password for ${cred.username}';
            })();
        """.trimIndent()

        evaluateJavaScript(fillJs)
        logEvent("ROOM_VAULT", "Authorized credential injected into WebView for ${cred.serviceDomain}")
        addChatMessage("AURA", "Credentials securely injected for ${cred.serviceDomain} after user authorization.")
        _isAuthModalVisible.value = false
        _pendingAuthCredential.value = null
    }

    fun cancelCredentialInjection() {
        logEvent("ROOM_VAULT", "User denied credential injection request.")
        addChatMessage("AURA", "Credential injection cancelled by user.")
        _isAuthModalVisible.value = false
        _pendingAuthCredential.value = null
    }

    // Memory Rule Management
    fun addMemoryRule(category: String, domainFilter: String, title: String, desc: String, js: String?) {
        viewModelScope.launch {
            memoryStore.saveRule(
                MemoryRuleEntity(
                    category = category,
                    domainFilter = domainFilter,
                    ruleTitle = title,
                    ruleDescription = desc,
                    jsSnippet = js
                )
            )
            logEvent("SELF_MEMORY", "Rule added manually: $title")
        }
    }

    fun deleteMemoryRule(rule: MemoryRuleEntity) {
        viewModelScope.launch {
            memoryStore.deleteRule(rule)
            logEvent("SELF_MEMORY", "Rule deleted: ${rule.ruleTitle}")
        }
    }

    fun saveCustomApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        prefs.edit().putString("custom_gemini_api_key", trimmed).apply()
        _customApiKey.value = trimmed
        logEvent("SETTINGS", "Custom Gemini API Key updated in local storage.")
    }

    fun clearCustomApiKey() {
        prefs.edit().remove("custom_gemini_api_key").apply()
        _customApiKey.value = ""
        logEvent("SETTINGS", "Custom Gemini API Key cleared.")
    }

    private fun addChatMessage(
        sender: String,
        text: String,
        actionType: String? = null,
        jsExecuted: String? = null
    ) {
        val msg = ChatMessage(
            sender = sender,
            text = text,
            actionType = actionType,
            jsExecuted = jsExecuted
        )
        _chatHistory.value = _chatHistory.value + msg
    }

    private fun logEvent(module: String, message: String) {
        val event = SystemLogEvent(module = module, message = message)
        _systemLogs.value = listOf(event) + _systemLogs.value.take(49)
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
    }
}
