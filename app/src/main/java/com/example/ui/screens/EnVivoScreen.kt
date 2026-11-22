package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CristoReyConfig
import com.example.data.model.LiveStreamStatus
import com.example.ui.components.LiveStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.YouTubeEmbedPlayer
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SacredGold

@Composable
fun EnVivoScreen(
    liveStatus: LiveStreamStatus,
    isCheckingLive: Boolean,
    onRefreshLive: () -> Unit,
    onOpenYouTubeLive: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                SectionHeader(
                    title = "Transmisión en Vivo",
                    subtitle = "Canal oficial ${CristoReyConfig.YOUTUBE_HANDLE}",
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LiveStatusBadge(liveActive = liveStatus.liveActive)
                            IconButton(
                                onClick = onRefreshLive,
                                enabled = !isCheckingLive,
                                modifier = Modifier.testTag("refresh_live_status_button")
                            ) {
                                if (isCheckingLive) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = SacredGold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Actualizar estado en vivo"
                                    )
                                }
                            }
                        }
                    }
                )
            }

            item {
                YouTubeEmbedPlayer(
                    embedUrl = liveStatus.embedUrl,
                    fallbackWatchUrl = liveStatus.watchUrl,
                    onOpenExternal = onOpenYouTubeLive,
                    modifier = Modifier.testTag("live_youtube_iframe_player")
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LiveTv,
                                contentDescription = null,
                                tint = SacredGold
                            )
                            Text(
                                text = liveStatus.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = if (liveStatus.liveActive) {
                                "Estamos transmitiendo en directo en este momento. Puedes seguir el culto aquí mismo o abrirlo en la app de YouTube."
                            } else {
                                "No hay una transmisión activa en este instante. Activa las notificaciones de En Vivo en Info para recibir un aviso cuando iniciemos."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onOpenYouTubeLive(liveStatus.watchUrl) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("watch_on_youtube_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.2.dp, RoyalBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.watch_on_youtube),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = onRefreshLive,
                                enabled = !isCheckingLive,
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("check_live_now_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalBlue,
                                    contentColor = PureWhite
                                )
                            ) {
                                Text("Verificar")
                            }
                        }
                    }
                }
            }
        }
    }
}
