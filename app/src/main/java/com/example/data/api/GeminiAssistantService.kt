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
        preferredLanguage: String = "en", // "en" or "bn"
        customApiKey: String? = null
    ): AuraAiActionResponse = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) {
            customApiKey.trim()
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackResponse(
                userInput = userInput,
                currentUrl = currentUrl,
                language = preferredLanguage,
                error = if (preferredLanguage == "bn")
                    "Gemini API Key সেট করা নেই। Gemini AI Setup স্ক্রিনে নতুন API Key দিন।"
                else
                    "Gemini API Key missing. Please set your API Key in the Gemini AI Setup screen."
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
            - If user asks to search or open a web page (e.g. "Go to https://example.com", "Search for latest news"):
              Set actionType = "NAVIGATE" and provide clean targetUrl.
            - If user asks to click a button, login with Google, continue with Google, fill forms, or interact with DOM:
              Set actionType = "EXECUTE_JS" and generate precise Vanilla JavaScript code in 'jsCode'.
              JS Examples:
              - Click Google login button: "var b = Array.from(document.querySelectorAll('button, a, div[role=button], input')).find(e => /google|continue|sign in|login/i.test(e.innerText || e.value || e.getAttribute('aria-label') || '')); if(b) { b.click(); AuraBridge.onDataExtracted('Clicked button: ' + (b.innerText||'Login')); } else { AuraBridge.onDataExtracted('Google button not found'); }"
              - Type input: "if (window.AuraHumanType) { window.AuraHumanType('input[type=text]', 'text'); }"
              - Scroll: "window.scrollTo({top: 500, behavior: 'smooth'});"
              - Data extract: "var data = Array.from(document.querySelectorAll('h1, h2, p')).map(e=>e.innerText).join('\n'); AuraBridge.onDataExtracted(data.substring(0, 500));"
            - If user asks to log in or use saved credentials:
              Set actionType = "REQUEST_CREDENTIAL_AUTH" and set requestedCredentialDomain = domain name.
            - If user asks to learn or remember a pattern/preference:
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

        val modelCandidates = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
        var lastError = "Network error"

        for (model in modelCandidates) {
            try {
                Log.d("GeminiService", "Attempting API call with model: $model")
                val response = apiService.generateContent(model, apiKey, requestBody)
                val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (jsonText != null) {
                    val parsed = adapter.fromJson(jsonText)
                    if (parsed != null) return@withContext parsed
                }
            } catch (e: Exception) {
                Log.w("GeminiService", "Model $model failed: ${e.localizedMessage}")
                lastError = e.localizedMessage ?: "HTTP error"
            }
        }

        return@withContext fallbackResponse(userInput, currentUrl, preferredLanguage, lastError)
    }

    private fun fallbackResponse(
        userInput: String,
        currentUrl: String,
        language: String,
        error: String
    ): AuraAiActionResponse {
        val lower = userInput.lowercase().trim()
        val isBn = language == "bn"

        return when {
            // Facebook specific automation triggers
            lower.contains("facebook") || lower.contains("fb") || lower.contains("ফেসবুক") -> {
                val isOnFb = currentUrl.lowercase().contains("facebook.com")
                when {
                    lower.contains("reply") || lower.contains("message") || lower.contains("মেসেজ") || lower.contains("রিপ্লাই") || lower.contains("chat") -> {
                        if (!isOnFb) {
                            AuraAiActionResponse(
                                spokenResponse = if (isBn) "ফেসবুক মেসেঞ্জারে যাওয়া হচ্ছে..." else "Navigating to Facebook messages...",
                                actionType = "NAVIGATE",
                                targetUrl = "https://m.facebook.com/messages",
                                extractedResultSummary = "Navigating to Facebook messages"
                            )
                        } else {
                            val replyJs = """
                                (function() {
                                    var inputBox = document.querySelector('div[contenteditable="true"][role="textbox"], textarea[name="body"], input[type="text"][name="body"], div[aria-label="Message"], div[aria-label="মেসেজ"]');
                                    var sendBtn = document.querySelector('div[aria-label="Send"], button[type="submit"], div[role="button"][aria-label="পাঠান"], div[aria-label="Press enter to send"]');
                                    if (inputBox) {
                                        inputBox.focus();
                                        document.execCommand('insertText', false, 'Hello! Aura AI automated response.');
                                        if (sendBtn) { sendBtn.click(); }
                                        AuraBridge.onDataExtracted('FB Message auto-reply sent.');
                                    } else {
                                        AuraBridge.onDataExtracted('No open Facebook chat box found. Please open a chat first.');
                                    }
                                })();
                            """.trimIndent()
                            AuraAiActionResponse(
                                spokenResponse = if (isBn) "ফেসবুক মেসেজে অটো-রিপ্লাই দেওয়া হচ্ছে।" else "Executing Facebook message auto-reply.",
                                actionType = "EXECUTE_JS",
                                jsCode = replyJs,
                                extractedResultSummary = "FB Auto-reply triggered"
                            )
                        }
                    }
                    lower.contains("post") || lower.contains("group") || lower.contains("পোস্ট") || lower.contains("গ্রুপ") -> {
                        if (!isOnFb) {
                            AuraAiActionResponse(
                                spokenResponse = if (isBn) "ফেসবুক গ্রুপে নেভিগেট করা হচ্ছে..." else "Navigating to Facebook Groups...",
                                actionType = "NAVIGATE",
                                targetUrl = "https://m.facebook.com/groups",
                                extractedResultSummary = "Navigating to Facebook Groups"
                            )
                        } else {
                            val postJs = """
                                (function() {
                                    var postBox = document.querySelector('div[role="button"][aria-label*="Create"], div[role="button"][aria-label*="Write something"], div[role="button"][aria-label*="পোস্ট"]') || document.querySelector('div[contenteditable="true"]');
                                    if (postBox) {
                                        postBox.click();
                                        setTimeout(function() {
                                            var editor = document.querySelector('div[contenteditable="true"][role="textbox"]');
                                            if (editor) {
                                                editor.focus();
                                                document.execCommand('insertText', false, 'Aura Assistant Automated Group Post');
                                                AuraBridge.onDataExtracted('Post draft created in Facebook Group.');
                                            }
                                        }, 1000);
                                    } else {
                                        AuraBridge.onDataExtracted('Open a Facebook group to auto post.');
                                    }
                                })();
                            """.trimIndent()
                            AuraAiActionResponse(
                                spokenResponse = if (isBn) "ফেসবুক গ্রুপে পোস্ট ড্রাফট করা হচ্ছে।" else "Creating post draft in Facebook Group.",
                                actionType = "EXECUTE_JS",
                                jsCode = postJs,
                                extractedResultSummary = "FB Group Auto-post initiated"
                            )
                        }
                    }
                    lower.contains("invite") || lower.contains("friend") || lower.contains("ইনভাইট") || lower.contains("বন্ধু") -> {
                        if (!isOnFb) {
                            AuraAiActionResponse(
                                spokenResponse = if (isBn) "ফেসবুকে নেভিগেট করা হচ্ছে, ইনভাইট বাটনে স্বয়ংক্রিয়ভাবে ক্লিক করা হবে..." else "Navigating to Facebook to send friend invites...",
                                actionType = "NAVIGATE",
                                targetUrl = "https://m.facebook.com/friends",
                                extractedResultSummary = "Navigating to Facebook friends/invites page"
                            )
                        } else {
                            val inviteJs = """
                                (function() {
                                    var btns = Array.from(document.querySelectorAll('div[role="button"][aria-label*="Invite"], div[role="button"][aria-label*="ইনভাইট"], div[aria-label*="Add Friend"], div[aria-label*="যোগ করুন"]'));
                                    if (btns.length === 0) {
                                        btns = Array.from(document.querySelectorAll('button, div[role="button"]')).filter(function(e) { return /invite|আমন্ত্রণ|add friend|ফ্রেন্ড তৈরি/i.test(e.innerText); });
                                    }
                                    var count = 0;
                                    btns.slice(0, 5).forEach(function(btn, i) {
                                        setTimeout(function() { btn.click(); count++; }, i * 1000);
                                    });
                                    AuraBridge.onDataExtracted('Auto invite triggered on ' + btns.length + ' profile/group buttons.');
                                })();
                            """.trimIndent()
                            AuraAiActionResponse(
                                spokenResponse = if (isBn) "ফেসবুক ইনভাইট বাটনে ক্লিক পাঠানো হচ্ছে।" else "Sending auto-invites on Facebook.",
                                actionType = "EXECUTE_JS",
                                jsCode = inviteJs,
                                extractedResultSummary = "FB Auto-invite executed"
                            )
                        }
                    }
                    else -> {
                        AuraAiActionResponse(
                            spokenResponse = if (isBn) "ফেসবুকে নেভিগেট করা হচ্ছে..." else "Navigating to Facebook...",
                            actionType = "NAVIGATE",
                            targetUrl = "https://m.facebook.com",
                            extractedResultSummary = "Navigating to m.facebook.com"
                        )
                    }
                }
            }
            // Google Login or Button click triggers
            lower.contains("login with google") || lower.contains("continue with google") || lower.contains("sign in with google") ||
            (lower.contains("google") && (lower.contains("login") || lower.contains("sign in") || lower.contains("continue") || lower.contains("লগইন") || lower.contains("বাটন") || lower.contains("ক্লিক"))) -> {
                val googleClickJs = """
                    (function() {
                        var targets = Array.from(document.querySelectorAll('button, a, div[role="button"], input[type="submit"], input[type="button"], span[role="button"], div[aria-label]'));
                        var googleMatch = targets.find(function(el) {
                            var txt = (el.innerText || el.value || el.getAttribute('aria-label') || el.title || '').toLowerCase();
                            return txt.includes('google') || txt.includes('continue with') || txt.includes('sign in') || txt.includes('log in');
                        });
                        if (!googleMatch) {
                            var all = Array.from(document.querySelectorAll('*'));
                            googleMatch = all.find(function(el) {
                                var txt = (el.innerText || '').toLowerCase();
                                return (txt.includes('continue with google') || txt.includes('sign in with google') || txt.includes('login with google')) && el.children.length === 0;
                            });
                        }
                        if (googleMatch) {
                            googleMatch.focus();
                            googleMatch.click();
                            AuraBridge.onDataExtracted('Clicked Google login button: ' + (googleMatch.innerText || 'Google Sign-In'));
                        } else {
                            AuraBridge.onDataExtracted('Google login button not found on this page.');
                        }
                    })();
                """.trimIndent()
                AuraAiActionResponse(
                    spokenResponse = if (isBn) "গুগল লগইন বাটনে ক্লিক করা হচ্ছে..." else "Clicking Google login button...",
                    actionType = "EXECUTE_JS",
                    jsCode = googleClickJs,
                    extractedResultSummary = "Google login button click executed"
                )
            }
            // Dynamic link creation / Website extraction triggers
            lower.contains("link") || lower.contains("লিঙ্ক") || lower.contains("ইউআরএল") || lower.contains("url") -> {
                val linkJs = """
                    (function() {
                        var pageUrl = window.location.href;
                        var title = document.title;
                        var metaDesc = document.querySelector('meta[name="description"]')?.content || '';
                        var summary = "URL: " + pageUrl + "\nTitle: " + title + "\nDescription: " + metaDesc;
                        AuraBridge.onDataExtracted(summary);
                    })();
                """.trimIndent()
                AuraAiActionResponse(
                    spokenResponse = if (isBn) "বর্তমান পেজের লিঙ্ক ও তথ্য সংগ্রহ করা হয়েছে।" else "Extracted page URL and metadata link.",
                    actionType = "EXECUTE_JS",
                    jsCode = linkJs,
                    extractedResultSummary = "Web link extracted"
                )
            }
            // Direct URL or Site navigation triggers (Extract clean URL using Regex)
            Regex("https?://[a-zA-Z0-9.-]+(?:/[^\\s]*)?").containsMatchIn(userInput) -> {
                val cleanUrl = Regex("https?://[a-zA-Z0-9.-]+(?:/[^\\s]*)?").find(userInput)?.value ?: "https://www.google.com"
                AuraAiActionResponse(
                    spokenResponse = if (isBn) "$cleanUrl এ যাওয়া হচ্ছে..." else "Navigating to $cleanUrl",
                    actionType = "NAVIGATE",
                    targetUrl = cleanUrl,
                    extractedResultSummary = "Extracted clean URL navigation"
                )
            }
            lower.contains("goto") || lower.contains("open") || lower.contains("go to") || lower.contains("যাও") || lower.contains("ওপেন") || lower.contains(".com") || lower.contains(".org") || lower.contains(".net") -> {
                var clean = lower
                    .replace("goto", "")
                    .replace("go to", "")
                    .replace("open", "")
                    .replace("যাও", "")
                    .replace("ওপেন", "")
                    .replace("সাইটে", "")
                    .trim()

                val targetUrl = when {
                    clean.contains(".") -> if (clean.startsWith("http")) clean else "https://$clean"
                    clean.contains("facebook") || clean.contains("fb") -> "https://m.facebook.com"
                    clean.contains("youtube") -> "https://m.youtube.com"
                    clean.contains("github") -> "https://github.com"
                    else -> "https://www.google.com/search?q=${clean}"
                }

                AuraAiActionResponse(
                    spokenResponse = if (isBn) "$targetUrl এ নেভিগেট করা হচ্ছে..." else "Navigating to $targetUrl",
                    actionType = "NAVIGATE",
                    targetUrl = targetUrl,
                    extractedResultSummary = "Site navigation triggered"
                )
            }
            // Explicit Web Search triggers only
            lower.startsWith("search") || lower.contains("search for") || lower.contains("খুঁজো") || lower.contains("সার্চ") -> {
                val query = userInput.replace("search for", "", ignoreCase = true)
                    .replace("search", "", ignoreCase = true)
                    .replace("খুঁজো", "", ignoreCase = true)
                    .replace("সার্চ", "", ignoreCase = true).trim()
                val target = if (query.isNotEmpty()) "https://www.google.com/search?q=${query}" else "https://www.google.com"
                AuraAiActionResponse(
                    spokenResponse = if (isBn) "গুগলে সার্চ নেভিগেট করা হচ্ছে..." else "Navigating web search for: $query",
                    actionType = "NAVIGATE",
                    targetUrl = target,
                    extractedResultSummary = "Web search triggered"
                )
            }
            // Button Click triggers
            lower.contains("click") || lower.contains("press") || lower.contains("ক্লিক") || lower.contains("বাটন") -> {
                val targetName = userInput.replace("click", "", ignoreCase = true)
                    .replace("press", "", ignoreCase = true)
                    .replace("ক্লিক", "", ignoreCase = true)
                    .replace("বাটন", "", ignoreCase = true).trim()

                val clickJs = """
                    (function() {
                        var kw = '$targetName'.toLowerCase();
                        var targets = Array.from(document.querySelectorAll('button, a, div[role="button"], input[type="submit"], input[type="button"]'));
                        var match = targets.find(function(el) {
                            var txt = (el.innerText || el.value || el.getAttribute('aria-label') || '').toLowerCase();
                            return kw.length > 0 ? txt.includes(kw) : true;
                        });
                        if (match) {
                            match.focus();
                            match.click();
                            AuraBridge.onDataExtracted('Clicked button: ' + (match.innerText || '$targetName'));
                        } else {
                            AuraBridge.onDataExtracted('Button matching "$targetName" not found.');
                        }
                    })();
                """.trimIndent()
                AuraAiActionResponse(
                    spokenResponse = if (isBn) "$targetName বাটনে ক্লিক করা হচ্ছে..." else "Clicking button $targetName...",
                    actionType = "EXECUTE_JS",
                    jsCode = clickJs,
                    extractedResultSummary = "Button click triggered"
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
                        "কমান্ডের জন্য তৈরি। $error"
                    else
                        "Ready for commands. Status: $error",
                    actionType = "CONVERSATION",
                    extractedResultSummary = "System ready. API Status: $error"
                )
            }
        }
    }
}
