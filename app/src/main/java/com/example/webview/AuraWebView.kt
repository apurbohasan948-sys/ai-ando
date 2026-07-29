package com.example.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AuraWebViewContainer(
    currentUrl: String,
    bridge: WebViewBridge,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onLoadingStateChanged: (Boolean, Int) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 AuraAssistant/1.0"
                }

                addJavascriptInterface(bridge, "AuraBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { onUrlChanged(it) }
                        onLoadingStateChanged(true, 10)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        url?.let { onUrlChanged(it) }
                        view?.title?.let { onTitleChanged(it) }
                        onLoadingStateChanged(false, 100)

                        // Inject Human Typer Helper & DOM Inspector Scripts
                        val scriptHelper = """
                            (function() {
                                if (!window.AuraHumanType) {
                                    window.AuraHumanType = function(selector, text, delayMs, onComplete) {
                                        var el = typeof selector === 'string' ? document.querySelector(selector) : selector;
                                        if (!el) {
                                            if (window.AuraBridge) window.AuraBridge.onDataExtracted('Field not found: ' + selector);
                                            return;
                                        }
                                        el.focus();
                                        try { el.click(); } catch(e) {}
                                        if (el.isContentEditable) {
                                            el.innerHTML = '';
                                        } else if (el.value !== undefined) {
                                            el.value = '';
                                        }
                                        var i = 0;
                                        var speed = delayMs || 50;
                                        function typeChar() {
                                            if (i < text.length) {
                                                var ch = text.charAt(i);
                                                if (el.isContentEditable) {
                                                    document.execCommand('insertText', false, ch);
                                                } else if (el.value !== undefined) {
                                                    el.value += ch;
                                                    el.dispatchEvent(new Event('input', { bubbles: true }));
                                                    el.dispatchEvent(new Event('change', { bubbles: true }));
                                                }
                                                i++;
                                                setTimeout(typeChar, speed + Math.floor(Math.random() * 30));
                                            } else {
                                                if (el.value !== undefined) {
                                                    el.dispatchEvent(new Event('blur', { bubbles: true }));
                                                }
                                                if (window.AuraBridge) {
                                                    window.AuraBridge.onDataExtracted('Typed (' + text.length + ' chars) into ' + selector);
                                                }
                                                if (typeof onComplete === 'function') onComplete();
                                            }
                                        }
                                        typeChar();
                                    };
                                }

                                try {
                                    var title = document.title || '';
                                    var headings = Array.from(document.querySelectorAll('h1, h2, h3')).map(h => h.innerText.trim()).filter(t => t.length > 0).slice(0, 5).join(' | ');
                                    var inputs = Array.from(document.querySelectorAll('input, textarea, div[contenteditable="true"]')).map(i => i.placeholder || i.name || i.ariaLabel || i.tagName).slice(0, 8).join(', ');
                                    var buttons = Array.from(document.querySelectorAll('button, div[role="button"], a')).map(b => b.innerText.trim()).filter(t => t.length > 0 && t.length < 30).slice(0, 8).join(', ');
                                    var bodySnippet = (document.body ? document.body.innerText.replace(/\s+/g, ' ') : '').slice(0, 1000);

                                    var domSummary = "Title: " + title + "\nHeadings: " + headings + "\nInputs: [" + inputs + "]\nButtons: [" + buttons + "]\nText Snippet: " + bodySnippet;

                                    if (window.AuraBridge) {
                                        window.AuraBridge.onPageInspected(title, window.location.href, domSummary);
                                    }
                                } catch(e) {}
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(scriptHelper, null)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onLoadingStateChanged(newProgress < 100, newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChanged(it) }
                    }
                }

                loadUrl(currentUrl)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            if (webView.url != currentUrl && currentUrl.isNotEmpty()) {
                webView.loadUrl(currentUrl)
            }
        },
        modifier = modifier
    )
}
