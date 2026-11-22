package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.authStateFlow
import com.example.data.auth.signOut
import com.example.data.model.YouTubeVideo
import com.example.data.notifications.NotificationPreferencesRepository
import com.example.data.youtube.YouTubeRepository
import com.example.notifications.CristoReyMessagingService
import com.example.ui.CristoReyViewModel
import com.example.ui.UiState
import com.example.ui.components.CristoReyLogoBadge
import com.example.ui.components.CristoReyVideoPlayerOverlay
import com.example.ui.components.FullscreenPlayerOverlay
import com.example.ui.components.FullscreenPlayerState
import com.example.ui.components.LiveStatusBadge
import com.example.ui.components.LocalFullscreenPlayerState
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EnVivoScreen
import com.example.ui.screens.InfoScreen
import com.example.ui.screens.InicioScreen
import com.example.ui.screens.SplashOverlay
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CristoReyTheme
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class CristoReyTab(
    val routeKey: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    INICIO(
        routeKey = "home",
        labelRes = R.string.tab_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home"
    ),
    EN_VIVO(
        routeKey = "live",
        labelRes = R.string.tab_live,
        selectedIcon = Icons.Filled.LiveTv,
        unselectedIcon = Icons.Outlined.LiveTv,
        testTag = "nav_tab_live"
    ),
    VIDEOS(
        routeKey = "videos",
        labelRes = R.string.tab_videos,
        selectedIcon = Icons.Filled.VideoLibrary,
        unselectedIcon = Icons.Outlined.VideoLibrary,
        testTag = "nav_tab_videos"
    ),
    INFO(
        routeKey = "info",
        labelRes = R.string.tab_info,
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info,
        testTag = "nav_tab_info"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CristoReyMessagingService.createNotificationChannel(this)
        val initialTabKey = intent?.getStringExtra("target_tab")

        setContent {
            CristoReyTheme {
                CristoReyRoot(initialTabKey = initialTabKey)
            }
        }
    }
}

