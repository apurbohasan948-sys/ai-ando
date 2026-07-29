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

                        // Inject Human Typer Helper, Force Click & Video Link Extractor Scripts
                        val scriptHelper = """
                            (function() {
                                if (!window.AuraForceClick) {
                                    window.AuraForceClick = function(el) {
                                        if (!el) return false;
                                        try {
                                            el.focus();
                                            ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(function(evt) {
                                                el.dispatchEvent(new MouseEvent(evt, { bubbles: true, cancelable: true, view: window }));
                                            });
                                            return true;
                                        } catch(e) {
                                            try { el.click(); return true; } catch(err) { return false; }
                                        }
                                    };
                                }

                                if (!window.AuraHumanType) {
                                    window.AuraHumanType = function(selector, text, delayMs, onComplete) {
                                        var el = typeof selector === 'string' ? document.querySelector(selector) : selector;
                                        if (!el) {
                                            if (window.AuraBridge) window.AuraBridge.onDataExtracted('Field not found: ' + selector);
                                            return;
                                        }
                                        el.focus();
                                        if (window.AuraForceClick) window.AuraForceClick(el);
                                        if (el.isContentEditable) {
                                            el.innerHTML = '';
                                        } else if (el.value !== undefined) {
                                            el.value = '';
                                        }
                                        var i = 0;
                                        var speed = delayMs || 40;
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
                                                setTimeout(typeChar, speed + Math.floor(Math.random() * 20));
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

                                if (!window.AuraExtractVideoLinks) {
                                    window.AuraExtractVideoLinks = function() {
                                        var elements = Array.from(document.querySelectorAll('a[href], button[onclick], iframe[src], source[src], video[src], embed[src]'));
                                        var found = [];
                                        var kwRegex = /download|link|stream|drive|mega|magnet|480p|720p|1080p|4k|mkv|mp4|play|file|watch|embed/i;
                                        
                                        elements.forEach(function(el) {
                                            var href = el.href || el.src || el.getAttribute('onclick') || '';
                                            var txt = (el.innerText || el.title || el.getAttribute('aria-label') || el.value || '').trim();
                                            if (href && (kwRegex.test(href) || kwRegex.test(txt))) {
                                                found.push({ title: txt || document.title, link: href });
                                            }
                                        });

                                        var pageTitle = document.title || 'Extracted Media';
                                        var finalLink = found.length > 0 ? found[0].link : window.location.href;
                                        var resultText = "MEDIA_TITLE: " + pageTitle + "\nMEDIA_LINK: " + finalLink;
                                        if (found.length > 1) {
                                            resultText += "\nALL_LINKS:\n" + found.slice(0, 5).map(f => "- " + f.title + ": " + f.link).join("\n");
                                        }
                                        if (window.AuraBridge) {
                                            window.AuraBridge.onDataExtracted(resultText);
                                        }
                                        return resultText;
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
