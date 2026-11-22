package com.example.ui.components

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.PureWhite

private const val TAG = "YouTubeEmbedPlayer"

private fun isEmulatorEnvironment(): Boolean {
    return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
        Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.HARDWARE.contains("goldfish") ||
        Build.HARDWARE.contains("ranchu") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for") ||
        Build.PRODUCT.contains("sdk") ||
        Build.PRODUCT.contains("emulator")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedPlayer(
    embedUrl: String,
    fallbackWatchUrl: String = "",
    onOpenExternal: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var retryKey by remember(embedUrl) { mutableIntStateOf(0) }
    var isLoading by remember(embedUrl, retryKey) { mutableStateOf(true) }
    var rendererCrashed by remember(embedUrl, retryKey) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(18.dp))
            .background(DeepNavy),
        contentAlignment = Alignment.Center
    ) {
        if (rendererCrashed) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = BrightGold,
                    modifier = Modifier.size(38.dp)
                )
                Text(
                    text = "El reproductor web tuvo una interrupción en este dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PureWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { retryKey++ },
                        border = BorderStroke(1.dp, BrightGold),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrightGold)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reintentar")
                    }
                    if (fallbackWatchUrl.isNotBlank() && onOpenExternal != null) {
                        Button(
                            onClick = { onOpenExternal(fallbackWatchUrl) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrightGold,
                                contentColor = DeepNavy
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver en YouTube")
                        }
                    }
                }
            }
        } else {
            val webView = remember(retryKey) {
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.parseColor("#0A1D3E"))
                    if (isEmulatorEnvironment()) {
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    }
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    }
                    webChromeClient = WebChromeClient()
                }
            }

            DisposableEffect(webView) {
                onDispose {
                    webView.stopLoading()
                    webView.destroy()
                }
            }

            AndroidView(
                factory = {
                    webView.apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return false
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                Log.w(
                                    TAG,
                                    "WebView renderer process gone (didCrash=${
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) detail?.didCrash() else "unknown"
                                    })"
                                )
                                isLoading = false
                                rendererCrashed = true
                                view?.destroy()
                                return true
                            }
                        }
                    }
                },
                update = { view ->
                    if (view.tag != embedUrl) {
                        view.tag = embedUrl
                        isLoading = true
                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                              <style>
                                html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #0A1D3E; overflow: hidden; }
                                iframe { width: 100%; height: 100%; border: 0; }
                              </style>
                            </head>
                            <body>
                              <iframe
                                src="$embedUrl"
                                title="Cristo Rey Video Player"
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                                allowfullscreen>
                              </iframe>
                            </body>
                            </html>
                        """.trimIndent()
                        view.loadDataWithBaseURL(
                            "https://www.youtube.com",
                            html,
                            "text/html",
                            "utf-8",
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                CircularProgressIndicator(
                    color = BrightGold,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            }
        }
    }
}
