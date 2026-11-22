package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.PureWhite

sealed interface FullscreenSession {
    data class InlinePlayer(
        val id: String,
        val title: String,
        val subtitle: String,
        val isLive: Boolean,
        val watchUrl: String,
        val controller: YouTubePlayerController,
        val playerContainer: FrameLayout,
        val onOpenExternal: (String) -> Unit
    ) : FullscreenSession

    data class WebCustomView(
        val id: String,
        val customView: View,
        val callback: WebChromeClient.CustomViewCallback?
    ) : FullscreenSession
}

@Stable
class FullscreenPlayerState {
    var activeSession by mutableStateOf<FullscreenSession?>(null)
        private set

    var isLandscape by mutableStateOf(true)
        private set

    val isFullscreen: Boolean
        get() = activeSession != null

    fun isPlayerFullscreen(playerId: String): Boolean {
        val session = activeSession
        return session is FullscreenSession.InlinePlayer && session.id == playerId
    }

    fun enterInlineFullscreen(
        id: String,
        title: String,
        subtitle: String,
        isLive: Boolean,
        watchUrl: String,
        controller: YouTubePlayerController,
        playerContainer: FrameLayout,
        onOpenExternal: (String) -> Unit
    ) {
        isLandscape = true
        activeSession = FullscreenSession.InlinePlayer(
            id = id,
            title = title,
            subtitle = subtitle,
            isLive = isLive,
            watchUrl = watchUrl,
            controller = controller,
            playerContainer = playerContainer,
            onOpenExternal = onOpenExternal
        )
    }

    fun showWebCustomView(
        id: String,
        customView: View,
        callback: WebChromeClient.CustomViewCallback?
    ) {
        val current = activeSession
        if (current is FullscreenSession.WebCustomView) {
            callback?.onCustomViewHidden()
            return
        }
        isLandscape = true
        activeSession = FullscreenSession.WebCustomView(
            id = id,
            customView = customView,
            callback = callback
        )
    }

    fun hideWebCustomView(id: String? = null) {
        val current = activeSession
        if (current is FullscreenSession.WebCustomView && (id == null || current.id == id)) {
            current.callback?.onCustomViewHidden()
            activeSession = null
        }
    }

    fun exitFullscreenFor(id: String) {
        when (val current = activeSession) {
            is FullscreenSession.InlinePlayer -> if (current.id == id) activeSession = null
            is FullscreenSession.WebCustomView -> if (current.id == id) hideWebCustomView(id)
            null -> Unit
        }
    }

    fun exitFullscreen() {
        when (val current = activeSession) {
            is FullscreenSession.WebCustomView -> {
                current.callback?.onCustomViewHidden()
                activeSession = null
            }
            is FullscreenSession.InlinePlayer -> {
                activeSession = null
            }
            null -> Unit
        }
    }

    fun toggleOrientation() {
        isLandscape = !isLandscape
    }
}

val LocalFullscreenPlayerState = compositionLocalOf<FullscreenPlayerState?> { null }

