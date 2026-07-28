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

                        // Inject inspector helper script
                        val autoInspectorJs = """
                            (function() {
                                try {
                                    var forms = Array.from(document.querySelectorAll('form')).map(f => {
                                        return {
                                            id: f.id || '',
                                            action: f.action || '',
                                            inputs: Array.from(f.querySelectorAll('input')).map(i => ({ name: i.name, type: i.type, id: i.id }))
                                        };
                                    });
                                    if (window.AuraBridge) {
                                        window.AuraBridge.onPageInspected(document.title, window.location.href, JSON.stringify(forms));
                                    }
                                } catch(e) {}
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(autoInspectorJs, null)
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