@Composable
private fun CristoReyRoot(initialTabKey: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showSplash by rememberSaveable { mutableStateOf(true) }
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val user = currentUser
        if (user == null) {
            AuthScreen(
                onAuthSuccess = {}
            )
        } else {
            key(user.uid) {
                CristoReyAuthenticatedApp(
                    user = user,
                    initialTabKey = initialTabKey,
                    onSignOut = {
                        signOut(
                            credentialManager = credentialManager,
                            scope = scope
                        )
                    }
                )
            }
        }

        SplashOverlay(
            visible = showSplash,
            onFinished = { showSplash = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CristoReyAuthenticatedApp(
    user: FirebaseUser,
    initialTabKey: String?,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId = user.uid

    val viewModel: CristoReyViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val auth = Firebase.auth
                CristoReyViewModel(
                    currentUserId = currentUserId,
                    youTubeRepository = YouTubeRepository(),
                    notificationRepository = NotificationPreferencesRepository(db = db, auth = auth)
                )
            }
        }
    )

    // Solicitar permiso de notificaciones al primer inicio en Android 13+
    var askedNotificationPermission by rememberSaveable(currentUserId) { mutableStateOf(false) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.syncFcmTokenOnStart()
        }
    }

    LaunchedEffect(currentUserId) {
        if (!askedNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askedNotificationPermission = true
            val alreadyGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!alreadyGranted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val initialTab = remember(initialTabKey) {
        CristoReyTab.entries.firstOrNull { it.routeKey == initialTabKey } ?: CristoReyTab.INICIO
    }
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var selectedVideoForPlayer by remember { mutableStateOf<YouTubeVideo?>(null) }

    val videosState by viewModel.videosState.collectAsStateWithLifecycle()
    val liveStatus by viewModel.liveStreamStatus.collectAsStateWithLifecycle()
    val isCheckingLive by viewModel.isCheckingLive.collectAsStateWithLifecycle()
    val notificationPrefsState by viewModel.notificationPrefsState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val fullscreenState = remember { FullscreenPlayerState() }

    BackHandler(
        enabled = !fullscreenState.isFullscreen &&
            (selectedVideoForPlayer != null || selectedTab != CristoReyTab.INICIO)
    ) {
        if (selectedVideoForPlayer != null) {
            selectedVideoForPlayer = null
        } else if (selectedTab != CristoReyTab.INICIO) {
            selectedTab = CristoReyTab.INICIO
        }
    }

    CompositionLocalProvider(LocalFullscreenPlayerState provides fullscreenState) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    containerColor = RoyalBlue,
                    contentColor = PureWhite
                ) {
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    CristoReyLogoBadge(size = 44.dp)
                    Spacer(modifier = Modifier.padding(top = 16.dp))
                    CristoReyTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(id = tab.labelRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(id = tab.labelRes),
                                    maxLines = 1
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = DeepNavy,
                                selectedTextColor = BrightGold,
                                indicatorColor = BrightGold,
                                unselectedIconColor = PureWhite.copy(alpha = 0.78f),
                                unselectedTextColor = PureWhite.copy(alpha = 0.78f)
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            Box(modifier = Modifier.padding(start = 12.dp)) {
                                CristoReyLogoBadge(size = 36.dp)
                            }
                        },
                        actions = {
                            Box(modifier = Modifier.padding(end = 12.dp)) {
                                LiveStatusBadge(liveActive = liveStatus.liveActive)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = RoyalBlue,
                            contentColor = PureWhite
                        ) {
                            CristoReyTab.entries.forEach { tab ->
                                val isSelected = selectedTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(id = tab.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(id = tab.labelRes),
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DeepNavy,
                                        selectedTextColor = BrightGold,
                                        indicatorColor = BrightGold,
                                        unselectedIconColor = PureWhite.copy(alpha = 0.78f),
                                        unselectedTextColor = PureWhite.copy(alpha = 0.78f)
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        CristoReyTab.INICIO -> InicioScreen(
                            liveStatus = liveStatus,
                            videosState = videosState,
                            onNavigateToLive = { selectedTab = CristoReyTab.EN_VIVO },
                            onNavigateToVideos = { selectedTab = CristoReyTab.VIDEOS },
                            onPlayVideo = { video ->
                                selectedVideoForPlayer = video
                            }
                        )
                        CristoReyTab.EN_VIVO -> EnVivoScreen(
                            liveStatus = liveStatus,
                            isCheckingLive = isCheckingLive,
                            onRefreshLive = viewModel::refreshLiveStatus,
                            onOpenYouTubeLive = { url -> openExternalUrl(context, url) }
                        )
                        CristoReyTab.VIDEOS -> VideosScreen(
                            videosState = videosState,
                            onSelectVideo = { selectedVideoForPlayer = it },
                            onRefreshVideos = viewModel::refreshVideos
                        )
                        CristoReyTab.INFO -> InfoScreen(
                            userDisplayName = user.displayName,
                            userEmail = user.email,
                            notificationPrefsState = notificationPrefsState,
                            onUpdateNotificationPrefs = viewModel::updateNotificationToggles,
                            onPermissionGrantedSync = viewModel::syncFcmTokenOnStart,
                            onOpenUrl = { url -> openExternalUrl(context, url) },
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }

            val activeVideo = selectedVideoForPlayer
            if (activeVideo != null) {
                val allVideos = (videosState as? UiState.Success)?.data.orEmpty()
                val upNext = remember(allVideos, activeVideo.videoId) {
                    allVideos.filter { it.videoId != activeVideo.videoId }
                }
                CristoReyVideoPlayerOverlay(
                    video = activeVideo,
                    upNextVideos = upNext,
                    onSelectVideo = { selectedVideoForPlayer = it },
                    onOpenExternal = { url -> openExternalUrl(context, url) },
                    onClose = { selectedVideoForPlayer = null }
                )
            }

            FullscreenPlayerOverlay(fullscreenState = fullscreenState)
        }
    }
}

private fun openExternalUrl(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        // Ningún navegador o app disponible para abrir el enlace
    }
}