@Composable
fun FullscreenPlayerOverlay(
    fullscreenState: FullscreenPlayerState
) {
    val session = fullscreenState.activeSession
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var wasInFullscreen by remember { mutableStateOf(false) }

    // Aplicar modo inmersivo y orientación en pantalla completa dentro de la app
    LaunchedEffect(session != null, fullscreenState.isLandscape) {
        val window = activity?.window
        if (session != null) {
            wasInFullscreen = true
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = if (fullscreenState.isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        } else if (wasInFullscreen) {
            wasInFullscreen = false
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(activity) {
        onDispose {
            if (wasInFullscreen) {
                activity?.window?.let { window ->
                    WindowCompat.getInsetsController(window, window.decorView)
                        .show(WindowInsetsCompat.Type.systemBars())
                }
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    if (session == null) return

    BackHandler {
        fullscreenState.exitFullscreen()
    }

    var showOverlayControls by remember(session) { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(50f)
            .background(Color.Black)
    ) {
        when (session) {
            is FullscreenSession.InlinePlayer -> {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.BLACK)
                            (session.playerContainer.parent as? ViewGroup)
                                ?.removeView(session.playerContainer)
                            addView(
                                session.playerContainer,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    update = { host ->
                        if (session.playerContainer.parent !== host) {
                            (session.playerContainer.parent as? ViewGroup)
                                ?.removeView(session.playerContainer)
                            host.addView(
                                session.playerContainer,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    onRelease = { host ->
                        if (session.playerContainer.parent === host) {
                            host.removeView(session.playerContainer)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Barra superior en pantalla completa
                AnimatedVisibility(
                    visible = showOverlayControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        DeepNavy.copy(alpha = 0.88f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .safeDrawingPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                CristoReyLogoBadge(size = 32.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = PureWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = session.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrightGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { fullscreenState.toggleOrientation() },
                                    modifier = Modifier.testTag("fullscreen_rotate_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ScreenRotation,
                                        contentDescription = "Cambiar orientación",
                                        tint = PureWhite
                                    )
                                }
                                IconButton(
                                    onClick = { showOverlayControls = false },
                                    modifier = Modifier.testTag("fullscreen_hide_controls_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VisibilityOff,
                                        contentDescription = "Ocultar controles",
                                        tint = PureWhite
                                    )
                                }
                                IconButton(
                                    onClick = { fullscreenState.exitFullscreen() },
                                    modifier = Modifier.testTag("player_exit_fullscreen_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FullscreenExit,
                                        contentDescription = "Salir de pantalla completa",
                                        tint = BrightGold
                                    )
                                }
                            }
                        }
                    }
                }

                // Controles inferiores propios en pantalla completa
                AnimatedVisibility(
                    visible = showOverlayControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        DeepNavy.copy(alpha = 0.9f)
                                    )
                                )
                            )
                            .safeDrawingPadding()
                    ) {
                        CustomPlayerControlsSection(
                            title = session.title,
                            subtitle = session.subtitle,
                            isLive = session.isLive,
                            controller = session.controller,
                            isFullscreen = true,
                            onToggleFullscreen = { fullscreenState.exitFullscreen() },
                            watchUrl = session.watchUrl,
                            onOpenExternal = session.onOpenExternal,
                            showHeaderMetadata = false
                        )
                    }
                }

                // Botón discreto en esquina cuando el usuario oculta la barra de controles
                if (!showOverlayControls) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .safeDrawingPadding()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DeepNavy.copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.5f))
                        ) {
                            IconButton(
                                onClick = { showOverlayControls = true },
                                modifier = Modifier.testTag("fullscreen_show_controls_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = "Mostrar controles",
                                    tint = PureWhite
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = DeepNavy.copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.6f))
                        ) {
                            IconButton(
                                onClick = { fullscreenState.exitFullscreen() },
                                modifier = Modifier.testTag("player_exit_fullscreen_pill_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FullscreenExit,
                                    contentDescription = "Salir de pantalla completa",
                                    tint = BrightGold
                                )
                            }
                        }
                    }
                }
            }

            is FullscreenSession.WebCustomView -> {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.BLACK)
                            (session.customView.parent as? ViewGroup)?.removeView(session.customView)
                            addView(
                                session.customView,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    update = { host ->
                        if (session.customView.parent !== host) {
                            (session.customView.parent as? ViewGroup)?.removeView(session.customView)
                            host.addView(
                                session.customView,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    onRelease = { host ->
                        if (session.customView.parent === host) {
                            host.removeView(session.customView)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { fullscreenState.toggleOrientation() },
                        border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ScreenRotation,
                            contentDescription = "Rotar pantalla",
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DeepNavy.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, BrightGold)
                    ) {
                        IconButton(
                            onClick = { fullscreenState.hideWebCustomView() },
                            modifier = Modifier.testTag("web_custom_view_exit_fullscreen")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FullscreenExit,
                                contentDescription = "Salir de pantalla completa",
                                tint = BrightGold
                            )
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
