package com.example.ui.components

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.Keep
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
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
import java.util.UUID
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.CristoReyConfig
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.PureWhite

private const val TAG = "YouTubePlayer"

@Stable
class YouTubePlayerController {
    internal var webViewRef: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    var isReady by mutableStateOf(false)
        internal set
    var isPlaying by mutableStateOf(false)
        internal set
    var isBuffering by mutableStateOf(false)
        internal set
    var isMuted by mutableStateOf(false)
        internal set
    var currentSeconds by mutableDoubleStateOf(0.0)
        internal set
    var durationSeconds by mutableDoubleStateOf(0.0)
        internal set
    var playerErrorCode by mutableStateOf<Int?>(null)
        internal set

    fun play() = evalJs(
        """
        (function() {
          var p = document.getElementById('movie_player');
          if (p && typeof p.playVideo === 'function') { p.playVideo(); return; }
          var v = document.querySelector('video');
          if (v) v.play();
        })();
        """.trimIndent()
    )

    fun pause() = evalJs(
        """
        (function() {
          var p = document.getElementById('movie_player');
          if (p && typeof p.pauseVideo === 'function') { p.pauseVideo(); return; }
          var v = document.querySelector('video');
          if (v) v.pause();
        })();
        """.trimIndent()
    )

    fun togglePlayPause() {
        if (isPlaying) pause() else play()
    }

    fun seekTo(seconds: Double) {
        val target = seconds.coerceAtLeast(0.0)
        currentSeconds = target
        evalJs(
            """
            (function() {
              var p = document.getElementById('movie_player');
              if (p && typeof p.seekTo === 'function') { p.seekTo($target, true); return; }
              var v = document.querySelector('video');
              if (v) v.currentTime = $target;
            })();
            """.trimIndent()
        )
    }

    fun seekBy(deltaSeconds: Double) {
        val maxDur = if (durationSeconds > 0.0) durationSeconds else Double.MAX_VALUE
        val target = (currentSeconds + deltaSeconds).coerceIn(0.0, maxDur)
        seekTo(target)
    }

    fun toggleMute() {
        val nextMuted = !isMuted
        isMuted = nextMuted
        evalJs(
            """
            (function() {
              var p = document.getElementById('movie_player');
              if (p) {
                if ($nextMuted && typeof p.mute === 'function') p.mute();
                else if (!$nextMuted && typeof p.unMute === 'function') p.unMute();
              }
              var v = document.querySelector('video');
              if (v) v.muted = $nextMuted;
            })();
            """.trimIndent()
        )
    }

    internal fun resetForNewSource() {
        isReady = false
        isPlaying = false
        isBuffering = false
        currentSeconds = 0.0
        durationSeconds = 0.0
        playerErrorCode = null
    }

    private fun evalJs(script: String) {
        mainHandler.post {
            webViewRef?.evaluateJavascript(script, null)
        }
    }
}

@Keep
private class CristoReyJsBridge(
    private val controller: YouTubePlayerController,
    private val mainHandler: Handler
) {
    @JavascriptInterface
    fun onStateSnapshot(playing: Boolean, muted: Boolean, currentSec: Double, durationSec: Double) {
        mainHandler.post {
            controller.isReady = true
            controller.isPlaying = playing
            controller.isMuted = muted
            if (currentSec >= 0.0) controller.currentSeconds = currentSec
            if (durationSec > 0.0) controller.durationSeconds = durationSec
        }
    }
}

