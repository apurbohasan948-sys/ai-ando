package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = "user"
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = 0.2f
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

// Structured Output model returned by Gemini AI
@JsonClass(generateAdapter = true)
data class AuraAiActionResponse(
    @Json(name = "spokenResponse") val spokenResponse: String = "",
    @Json(name = "actionType") val actionType: String = "CONVERSATION", // "NAVIGATE", "EXECUTE_JS", "REQUEST_CREDENTIAL_AUTH", "UPDATE_MEMORY", "CONVERSATION"
    @Json(name = "targetUrl") val targetUrl: String? = null,
    @Json(name = "jsCode") val jsCode: String? = null,
    @Json(name = "requestedCredentialDomain") val requestedCredentialDomain: String? = null,
    @Json(name = "newMemoryCategory") val newMemoryCategory: String? = null,
    @Json(name = "newMemoryTitle") val newMemoryTitle: String? = null,
    @Json(name = "newMemoryDescription") val newMemoryDescription: String? = null,
    @Json(name = "extractedResultSummary") val extractedResultSummary: String? = null
)
