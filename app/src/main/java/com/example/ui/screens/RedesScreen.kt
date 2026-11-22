package com.example.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.CristoReyConfig
import com.example.data.model.SocialPost
import com.example.ui.UiState
import com.example.ui.components.SectionHeader
import com.example.ui.components.SocialPostCard
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SacredGold

private const val REDES_TAG = "RedesScreen"

private fun isEmulatorForSocialWebView(): Boolean {
    return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
        Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.HARDWARE.contains("goldfish") ||
        Build.HARDWARE.contains("ranchu") ||
        Build.MODEL.contains("Emulator") ||
        Build.PRODUCT.contains("sdk") ||
        Build.PRODUCT.contains("emulator")
}

@Composable
fun RedesScreen(
    socialPostsState: UiState<List<SocialPost>>,
    isRefreshing: Boolean,
    onRefreshSocial: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasNativePosts = (socialPostsState as? UiState.Success)?.data?.isNotEmpty() == true
    var showEmbeddedFeed by rememberSaveable(hasNativePosts) {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader(
                    title = "Redes Sociales",
                    subtitle = "Instagram oficial ${CristoReyConfig.INSTAGRAM_HANDLE}",
                    trailing = {
                        IconButton(
                            onClick = onRefreshSocial,
                            enabled = !isRefreshing,
                            modifier = Modifier.testTag("refresh_social_button")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = SacredGold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Actualizar redes"
                                )
                            }
                        }
                    }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                tint = SacredGold
                            )
                            Text(
                                text = CristoReyConfig.INSTAGRAM_HANDLE,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = !showEmbeddedFeed,
                                onClick = { showEmbeddedFeed = false },
                                label = { Text("Publicaciones") },
                                modifier = Modifier.testTag("social_mode_cards_chip")
                            )
                            FilterChip(
                                selected = showEmbeddedFeed,
                                onClick = { showEmbeddedFeed = true },
                                label = { Text("Visor Instagram") },
                                modifier = Modifier.testTag("social_mode_embed_chip")
                            )
                        }

                        Button(
                            onClick = { onOpenUrl(CristoReyConfig.INSTAGRAM_PROFILE_URL) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_instagram_profile_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalBlue,
                                contentColor = PureWhite
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir ${CristoReyConfig.INSTAGRAM_HANDLE} en Instagram")
                        }
                    }
                }
            }

            if (showEmbeddedFeed) {
                item {
                    val embedTarget = CristoReyConfig.SOCIAL_WIDGET_EMBED_URL.ifBlank {
                        CristoReyConfig.INSTAGRAM_EMBED_URL
                    }
                    InstagramEmbedWebView(
                        url = embedTarget,
                        onOpenExternal = onOpenUrl,
                        modifier = Modifier.testTag("instagram_embed_webview")
                    )
                }
            } else {
                when (socialPostsState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SacredGold)
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "No pudimos cargar las publicaciones",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = socialPostsState.message,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    OutlinedButton(onClick = { showEmbeddedFeed = true }) {
                                        Text("Usar Visor Instagram")
                                    }
                                }
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (socialPostsState.data.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Conecta el agregador o abre el visor",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Puedes ver el perfil oficial directamente en 'Visor Instagram' o configurar un feed gratuito de RSS.app / SnapWidget / webhook de Cloud Functions.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        OutlinedButton(onClick = { showEmbeddedFeed = true }) {
                                            Text("Cargar Visor Instagram")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(socialPostsState.data, key = { it.id }) { post ->
                                SocialPostCard(
                                    post = post,
                                    onOpenPost = onOpenUrl
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InstagramEmbedWebView(
    url: String,
    onOpenExternal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var retryKey by remember(url) { mutableIntStateOf(0) }
    var isLoading by remember(url, retryKey) { mutableStateOf(true) }
    var rendererCrashed by remember(url, retryKey) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(520.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (rendererCrashed) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = SacredGold,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "El visor web de Instagram no pudo renderizarse en este dispositivo.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { retryKey++ }) {
                            Text("Reintentar")
                        }
                        Button(
                            onClick = { onOpenExternal(CristoReyConfig.INSTAGRAM_PROFILE_URL) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalBlue,
                                contentColor = PureWhite
                            )
                        ) {
                            Text("Abrir Instagram")
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
                        if (isEmulatorForSocialWebView()) {
                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        }
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
                                    val target = request?.url?.toString().orEmpty()
                                    return if (target.startsWith("https://www.instagram.com/cccristoreyve")) {
                                        false
                                    } else if (target.startsWith("https://")) {
                                        onOpenExternal(target)
                                        true
                                    } else {
                                        false
                                    }
                                }

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    Log.w(
                                        REDES_TAG,
                                        "Instagram WebView renderer crashed (didCrash=${
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
                        if (view.tag != url) {
                            view.tag = url
                            isLoading = true
                            view.loadUrl(url)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                )

                if (isLoading) {
                    CircularProgressIndicator(color = SacredGold)
                }
            }
        }
    }
}