/**
 * Componente reproductor de YouTube basado en `AndroidView` con un `WebView` interno
 * alojado en un `android.view.ViewGroup`. Carga directamente la URL del embed usando
 * `WebView.loadUrl(url, headers)` con los headers `Referer` y `Referrer-Policy`
 * requeridos por YouTube para evitar el error 152-4.
 */
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
fun YouTubePlayer(
    videoId: String? = null,
    isLive: Boolean = false,
    channelId: String = CristoReyConfig.YOUTUBE_CHANNEL_ID,
    embedUrlOverride: String? = null,
    controller: YouTubePlayerController = remember { YouTubePlayerController() },
    fallbackWatchUrl: String = if (isLive) {
        CristoReyConfig.YOUTUBE_LIVE_WATCH_URL
    } else if (!videoId.isNullOrBlank()) {
        CristoReyConfig.videoWatchUrl(videoId)
    } else {
        CristoReyConfig.YOUTUBE_CHANNEL_URL
    },
    onOpenExternal: ((String) -> Unit)? = null,
    onContainerReady: ((playerId: String, container: FrameLayout) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fullscreenState = LocalFullscreenPlayerState.current

    val resolvedEmbedUrl = remember(videoId, isLive, channelId, embedUrlOverride) {
        when {
            !embedUrlOverride.isNullOrBlank() -> embedUrlOverride
            isLive -> "https://www.youtube.com/embed/live_stream?channel=$channelId&autoplay=1&playsinline=1&rel=0"
            !videoId.isNullOrBlank() -> "https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0"
            else -> "https://www.youtube.com/embed/live_stream?channel=$channelId&autoplay=1&playsinline=1&rel=0"
        }
    }

    val embedHeaders = remember { YouTubeEmbedWebViewClient.AUTHORIZED_HEADERS }

    var retryKey by remember(resolvedEmbedUrl) { mutableIntStateOf(0) }
    var isLoading by remember(resolvedEmbedUrl, retryKey) { mutableStateOf(true) }
    var rendererCrashed by remember(resolvedEmbedUrl, retryKey) { mutableStateOf(false) }
    val playerId = remember(resolvedEmbedUrl, retryKey) { UUID.randomUUID().toString() }
    val isInExternalFullscreen = fullscreenState?.isPlayerFullscreen(playerId) == true

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
            key(resolvedEmbedUrl, retryKey) {
                var isDestroyed by remember { mutableStateOf(false) }
                val mainHandler = remember { Handler(Looper.getMainLooper()) }
                val lifecycleOwner = LocalLifecycleOwner.current

                val playerContainer = remember(context, resolvedEmbedUrl, retryKey) {
                    val container = FrameLayout(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.parseColor("#0A1D3E"))
                    }

                    var inlineCustomView: View? = null
                    var inlineCustomCallback: WebChromeClient.CustomViewCallback? = null

                    val webView = WebView(context).apply {
                        id = R.id.youtube_webview
                        setBackgroundColor(android.graphics.Color.parseColor("#0A1D3E"))
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                        }
                        settings.apply {
                            setJavaScriptEnabled(true)
                            setDomStorageEnabled(true)
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        }
                        runCatching {
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                        }
                        addJavascriptInterface(
                            CristoReyJsBridge(controller, mainHandler),
                            "CristoReyBridge"
                        )
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(
                                view: View?,
                                callback: CustomViewCallback?
                            ) {
                                if (view == null) return
                                if (fullscreenState != null) {
                                    fullscreenState.showWebCustomView(playerId, view, callback)
                                    return
                                }
                                if (inlineCustomView != null) {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                inlineCustomView = view
                                inlineCustomCallback = callback
                                visibility = View.GONE
                                container.addView(
                                    view,
                                    ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }

                            override fun onHideCustomView() {
                                if (fullscreenState != null) {
                                    fullscreenState.hideWebCustomView(playerId)
                                    return
                                }
                                inlineCustomView?.let { container.removeView(it) }
                                inlineCustomView = null
                                inlineCustomCallback?.onCustomViewHidden()
                                inlineCustomCallback = null
                                visibility = View.VISIBLE
                            }
                        }
                        webViewClient = YouTubeEmbedWebViewClient(
                            onPageFinishedCallback = { view, _ ->
                                isLoading = false
                                view?.evaluateJavascript(PROGRESS_OBSERVER_JS, null)
                            },
                            onMainFrameErrorCallback = { code ->
                                isLoading = false
                                controller.playerErrorCode = code
                            },
                            onOpenExternal = onOpenExternal,
                            onRenderProcessGoneCallback = { view, detail ->
                                Log.w(
                                    TAG,
                                    "WebView renderer process gone (didCrash=${
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) detail?.didCrash() else "unknown"
                                    })"
                                )
                                isLoading = false
                                rendererCrashed = true
                                fullscreenState?.exitFullscreenFor(playerId)
                                controller.webViewRef = null
                                if (!isDestroyed) {
                                    isDestroyed = true
                                    container.removeAllViews()
                                    view?.destroy()
                                }
                                true
                            }
                        )
                    }

                    controller.webViewRef = webView
                    controller.resetForNewSource()
                    container.addView(
                        webView,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    webView.tag = resolvedEmbedUrl
                    webView.loadUrl(resolvedEmbedUrl, embedHeaders)
                    container
                }

                DisposableEffect(lifecycleOwner, playerContainer) {
                    onContainerReady?.invoke(playerId, playerContainer)
                    val observer = LifecycleEventObserver { _, event ->
                        val activeWebView = controller.webViewRef
                        if (!isDestroyed && activeWebView != null) {
                            when (event) {
                                Lifecycle.Event.ON_RESUME -> activeWebView.onResume()
                                Lifecycle.Event.ON_PAUSE -> activeWebView.onPause()
                                else -> Unit
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        fullscreenState?.exitFullscreenFor(playerId)
                        val webView = playerContainer.findViewById<WebView>(R.id.youtube_webview)
                        if (controller.webViewRef === webView) {
                            controller.webViewRef = null
                        }
                        if (!isDestroyed) {
                            isDestroyed = true
                            (playerContainer.parent as? ViewGroup)?.removeView(playerContainer)
                            playerContainer.removeAllViews()
                            webView?.onPause()
                            webView?.stopLoading()
                            webView?.loadUrl("about:blank")
                            webView?.removeAllViews()
                            webView?.destroy()
                        }
                    }
                }

                if (isInExternalFullscreen) {
                    Text(
                        text = "Reproduciendo en pantalla completa",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrightGold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    AndroidView<ViewGroup>(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(android.graphics.Color.parseColor("#0A1D3E"))
                                (playerContainer.parent as? ViewGroup)?.removeView(playerContainer)
                                addView(
                                    playerContainer,
                                    ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        },
                        update = { host ->
                            if (!isDestroyed && playerContainer.parent !== host) {
                                (playerContainer.parent as? ViewGroup)?.removeView(playerContainer)
                                host.addView(
                                    playerContainer,
                                    ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        },
                        onRelease = { host ->
                            if (playerContainer.parent === host) {
                                host.removeView(playerContainer)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (isLoading && !isInExternalFullscreen) {
                CircularProgressIndicator(
                    color = BrightGold,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            }
        }
    }
}

private val PROGRESS_OBSERVER_JS = """
    (function() {
      if (window.__crObserverStarted) return;
      window.__crObserverStarted = true;
      setInterval(function() {
        var v = document.querySelector('video');
        if (v && window.CristoReyBridge) {
          var playing = !v.paused && !v.ended;
          var muted = Boolean(v.muted);
          var cur = isFinite(v.currentTime) ? v.currentTime : 0;
          var dur = isFinite(v.duration) ? v.duration : 0;
          window.CristoReyBridge.onStateSnapshot(playing, muted, cur, dur);
        }
      }, 500);
    })();
""".trimIndent()
