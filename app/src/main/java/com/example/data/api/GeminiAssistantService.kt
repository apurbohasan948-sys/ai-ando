package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.db.CredentialEntity
import com.example.data.db.MemoryRuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAssistantService {

    private val apiService = RetrofitClient.geminiService
    private val moshi = RetrofitClient.moshiInstance
    private val adapter = moshi.adapter(AuraAiActionResponse::class.java)

    suspend fun processUserCommand(
        userInput: String,
        currentUrl: String,
        pageTitle: String,
        domContentSummary: String,
        availableCredentials: List<CredentialEntity>,
        learnedRules: List<MemoryRuleEntity>,
        preferredLanguage: String = "en" // "en" or "bn"
    ): AuraAiActionResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackResponse(
                userInput = userInput,
                currentUrl = currentUrl,
                language = preferredLanguage,
                error = "Gemini API Key is missing or not configured in Secrets panel."
            )
        }

        val credentialsPrompt = if (availableCredentials.isNotEmpty()) {
            "Available Encrypted Vault Service Domains: " + availableCredentials.joinToString(", ") { "${it.serviceDomain} (User: ${it.username})" }
        } else {
            "No stored credentials in local vault."
        }

        val rulesPrompt = if (learnedRules.isNotEmpty()) {
            "Stored Learned Task Memory & Rules:\n" + learnedRules.joinToString("\n") { "- [${it.category} for ${it.domainFilter}] ${it.ruleTitle}: ${it.ruleDescription}" }
        } else {
            "No prior task patterns stored in memory."
        }

        val langInstruction = if (preferredLanguage == "bn") {
            "You MUST output the spokenResponse strictly in Bengali (বাংলা). Response should be natural and polite."
        } else {
            "Output the spokenResponse in clear English."
        }

        val systemPrompt = """
            You are AuraAssistant, an autonomous full-stack AI Assistant controlling an embedded WebView browser on Android.
            Language Mode: $preferredLanguage. $langInstruction

            System Architecture Guidelines:
            1. Analyze user intent from voice/text in English or Bengali (বাংলা).
            2. Current WebView Context:
               - URL: "$currentUrl"
               - Page Title: "$pageTitle"
               - DOM Summary / Forms: "$domContentSummary"
            3. Vault Context: $credentialsPrompt
            4. Memory Context: $rulesPrompt

            Capabilities:
            - If user asks to search or open a web page (e.g. "Go to Wikipedia", "Search for latest quantum news"):
              Set actionType = "NAVIGATE" or "EXECUTE_JS" and provide targetUrl or search query.
            - If user asks to auto-fill forms, click buttons, scroll, or extract content from current web page:
              Set actionType = "EXECUTE_JS" and generate precise, safe Vanilla JavaScript code in 'jsCode'.
              JS Examples:
              - Form fill: "document.querySelector('input[type=search], input[name=q], input[type=text]').value = 'query'; document.querySelector('form').submit();"
              - Scroll: "window.scrollTo({top: 500, behavior: 'smooth'});"
              - Data extract: "var data = Array.from(document.querySelectorAll('h1, h2, p')).map(e=>e.innerText).join('\n'); AuraBridge.onDataExtracted(data.substring(0, 500));"
            - If user asks to log in or use saved credentials:
              Set actionType = "REQUEST_CREDENTIAL_AUTH" and set requestedCredentialDomain = domain name.
            - If user asks to learn or remember a pattern/preference (or a task succeeded):
              Set actionType = "UPDATE_MEMORY", provide newMemoryCategory ("TASK_PATTERN" / "USER_PREFERENCE"), newMemoryTitle, newMemoryDescription.

            MUST return JSON matching this exact structure:
            {
              "spokenResponse": "Voice reply to user in Bengali or English",
              "actionType": "NAVIGATE" | "EXECUTE_JS" | "REQUEST_CREDENTIAL_AUTH" | "UPDATE_MEMORY" | "CONVERSATION",
              "targetUrl": "https://..." (or null),
              "jsCode": "document.querySelector(...)..." (or null),
              "requestedCredentialDomain": "domain.com" (or null),
              "newMemoryCategory": "TASK_PATTERN" (or null),
              "newMemoryTitle": "Title" (or null),
              "newMemoryDescription": "Description" (or null),
              "extractedResultSummary": "Summary of data extracted" (or null)
            }
        """.trimIndent()

        val userPrompt = "User Command: \"$userInput\""

        val requestBody = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = userPrompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = systemPrompt))
            )
        )

        try {
            val response = apiService.generateContent(apiKey, requestBody)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val parsed = adapter.fromJson(jsonText)
                if (parsed != null) return@withContext parsed
            }
            return@withContext fallbackResponse(userInput, currentUrl, preferredLanguage, "Failed to parse AI response")
        } catch (e: Exception) {
            Log.e("GeminiService", "API call error", e)
            return@withContext fallbackResponse(userInput, currentUrl, preferredLanguage, e.localizedMessage ?: "Network error")
        }
    }

    private fun fallbackResponse(
        userInput: String,
        currentUrl: String,
        language: String,
        error: String
    ): AuraAiActionResponse {
        val lower = userInput.lowercase()
        val isBn = language == "bn"

        return when {
            lower.contains("google") || lower.contains("search") || lower.contains("খুঁজো") || lower.contains("সার্চ") -> {
                val query = userInput.replace("search", "", ignoreCase = true)
                    .replace("খুঁজো", "", ignoreCase = true)
                    .replace("সার্চ", "", ignoreCase = true).trim()
                val target = if (query.isNotEmpty()) "https://www.google.com/search?q=${query}" else "https://www.google.com"
                AuraAiActionResponse(
                    spokenResponse = if (isBn) "গুগলে সার্চ নেভিগেট করা হচ্ছে..." else "Navigating web search for: $query",
                    actionType = "NAVIGATE",
                    targetUrl = target,
                    extractedResultSummary = "Local fallback triggered ($error)"
                )
            }
            lower.contains("scroll") || lower.contains("স্ক্রোল") -> {
                AuraAiActionResponse(
                    spokenResponse = if (isBn) "ওয়েব পেজটি নিচের দিকে স্ক্রোল করা হচ্ছে।" else "Scrolling web page down.",
                    actionType = "EXECUTE_JS",
                    jsCode = "window.scrollBy({top: 400, behavior: 'smooth'});",
                    extractedResultSummary = "Local JS scroll script executed"
                )
            }
            lower.contains("extract") || lower.contains("read") || lower.contains("পড়ো") || lower.contains("তথ্য") -> {
                AuraAiActionResponse(
                    spokenResponse = if (isBn) "পেজের প্রধান শিরোনাম ও লেখা সংগ্রহ করা হচ্ছে।" else "Extracting main page headings and content.",
                    actionType = "EXECUTE_JS",
                    jsCode = "var t = document.title + '\\n' + Array.from(document.querySelectorAll('h1, h2, p')).slice(0, 5).map(e => e.innerText).join('\\n'); AuraBridge.onDataExtracted(t);",
                    extractedResultSummary = "Page text extraction initiated"
                )
            }
            lower.contains("login") || lower.contains("password") || lower.contains("লগইন") || lower.contains("পাসওয়ার্ড") -> {
                AuraAiActionResponse(
                    spokenResponse = if (isBn) "ভল্ট থেকে নিরাপদ ক্রেডেনশিয়াল নিশ্চিতকরণ প্রয়োজন।" else "Requesting authorization to access credential vault.",
                    actionType = "REQUEST_CREDENTIAL_AUTH",
                    requestedCredentialDomain = if (currentUrl.contains("http")) currentUrl else "general",
                    extractedResultSummary = "Vault authorization requested"
                )
            }
            else -> {
                AuraAiActionResponse(
                    spokenResponse = if (isBn)
                        "কমান্ডটির জন্য প্রস্তুতি নিচ্ছি। $error। সেটিংসে GEMINI_API_KEY কনফিগার করুন।"
                    else
                        "Processing command locally ($error). Please ensure GEMINI_API_KEY is configured in Secrets panel.",
                    actionType = "CONVERSATION",
                    extractedResultSummary = "System ready. API Status: $error"
                )
            }
        }
    }
}
