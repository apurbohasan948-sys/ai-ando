package com.example.webview

import android.webkit.JavascriptInterface
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class WebViewBridge {

    data class ExtractedDataEvent(val content: String, val timestamp: Long = System.currentTimeMillis())
    data class PageInspectionEvent(val title: String, val url: String, val formsJson: String)
    data class ScriptExecutionEvent(val scriptId: String, val success: Boolean, val message: String)

    private val _extractedDataFlow = MutableSharedFlow<ExtractedDataEvent>(extraBufferCapacity = 10)
    val extractedDataFlow: SharedFlow<ExtractedDataEvent> = _extractedDataFlow

    private val _pageInspectionFlow = MutableSharedFlow<PageInspectionEvent>(extraBufferCapacity = 10)
    val pageInspectionFlow: SharedFlow<PageInspectionEvent> = _pageInspectionFlow

    private val _scriptExecutionFlow = MutableSharedFlow<ScriptExecutionEvent>(extraBufferCapacity = 10)
    val scriptExecutionFlow: SharedFlow<ScriptExecutionEvent> = _scriptExecutionFlow

    private val _credentialRequestedFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val credentialRequestedFlow: SharedFlow<String> = _credentialRequestedFlow

    @JavascriptInterface
    fun onDataExtracted(data: String) {
        _extractedDataFlow.tryEmit(ExtractedDataEvent(data))
    }

    @JavascriptInterface
    fun onPageInspected(title: String, url: String, formsJson: String) {
        _pageInspectionFlow.tryEmit(PageInspectionEvent(title, url, formsJson))
    }

    @JavascriptInterface
    fun onScriptResult(scriptId: String, success: Boolean, resultMsg: String) {
        _scriptExecutionFlow.tryEmit(ScriptExecutionEvent(scriptId, success, resultMsg))
    }

    @JavascriptInterface
    fun onRequestCredentials(domain: String) {
        _credentialRequestedFlow.tryEmit(domain)
    }
}
