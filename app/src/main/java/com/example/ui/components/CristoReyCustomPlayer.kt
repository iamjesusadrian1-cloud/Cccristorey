package com.example.ui.components

import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CristoReyConfig
import com.example.data.model.YouTubeVideo
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SacredGold
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Reproductor propio de Cristo Rey con controles nativos en Compose
 * (Play/Pausa, -10s/+10s, barra de progreso, silencio, pantalla completa)
 * conectado al reproductor embebido YouTubePlayer.
 */
@Composable
fun CristoReyCustomPlayerCard(
    title: String,
    subtitle: String,
    isLive: Boolean,
    videoId: String? = null,
    embedUrlOverride: String? = null,
    watchUrl: String,
    onOpenExternal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = remember(videoId, isLive, embedUrlOverride) { YouTubePlayerController() }
    val fullscreenState = LocalFullscreenPlayerState.current
    var activePlayerId by remember { mutableStateOf<String?>(null) }
    var activeContainer by remember { mutableStateOf<FrameLayout?>(null) }
    val isFullscreen = activePlayerId != null && fullscreenState?.isPlayerFullscreen(activePlayerId!!) == true

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DeepNavy),
        border = BorderStroke(1.5.dp, BrightGold.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepNavy, RoyalBlue.copy(alpha = 0.92f), DeepNavy)
                    )
                )
        ) {
            YouTubePlayer(
                videoId = videoId,
                isLive = isLive,
                channelId = CristoReyConfig.YOUTUBE_CHANNEL_ID,
                embedUrlOverride = embedUrlOverride,
                controller = controller,
                fallbackWatchUrl = watchUrl,
                onOpenExternal = onOpenExternal,
                onContainerReady = { id, container ->
                    activePlayerId = id
                    activeContainer = container
                }
            )

            CustomPlayerControlsSection(
                title = title,
                subtitle = subtitle,
                isLive = isLive,
                controller = controller,
                isFullscreen = isFullscreen,
                onToggleFullscreen = {
                    val id = activePlayerId
                    val container = activeContainer
                    if (fullscreenState != null && id != null && container != null) {
                        if (isFullscreen) {
                            fullscreenState.exitFullscreenFor(id)
                        } else {
                            fullscreenState.enterInlineFullscreen(
                                id = id,
                                title = title,
                                subtitle = subtitle,
                                isLive = isLive,
                                watchUrl = watchUrl,
                                controller = controller,
                                playerContainer = container,
                                onOpenExternal = onOpenExternal
                            )
                        }
                    }
                },
                watchUrl = watchUrl,
                onOpenExternal = onOpenExternal
            )
        }
    }
}

/**
 * Pantalla dedicada de Reproductor Propio para videos bajo demanda con lista "A continuación".
 */
@Composable
fun CristoReyVideoPlayerOverlay(
    video: YouTubeVideo,
    upNextVideos: List<YouTubeVideo>,
    onSelectVideo: (YouTubeVideo) -> Unit,
    onOpenExternal: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            // Top bar del Reproductor Propio Cristo Rey
            Surface(
                color = DeepNavy,
                contentColor = PureWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("custom_player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = PureWhite
                        )
                    }
                    CristoReyLogoBadge(size = 34.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reproductor Cristo Rey",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrightGold
                        )
                        Text(
                            text = video.formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite.copy(alpha = 0.78f)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item(key = "player_${video.videoId}") {
                        CristoReyCustomPlayerCard(
                            title = video.title,
                            subtitle = "Publicado el ${video.formattedDate} • ${CristoReyConfig.YOUTUBE_HANDLE}",
                            isLive = false,
                            videoId = video.videoId,
                            embedUrlOverride = video.embedUrl,
                            watchUrl = video.watchUrl,
                            onOpenExternal = onOpenExternal,
                            modifier = Modifier.testTag("custom_video_player_card")
                        )
                    }

                    if (upNextVideos.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "A continuación",
                                subtitle = "Otros mensajes de ${CristoReyConfig.YOUTUBE_HANDLE}"
                            )
                        }
                        items(upNextVideos, key = { it.videoId }) { nextVideo ->
                            CompactVideoPreviewRow(
                                video = nextVideo,
                                onClick = { onSelectVideo(nextVideo) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CustomPlayerControlsSection(
    title: String,
    subtitle: String,
    isLive: Boolean,
    controller: YouTubePlayerController,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    watchUrl: String,
    onOpenExternal: (String) -> Unit,
    showHeaderMetadata: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showHeaderMetadata) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite.copy(alpha = 0.76f)
                    )
                }
                if (isLive) {
                    LiveStatusBadge(liveActive = true)
                }
            }
        }

        // Barra de progreso para videos bajo demanda
        if (!isLive && controller.durationSeconds > 0.0) {
            val duration = controller.durationSeconds.toFloat().coerceAtLeast(1f)
            val current = controller.currentSeconds.toFloat().coerceIn(0f, duration)
            Column {
                Slider(
                    value = current,
                    onValueChange = { controller.seekTo(it.toDouble()) },
                    valueRange = 0f..duration,
                    colors = SliderDefaults.colors(
                        thumbColor = BrightGold,
                        activeTrackColor = BrightGold,
                        inactiveTrackColor = PureWhite.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.testTag("custom_player_seekbar")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDurationSeconds(current.roundToInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatDurationSeconds(duration.roundToInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Controles de reproducción propios
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { controller.toggleMute() },
                    modifier = Modifier.testTag("custom_player_mute_button")
                ) {
                    Icon(
                        imageVector = if (controller.isMuted) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = if (controller.isMuted) "Activar sonido" else "Silenciar",
                        tint = PureWhite
                    )
                }

                if (!isLive) {
                    IconButton(
                        onClick = { controller.seekBy(-10.0) },
                        modifier = Modifier.testTag("custom_player_rewind_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Retroceder 10 segundos",
                            tint = PureWhite
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = BrightGold,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = { controller.togglePlayPause() },
                        modifier = Modifier.testTag("custom_player_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (controller.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (controller.isPlaying) "Pausar" else "Reproducir",
                            tint = DeepNavy,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (!isLive) {
                    IconButton(
                        onClick = { controller.seekBy(10.0) },
                        modifier = Modifier.testTag("custom_player_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Adelantar 10 segundos",
                            tint = PureWhite
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenExternal(watchUrl) },
                    border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("custom_player_open_youtube_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = BrightGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.watch_on_youtube),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BrightGold
                    )
                }

                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.testTag("custom_player_fullscreen_button")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) {
                            Icons.Filled.FullscreenExit
                        } else {
                            Icons.Filled.Fullscreen
                        },
                        contentDescription = if (isFullscreen) "Salir de pantalla completa" else "Pantalla completa",
                        tint = PureWhite
                    )
                }
            }
        }

        if (controller.playerErrorCode != null) {
            Text(
                text = if (isLive) {
                    "La transmisión en vivo aún no está disponible en YouTube. Puedes verificar nuevamente o abrir el canal."
                } else {
                    "No se pudo iniciar la reproducción embebida (código ${controller.playerErrorCode}). Usa 'Ver en YouTube' si el video restringe reproducción externa."
                },
                style = MaterialTheme.typography.bodySmall,
                color = SacredGold
            )
        }
    }
}

private fun formatDurationSeconds(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
