package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CristoReyConfig
import com.example.data.model.LiveStreamStatus
import com.example.data.model.SocialPost
import com.example.data.model.YouTubeVideo
import com.example.ui.UiState
import com.example.ui.components.CristoReyLogoBadge
import com.example.ui.components.LiveStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.SocialPostCard
import com.example.ui.components.VideoItemCard
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SacredGold
import com.example.ui.theme.SanctuaryBlue

@Composable
fun InicioScreen(
    liveStatus: LiveStreamStatus,
    videosState: UiState<List<YouTubeVideo>>,
    socialPostsState: UiState<List<SocialPost>>,
    onNavigateToLive: () -> Unit,
    onNavigateToVideos: () -> Unit,
    onNavigateToSocial: () -> Unit,
    onPlayVideo: (YouTubeVideo) -> Unit,
    onOpenUrl: (String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Welcome Hero Banner with Official Logo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(DeepNavy, RoyalBlue, SanctuaryBlue)
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CristoReyLogoBadge(size = 64.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bienvenidos a",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = BrightGold
                                    )
                                    Text(
                                        text = stringResource(id = R.string.app_name),
                                        style = MaterialTheme.typography.displayMedium,
                                        color = PureWhite
                                    )
                                    Text(
                                        text = CristoReyConfig.ORGANIZATION_SUBTITLE,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PureWhite.copy(alpha = 0.82f)
                                    )
                                }
                            }

                            Text(
                                text = "Unidos en fe, adoración y palabra. Acompaña nuestros cultos en vivo, mensajes recientes y vida comunitaria.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureWhite.copy(alpha = 0.9f)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LiveStatusBadge(liveActive = liveStatus.liveActive)
                                Text(
                                    text = if (liveStatus.liveActive) {
                                        "Transmisión activa ahora"
                                    } else {
                                        "Canal ${CristoReyConfig.YOUTUBE_HANDLE}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PureWhite.copy(alpha = 0.85f)
                                )
                            }

                            Button(
                                onClick = onNavigateToLive,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("home_watch_live_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrightGold,
                                    contentColor = DeepNavy
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LiveTv,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(id = R.string.watch_live_cta),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Latest Video Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "Último Video",
                        subtitle = "Mensajes de ${CristoReyConfig.YOUTUBE_HANDLE}",
                        trailing = {
                            OutlinedButton(
                                onClick = onNavigateToVideos,
                                modifier = Modifier.testTag("home_see_all_videos_button"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text("Ver todos")
                            }
                        }
                    )

                    when (videosState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SacredGold)
                            }
                        }
                        is UiState.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No pudimos cargar el último video.",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = videosState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedButton(onClick = onNavigateToVideos) {
                                        Text("Ir a Videos")
                                    }
                                }
                            }
                        }
                        is UiState.Success -> {
                            val latestVideo = videosState.data.firstOrNull()
                            if (latestVideo != null) {
                                VideoItemCard(
                                    video = latestVideo,
                                    onPlayClick = onPlayVideo
                                )
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VideoLibrary,
                                            contentDescription = null,
                                            tint = RoyalBlue
                                        )
                                        Text(
                                            text = "Explora nuestros mensajes y prédicas en la pestaña Videos.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Latest Social Highlight Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = "Publicación Destacada",
                        subtitle = "Instagram ${CristoReyConfig.INSTAGRAM_HANDLE}",
                        trailing = {
                            OutlinedButton(
                                onClick = onNavigateToSocial,
                                modifier = Modifier.testTag("home_see_social_button"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text("Abrir Redes")
                            }
                        }
                    )

                    val latestSocial = (socialPostsState as? UiState.Success)?.data?.firstOrNull()
                    if (latestSocial != null) {
                        SocialPostCard(
                            post = latestSocial,
                            onOpenPost = onOpenUrl
                        )
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onNavigateToSocial() }
                                .testTag("home_social_highlight_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, BrightGold.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.PhotoLibrary,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Síguenos en ${CristoReyConfig.INSTAGRAM_HANDLE}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Entérate de anuncios, eventos y devocionales diarios de nuestra comunidad.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Ir a Redes",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
